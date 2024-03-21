package org.literacybridge.talkingbookapp.view_models

import Screen
import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.amplifyframework.core.Amplify
import com.amplifyframework.storage.options.StorageDownloadFileOptions
import com.amplifyframework.storage.options.StoragePagedListOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.literacybridge.core.fs.ZipUnzip
import org.literacybridge.talkingbookapp.App
import org.literacybridge.talkingbookapp.database.ProgramContentDao
import org.literacybridge.talkingbookapp.database.ProgramContentEntity
import org.literacybridge.talkingbookapp.models.Deployment
import org.literacybridge.talkingbookapp.models.Program
import org.literacybridge.talkingbookapp.util.LOG_TAG
import org.literacybridge.talkingbookapp.util.PathsProvider
import java.io.File
import java.time.LocalDateTime
import javax.inject.Inject


@HiltViewModel
class ContentDownloaderViewModel @Inject constructor() : ViewModel() {
    val downloadProgress = mutableFloatStateOf(0.0F)
    val syncState = mutableStateOf("comparing")
    val displayText = mutableStateOf("Searching for new deployments, please wait...")

    lateinit var program: Program
    lateinit var deployment: Deployment
    var latestDeploymentRevision: String? = null

    suspend fun syncProgramContent(
        navController: NavController
    ) {
        Log.d(LOG_TAG, "Started download")


//        val contentInfo = ContentInfo(model.program.value!!.program_id)
//
//        var manager = ContentManager(App())
//        manager.startDownload(context, contentInfo, mTransferListener)

//        val request = ListObjectsV2Request {
//            this.bucket = CONTENT_BUCKET_NAME
//            this.prefix = "${program.program_id}/TB-Loaders/published/${deployment.deploymentname}"
//        }

//        1. get latest revision
//        1a. compare to latest db record, and download if neccessary
//        2. download and unzip
//        3. update db with revision & deployment stats
//        4. navigate to home screen

//        viewModelScope.launch {
//            withContext(Dispatchers.IO) {
        val basePath = getBasePath(program.program_id)
        val options = StoragePagedListOptions.builder()
            .setPageSize(1000)
            .build()

        Amplify.Storage.list("$basePath/${deployment.deploymentname}",
            options,
            { result ->
                if (result.items.isEmpty()) {
                    Log.d(LOG_TAG, "Program content empty")
                    // TODO: show error
                    return@list
                }

                val latest = result.items.sortedByDescending { it.key }.first()
                latestDeploymentRevision = getLatestRevisionName(latest.key)

                val lastDownload =
                    App().db.programContentDao().findLatestRevision(deployment.deploymentname)
                if (lastDownload != null && lastDownload.latestRevision == latestDeploymentRevision) {
                    // Content is up to date, no need to download
                    navController.navigate(Screen.HOME.name);
                }

                // No local record is available, download
                val downloadPath =
                    "$basePath/$latestDeploymentRevision/content-$latestDeploymentRevision.zip"

                viewModelScope.launch {
                    downloadContent(
                        s3key = downloadPath,
                        navController = navController
                    )
                }
            },
            { Log.e("MyAmplifyApp", "List failure", it) }
        )

//            }
//        }
    }

//    private fun getLastRevisionPath(files: List<Object>): String {
//        val latest = files.sortedByDescending { it.key }.first()
//        val basePath = latest.key!!.split("/programspec").first()
//
//        return "$basePath/content-${basePath.split("/").last()}.zip"
//    }

    private suspend fun downloadContent(s3key: String, navController: NavController) {
        syncState.value = "downloading"
        displayText.value =
            "New deployment found: ${this.latestDeploymentRevision}, downloading content package..."
        val dest =
            File("${PathsProvider.getProjectDirectory(program.program_id).path}/${deployment.deploymentname}")
        if (!dest.exists()) {
            dest.mkdirs()
        }

        val downloadedFile = File("${dest.path}/content.zip")
        val options = StorageDownloadFileOptions.defaultInstance()

        // TODO: implement auto download resume
        Amplify.Storage.downloadFile(s3key, downloadedFile, options,
            { progress ->
                downloadProgress.floatValue = progress.fractionCompleted.toFloat()
            },
            { done ->
                Log.i("MyAmplifyApp", "Successfully downloaded: ${done.file.name}")
                displayText.value = "Downloaded successfully! unzipping content"
                syncState.value = "unzipping"

                val contentDir = unzipFile(downloadedZip = done.file, destinationDir = dest)

                // Update database
                viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        return@withContext App().db.programContentDao().insert(
                            ProgramContentEntity(
                                programId = program.program_id,
                                deploymentName = deployment.deploymentname,
                                latestRevision = latestDeploymentRevision,
                                localPath = contentDir.path,
                                status = ProgramContentDao.ProgramEntityStatus.SYNCNED,
                                lastSync = LocalDateTime.now(),
                                s3Path = s3key
                            )
                        )
                    }
                }.invokeOnCompletion {
                    navController.navigate(Screen.HOME.name);
                }
            },
            { Log.e("MyAmplifyApp", "Download Failure", it) }
        )
    }

