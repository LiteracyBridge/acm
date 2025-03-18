package org.literacybridge.acm.store

import org.literacybridge.acm.config.ACMConfiguration
import org.literacybridge.acm.gui.assistants.ContentImport.AudioTarget
import org.literacybridge.acm.gui.assistants.util.AudioUtils
import org.literacybridge.core.spec.ContentSpec
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.properties.Delegates

enum class DeplomentPlatform {
    TalkingBook,
    CompanionApp
}

class AudioItemModel {
    var id by Delegates.notNull<Int>()
    lateinit var title: String
    lateinit var language: String
    var variant: String? = null
    lateinit var acm_id: String
    lateinit var type: String
    var playlist_title: String? = null // NB: Only populated in join queries

    enum class ItemType {
        PlaylistPrompt,
        SystemPrompt,
        Message,
        Survey
    }

    companion object {
        fun create(
            audioType: ItemType,
            audioTarget: AudioTarget
        ) {
            // Find message/playlist spec
            // fill out remaing section
            val msg = audioTarget.messageSpec
            val audio = audioTarget.item
            val playlistQuery = if (audioType != ItemType.SystemPrompt) {
                "(SELECT p.id FROM playlists p" +
                        " INNER JOIN deployments d ON d.id = p.deployment_id " +
                        " AND d.deployment_number = ${msg.deploymentNumber} " +
                        " WHERE p.title = '${audioTarget.playlistSpec.playlistTitle}'\n" +
                        " LIMIT 1)"
            } else {
                "null"
            }

            val title = if (audioType == ItemType.Message) {
                audioTarget.messageSpec.title
            } else {
                audio.title
            }

            val variant = if (msg?.variant == null) {
                "null"
            } else if (msg.variant.isEmpty()) {
                "null"
            } else {
                msg.variant
            }

            ACMConfiguration.getInstance().currentDB.db.update(
                "INSERT OR IGNORE INTO audio_items(title, language, duration, file_path, position," +
                        " format, default_category_code, variant, sdg_goal_id, key_points, created_at, status, " +
                        " volume, keywords,timing, primary_speaker, acm_id, related_id, transcription, " +
                        " note, beneficiary, category, type, committed, source, playlist_id)" +
                        " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
                        + "$playlistQuery)",
                title,
                audio?.languageCode ?: msg?.languagecode,
                audio?.duration,
                null,
                0,
                audio?.metadata?.get(MetadataSpecification.LB_MESSAGE_FORMAT) ?: msg?.format,
                variant,
                msg?.sdg_goals,
                msg?.sdg_targets,
                msg?.keyPoints,
                Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT),
                audio?.metadata?.get(MetadataSpecification.LB_STATUS),
                audio?.metadata?.get(MetadataSpecification.LB_VOLUME),
                audio?.metadata?.get(MetadataSpecification.LB_KEYWORDS),
                audio?.metadata?.get(MetadataSpecification.LB_TIMING),
                audio?.metadata?.get(MetadataSpecification.LB_PRIMARY_SPEAKER),
                audio?.metadata?.get(MetadataSpecification.DC_IDENTIFIER),
                audio?.metadata?.get(MetadataSpecification.DC_RELATION),
                audio?.metadata?.get(MetadataSpecification.LB_ENGLISH_TRANSCRIPTION),
                audio?.metadata?.get(MetadataSpecification.LB_NOTES),
                audio?.metadata?.get(MetadataSpecification.LB_BENEFICIARY),
                audio?.categoryList?.joinToString(",") { it.categoryName },
                audioType,
                false,
                audio?.metadata?.get(MetadataSpecification.DC_SOURCE),
            )
        }

        fun delete(
            audioItem: AudioItem
        ) {
            ACMConfiguration.getInstance().currentDB.db.update("DELETE FROM audio_items WHERE acm_id = ?", audioItem.id)
        }
    }
}