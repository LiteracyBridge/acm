package org.literacybridge.tbloaderandroid.view_models

import android.hardware.usb.UsbDevice
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amplifyframework.core.Amplify
import com.amplifyframework.storage.options.StorageUploadFileOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import io.sentry.Sentry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.literacybridge.core.fs.FsFile
import org.literacybridge.core.fs.OperationLog
import org.literacybridge.core.fs.TbFile
import org.literacybridge.core.fs.ZipUnzip
import org.literacybridge.core.spec.Recipient
import org.literacybridge.core.tbdevice.TbDeviceInfo
import org.literacybridge.core.tbloader.DeploymentInfo
import org.literacybridge.core.tbloader.DeploymentInfo.DeploymentInfoBuilder
import org.literacybridge.core.tbloader.PackagesData
import org.literacybridge.core.tbloader.ProgressListener
import org.literacybridge.core.tbloader.TBLoaderConfig
import org.literacybridge.core.tbloader.TBLoaderConstants
import org.literacybridge.core.tbloader.TBLoaderCore
import org.literacybridge.core.tbloader.TBLoaderUtils
import org.literacybridge.tbloaderandroid.App
import org.literacybridge.tbloaderandroid.database.S3SyncEntity
import org.literacybridge.tbloaderandroid.database.S3SyncEntityDao
import org.literacybridge.tbloaderandroid.models.Deployment
import org.literacybridge.tbloaderandroid.models.UserModel
import org.literacybridge.tbloaderandroid.util.Constants
import org.literacybridge.tbloaderandroid.util.Constants.Companion.COLLECTED_DATA_DIR_NAME
import org.literacybridge.tbloaderandroid.util.Constants.Companion.LOG_TAG
import org.literacybridge.tbloaderandroid.util.PathsProvider
import org.literacybridge.tbloaderandroid.util.PathsProvider.getLocalDeploymentDirectory
import org.literacybridge.tbloaderandroid.util.Util
import org.literacybridge.tbloaderandroid.util.dataStoreManager
import org.literacybridge.tbloaderandroid.util.device_manager.Dfu
import org.literacybridge.tbloaderandroid.util.device_manager.Usb
import java.io.File
import java.io.IOException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Date
import java.util.Locale
import java.util.Properties
import javax.inject.Inject


@HiltViewModel
class TalkingBookViewModel @Inject constructor() : ViewModel(), Dfu.DfuListener {
    enum class TalkingBookOperation {
        UPDATE_DEVICE,
        COLLECT_STATS_ONLY,
        COLLECT_STATS_AND_REFRESH_FIRMWARE
    }

    enum class OperationResult {
        Success,
        Failure,
        InProgress,
    }

    val isOperationInProgress = mutableStateOf(false)
    val operationResult = mutableStateOf(OperationResult.InProgress)

    val operationType = mutableStateOf(TalkingBookOperation.COLLECT_STATS_ONLY)
    val operationStep = mutableStateOf("")
    val operationStepDetail = mutableStateOf("")

    // Device info
    private val talkingBookDevice = mutableStateOf<Usb.TalkingBook?>(null)
    val talkingBookDeviceInfo = MutableStateFlow<TbDeviceInfo?>(null)
    val isMassStorageReady = mutableStateOf(false)
    val isTestingDeployment = MutableStateFlow(false)

    val usbDevice = MutableStateFlow<UsbDevice?>(null)
    val usbState = MutableStateFlow<Usb?>(null)

    // Firmware update
    val firmwareUpdateStatus = mutableStateOf<OperationResult?>(null)

    val app = App.getInstance()
    private var deployment: Deployment? = null

    fun setUsb(usb: Usb) {
        usbState.value = usb
    }

    fun setDevice(device: UsbDevice?, talkingBook: Usb.TalkingBook?) {
        usbDevice.value = device
        viewModelScope.launch {
            discoverMassStorageDevice(talkingBook)
        }
    }

    fun disconnected() {
        isMassStorageReady.value = false
        usbDevice.value = null;
    }

