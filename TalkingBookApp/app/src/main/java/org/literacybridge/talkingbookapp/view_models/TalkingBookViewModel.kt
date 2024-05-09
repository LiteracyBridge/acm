package org.literacybridge.talkingbookapp.view_models

import android.hardware.usb.UsbDevice
import android.util.Log
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amplifyframework.core.Amplify
import com.amplifyframework.storage.options.StorageUploadFileOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
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
import org.literacybridge.core.tbloader.ProgressListener
import org.literacybridge.core.tbloader.TBLoaderConfig
import org.literacybridge.core.tbloader.TBLoaderConstants
import org.literacybridge.core.tbloader.TBLoaderCore
import org.literacybridge.core.tbloader.TBLoaderUtils
import org.literacybridge.talkingbookapp.App
import org.literacybridge.talkingbookapp.models.Deployment
import org.literacybridge.talkingbookapp.models.UserModel
import org.literacybridge.talkingbookapp.util.Constants
import org.literacybridge.talkingbookapp.util.Constants.Companion.COLLECTED_DATA_DIR_NAME
import org.literacybridge.talkingbookapp.util.Constants.Companion.LOG_TAG
import org.literacybridge.talkingbookapp.util.PathsProvider
import org.literacybridge.talkingbookapp.util.PathsProvider.getLocalDeploymentDirectory
import org.literacybridge.talkingbookapp.util.Util
import org.literacybridge.talkingbookapp.util.dataStoreManager
import org.literacybridge.talkingbookapp.util.device_manager.Usb
import java.io.File
import java.io.IOException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties
import javax.inject.Inject


data class DeviceState(
    var device: UsbDevice? = null,
)

@HiltViewModel
class TalkingBookViewModel @Inject constructor() : ViewModel() {
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

    val totalFilesPendingUpload = mutableIntStateOf(0)
    val totalFilesUploaded = mutableIntStateOf(0)
    val isOperationInProgress = mutableStateOf(false)
    val operationResult = mutableStateOf(OperationResult.InProgress)

    val operationType = mutableStateOf(TalkingBookOperation.COLLECT_STATS_ONLY)
    val operationStep = mutableStateOf("")
    val operationStepDetail = mutableStateOf("")

    // These fields track the recipient of the talking book device. They are populated
    // only when the user is performing Tb device update, ie. on the recipient screen
    val recipients = mutableStateListOf<Recipient>()
    val districts = mutableStateListOf<String>()
    val selectedDistrict = mutableStateOf<String?>(null)
    val selectedCommunity = mutableStateOf<String?>(null)
    val selectedGroup = mutableStateOf<String?>(null)
    val selectedRecipient = mutableStateOf<Recipient?>(null)

    // Device info
    val talkingBookDeviceInfo = mutableStateOf<TbDeviceInfo?>(null)
    private val talkingBookDevice = mutableStateOf<Usb.TalkingBook?>(null)
    val isMassStorageReady = mutableStateOf(false)

    private val _deviceState = MutableStateFlow(DeviceState())
    val deviceState: StateFlow<DeviceState> = _deviceState.asStateFlow()

    val app = App.getInstance()
    private var deployment: Deployment? = null

//    fun getDevice(): UsbDevice? {
//        return deviceState.value.device
//    }
//
//    fun isDeviceConnected(): Boolean {
//        return isMassStorageReady.value
////        return deviceState.value.device?.deviceName != null
//    }

//    private fun setTalkingBookDevice(tb: Usb.TalkingBook?) {
//        if (tb == null) {
//            TODO("Display error to user that TB is not connected")
//        }
//    }

    fun setDevice(device: UsbDevice?, talkingBook: Usb.TalkingBook?) {
        Log.d(LOG_TAG, "Device has been set $device");

        _deviceState.updateAndGet { state ->
            state.copy(
                device = device
            )
        }

        // Start device discovery
        viewModelScope.launch {
            discoverMassStorageDevice(talkingBook)
        }
    }

    fun disconnected() {
        isMassStorageReady.value = false

        _deviceState.update { state ->
            state.copy(device = null)
        }

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

    /**
     * Updates the value of selectedRecipient state based on the value of
     * the selected district/community/group
     * NB: This function called from the recipients screen
     */
    fun updateSelectedRecipient() {
        selectedRecipient.value = if (!selectedGroup.value.isNullOrBlank()) {
            recipients.find {
                it.groupname.equals(
                    selectedGroup.value,
                    true
                ) && it.communityname.equals(
                    selectedCommunity.value,
                    true
                ) && it.district.equals(
                    selectedDistrict.value,
                    true
                )
            }
        } else if (!selectedCommunity.value.isNullOrBlank()) {
            recipients.find {
                it.communityname.equals(
                    selectedCommunity.value,
                    true
                ) && it.district.equals(selectedDistrict.value, true)
            }
        } else if (!selectedDistrict.value.isNullOrBlank()) {
            recipients.find {
                it.district.equals(
                    selectedDistrict.value,
                    true
                )
            }
        } else {
            null
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
            user = user,
            deployment = deployment,
            deviceSerialNumber = talkingBookDeviceInfo.value!!.serialNumber,
            tbDeviceInfo = talkingBookDeviceInfo.value!!
        )
    }

    suspend fun updateDevice(
        user: UserModel,
        deployment: Deployment,
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
        )
    }

