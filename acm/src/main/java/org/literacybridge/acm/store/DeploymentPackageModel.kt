package org.literacybridge.acm.store

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.apache.commons.io.FilenameUtils
import org.json.simple.JSONObject
import org.literacybridge.acm.cloud.Authenticator
import org.literacybridge.acm.config.ACMConfiguration
import java.io.File
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*
import java.util.regex.Pattern
import kotlin.properties.Delegates

class DeploymentPackageModel {
    var id by Delegates.notNull<Int>()
    lateinit var revision: String
    lateinit var platform: String
    lateinit var createdAt: String
    var metadata: String? = null
    var deploymentId: Int? = null
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
                pkg.isPublished,
                pkg.createdAt,
                pkg.toJson(),
                pkg.deployment.number,
            )
        }

        /**
         * Given a TB-Loader "published" directory and a Deployment name, find the next revision
         * for the Deployment, and create a .rev file with that revision. Return the revision.
         *
         * @param deploymentName          The Deployment (name) for which we want the next revision suffix.
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

            var revision = 1

            // If we don't find anything higher, start with 'a'.
            if (latest == null) {
                return "${deploymentName}-r$revision"
            }

            val revisionMatcher = Pattern.compile("r(\\d+)$").matcher(latest.revision)
            if (revisionMatcher.find()) {
                // Revision pattern uses new format "[deployment-name]-r[revision-number]". Increment the revision number
                revision = revisionMatcher.group(1).toInt() + 1
            } else {
                // Old revision format. Count all past deployments and generate revision in the new format
                val count = ACMConfiguration.getInstance().currentDB.db.query<DeploymentPackageModel>(
                    "SELECT dp.* FROM deployment_packages dp \n" +
                            " INNER JOIN deployments d ON d.deployment_number = ? AND d.id = dp.deployment_id",
                    deploymentNo
                )?.size ?: 0
                revision = count + 1
            }

            var name = "${deploymentName}-r${revision}"

            // Revision dir may exist on disk if deployment was created in demo mode (hence not commit to db)
            // Verify the generated revision is not a duplicate
            val tbLoaderDir = ACMConfiguration.getInstance().currentDB.programTbLoadersDir
            while (true) {
                if (File(tbLoaderDir, "published/$name").exists()) {
                    name = "${deploymentName}-r${revision + 1}"
                } else {
                    break
                }
            }

            return name
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
class PackageMetadata(val project: String) {
    lateinit var deployment: DeploymentDescription
    lateinit var platform: String
    lateinit var revision: String
    lateinit var createdAt: String
    var createdBy: String
    var computerName: String
    var packages: MutableList<String> = mutableListOf() // [deployment-name-{language|variant}]
    var size by Delegates.notNull<Long>()
    var categories: JsonArray
    var isPublished = false

    /**
     * Messages and playlist prompts metadata
     * {language}: {messages: [...], playlist_prompts: [...]}
     */
    private val contents: HashMap<String, PackageContent> = HashMap()

    /**
     * System prompts metadata
     * {language}: [...]
     */
    private val systemPrompts: HashMap<String, ArrayList<SystemPromptContent>> = HashMap()

    /**
     * Boolean property is serialized in the converted json string, kotlin bug :).
     * This is a workaround. MUST have the same value as "is_published"
     */
    lateinit var published: String


    init {
        createdBy = ACMConfiguration.getInstance().userContact
        computerName = try {
            InetAddress
                .getLocalHost().hostName
        } catch (e1: UnknownHostException) {
            "UNKNOWN"
        }

        // Generate categories list
        val leaf = ACMConfiguration.getInstance().currentDB
            .metadataStore.taxonomy.rootCategory

        categories = buildJsonArray {
            val gathered: ArrayList<Array<String>> = arrayListOf()
            for (item in getCategoryChildren(leaf, gathered)) {
                addJsonObject {
                    put("id", item[0])
                    put("name", item[1])
                    put("project", project)
                }
            }
        }
    }

    fun addMessage(languageOrVariant: String, content: PackageContent) {
        val pkg = "${deployment.name}-${languageOrVariant}"
        packages.add(pkg)
        content.packageName = pkg
        contents[languageOrVariant] = content

    }

    /**
     * Converts the
     */
    fun toJson(): String {
        return Json.encodeToString(this)
    }

    fun save(packageDir: File) {
        val metadataFile = File(packageDir, "metadata.json")
        metadataFile.writeText(toJson(), Charsets.UTF_8)

        // Save to db
        DeploymentPackageModel.create(this)

        if (isPublished) {
            uploadToServer()
        }
    }

    fun addSystemPrompt(audioItem: AudioItemModel, file: File, baseDir: File) {
        if (systemPrompts[audioItem.language] == null) {
            systemPrompts[audioItem.language] = ArrayList()
        }

        systemPrompts[audioItem.language]?.add(
            SystemPromptContent(
                title = audioItem.title,
                contentId = audioItem.acm_id,
                language = audioItem.language,
                path = FilenameUtils.separatorsToUnix(file.toRelativeString(baseDir)),
                size = file.length()
            )
        )
    }

    private fun uploadToServer() {
        val requestURL = Authenticator.ACCESS_CONTROL_API + "/deployment-metadata?platform=app"
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
        println(jsonResponse)

    }

    private fun getCategoryChildren(cat: Category, gathered: ArrayList<Array<String>>): ArrayList<Array<String>> {
        for (child in cat.sortedChildren) {
            gathered.add(arrayOf(child.id, child.categoryName))
            if (child.hasChildren()) {
                getCategoryChildren(child, gathered)
            }
        }

        return gathered
    }


    @Serializable
    class PackageContent {
        val messages: ArrayList<MessageContent> = ArrayList()
        private val playlistPrompts: ArrayList<MessageContent> = ArrayList()
        var packageName: String = "" // deploymentName-{language|variant}

        fun addMessage(audioItem: AudioItemModel, position: Int, file: File, baseDir: File) {
            messages.add(
                MessageContent(
                    title = audioItem.title,
                    contentId = audioItem.acm_id,
                    language = audioItem.language,
                    variant = audioItem.variant,
                    path = FilenameUtils.separatorsToUnix(file.toRelativeString(baseDir)),
                    playlist = audioItem.playlist_title,
                    size = file.length(),
                    position = position,
                    publisher = audioItem.publisher,
                    source = audioItem.source,
                    relatedId = audioItem.related_id,
                    dtbRevision = audioItem.dtb_revision,
                    duration = audioItem.duration,
                    recordedAt = audioItem.recorded_at,
                    keywords = audioItem.keywords,
                    timing = audioItem.timing,
                    speaker = audioItem.speaker,
                    goal = audioItem.goal,
                    transcription = audioItem.transcription,
                    notes = audioItem.notes,
                    status = audioItem.status,
                    category = audioItem.category?.split(',')?.first()?.trim(),
                )
            )
        }

        fun addPlaylistPrompt(audioItem: AudioItemModel, file: File, baseDir: File) {
            playlistPrompts.add(
                MessageContent(
                    title = audioItem.title,
                    contentId = audioItem.acm_id,
                    language = audioItem.language,
                    variant = audioItem.variant,
                    path = FilenameUtils.separatorsToUnix(file.toRelativeString(baseDir)),
                    playlist = audioItem.playlist_title,
                    size = file.length(),
                    position = null,
                    publisher = audioItem.publisher,
                    source = audioItem.source,
                    relatedId = audioItem.related_id,
                    dtbRevision = audioItem.dtb_revision,
                    duration = audioItem.duration,
                    recordedAt = audioItem.recorded_at,
                    keywords = audioItem.keywords,
                    timing = audioItem.timing,
                    speaker = audioItem.speaker,
                    goal = audioItem.goal,
                    transcription = audioItem.transcription,
                    notes = audioItem.notes,
                    status = audioItem.status,
                    category = audioItem.category?.split(',')?.first()?.trim(),
                )
            )
        }
    }

    @Serializable
    data class MessageContent(
        val title: String,
        val contentId: String,
        val path: String,
        val language: String,
        val playlist: String?,
        val size: Long,
        val variant: String?,
        val position: Int?,
        val publisher: String?,
        val source: String?,
        val relatedId: String?,
        val dtbRevision: String?,
        val duration: String?,
        val recordedAt: String?,
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
