package org.literacybridge.acm.store
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
class PackageMetadata {
    private val contents: HashMap<String, PackageContent> = HashMap()
    private lateinit var description: PackageDescription

    fun addMessage(languageOrVariant: String, content: PackageContent) {
        contents.put(languageOrVariant, content)
    }

    fun setDecription(value: PackageDescription){
        description = value
    }

    @Serializable
    class PackageContent {
        val messages: ArrayList<MessageContent> = ArrayList()
        val playlistPrompts: ArrayList<MessageContent> = ArrayList()
        val systemPrompts: ArrayList<SystemPromptContent> = ArrayList()

        fun addMessage(value: MessageContent) {
            messages.add(value)
        }

        fun addPlaylistPrompt(value: MessageContent) {
            playlistPrompts.add(value)
        }

        fun addSystemPrompt(value: SystemPromptContent) {
            systemPrompts.add(value)
        }

        fun toJson() {
//            val json = buildJsonObject {
//                put("booleanKey", true)
//                putJsonArray("arrayKey") {
//                    for (i in 1..10) add(i)
//                }
//                putJsonObject("objectKey") {
//                    put("stringKey", "stringValue")
//                }
//            }
        }
    }

    @Serializable
    data class MessageContent(
        val title: String,
        val contentId: String,
        val path: String,
        val language: String,
        val playlist: String,
        val size: Long,
        val variant: String
    );

    @Serializable
    data class SystemPromptContent(
        val title: String,
        val contentId: String,
        val path: String,
        val language: String,
        val size: Long,
    )

    @Serializable
    data class PackageDescription(
        val deployment: DeploymentDescription,
        val name: String, val createdAt: String,
        val createdBy: String,
        val computerName: String, val size: Float, val project: String
    )

    @Serializable
    data class DeploymentDescription(val name: String, val number: Int)
}

//private class PackageContent {
//    val messages: ArrayList<MessageContent> = ArrayList()
//
//    fun addMessage(value: MessageContent){
//        messages.add(value)
//    }
//}
//
//private data class MessageContent(
//    val title: String,
//    val contentId: String,
//    val path: String,
//    val language: String,
//    val playlist: String,
//    val size: Float,
//    val variant: String
//);