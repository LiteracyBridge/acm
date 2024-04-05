package org.literacybridge.talkingbookapp.view_models

//import dagger.hilt.android.lifecycle.HiltViewModel

import Screen
import android.hardware.usb.UsbDevice
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
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
import org.literacybridge.talkingbookapp.util.Constants.Companion.LOG_TAG
import org.literacybridge.talkingbookapp.util.PathsProvider
import org.literacybridge.talkingbookapp.util.Util.getStackTrace
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
        UPDATE_FIRMWARE,
        COLLECT_STATS_ONLY,
        COLLECT_STATS_AND_REFRESH_FIRMWARE
    }

    val talkingBookDevice = mutableStateOf<Usb.TalkingBook?>(null)
    private val tbOperation = mutableStateOf(TalkingBookOperation.COLLECT_STATS_ONLY)

    private val _deviceState = MutableStateFlow(DeviceState())
    val deviceState: StateFlow<DeviceState> = _deviceState.asStateFlow()

    private val app = App.getInstance()
    private val dataStore get() = dataStoreManager.data

    // Handle business logic
//    fun rollDice() {
//        _uiState.update { currentState ->
//            currentState.copy(
//                firstDieValue = Random.nextInt(from = 1, until = 7),
//                secondDieValue = Random.nextInt(from = 1, until = 7),
//                numberOfRolls = currentState.numberOfRolls + 1,
//            )
//        }
//    }

    fun getDevice(): UsbDevice? {
        return deviceState.value.device
    }

    fun setDevice(device: UsbDevice?) {
        Log.d(LOG_TAG, "Device has been set $device");

        _deviceState.updateAndGet { state ->
            state.copy(
                device = device
            )
        }
    }

    fun disconnected() {
        _deviceState.update { state ->
            state.copy(device = null)
        }
    }

    fun collectUsageStatistics(
        user: UserModel,
        deployment: Deployment,
        navController: NavController
    ) {
        tbOperation.value = TalkingBookOperation.COLLECT_STATS_ONLY
        val tb = talkingBookDevice.value
        if (tb == null) {
            TODO("Display error to user that TB is not connected")
        }

        val tbDeviceInfo = TbDeviceInfo.getDeviceInfoFor(
            tb.root,
            tb.deviceLabel,
            TBLoaderConstants.NEW_TB_SRN_PREFIX,
            TbDeviceInfo.DEVICE_VERSION.TBv2
        )

//                            val deviceSerialNumber = tbDeviceInfo.serialNumber
        updateTalkingBook(
            user = user,
            deployment = deployment,
//            deviceSerialNumber = "C-0011",
            tbDeviceInfo = tbDeviceInfo
        )
        navController.navigate(Screen.COLLECT_DATA.name)
    }

    private fun updateTalkingBook(
        tbDeviceInfo: TbDeviceInfo,
//        deviceSerialNumber: String,
        user: UserModel,
        deployment: Deployment
    ) {
        val collectStatsOnly = tbOperation.value == TalkingBookOperation.COLLECT_STATS_ONLY
        val opLog = OperationLog.startOperation(
            if (collectStatsOnly) "CollectStatistics" else "UpdateTalkingBook"
        )

//        val config: Config = App.getInstance().config
        val startTime = System.currentTimeMillis()

        // Where to gather the statistics and user recordings, to be uploaded.
//        TbFile collectionTbFile = new FsFile(PathsProvider.getUploadDirectory());
//        TbFile collectedDataDirectory = collectionTbFile.open(COLLECTED_DATA_SUBDIR_NAME);
        val collectionTimestamp = TBLoaderConstants.ISO8601.format(Date())
        val df: DateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.US)
        df.setTimeZone(TBLoaderConstants.UTC)
        val todaysDate: String = df.format(Date())
        val collectedDataDirectory = PathsProvider.getStatsDirectory(collectionTimestamp)
        val collectedDataTbFile: TbFile = FsFile(collectedDataDirectory)

        // Working storage.
        val tempTbFile: TbFile = FsFile(PathsProvider.localTempDirectory)
//            .open("temp")
        tempTbFile.parent.mkdirs()
        val tbLoaderConfig = TBLoaderConfig.Builder()
            .withTbLoaderId(dataStoreManager.tbcdid)
            .withCollectedDataDirectory(collectedDataTbFile)
            .withTempDirectory(tempTbFile)
            .withUserEmail(user.email)
            .withUserName(user.first_name)
            .build()

        val oldDeploymentInfo = tbDeviceInfo.createDeploymentInfo(deployment.program_id)