//    private fun updateProgramsContentRecord(contentDir: File, s3key: String) {
//
//    }

    private fun unzipFile(downloadedZip: File, destinationDir: File): File {
        val temp = File("${destinationDir.absolutePath}/tmp")
        if (!temp.exists()) {
            temp.mkdirs()
        }

        ZipUnzip.unzip(
            downloadedZip, temp
        ) { _, _ -> true }

        // TODO: write to log files

        // Done unzipping, move unzipped files from {programId}/{deploymentname}/tmp/content/{deploymentname}
        // to {programId}/{deploymentname}/content
        val contentDir = File("${destinationDir.absolutePath}/content")
        if (!contentDir.exists()) {
            contentDir.mkdirs()
        }

        val sourceDir = File("${temp.absolutePath}/content/${deployment.deploymentname}")
        val result = PathsProvider.moveDirectory(
            sourceDir.toPath(),
            contentDir.toPath()
        )
        if (result) {
            // Cleanup resources
            temp.deleteRecursively()
            downloadedZip.delete()
            return contentDir
        }

        // An error occurred during folder restructuring, we use the tmp source directory as the default
        // content dir
        return sourceDir
    }

    /**
     * TODO: add docs
     */
    private fun getLatestRevisionName(key: String): String? {
        val result = "published/(.*)/programspec".toRegex().find(key)

        if (result?.groupValues?.size == 2) {
            return result.groupValues.last()
        }

        // No deployment has been created
        return null
    }

    private fun getBasePath(programId: String): String {
        return "$programId/TB-Loaders/published";
    }


    /**
     * Listener for the progress of downloads.
     */
//    private val mTransferListener: ContentDownloader.DownloadListener =
//        object : ContentDownloader.DownloadListener {
//            var prevProgress: Long = 0
//            private fun update(notify: Boolean) {
//                Log.d(LOG_TAG, "On download update $notify")
////                mManageContentActivity.runOnUiThread(Runnable {
////                    updateView()
////                    if (notify) {
////                        mManageContentActivity.mAdapter.notifyDataSetChanged()
////                    }
////                    // This can only happen in the download listener. We only allow one download at a time,
////                    // so, if this code runs, it is/was the active download. If not longer an active
////                    // download, don't need the cancel button any more.
////                    if (!mContentInfo.isDownloading()) {
////                        mManageContentActivity.disableCancel()
////                    }
////                })
//            }
//
//            override fun onUnzipProgress(id: Int, current: Long, total: Long) {
//                Log.d(LOG_TAG, "onUnzipProgress called")
//                var total = total
////                if (BuildConfig.DEBUG) {
////                    total = Math.max(total, 1)
////                    val progress = 100 * current / total
////                    if (Math.abs(progress - prevProgress) > 10) {
////                        Log.d(
////                            TAG, String.format(
////                                "Unzipping content, progress: %d/%d (%d%%)", current, total,
////                                progress
////                            )
////                        )
////                        prevProgress = progress
////                    }
////                }
//                update(false)
//            }
//
//            override fun onStateChanged(id: Int, state: TransferState) {
//                Log.d(
//                    LOG_TAG,
//                    java.lang.String.format("Downloading content, state: %s", state.toString())
//                )
//                update(true)
//            }
//
//            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
//                var bytesTotal = bytesTotal
////                if (BuildConfig.DEBUG) {
////                    bytesTotal = Math.max(bytesTotal, 1)
////                    val progress = 100 * bytesCurrent / bytesTotal
////                    if (progress != prevProgress) Log.d(
////                        TAG, String.format(
////                            "Downloading content, progress: %d/%d (%d%%)", bytesCurrent,
////                            bytesTotal, progress
////                        )
////                    )
////                    prevProgress = progress
////                }
//                update(false)
//            }
//
//            override fun onError(id: Int, ex: Exception?) {
//                Log.d(LOG_TAG, "Downloading content, error: ", ex)
//                update(true)
//            }
//        }
}