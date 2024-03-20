package org.literacybridge.talkingbookapp.view_models

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import aws.sdk.kotlin.services.s3.model.ListObjectsV2Request
import aws.sdk.kotlin.services.s3.model.Object
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.literacybridge.talkingbookapp.App
import org.literacybridge.talkingbookapp.models.Deployment
import org.literacybridge.talkingbookapp.models.Program
import org.literacybridge.talkingbookapp.util.CONTENT_BUCKET_NAME
import org.literacybridge.talkingbookapp.util.LOG_TAG
import org.literacybridge.talkingbookapp.util.content_manager.S3Helper
import java.io.File
import javax.inject.Inject


@HiltViewModel
class ContentDownloaderViewModel @Inject constructor() : ViewModel() {

    suspend fun startDownload(program: Program, deployment: Deployment) {
        Log.d(LOG_TAG, "Started download")

        val dir = File(App.context.getExternalFilesDir("localrepository"), program.program_id)

//        val contentInfo = ContentInfo(model.program.value!!.program_id)
//
//        var manager = ContentManager(App())
//        manager.startDownload(context, contentInfo, mTransferListener)

        val request = ListObjectsV2Request {
            this.bucket = CONTENT_BUCKET_NAME
            this.prefix =
                "${program.program_id}/TB-Loaders/published/${deployment.deploymentname}"
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val data = S3Helper.listObjects(request)
                if (data.isNullOrEmpty()) {
                    Log.d(LOG_TAG, "Program content empty")
                    // TODO: show error
                    return@withContext
                }

                Log.d(LOG_TAG, "Program content ${data.size}")
                val latest = data.sortedByDescending { it.key }.first()
                l
                Log.d(LOG_TAG, "Program content ${latest.toString()}")

            }
        }
    }

    private fun getLastRevisionPath(files: List<Object>): String {
        val latest = files.sortedByDescending { it.key }.first()
        val basePath = latest.key!!.split("/programspec").first()

        return "$basePath/content-${basePath.split("/").last()}.zip"
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