    /**
     * Continuously checks for whether the device is ready in mass storage mode every
     * 500ms. This It takes a while (maybe 1s+ depending on the device's performance) for connected
     * talking book to be ready for mass storage.
     * This does the trick, FileObserver is **very unreliable
     */
    private suspend fun discoverMassStorageDevice(talkingBook: Usb.TalkingBook?) {
        withContext(Dispatchers.IO) {
            val f = File(Usb.MASS_STORAGE_PATH)
            while (!isMassStorageReady.value) {
                if (f.exists()) {
                    if (talkingBook == null) {
                        TODO("Display error to user that TB is not connected")
                    }

                    isMassStorageReady.value = true
                    talkingBookDevice.value = talkingBook

                    talkingBookDeviceInfo.value = TbDeviceInfo.getDeviceInfoFor(
                        talkingBook.root,
                        talkingBook.deviceLabel,
                        TBLoaderConstants.NEW_TB_SRN_PREFIX,
                        TbDeviceInfo.DEVICE_VERSION.TBv2
                    )
                    break
                }
                delay(100) // sleep for half a second
            }
        }
    }

    suspend fun collectUsageStatistics(
        user: UserModel,
        deployment: Deployment,
    ) {
        val tb = talkingBookDevice.value
        if (tb == null) {
            TODO("Display error to user that TB is not connected")
        }

        performOperation(
            tbDeviceInfo = talkingBookDeviceInfo.value!!,
            deviceSerialNumber = talkingBookDeviceInfo.value!!.serialNumber,
            user = user,
            deployment = deployment,
            recipient = null,
            packages = emptyList()
        )
    }

    suspend fun updateDevice(
        user: UserModel,
        deployment: Deployment,
        recipient: Recipient,
        packages: List<ContentPackage>
    ) {
        val tb = talkingBookDevice.value
        if (tb == null) {
            TODO("Display error to user that TB is not connected")
        }

        operationType.value = TalkingBookOperation.UPDATE_DEVICE
        performOperation(
            user = user,
            deployment = deployment,
            deviceSerialNumber = talkingBookDeviceInfo.value!!.serialNumber,
            tbDeviceInfo = talkingBookDeviceInfo.value!!,
            recipient = recipient,
            packages = packages
        )
    }

