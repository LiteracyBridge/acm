package org.literacybridge.talkingbookapp.view_models

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.literacybridge.core.fs.ZipUnzip
import org.literacybridge.talkingbookapp.App
import org.literacybridge.talkingbookapp.BuildConfig
import org.literacybridge.talkingbookapp.database.ProgramContentDao
import org.literacybridge.talkingbookapp.database.ProgramContentEntity
import org.literacybridge.talkingbookapp.models.Deployment
import org.literacybridge.talkingbookapp.models.Program
import org.literacybridge.talkingbookapp.util.Constants.Companion.LOG_TAG
import org.literacybridge.talkingbookapp.util.PathsProvider
import java.io.File
import java.time.LocalDateTime
import javax.inject.Inject


@HiltViewModel
class ContentDownloaderViewModel @Inject constructor() : ViewModel() {
    enum class SyncState {
        COMPARING,
        DOWNLOADING,
        NO_CONTENT,
        OUT_DATED,
        ERROR,
        SUCCESS,
        UNZIPPING
    }

    val downloadProgress = mutableFloatStateOf(0.0F)

    //    val syncState = mutableStateOf(SyncState.COMPARING)
    private val _syncState = MutableStateFlow(SyncState.COMPARING)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

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

        // To speed up development, skip revision check in development
        if (BuildConfig.DEBUG) {
            val result = getLocalContent(null)
            if (result != null) {
                return
            }
        }

        Amplify.Storage.list("$basePath/${deployment.deploymentname}",
            options,
            { result ->
                if (result.items.isEmpty()) {
                    Log.d(LOG_TAG, "Program content empty")
                    // TODO: show error
                    _syncState.value = SyncState.NO_CONTENT
                    return@list
                }

                viewModelScope.launch {
                    val latest = result.items.sortedByDescending { it.key }.first()
                    latestDeploymentRevision = getLatestRevisionName(latest.key)

                    val content = getLocalContent(latestDeploymentRevision)
                    if (_syncState.value == SyncState.SUCCESS && content != null) {
                        return@launch
                    }

                    // No local record is available, download
                    val downloadPath =
                        "$basePath/$latestDeploymentRevision/content-$latestDeploymentRevision.zip"

                    downloadContent(
                        s3key = downloadPath,
                        navController = navController
                    )
                }
            },
            { error ->
                // TODO: show a dialog error, with option to retry content download / use local content

                viewModelScope.launch {
                    // If a local content exists, use it like that
                    val result = getLocalContent(null)
                    if (result != null) {
                        _syncState.value = SyncState.SUCCESS
                    } else {
                        Log.e(LOG_TAG, "List failure", error)
                        _syncState.value = SyncState.ERROR
                    }
                }

            }
        )
    }

    private suspend fun downloadContent(s3key: String, navController: NavController) {
        _syncState.value = SyncState.DOWNLOADING
        displayText.value =
            "${this.latestDeploymentRevision} found, downloading content package..."

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
                displayText.value = "Downloaded successfully! unzipping content"
                _syncState.value = SyncState.UNZIPPING

                // Update database
                viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        unzipFile(downloadedZip = done.file, destinationDir = dest)
                        val entity = ProgramContentEntity(
                            programId = program.program_id,
                            deploymentName = deployment.deploymentname,
                            latestRevision = latestDeploymentRevision!!,
                            localPath = dest.path,
                            status = ProgramContentDao.ProgramEntityStatus.SYNCNED,
                            lastSync = LocalDateTime.now(),
                            s3Path = s3key
                        )

                        App().db.programContentDao().insert(entity)
                        App.getInstance().setProgramSpec(entity)
                    }
                }.invokeOnCompletion {
                    _syncState.value = SyncState.SUCCESS
                }
            },
            { _syncState.value = SyncState.ERROR }
        )
    }

    private fun unzipFile(downloadedZip: File, destinationDir: File): File {
        val temp = File("${destinationDir.absolutePath}/tmp")
        if (!temp.exists()) {
            temp.mkdirs()
        }

        ZipUnzip.unzip(
            downloadedZip, temp
        ) { _, _ -> true }

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

    private suspend fun getLocalContent(latestDeploymentRevision: String?): ProgramContentEntity? {
        return withContext(Dispatchers.IO) {
            val result = App().db.programContentDao().findLatestRevision(deployment.deploymentname)

            // No content stored, needs to download
            if (result == null) {
                _syncState.value = SyncState.DOWNLOADING
                return@withContext null
            }

            // If record exists in db but actual has been deleted from disk, assume there's no content
            // and download from server
            if (!File(result.localPath).exists()) {
                _syncState.value = SyncState.DOWNLOADING
                return@withContext null
            }

            // If no latest revision is provided, assume local content is the latest
            // This might be due to network error whilst fetching from s3 or no internet available
            if (latestDeploymentRevision == null) {
                // Content is up to date, no need to download
                App.getInstance().setProgramSpec(result)
                _syncState.value = SyncState.SUCCESS
                return@withContext result
            }

            return@withContext if (result.latestRevision == latestDeploymentRevision) {
                // Content up to date, can proceed with the stored deployment revision
                App.getInstance().setProgramSpec(result)
                _syncState.value = SyncState.SUCCESS
                result
            } else { // Outdated content, set program spec and try to download latest version
                App.getInstance().setProgramSpec(result)
                _syncState.value = SyncState.OUT_DATED
                result
            }
        }
    }
}