    private suspend fun performOperation(
        tbDeviceInfo: TbDeviceInfo,
        deviceSerialNumber: String,
        user: UserModel,
        deployment: Deployment,
    ) {
        this.deployment = deployment
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

                val oldDeploymentInfo = tbDeviceInfo.createDeploymentInfo(deployment.program_id)
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
                operationResult.value = OperationResult.Success
                isOperationInProgress.value = false
            } catch (e: IOException) {
                e.printStackTrace()
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
    ): DeploymentInfo {
        val recipient = selectedRecipient.value!!

        // Get image for recipient; if not found, fall back to directory.
        var imageName: String = getPackageForRecipient(recipient)
        if (imageName.isEmpty()) {
            // Find the image with the community's language and/or group (such as a/b test group).
            imageName =
                TBLoaderUtils.getPackageForCommunity(deploymentDirectory, recipient.communityname)
        }

        // What firmware comes with this Deployment?
        val firmwareRevision = TBLoaderUtils.getFirmwareVersionNumbers(deploymentDirectory)
        val builder = DeploymentInfoBuilder()
            .withSerialNumber(deviceSerialNumber)
            .withNewSerialNumber(tbDeviceInfo.newSerialNumberNeeded())
            .withProjectName(deployment!!.program_id)
            .withDeploymentName(deploymentDirectory.name)
            .withPackageName(imageName)
            .withUpdateDirectory(collectedDataDirectory.name)
            .withUpdateTimestamp(todaysDate) // TODO: this should be the "Deployment date", the first date the new content is deployed.
            .withFirmwareRevision(firmwareRevision)
            .withCommunity(recipient.communityname)
            .withRecipientid(recipient.recipientid)
//            .asTestDeployment(mTestingDeployment) // TODO: set these values

        val newDeploymentInfo = builder.build()
        // TODO: set these values
        opLog.put<Any>("project", this.deployment!!.program_id)
            .put("deployment", deploymentDirectory.name)
            .put("package", imageName)
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

    private fun uploadCollectedData(file: File, s3Key: String) {
        val options = StorageUploadFileOptions.defaultInstance()

        val transfer = Amplify.Storage.uploadFile("$COLLECTED_DATA_DIR_NAME/$s3Key", file,
            options,
            { progress ->
//                result.
                Log.i("MyAmplifyApp", "Successfully uploaded: ${progress.fractionCompleted}")
            },
            { result ->
                totalFilesPendingUpload.intValue = totalFilesPendingUpload.intValue - 1
                totalFilesUploaded.intValue = totalFilesUploaded.intValue + 1

//                result.
                Log.i("MyAmplifyApp", "Successfully uploaded: ${result.key}")
            },

            { Log.e("MyAmplifyApp", "Upload failed", it) }

        )
    }

    /**
     * Listens to progress from the tbloader, updates the progress display.
     */
    internal abstract class MyProgressListener : ProgressListener() {
        abstract fun clear()
        abstract fun refresh()
        abstract fun extraStep(step: String?)

        abstract override fun error(value: String)
    }

    private val mProgressListener: MyProgressListener = object : MyProgressListener() {
        private var mLog: String? = null

        override fun clear() {
            operationStepDetail.value = ""
            operationStep.value = ""
            mLog = ""
            refresh()
        }

        override fun refresh() {
            operationStepDetail.value = ""
            operationStep.value = ""
        }

        /**
         * Not part of the progress interface; this activity has a few steps of its own.
         * @param step in string form.
         */
        override fun extraStep(step: String?) {
            operationStep.value = step ?: ""
            operationStepDetail.value = ""
        }

        override fun step(step: Steps) {
            operationStep.value = step.description ?: ""
            operationStepDetail.value = ""

            Log.d(LOG_TAG, "$step")

        }

        override fun detail(detail: String?) {
            operationStepDetail.value = detail ?: ""
            Log.d(LOG_TAG, "$detail")

        }

        override fun log(line: String) {
            mLog = "$line\n$mLog\n".trimIndent()
            operationStepDetail.value = mLog!!

            Log.d(LOG_TAG, "$mLog")
        }

        override fun log(append: Boolean, line: String) {
            mLog = if (!append) {
                "$line\n$mLog\n".trimIndent()
            } else {
                // Find first (or only) line break
                val nl = mLog!!.indexOf("\n")
                if (nl > 0) {
                    // {old stuff from before line break} {new stuff} {line break} {old stuff from after line break}
                    val pref = mLog!!.substring(0, nl)
                    val suff = mLog!!.substring(nl + 1)
                    "$pref\n$line\n$suff\n".trimIndent()
                } else {
                    // No line breaks, so simply append to anything already there.
                    mLog + line
                }
            }
            Log.d(LOG_TAG, "$mLog")
            operationStepDetail.value = mLog ?: ""
        }

        override fun error(value: String) {
            throw Exception(value)
        }
    }

}
