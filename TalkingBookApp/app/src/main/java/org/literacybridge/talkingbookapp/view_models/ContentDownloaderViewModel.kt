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
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.literacybridge.core.fs.ZipUnzip
import org.literacybridge.talkingbookapp.App
import org.literacybridge.talkingbookapp.database.ProgramContentDao
import org.literacybridge.talkingbookapp.database.ProgramContentEntity
import org.literacybridge.talkingbookapp.database.ProgramSpecEntity
import org.literacybridge.talkingbookapp.models.Deployment
import org.literacybridge.talkingbookapp.models.DirectBeneficiariesAdditionalMap
import org.literacybridge.talkingbookapp.models.DirectBeneficiariesMap
import org.literacybridge.talkingbookapp.models.Message
import org.literacybridge.talkingbookapp.models.Program
import org.literacybridge.talkingbookapp.models.Recipient
import org.literacybridge.talkingbookapp.util.CsvParser
import org.literacybridge.talkingbookapp.util.LOG_TAG
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

        Amplify.Storage.list("$basePath/${deployment.deploymentname}",
            options,
            { result ->
                if (result.items.isEmpty()) {
                    Log.d(LOG_TAG, "Program content empty")
                    // TODO: show error
                    _syncState.value = SyncState.NO_CONTENT
                    return@list
                }

                val latest = result.items.sortedByDescending { it.key }.first()
                latestDeploymentRevision = getLatestRevisionName(latest.key)

                val lastDownload =
                    App().db.programContentDao().findLatestRevision(deployment.deploymentname)
                if (lastDownload != null && lastDownload.latestRevision == latestDeploymentRevision && File(
                        lastDownload.localPath
                    ).exists()
                ) {
                    // Content is up to date, no need to download
                    App.getInstance().setProgramSpec(lastDownload)
                    _syncState.value = SyncState.SUCCESS
                    return@list
                }

                viewModelScope.launch {
                    // No local record is available, download
                    val downloadPath =
                        "$basePath/$latestDeploymentRevision/content-$latestDeploymentRevision.zip"

                    downloadContent(
                        s3key = downloadPath,
                        navController = navController
                    )
                }
            },
            { Log.e("MyAmplifyApp", "List failure", it) }
        )
    }

    private suspend fun downloadContent(s3key: String, navController: NavController) {
        _syncState.value = SyncState.DOWNLOADING
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
                _syncState.value = SyncState.UNZIPPING

                // Update database
                viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        val contentDir = unzipFile(downloadedZip = done.file, destinationDir = dest)
                        val entity = ProgramContentEntity(
                            programId = program.program_id,
                            deploymentName = deployment.deploymentname,
                            latestRevision = latestDeploymentRevision!!,
                            localPath = contentDir.path,
                            status = ProgramContentDao.ProgramEntityStatus.SYNCNED,
                            lastSync = LocalDateTime.now(),
                            s3Path = s3key
                        )

                        App().db.programContentDao().insert(entity)
                        App.getInstance().setProgramSpec(entity)

//                        // TODO: parse program spec, insert into program_spec table
//                        parseAndSaveProgramSpec(contentDir)

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

    private suspend fun parseAndSaveProgramSpec(contentDir: File) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // Parse recipients
                var csvFile = File("${contentDir.path}/programspec/pub_recipients.csv")
                val recipients: MutableList<Recipient> = mutableListOf()
                var lineCount = 0;

                csvFile.forEachLine { line ->
                    if (lineCount == 0) {
                        lineCount++
                        return@forEachLine
                    }

                    val tokens = line.trim().split(",").map { it.trim() }
                    if (tokens[0].lowercase() != "country") {
                        recipients.add(
                            Recipient(
                                country = tokens[0],
                                language = tokens[1],
                                region = tokens[2],
                                district = tokens[3],
                                communityname = tokens[4],
                                groupname = tokens[5],
                                agent = tokens[6],
                                variant = tokens[7],
                                listening_model = tokens[8],
                                group_size = tokens[9].toInt(),
                                numhouseholds = tokens[10].toInt(),
                                numtbs = tokens[11].toInt(),
                                supportentity = tokens[12],
                                agent_gender = tokens[13],
                                deployments = Gson().fromJson<List<Int>>(
                                    tokens[17],
                                    List::class.java
                                ),
                                recipientid = tokens[18],
                                affiliate = tokens[19],
                                partner = tokens[20],
                                component = tokens[21]
                            )
                        )
                    }
                }

                // Parse general
                csvFile = File("${contentDir.path}/programspec/pub_general.csv")
                val general: MutableList<Program> = mutableListOf()
                lineCount = 0;

                csvFile.forEachLine { line ->
                    if (lineCount == 0) {
                        lineCount++
                        return@forEachLine
                    }

                    Log.d(LOG_TAG, "Parsed line ${CsvParser().parse(line)}")
                    val tokens = line.trim().split(",").map { it.trim() }
                    general += Program(
                        program_id = tokens[0],
                        country = tokens[1],
                        region = Gson().fromJson<List<String>>(tokens[2], List::class.java),
                        languages = Gson().fromJson<List<String>>(tokens[3], List::class.java),
                        deployments_count = tokens[4].toInt(),
                        deployments_length = tokens[5],
                        deployments_first = tokens[6],
                        listening_models = Gson().fromJson<List<String>>(
                            tokens[6],
                            List::class.java
                        ),
                        feedback_frequency = tokens[7],
                        sustainable_development_goals = Gson().fromJson<List<String>>(
                            tokens[8],
                            List::class.java
                        ),
                        direct_beneficiaries_map = Gson().fromJson(
                            tokens[9],
                            DirectBeneficiariesMap::class.java
                        ),
                        direct_beneficiaries_additional_map = Gson().fromJson(
                            tokens[10],
                            DirectBeneficiariesAdditionalMap::class.java
                        ),
                        affiliate = tokens[11],
                        partner = tokens[12],
                    )
                }

                // Parse deployments
                csvFile = File("${contentDir.path}/programspec/pub_deployments.csv")
                val deployments: MutableList<Deployment> = mutableListOf()
                lineCount = 0;

                csvFile.forEachLine { line ->
                    if (lineCount == 0) {
                        lineCount++
                        return@forEachLine
                    }

                    val tokens = line.trim().split(",").map { it.trim() }
                    deployments += Deployment(
                        deploymentnumber = tokens[0].toInt(),
                        start_date = tokens[1],
                        end_date = tokens[2],
                        deploymentname = tokens[3],
                        deployment = tokens[4],
                    )
                }

                // Parse contents
                csvFile = File("${contentDir.path}/programspec/pub_content.csv")
                val contents: MutableList<Message> = mutableListOf()
                lineCount = 0;

                csvFile.forEachLine { line ->
                    if (lineCount == 0) {
                        lineCount++
                        return@forEachLine
                    }

                    val tokens = line.trim().split(",").map { it.trim() }
                    contents += Message(
                        deployment_num = tokens[0].toInt(),
                        playlist_title = tokens[1],
                        message_title = tokens[2],
                        key_points = tokens[3],
                        languagecode = tokens[4],
                        variant = tokens[5],
                        format = tokens[6],
                        audience = tokens[7],
                        default_category = tokens[8],
                        sdg_goals = tokens[9],
                        sdg_targets = tokens[10]
                    )
                }


                App().db.specDao().insert(
                    ProgramSpecEntity(
                        programId = program.program_id,
                        deploymentName = deployment.deploymentname,
                        recipients = recipients,
                        general = general.first(),
                        deployments = deployments,
                        contents = contents,
                        updatedAt = LocalDateTime.now()
                    )
                )
            }
        }
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

}