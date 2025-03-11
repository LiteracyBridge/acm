package org.literacybridge.acm.store

import kotlinx.serialization.Serializable
import java.util.*
import kotlinx.serialization.json.Json
import org.apache.commons.io.FilenameUtils
import java.io.File
import kotlin.properties.Delegates

@Serializable
class PackageMetadata {
    lateinit var deployment: DeploymentDescription
    lateinit var revision: String
    lateinit var createdAt: String
    lateinit var createdBy: String
    lateinit var computerName: String
    var size by Delegates.notNull<Long>()
    lateinit var project: String
    private val contents: HashMap<String, PackageContent> = HashMap()

    fun addMessage(languageOrVariant: String, content: PackageContent) {
        contents[languageOrVariant] = content
    }

    fun toJson(): String {
        return Json.encodeToString(this)
    }

    @Serializable
    class PackageContent() {
        val messages: ArrayList<MessageContent> = ArrayList()
        private val playlistPrompts: ArrayList<MessageContent> = ArrayList()
        private val systemPrompts: ArrayList<SystemPromptContent> = ArrayList()

        fun addMessage(audioItem: AudioItemModel, file: File, baseDir: File) {
            messages.add(
                MessageContent(
                    title = audioItem.title,
                    contentId = audioItem.acm_id,
                    language = audioItem.language,
                    variant = audioItem.variant,
                    path = FilenameUtils.separatorsToUnix(file.toRelativeString(baseDir)),
                    playlist = audioItem.playlist_title,
                    size = file.length()
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
                    size = file.length()
                )
            )
        }

        fun addSystemPrompt(audioItem: AudioItemModel, file: File, baseDir: File) {
            systemPrompts.add(
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
        val contentId: String,
        val path: String,
        val language: String,
        val playlist: String?,
        val size: Long,
        val variant: String?
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
