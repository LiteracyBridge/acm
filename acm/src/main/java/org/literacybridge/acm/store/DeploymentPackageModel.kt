package org.literacybridge.acm.store

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.apache.commons.io.FilenameUtils
import org.json.simple.JSONArray
import org.json.simple.JSONAware
import org.json.simple.JSONObject
import org.literacybridge.acm.Constants
import org.literacybridge.acm.cloud.Authenticator
import org.literacybridge.acm.config.ACMConfiguration
import org.literacybridge.acm.utils.EmailHelper
import org.literacybridge.core.tbloader.TBLoaderConstants
import java.io.File
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*
import kotlin.properties.Delegates

class DeploymentPackageModel {
    var id by Delegates.notNull<Int>()
    lateinit var revision: String
    lateinit var platform: String
    lateinit var created_at: String
    var metadata: String? = null
    var deployment_id: Int? = null
    var published by Delegates.notNull<Boolean>()

    companion object {
        fun create(pkg: PackageMetadata) {
            ACMConfiguration.getInstance().currentDB.db.update(
                "INSERT INTO " +
                        "deployment_packages(revision, platform, published, created_at," +
                        " metadata, deployment_id)" +
                        " VALUES(?, ?, ?, ?, ?," +
                        "(SELECT id FROM deployments WHERE deployment_number = ?))",
                pkg.revision,
                pkg.platform,
                pkg.published,
                pkg.created_at,
                pkg.toJson(),
                pkg.deployment.number,
            )
        }

        /**
         * Given a TB-Loader "published" directory and a Deployment name, find the next revision
         * for the Deployment, and create a .rev file with that revision. Return the revision.
         *
         * @param publishTbLoadersDir The directory in which the deployments are published.
         * @param deployment          The Deployment (name) for which we want the next revision suffix.
         * @return the revision suffix as a String. Like "a", "b"... "aa"... "aaaaba", etc
         * @throws Exception if the new .rev file can't be created.
         */
        fun getNextRevision(deploymentName: String, deploymentNo: Int): String {
            val latest = ACMConfiguration.getInstance().currentDB.db.query<DeploymentPackageModel>(
                "SELECT dp.* FROM deployment_packages dp \n" +
                        " INNER JOIN deployments d ON d.deployment_number = ? AND d.id = dp.deployment_id \n" +
                        " ORDER BY created_at DESC LIMIT 1",
                deploymentNo
            )?.firstOrNull()

            var revision = "a"

            // If we don't find anything higher, start with 'a'.
            if (latest == null) {
                return "${deploymentName}-$revision"
            }

            val deplMatcher = TBLoaderConstants.DEPLOYMENT_REVISION_PATTERN.matcher(latest.revision)
            if (deplMatcher.matches()) {
                revision = deplMatcher.group(2).lowercase(Locale.getDefault())
            }

            return "${deploymentName}-${incrementRevision(revision)}"
        }

        /**
         * Given a revision string, like "a", or "zz", create the next higher value, like "b" or "aaa".
         *
         * @param revision to be incremented
         * @return the incremented value
         */
        private fun incrementRevision(revision: String): String {
            require(revision.matches("^[a-z]+$".toRegex())) { "Revision string must match \"^[a-z]+$\"." }

            val chars = revision.toCharArray()

            // Looking for a digit we can add to.
            var looking = true
            var ix = chars.size - 1
            while (ix >= 0 && looking) {
                if (++chars[ix] <= 'z') {
                    looking = false
                } else {
                    chars[ix] = 'a'
                }
                ix--
            }
            var result = String(chars)
            if (looking) {
                // still looking, add another "digit".
                result = "a$result"
            }
            return result
        }
    }
}

@Serializable
class PackageMetadata {
    lateinit var deployment: DeploymentDescription
    lateinit var platform: String
    lateinit var revision: String
    lateinit var created_at: String
    lateinit var created_by: String
    var computer_name: String
    var published by Delegates.notNull<Boolean>()
    var size by Delegates.notNull<Long>()
    lateinit var project: String
    private val contents: HashMap<String, PackageContent> = HashMap()

    init {
        try {
            created_by = ACMConfiguration.getInstance().userName
            computer_name = InetAddress
                .getLocalHost().hostName
        } catch (e1: UnknownHostException) {
            computer_name = "UNKNOWN"
        }

    }

    fun addMessage(languageOrVariant: String, content: PackageContent) {
        contents[languageOrVariant] = content
    }

    fun toJson(): String {
        return Json.encodeToString(this)
    }

