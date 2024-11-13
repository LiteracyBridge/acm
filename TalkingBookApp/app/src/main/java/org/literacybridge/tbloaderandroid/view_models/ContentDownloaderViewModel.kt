package org.literacybridge.tbloaderandroid.view_models

import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amplifyframework.core.Amplify
import com.amplifyframework.storage.StorageItem
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
import org.literacybridge.tbloaderandroid.App
import org.literacybridge.tbloaderandroid.BuildConfig
import org.literacybridge.tbloaderandroid.database.ProgramContentDao
import org.literacybridge.tbloaderandroid.database.ProgramContentEntity
import org.literacybridge.tbloaderandroid.models.Deployment
import org.literacybridge.tbloaderandroid.models.Program
import org.literacybridge.tbloaderandroid.util.Constants.Companion.LOG_TAG
import org.literacybridge.tbloaderandroid.util.PathsProvider
import java.io.File
import java.time.LocalDateTime
import java.util.regex.Matcher
import java.util.regex.Pattern
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

    // Matches deploymentName-suffix.current or .rev. Like TEST-19-2-ab.rev
    // group(0) is entire string, like 'TEST-19-2-ab.rev'
    // group(1) is deployment + revision, like 'TEST-19-2-ab'
    // group(2) is deployment, like 'TEST-19-2'
    // group(3) is revision, like 'ab'
    val markerPattern: Pattern =
        Pattern.compile("((\\w+(?:-\\w+)*)-(\\w+))\\.(current|rev)", Pattern.CASE_INSENSITIVE)

    val downloadProgress = mutableFloatStateOf(0.0F)

    //    val syncState = mutableStateOf(SyncState.COMPARING)
    private val _syncState = MutableStateFlow(SyncState.COMPARING)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    val displayText = mutableStateOf("Searching for new deployments, please wait...")

    lateinit var program: Program
    lateinit var deployment: Deployment
    private var latestDeploymentRevision: String? = null

    suspend fun syncProgramContent() {

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

        Amplify.Storage.list("$basePath/${deployment.project_id}",
            options,
            { result ->
                if (result.items.isEmpty()) {
                    Log.d(LOG_TAG, "Program content empty")
                    // TODO: show error
                    _syncState.value = SyncState.NO_CONTENT
                    return@list
                }

                viewModelScope.launch {
                    val latest = findLatestDeployment(result.items)
                    if (latest == null) {
                        TODO("Show error that no latest deployment was found")
                    }

                    latestDeploymentRevision =
                        latest.key.split('/').last().replace("\\.(current|rev)".toRegex(), "")

                    val content = getLocalContent(latestDeploymentRevision)
                    if (_syncState.value == SyncState.SUCCESS && content != null) {
                        return@launch
                    }

                    // No local record is available, download
                    val downloadPath =
                        "$basePath/$latestDeploymentRevision/content-$latestDeploymentRevision.zip"

                    downloadContent(
                        s3key = downloadPath
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

    /**
     * Given a list of s3ObjectSummaries and a map of program id to revision, find the latest
     * deployments from that list, and add or update the list of revisions.
     * @param s3ObjectSummaries The list of S3 objects.
     */
    private fun findLatestDeployment(
        s3ObjectSummaries: List<StorageItem>,
    ): StorageItem? {

        val revKeyLength = 4
        val zipKeyLength = 5
        val programidIx = 0

        // First, look for all of the ".current" (or ".rev") files. This will let us know what versions
        // are current in s3.
        for (summary in s3ObjectSummaries) {
            val parts: List<String> = summary.key.split("/")
            if (parts.size == revKeyLength) {
                val matcher: Matcher = markerPattern.matcher(parts[revKeyLength - 1])
                if (matcher.matches()) {
                    // The key is like projects/${programid}/TEST-19-2-ab.rev
                    // or ${programid}/TB-Loaders/published/${deplid}.rev
                    val programId = parts[programidIx]
                    val deployment = matcher.group(2)
                    val revision = matcher.group(3)
                    Log.i(
                        LOG_TAG,
                        String.format(
                            "Found deployment revision %s %s %s",
                            programId,
                            deployment,
                            revision
                        )
                    )
                    return summary
                }
            }
        }
        return null
    }

    private suspend fun downloadContent(s3key: String) {
        _syncState.value = SyncState.DOWNLOADING
        displayText.value =
            "${this.latestDeploymentRevision} found, downloading content package..."

        val dest =
            File("${PathsProvider.getProjectDirectory(program.program_id).path}/${deployment.deploymentname}")
        if (dest.exists()) { // Old deployment data already exists, delete them first
            dest.deleteRecursively();
        } else {
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
                        val content = unzipFile(downloadedZip = done.file, destinationDir = dest)
                        val entity = ProgramContentEntity(
                            programId = program.program_id,
                            deploymentName = deployment.deploymentname,
                            latestRevision = latestDeploymentRevision!!,
                            localPath = content.absolutePath,
                            status = ProgramContentDao.ProgramEntityStatus.SYNCED,
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
        ZipUnzip.unzip(
            downloadedZip, destinationDir
        ) { _, _ -> true }

        // Final destination is "content/{deploymentname}"
        return File("${destinationDir.absolutePath}/content/${this.deployment.deploymentname}")
    }

    /**
     * TODO: add docs
     */
//    private fun getLatestRevisionName(key: String): String? {
//        val result = "published/(.*)/programspec".toRegex().find(key)
//
//        if (result?.groupValues?.size == 2) {
//            return result.groupValues.last()
//        }
//
//        // No deployment has been created
//        return null
//    }

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
                null
            }
        }
    }
}
