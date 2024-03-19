package org.literacybridge.talkingbookapp.view_models

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.ListObjectsV2Request
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.CognitoCredentialsProvider
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession
import com.amplifyframework.core.Amplify
import com.amplifyframework.storage.options.StoragePagedListOptions
import com.amplifyframework.storage.s3.AWSS3StoragePlugin
import dagger.hilt.android.lifecycle.HiltViewModel
import org.literacybridge.talkingbookapp.App
import org.literacybridge.talkingbookapp.util.CONTENT_BUCKET_NAME
import org.literacybridge.talkingbookapp.util.LOG_TAG
import org.literacybridge.talkingbookapp.util.content_manager.ContentDownloader
import org.literacybridge.talkingbookapp.util.content_manager.ContentInfo
import org.literacybridge.talkingbookapp.util.content_manager.ContentManager
import org.literacybridge.talkingbookapp.util.content_manager.S3Helper
import javax.inject.Inject


@HiltViewModel
class ContentDownloaderViewModel @Inject constructor(application: Application) :
    AndroidViewModel(application) {
//    private val userViewModel: UserViewModel by viewModel()
//    private val talkingBookViewModel: TalkingBookViewModel by viewModel()

    //    val userMOdel
    private val context = getApplication<Application>().applicationContext

    @OptIn(InternalAmplifyApi::class)
    suspend fun startDownload(model: UserViewModel) {
        Log.d(LOG_TAG, "Started download")
//        val dir = File(context.getExternalFilesDir("localrepository"), model.program.value!!.program_id)

        val contentInfo = ContentInfo(model.program.value!!.program_id)

        var manager = ContentManager(App())
//        manager.startDownload(context, contentInfo, mTransferListener)

        val plugin = Amplify.Storage.getPlugin("awsS3StoragePlugin") as AWSS3StoragePlugin
//        val credentialsProvider = CognitoCredentialsProvider(
//            Amplify.Auth
//        )
//        val client = plugin.escapeHatch.
//            .listObjectsV2 {
//            this.
////        val request = ListObjectsV2Request {
////            this.continuationTokent
//            this.bucket = CONTENT_BUCKET_NAME
//            this.prefix =
//                "${model.program.value!!.program_id}/TB-Loaders/published/${model.deployment.value!!.deploymentname}"
//        }

//        val credentialsProvider = createCognitoCredentialsProvider(tokens, refreshToken)

//        val identityId: String = credentialsProvider.getIdentityId()
//        val credentialsProvider = CognitoCredentialsProvider(cognitoUserPool)
//        try {
//
//            val session = Amplify.Auth.fetchAuthSession() as AWSCognitoAuthSession
//
//            val id = session.identityIdResult
//            val credentialsProvider = CognitoCredentialsProvider(
//                Amplify.Auth
//            )
//            val s3Client = S3Client {
//                this.region = "us-west-2"
//                this.credentialsProvider = credentialsProvider
////                this.credentialsProvider = CognitoCredentialsProvider(
////                    Amplify.Auth
////                )
////                credentialsProvider = credentialsProvider
//            }
////            dentialsProvider = CognitoCredentialsProvider(
////                context.applicationContext,
////                "us-west-2_3evpQGyi5",
////                "us-west-2"
////            )
////            if (id.type == AuthSessionResult.Type.SUCCESS) {
////
////                Log.i("AuthQuickStart", "IdentityId: ${id.value}")
////
////            } else if (id.type == AuthSessionResult.Type.FAILURE) {
////
////                Log.i("AuthQuickStart", "IdentityId not present: ${id.error}")
////
////            }
//
//        } catch (error: AuthException) {
//
//            Log.e("AuthQuickStart", "Failed to fetch session", error)
//
//        }

        val options = StoragePagedListOptions.builder()
//            .accessLevel(StorageAccessLevel.PRIVATE)
            .setPageSize(1000)
            .build()


        Amplify.Storage.list("", options,

            { result ->

                result.items.forEach { item ->

                    Log.i("MyAmplifyApp", "Item: ${item.key}")

                }

                Log.i("MyAmplifyApp", "Next Token: ${result.nextToken}")

            },

            { Log.e("MyAmplifyApp", "List failure", it) }

        )
//
//       client.builder {
//
//       }
//        client.use { s3 ->
//
//            val response = s3.listObjectsV2(request)
//            response.contents?.forEach { s3Object ->
//                // Access object information like key and size
//                Log.d(LOG_TAG, "Object Key: ${s3Object.key}")
//                Log.d(
//                    LOG_TAG, "Object Size: ${
//                        s3Object.size?.let {
//                            it / 1024
//                        }
//                    } KB"
//                )
//            }
//        }

        val credentialsProvider = CognitoCredentialsProvider(
        )
//        credentialsProvider.
        Log.d(LOG_TAG, "aws IDENTITY ${credentialsProvider.getIdentityId()}")
        val s3Client = S3Client {
                this.region = "us-west-2"
                this.credentialsProvider = credentialsProvider
//                this.credentialsProvider = CognitoCredentialsProvider(
////                    Amplify.Auth
////                )
////                credentialsProvider = credentialsProvider
            }
        val request = ListObjectsV2Request {
//            this.continuationTokent
            this.bucket = CONTENT_BUCKET_NAME
            this.prefix =
                "${model.program.value!!.program_id}/TB-Loaders/published/${model.deployment.value!!.deploymentname}"
        }
        s3Client.use { s3 ->

            val response = s3.listObjectsV2(request)
            response.contents?.forEach { s3Object ->
                // Access object information like key and size
                Log.d(LOG_TAG, "Object Key: ${s3Object.key}")
                Log.d(
                    LOG_TAG, "Object Size: ${
                        s3Object.size?.let {
                            it / 1024
                        }
                    } KB"
                )
            }
        }
    }