    fun save(packageDir: File) {
        val metadataFile = File(packageDir, "metadata.json")
        metadataFile.writeText(toJson(), Charsets.UTF_8)

        // Save to db
        DeploymentPackageModel.create(this)

        if (published) {
            uploadToServer()
        }
    }

    private fun uploadToServer() {
        val requestURL = Authenticator.ACCESS_CONTROL_API + "/deployment-metadata"
        val requestBody = JSONObject(Json.encodeToJsonElement(this).jsonObject)

        val jsonResponse = Authenticator.getInstance().awsInterface.authenticatedPostCall(requestURL, requestBody)
        if (jsonResponse != null) {
//            var o = jsonResponse["ResponseMetadata"]
//            if (o is JSONObject) {
//                o = (o as JSONObject)["HTTPStatusCode"]
//            }
//            if (o is Long) {
//                status_aws = o == EmailHelper.EMAIL_SENT_RESPONSE.toLong()
//            }
//            EmailHelper.LOG.info(String.format("email: %s\n          %s\n", requestBody, jsonResponse))
        }
        // parse response
        println(jsonResponse)

//        return status_aws
    }

    @Serializable
    class PackageContent() {
        val messages: ArrayList<MessageContent> = ArrayList()
        private val playlist_prompts: ArrayList<MessageContent> = ArrayList()
        private val system_prompts: ArrayList<SystemPromptContent> = ArrayList()

        fun addMessage(audioItem: AudioItemModel, position: Int, file: File, baseDir: File) {
            messages.add(
                MessageContent(
                    title = audioItem.title,
                    acm_id = audioItem.acm_id,
                    language = audioItem.language,
                    variant = audioItem.variant,
                    path = FilenameUtils.separatorsToUnix(file.toRelativeString(baseDir)),
                    playlist = audioItem.playlist_title,
                    size = file.length(),
                    position = position,
                    publisher = audioItem.publisher,
                    source = audioItem.source,
                    related_id = audioItem.related_id,
                    dtb_revision = audioItem.dtb_revision,
                    duration = audioItem.duration,
                    recorded_at = audioItem.recorded_at,
                    keywords = audioItem.keywords,
                    timing = audioItem.timing,
                    speaker = audioItem.speaker,
                    goal = audioItem.goal,
                    transcription = audioItem.transcription,
                    notes = audioItem.notes,
                    status = audioItem.status,
                    category = audioItem.category,
                )
            )
        }

        fun addPlaylistPrompt(audioItem: AudioItemModel, file: File, baseDir: File) {
            playlist_prompts.add(
                MessageContent(
                    title = audioItem.title,
                    acm_id = audioItem.acm_id,
                    language = audioItem.language,
                    variant = audioItem.variant,
                    path = FilenameUtils.separatorsToUnix(file.toRelativeString(baseDir)),
                    playlist = audioItem.playlist_title,
                    size = file.length(),
                    position = null,
                    publisher = audioItem.publisher,
                    source = audioItem.source,
                    related_id = audioItem.related_id,
                    dtb_revision = audioItem.dtb_revision,
                    duration = audioItem.duration,
                    recorded_at = audioItem.recorded_at,
                    keywords = audioItem.keywords,
                    timing = audioItem.timing,
                    speaker = audioItem.speaker,
                    goal = audioItem.goal,
                    transcription = audioItem.transcription,
                    notes = audioItem.notes,
                    status = audioItem.status,
                    category = audioItem.category,
                )
            )
        }

        fun addSystemPrompt(audioItem: AudioItemModel, file: File, baseDir: File) {
            system_prompts.add(
                SystemPromptContent(
                    title = audioItem.title,
                    contentId = audioItem.acm_id,
                    language = audioItem.language,
                    path = FilenameUtils.separatorsToUnix(file.toRelativeString(baseDir)),
                    size = file.length()
                )
            )
        }
    }

    @Serializable
    data class MessageContent(
        val title: String,
        val acm_id: String,
        val path: String,
        val language: String,
        val playlist: String?,
        val size: Long,
        val variant: String?,
        val position: Int?,
        val publisher: String?,
        val source: String?,
        val related_id: String?,
        val dtb_revision: String?,
        val duration: String?,
        val recorded_at: String?,
        val keywords: String?,
        val timing: String?,
        val speaker: String?,
        val goal: String?,
        val transcription: String?,
        val notes: String?,
        val status: String?,
        val category: String?
    )

    @Serializable
    data class SystemPromptContent(
        val title: String,
        val contentId: String,
        val path: String,
        val language: String,
        val size: Long,
    )

    @Serializable
    data class DeploymentDescription(val name: String, val number: Int)
}
