package org.literacybridge.tbloaderandroid.view_models

import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.sentry.Sentry.captureException
import kotlinx.coroutines.flow.MutableStateFlow
import org.literacybridge.tbloaderandroid.App
import org.literacybridge.tbloaderandroid.BuildConfig
import org.literacybridge.tbloaderandroid.api_services.AppUpdateHttpClient
import org.literacybridge.tbloaderandroid.models.Asset
import org.literacybridge.tbloaderandroid.models.Release
import java.io.File
import javax.inject.Inject

@HiltViewModel
class UpdatesDownloaderViewModel @Inject constructor() : ViewModel() {
    private val FILENAME_APK = "update.apk"
    private val CONTENT_TYPE_APK = "application/vnd.android.package-archive"
    private val RELEASE_URL = "https://api.github.com/repos/LiteracyBridge/acm/releases/latest"

    private val isUpdateChecked = mutableStateOf(false)
    private val newVersionAsset = mutableStateOf<Asset?>(null)
    val showDialog = MutableStateFlow(false)
    val newRelease = MutableStateFlow<Release?>(null)

    fun checkUpdate() {
        if (isUpdateChecked.value) {
            return
        }

        val httpClient = AppUpdateHttpClient()
        if (!httpClient.isConnectedToInternet()) return

        Thread {
            var release: Release? = null
            try {
                release = httpClient.getLastRelease(RELEASE_URL)
            } catch (e: Exception) {
                captureException(e)
                e.printStackTrace()
            } finally {
                isUpdateChecked.value = true
            }

            if (release != null && !release.tag_name.isNullOrEmpty() && release.tag_name!!.startsWith(
                    "v/android"
                )
            ) {
                val versionCode =
                    release.tag_name!!.split("_").last().toInt() // Split to get version code

                if (versionCode > BuildConfig.VERSION_CODE) {
                    showUpdateAvailable(
                        release
                    )
                }
            }
        }.start()
    }

    private fun showUpdateAvailable(release: Release) {
        for (asset in release.assets) {
            if (asset.content_type == CONTENT_TYPE_APK && asset.browser_download_url.isNotEmpty()) {
                newVersionAsset.value = asset
                newRelease.value = release
                showDialog.value = true
                break
            }
        }
    }


    fun downloadUpdate() {
//        TODO: implement progress bar
//        val content: View =
//            activity!!.layoutInflater.inflate(R.layout.content_dialog_download, null)
//        val alertDialog: AlertDialog = Builder(activity)
//            .setTitle(activity.resources.getString(R.string.dialog_downloading_title))
//            .setView(content)
//            .setCancelable(false)
//            .show()
//        val progressBar = content.findViewById<ProgressBar>(R.id.progressBar)
//        val progressListener: ProgressListener = object : ProgressListener() {
//            fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
//                val progress = Math.round(bytesRead.toFloat() / contentLength * 100)
//                activity.runOnUiThread {
//                    if (!activity.isDestroyed) progressBar.progress = progress
//                }
//                if (done && !activity.isDestroyed) alertDialog.dismiss()
//            }
//        }
//
        Thread {
//            val interceptor: Interceptor = object : Interceptor() {
//                @Throws(IOException::class)
//                fun intercept(chain: Chain): Response {
//                    val originalResponse: Response = chain.proceed(chain.request())
//                    return originalResponse.newBuilder()
//                        .body(ProgressResponseBody(originalResponse.body(), progressListener))
//                        .build()
//                }
//            }
            try {
                val httpClient = AppUpdateHttpClient()
                val apkFile: File? =
                    httpClient.download(newVersionAsset.value!!.browser_download_url, FILENAME_APK)
                if (apkFile != null) installAPK(apkFile)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun installAPK(apkFile: File) {
        val uri =
            FileProvider.getUriForFile(
                App.context.applicationContext,
                App.context.packageName + ".provider",
                apkFile
            )
        val i = Intent(Intent.ACTION_VIEW)
        i.setDataAndType(uri, CONTENT_TYPE_APK)
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        App.context.startActivity(i)
    }
}