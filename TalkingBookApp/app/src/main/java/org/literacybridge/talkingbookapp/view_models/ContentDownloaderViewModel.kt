package org.literacybridge.talkingbookapp.view_models

import Screen
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import aws.sdk.kotlin.services.s3.model.Object
import com.amplifyframework.core.Amplify
import com.amplifyframework.storage.options.StorageDownloadFileOptions
import com.amplifyframework.storage.options.StoragePagedListOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import org.literacybridge.talkingbookapp.App
import org.literacybridge.talkingbookapp.models.Deployment
import org.literacybridge.talkingbookapp.models.Program
import org.literacybridge.talkingbookapp.util.LOG_TAG
import org.literacybridge.talkingbookapp.util.PathsProvider
import java.io.File
import javax.inject.Inject


@HiltViewModel
class ContentDownloaderViewModel @Inject constructor() : ViewModel() {
    val downloadProgress = mutableStateOf(0.0F)
    val syncState = mutableStateOf("comparing")

    fun syncProgramContent(
        program: Program,
        deployment: Deployment,
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
                Log.d(LOG_TAG, "Program content ${result.items.size}")

                if (result.items.isEmpty()) {
                    Log.d(LOG_TAG, "Program content empty")
                    // TODO: show error
                    return@list
                }

                val latest = result.items.sortedByDescending { it.key }.first()
                val revisionName = getLatestRevisionName(latest.key)

                val lastDownload =
                    App().db.programContentDao().findLatestRevision(deployment.deploymentname)
                if (lastDownload != null && lastDownload.latestRevision == revisionName) {
                    // Content is up to date, no need to download
                    navController.navigate(Screen.HOME.name);
                }

                // No local record is available, download
                val downloadPath = "$basePath/$revisionName/content-$revisionName.zip"

                Log.d(LOG_TAG, "Program content $downloadPath")
            },
            { Log.e("MyAmplifyApp", "List failure", it) }
        )

//            }
//        }
    }

    private fun getLastRevisionPath(files: List<Object>): String {
        val latest = files.sortedByDescending { it.key }.first()
        val basePath = latest.key!!.split("/programspec").first()

        return "$basePath/content-${basePath.split("/").last()}.zip"
    }

    private fun downloadContent(s3key: String, deploymentName: String, projectId: String) {
        syncState.value = "downloading"

        val dest = File("${PathsProvider.getProjectDirectory(projectId).path}/$deploymentName")
        if (!dest.exists()) {
            dest.mkdirs()
        }

        val file = File("${dest.path}/content.zip")
        val options = StorageDownloadFileOptions.defaultInstance()
        Amplify.Storage.downloadFile(s3key, file, options,
            { progress ->
                downloadProgress.value = progress.fractionCompleted.toFloat()
            },
            { done ->
                Log.i("MyAmplifyApp", "Successfully downloaded: ${done.file.name}")
            },
            { Log.e("MyAmplifyApp", "Download Failure", it) }
        )
    }

    /**
     * TODO: add docs
     */
    private fun getLatestRevisionName(key: String): String? {
        val result = "published\\/(.*)\\/programspec".toRegex().find(key)

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