//        getActivity().runOnUiThread { mTalkingBookWarningsTextView.setText(getString(R.string.do_not_disconnect)) }

        val acceptableFirmwareVersions: String = app.programSpec!!.deploymentProperties
            .getProperty(TBLoaderConstants.ACCEPTABLE_FIRMWARE_VERSIONS)

        val builder = TBLoaderCore.Builder()
            .withTbLoaderConfig(tbLoaderConfig)
            .withTbDeviceInfo(tbDeviceInfo)
            .withOldDeploymentInfo(oldDeploymentInfo)
//            .withLocation(mLocation)
            .withCoordinates(null) // May be null; ok because it's optional anyway.
            .withAcceptableFirmware(acceptableFirmwareVersions)
            .withRefreshFirmware(false)
            .withProgressListener(mProgressListener)
            .withStatsOnly(collectStatsOnly)
            .withPostUpdateDelay(Constants.AndroidPostUpdateSleepTime)

        val result: TBLoaderCore.Result
        result = if (collectStatsOnly) {
            builder.build().collectStatistics()
        } else {
            //TODO: implement this
            Log.d(LOG_TAG, "Multi operation")
            builder.build().update()
            // The directory with images. {project}/content/{Deployment}
//            val deploymentDirectory: File = getLocalDeploymentDirectory(app.programContent!!)!!
//            val newDeploymentInfo: DeploymentInfo = getUpdateDeploymentInfo(
//                opLog, tbDeviceInfo,
//                deviceSerialNumber,
//                collectionTimestamp, todaysDate,
//                collectedDataDirectory,
//                deploymentDirectory
//            )
//            // Add in the update specific data, then go!
//            builder
//                .withDeploymentDirectory(FsFile(deploymentDirectory))
//                .withNewDeploymentInfo(newDeploymentInfo)
//                .build().update()
        }
        opLog.put("gotstatistics", result.gotStatistics)

        // Zip up the files, and give the .zip to the uploader.
        try {
            val zipStart = System.currentTimeMillis()
            mProgressListener.extraStep("Zipping statistics and user feedback")
            val collectedDataZipName =
                "collected-data/tbcd" + dataStoreManager.tbcdid + "/" + collectionTimestamp + ".zip"
            val uploadableZipFile = File(PathsProvider.localTempDirectory, collectedDataZipName)

            // Zip all the files together. We don't really get any compression, but it collects them into
            // a single archive file.
            ZipUnzip.zip(collectedDataDirectory, uploadableZipFile, true)
            collectedDataTbFile.deleteDirectory()

            // TODO: add to amplify upload queue
//            mAppContext.getUploadService().uploadFileAsName(uploadableZipFile, collectedDataZipName)
//            var message = java.lang.String.format(
//                "Zipped statistics and user feedback in %s", Util.formatElapsedTime(
//                    System.currentTimeMillis() - zipStart
//                )
//            )
//
//            mProgressListener.log(message)
//            message = java.lang.String.format(
//                "TB-Loader completed in %s",
//                Util.formatElapsedTime(System.currentTimeMillis() - startTime)
//            )
//            mProgressListener.log(message)
//            mProgressListener.extraStep("Finished")
        } catch (e: IOException) {
            e.printStackTrace()
            mProgressListener.log(getStackTrace(e))
            mProgressListener.log("Exception zipping stats")
            opLog.put("zipException", e)
        }
        opLog.finish()
    }


    private fun getUpdateDeploymentInfo(
        opLog: OperationLog.Operation,
        tbDeviceInfo: TbDeviceInfo,
        deviceSerialNumber: String,
        collectionTimestamp: String, todaysDate: String,
        collectedDataDirectory: File,
        deploymentDirectory: File
    ): DeploymentInfo {
//        val config: Config = TBLoaderAppContext.getInstance().getConfig()
        val mRecipient = app.programSpec!!.recipients.getRecipient(emptyList());

        // TODO: check for null recipient
        val recipientid: String = mRecipient.recipientid
        // Get image for recipient; if not founc, fall back to directory.
        var imageName: String = getPackageForRecipient(mRecipient!!)
        if (imageName.isEmpty()) {
            // Find the image with the community's language and/or group (such as a/b test group).
//            TODO: add community directory
//            imageName = TBLoaderUtils.getImageForCommunity(deploymentDirectory, mCommunityDirectory)
        }

        // What firmware comes with this Deployment?
        val firmwareRevision = TBLoaderUtils.getFirmwareVersionNumbers(deploymentDirectory)
        val builder = DeploymentInfoBuilder()
            .withSerialNumber(deviceSerialNumber)
            .withNewSerialNumber(tbDeviceInfo.newSerialNumberNeeded())
            .withProjectName("Test project") // TODO: set project name
            .withDeploymentName(deploymentDirectory.name)
            .withPackageName(imageName)
            .withUpdateDirectory(collectedDataDirectory.name)
            .withUpdateTimestamp(todaysDate) // TODO: this should be the "Deployment date", the first date the new content is deployed.
            .withFirmwareRevision(firmwareRevision)
//            .withCommunity(mCommunityDirectory)  // TODO: set these values
            .withRecipientid(recipientid)
//            .asTestDeployment(mTestingDeployment) // TODO: set these values
        val newDeploymentInfo = builder.build()
        // TODO: set these values
//        opLog.put<Any>("project", mProject)
//            .put<String>("deployment", deploymentDirectory.name)
//            .put<String>("package", imageName)
//            .put<Any>("recipientid", mRecipient.recipientid)
//            .put<Any>("community", mCommunityDirectory)
//            .put<String>("sn", deviceSerialNumber)
//            .put("tbloaderId", config.getTbcdid())
//            .put("username", config.getName())
//            .put("useremail", config.getEmail())
//            .put<String>("timestamp", collectionTimestamp)
        return newDeploymentInfo
    }

    private fun getPackageForRecipient(recipient: Recipient): String {
        val deploymentProperties: Properties = app.programSpec!!.deploymentProperties
        var key: String = recipient.languagecode
        if (recipient.variant.isNotEmpty()) {
            key = key + ',' + recipient.variant
        }
        var imageName: String = deploymentProperties.getProperty(key)
        if (imageName == null) {
            imageName = deploymentProperties.getProperty(recipient.languagecode)
        }
        val ok = imageName != null
        if (!ok) {
            imageName = ""
        }
        return imageName
    }

    /**
     * Listens to progress from the tbloader, updates the progress display.
     */
    internal abstract class MyProgressListener : ProgressListener() {
        abstract fun clear()
        abstract fun refresh()
        abstract fun extraStep(step: String?)
    }

    private val mProgressListener: MyProgressListener = object : MyProgressListener() {
        private var mStep: String? = null
        private var mDetail: String? = null
        private var mLog: String? = null
//        private var mActivity: Activity? = null


        override fun clear() {
            mLog = ""
            mDetail = mLog
            mStep = mDetail
            refresh()
        }

        override fun refresh() {
            Log.d(LOG_TAG, "Refreshed")
//            mUpdateStepTextView.setText(mStep)
//            mUpdateDetailTextView.setText(mDetail)
//            mUpdateLogTextView.setText(mLog)
        }

        /**
         * Not part of the progress interface; this activity has a few steps of its own.
         * @param step in string form.
         */
        override fun extraStep(step: String?) {
            mStep = step
            mDetail = ""
            Log.d(LOG_TAG, "Extra steps")

//            activity()!!.runOnUiThread {
//                mUpdateStepTextView.setText(mStep)
//                mUpdateDetailTextView.setText(mDetail)
//            }
        }

        override fun step(step: Steps) {
            mStep = step.description()
            mDetail = ""
            Log.d(LOG_TAG, "$mStep")

//            activity()!!.runOnUiThread {
//                mUpdateStepTextView.setText(mStep)
//                mUpdateDetailTextView.setText(mDetail)
//            }
        }

        override fun detail(detail: String?) {
            mDetail = detail
            Log.d(LOG_TAG, "Detail")

//            activity()!!.runOnUiThread { mUpdateDetailTextView.setText(mDetail) }
        }

        override fun log(line: String) {
            mLog = """
             $line
             $mLog
             """.trimIndent()
            Log.d(LOG_TAG, "Logs")

//            activity()!!.runOnUiThread { mUpdateLogTextView.setText(mLog) }
        }

        override fun log(append: Boolean, line: String) {
            mLog = if (!append) {
                """
     $line
     $mLog
     """.trimIndent()
            } else {
                // Find first (or only) line break
                val nl = mLog!!.indexOf("\n")
                if (nl > 0) {
                    // {old stuff from before line break} {new stuff} {line break} {old stuff from after line break}
                    val pref = mLog!!.substring(0, nl)
                    val suff = mLog!!.substring(nl + 1)
                    """
     $pref$line
     $suff
     """.trimIndent()
                } else {
                    // No line breaks, so simply append to anything already there.
                    mLog + line
                }
            }
            Log.d(LOG_TAG, "Extra Log")

//            activity()!!.runOnUiThread { mUpdateLogTextView.setText(mLog) }
        }
    }

}