    private suspend fun performOperation(
        tbDeviceInfo: TbDeviceInfo,
        deviceSerialNumber: String,
        user: UserModel,
        deployment: Deployment,
        recipient: Recipient?,
        packages: List<ContentPackage> = emptyList(),
        retries: Int = 0
    ) {
        this.deployment = deployment

        if (retries > 2) { // max retries is 2
            isOperationInProgress.value = false
            operationResult.value = OperationResult.Failure
            return;
        }

        isOperationInProgress.value = true
        operationResult.value = OperationResult.InProgress

        val collectStatsOnly =
            operationType.value == TalkingBookOperation.COLLECT_STATS_ONLY

        withContext(Dispatchers.IO) {
            val opLog = OperationLog.startOperation(
                if (collectStatsOnly) "CollectStatistics" else "UpdateTalkingBook"
            )

            try {
                val startTime = System.currentTimeMillis()

                // Where to gather the statistics and user recordings, to be uploaded.
//        TbFile collectionTbFile = new FsFile(PathsProvider.getUploadDirectory());
//        TbFile collectedDataDirectory = collectionTbFile.open(COLLECTED_DATA_SUBDIR_NAME);
                val collectionTimestamp = TBLoaderConstants.ISO8601.format(Date())
                val df: DateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.US)
                df.setTimeZone(TBLoaderConstants.UTC)
                val todaysDate: String = df.format(Date())
                val collectedDataDirectory =
                    PathsProvider.getStatsDirectory(collectionTimestamp)
                val collectedDataTbFile: TbFile = FsFile(collectedDataDirectory)

                // Working storage.
                val tempTbFile: TbFile = FsFile(PathsProvider.localTempDirectory)
                tempTbFile.parent.mkdirs()

                val tbLoaderConfig = TBLoaderConfig.Builder()
                    .withTbLoaderId(dataStoreManager.tbcdid)
                    .withCollectedDataDirectory(collectedDataTbFile)
                    .withTempDirectory(tempTbFile)
                    .withUserEmail(user.email)
                    .withUserName(user.first_name)
                    .build()

                val oldDeploymentInfo = tbDeviceInfo.createDeploymentInfo(deployment.project_id)
                val acceptableFirmwareVersions: String = app.programSpec!!.deploymentProperties
                    .getProperty(TBLoaderConstants.ACCEPTABLE_FIRMWARE_VERSIONS)

                val builder = TBLoaderCore.Builder()
                    .withTbLoaderConfig(tbLoaderConfig)
                    .withTbDeviceInfo(tbDeviceInfo)
                    .withOldDeploymentInfo(oldDeploymentInfo)
//            .withLocation(mLocation)
//                    .withRefreshFirmware(!collectStatsOnly)
                    .withCoordinates(null) // May be null; ok because it's optional anyway.
                    .withAcceptableFirmware(acceptableFirmwareVersions)
                    .withRefreshFirmware(false)
                    .withProgressListener(mProgressListener)
                    .withStatsOnly(collectStatsOnly)
                    .withPostUpdateDelay(Constants.AndroidPostUpdateSleepTime)

                val result: TBLoaderCore.Result = if (collectStatsOnly) {
                    builder.build().collectStatistics()
                } else {
                    // The directory with images. {project}/content/{Deployment}
                    val deploymentDirectory: File =
                        getLocalDeploymentDirectory(app.programContent!!)
                    val newDeploymentInfo: DeploymentInfo = getUpdateDeploymentInfo(
                        opLog, tbDeviceInfo,
                        deviceSerialNumber,
                        collectionTimestamp, todaysDate,
                        collectedDataDirectory,
                        deploymentDirectory,
                        recipient!!,
                        packages = packages
                    )

                    // Add in the update specific data, then go!
                    builder
                        .withDeploymentDirectory(FsFile(deploymentDirectory))
                        .withNewDeploymentInfo(newDeploymentInfo)
                        .build().update()
                }
                opLog.put("gotstatistics", result.gotStatistics)

                // Zip up the files, and give the .zip to the uploader.
                val zipStart = System.currentTimeMillis()
                mProgressListener.extraStep("Zipping statistics and user feedback")

                val zippedPath = "tbcd${dataStoreManager.tbcdid}/$collectionTimestamp.zip"
                val uploadableZipFile =
                    File(PathsProvider.localTempDirectory, "collected-data/$zippedPath")

                // Zip all the files together. We don't really get any compression, but it collects them into
                // a single archive file.
                ZipUnzip.zip(collectedDataDirectory, uploadableZipFile, true)
                collectedDataTbFile.deleteDirectory()

                // TODO: add to amplify upload queue
                uploadCollectedData(uploadableZipFile, zippedPath)

//            mAppContext.getUploadService().uploadFileAsName(uploadableZipFile, collectedDataZipName)
                var message = java.lang.String.format(
                    "Zipped statistics and user feedback in %s", Util.formatElapsedTime(
                        System.currentTimeMillis() - zipStart
                    )
                )

                mProgressListener.log(message)
                message = java.lang.String.format(
                    "TB-Loader completed in %s",
                    Util.formatElapsedTime(System.currentTimeMillis() - startTime)
                )
                mProgressListener.log(message)
                mProgressListener.extraStep("Finished")

                // Update ui to reflect state
                if (operationResult.value != OperationResult.Failure && result.gotStatistics) { // operation successful
                    if (!collectStatsOnly) {
                        // For content update, re-verify that content/package_data.txt was created AND not empty!
                        val packageData = talkingBookDevice.value!!.root.open("content")
                            .open(PackagesData.PACKAGES_DATA_TXT)
                        if (packageData.exists() && File(packageData.absolutePath).length() > 0) {
                            operationResult.value = OperationResult.Success
                        } else {
                            // Operation completed but package_data.txt validation failed, retry operation again
                            return@withContext performOperation(
                                tbDeviceInfo,
                                deviceSerialNumber,
                                user,
                                deployment,
                                recipient,
                                packages,
                                retries + 1
                            )
                        }
                    } else {
                        operationResult.value = OperationResult.Success
                    }
                } else {
                    operationResult.value = OperationResult.Failure
                }
                isOperationInProgress.value = false
            } catch (e: IOException) {
                e.printStackTrace()
                Sentry.captureException(e)
                throw e
//                mProgressListener.log(getStackTrace(e))
//                mProgressListener.log("Exception zipping stats")
//                opLog.put("zipException", e)
//                Log.e(LOG_TAG, e.toString())
//
//                operationResult.value = OperationResult.Failure
//                isOperationInProgress.value = false
            } finally {
                opLog.finish()
                isOperationInProgress.value = false
            }
        }
    }

    private fun getUpdateDeploymentInfo(
        opLog: OperationLog.Operation,
        tbDeviceInfo: TbDeviceInfo,
        deviceSerialNumber: String,
        collectionTimestamp: String,
        todaysDate: String,
        collectedDataDirectory: File,
        deploymentDirectory: File,
        recipient: Recipient,
        packages: List<ContentPackage> = emptyList()
    ): DeploymentInfo {
        // What firmware comes with this Deployment?
        val firmwareRevision = TBLoaderUtils.getFirmwareVersionNumbers(deploymentDirectory)

        val packageNames: MutableList<String> = packages.map { it.name }.toMutableList()
        if (packages.isEmpty()) { // No packages was selected by the user
            // Get image for recipient; if not found, fall back to directory.
            var imageName: String = getPackageForRecipient(recipient)
            if (imageName.isEmpty()) {
                // Find the image with the community's language and/or group (such as a/b test group).
                imageName =
                    TBLoaderUtils.getPackageForCommunity(
                        deploymentDirectory,
                        recipient.communityname
                    )
            }
            packageNames.add(imageName)
        }

        val builder = DeploymentInfoBuilder()
            .withPackageNames(packageNames)
            .withSerialNumber(deviceSerialNumber)
            .withNewSerialNumber(tbDeviceInfo.newSerialNumberNeeded())
            .withProjectName(deployment!!.project_id)
            .withDeploymentName(deploymentDirectory.name)
            .withUpdateDirectory(collectedDataDirectory.name)
            .withUpdateTimestamp(todaysDate) // TODO: this should be the "Deployment date", the first date the new content is deployed.
            .withFirmwareRevision(firmwareRevision)
            .withCommunity(recipient.communityname)
            .withRecipientid(recipient.recipientid)
            .asTestDeployment(isTestingDeployment.value)

        val newDeploymentInfo = builder.build()
        opLog.put<Any>("project", this.deployment!!.project_id)
            .put("deployment", deploymentDirectory.name)
            .put("package", packageNames.joinToString(", "))
            .put<Any>("recipientid", recipient.recipientid)
            .put<Any>("community", "${this.app.programContent?.localPath}/communities")
            .put("sn", deviceSerialNumber)
            .put("tbloaderId", dataStoreManager.tbcdid)
            .put(
                "username",
                "${dataStoreManager.currentUser?.first_name} ${dataStoreManager.currentUser?.last_name}"
            )
            .put("useremail", dataStoreManager.currentUser?.email)
            .put("timestamp", collectionTimestamp)

        return newDeploymentInfo
    }

    private fun getPackageForRecipient(recipient: Recipient): String {
        val deploymentProperties: Properties = app.programSpec!!.deploymentProperties
        var key: String = recipient.languagecode
        if (recipient.variant.isNotEmpty()) {
            key = key + ',' + recipient.variant
        }
        var imageName: String? = deploymentProperties.getProperty(key)
        if (imageName == null) {
            imageName = deploymentProperties.getProperty(recipient.languagecode)
        }
        if (imageName == null) {
            imageName = ""
        }
        return imageName
    }

    private suspend fun uploadCollectedData(file: File, s3Key: String) {
        val _s3Key = "$COLLECTED_DATA_DIR_NAME/$s3Key"
        val options = StorageUploadFileOptions.defaultInstance()

        val transfer = Amplify.Storage.uploadFile(_s3Key, file,
            options,
            { },
            { e -> Sentry.captureException(e) }
        )

        val sync = S3SyncEntity(
            programId = deployment!!.project_id,
            awsTransferId = transfer.transferId,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            deletedAt = null,
            s3Key = _s3Key,
            path = file.absolutePath,
            status = S3SyncEntityDao.S3SyncStatus.Uploading,
            size = file.length(),
            fileName = file.name
        )
        App().db.s3SyncDoa().insert(sync)

        // Set event listeners
        transfer.setOnSuccess {
            viewModelScope.launch { // coroutine on Main
                App().db.s3SyncDoa().uploadCompleted(transfer.transferId)
            }
        }

        transfer.setOnProgress {
            viewModelScope.launch { // coroutine on Main
                App().db.s3SyncDoa().updateProgress(transfer.transferId, it.currentBytes)
            }
        }
        transfer.setOnError {
            viewModelScope.launch { // coroutine on Main
                App().db.s3SyncDoa()
                    .updateStatus(transfer.transferId, S3SyncEntityDao.S3SyncStatus.Failed)
                Sentry.captureException(it)
            }
        }
    }

    /**
     * Listens to progress from the tbloader module, updates the progress display.
     */
    internal abstract class MyProgressListener : ProgressListener() {
        val errorRegex =
            "(unable to update)|update failed|stats failed|java.*.Exception:|android.*.Exception".toRegex(
                setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
            )

        abstract fun clear()
        abstract fun refresh()
        abstract fun extraStep(step: String?)

        abstract override fun error(value: String)
    }

    private val mProgressListener: MyProgressListener = object : MyProgressListener() {

        private fun parseLineForError(line: String?) {
            if (line.isNullOrEmpty()) return

            if (this.errorRegex.containsMatchIn(line)) {
                Log.d(LOG_TAG, "ERROR LINE FOUND!")
                operationResult.value = OperationResult.Failure
            }
        }

        override fun clear() {
//            operationStepDetail.value = ""
            operationStep.value = ""
            refresh()
        }

        override fun refresh() {
//            operationStepDetail.value = ""
            operationStep.value = ""
        }

        /**
         * Not part of the progress interface; this activity has a few steps of its own.
         * @param step in string form.
         */
        override fun extraStep(step: String?) {
            operationStep.value = step ?: ""
//            operationStepDetail.value = ""
        }

        override fun step(step: Steps) {
            operationStep.value = step.description ?: ""
//            operationStepDetail.value = ""

            Log.d(LOG_TAG, "$step")
        }

        override fun detail(detail: String?) {
            operationStepDetail.value = "${operationStepDetail.value}\n$detail".trimIndent()
            Log.d(LOG_TAG, "$detail")
            parseLineForError(detail)
        }

        override fun log(line: String) {
            operationStepDetail.value = "${operationStepDetail.value}\n$line".trimIndent()
            Log.d(LOG_TAG, line)
            parseLineForError(line)
        }

        override fun log(append: Boolean, line: String) {
            var mLog = operationStepDetail.value

            mLog = if (!append) {
                "$line\n$mLog\n".trimIndent()
            } else {
                // Find first (or only) line break
                val nl = mLog.indexOf("\n")
                if (nl > 0) {
                    // {old stuff from before line break} {new stuff} {line break} {old stuff from after line break}
                    val pref = mLog.substring(0, nl)
                    val suff = mLog.substring(nl + 1)
                    "$pref\n$line\n$suff\n".trimIndent()
                } else {
                    // No line breaks, so simply append to anything already there.
                    mLog + line
                }
            }
            Log.d(LOG_TAG, mLog)
            operationStepDetail.value = "${operationStepDetail.value}\n$mLog".trimIndent()
        }

        override fun error(value: String) {
            operationResult.value = OperationResult.Failure
            throw Exception(value)
        }
    }

    fun updateFirmware() {
        if (usbState.value == null) {
            Toast.makeText(
                App.context,
                "No device found! Make sure the Talking Book is in firmware mode!",
                Toast.LENGTH_LONG
            ).show()
            return
        }


        val dfu = Dfu(Usb.USB_VENDOR_ID, Usb.USB_PRODUCT_ID)
        dfu.setListener(this)
        dfu.setUsb(usbState.value)

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                firmwareUpdateStatus.value = OperationResult.InProgress
                dfu.program()
            }
        }
    }

    override fun onStatusMsg(msg: String?) {
        Log.d(LOG_TAG, "$msg")
//        TODO("Implement logging update messages")
    }

    override fun onProgramingCompleted() {
        firmwareUpdateStatus.value = OperationResult.Success
    }

}