//    override fun onUnzipProgress(id: Int, current: Long, total: Long) {
//        TODO("Not yet implemented")
//    }

    /**
     * Listener for the progress of downloads.
     */
    private val mTransferListener: ContentDownloader.DownloadListener =
        object : ContentDownloader.DownloadListener {
            var prevProgress: Long = 0
            private fun update(notify: Boolean) {
                Log.d(LOG_TAG, "On download update $notify")
//                mManageContentActivity.runOnUiThread(Runnable {
//                    updateView()
//                    if (notify) {
//                        mManageContentActivity.mAdapter.notifyDataSetChanged()
//                    }
//                    // This can only happen in the download listener. We only allow one download at a time,
//                    // so, if this code runs, it is/was the active download. If not longer an active
//                    // download, don't need the cancel button any more.
//                    if (!mContentInfo.isDownloading()) {
//                        mManageContentActivity.disableCancel()
//                    }
//                })
            }

            override fun onUnzipProgress(id: Int, current: Long, total: Long) {
                Log.d(LOG_TAG, "onUnzipProgress called")
                var total = total
//                if (BuildConfig.DEBUG) {
//                    total = Math.max(total, 1)
//                    val progress = 100 * current / total
//                    if (Math.abs(progress - prevProgress) > 10) {
//                        Log.d(
//                            TAG, String.format(
//                                "Unzipping content, progress: %d/%d (%d%%)", current, total,
//                                progress
//                            )
//                        )
//                        prevProgress = progress
//                    }
//                }
                update(false)
            }

            override fun onStateChanged(id: Int, state: TransferState) {
                Log.d(
                    LOG_TAG,
                    java.lang.String.format("Downloading content, state: %s", state.toString())
                )
                update(true)
            }

            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                var bytesTotal = bytesTotal
//                if (BuildConfig.DEBUG) {
//                    bytesTotal = Math.max(bytesTotal, 1)
//                    val progress = 100 * bytesCurrent / bytesTotal
//                    if (progress != prevProgress) Log.d(
//                        TAG, String.format(
//                            "Downloading content, progress: %d/%d (%d%%)", bytesCurrent,
//                            bytesTotal, progress
//                        )
//                    )
//                    prevProgress = progress
//                }
                update(false)
            }

            override fun onError(id: Int, ex: Exception?) {
                Log.d(LOG_TAG, "Downloading content, error: ", ex)
                update(true)
            }
        }
}