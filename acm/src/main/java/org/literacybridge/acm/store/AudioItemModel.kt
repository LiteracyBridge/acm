package org.literacybridge.acm.store

import org.literacybridge.acm.config.ACMConfiguration
import org.literacybridge.acm.config.SqliteManager
import org.literacybridge.acm.gui.assistants.ContentImport.AudioTarget
import org.literacybridge.acm.gui.dialogs.audioItemPropertiesDialog.AudioItemPropertiesModel
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.logging.Level
import java.util.logging.Logger
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
        const val TABLE = "audio_items"
        private val logger: Logger = Logger.getLogger(AudioItemModel::class.java.name)

        private fun columnNameFromMetadataField(field: MetadataField<String>): String {
            return when (val col = field.name.replace("DC_", "").replace("LB_", "").lowercase()) {
                "english_transcription" -> "transcription"
                "message_format" -> "format"
                "target_audience" -> "audience"
                "date_recorded" -> "recorded_at"
                "goal" -> "sdg_goals"
                else -> col
            }

        }

        fun update(field: MetadataField<String>, acmId: String, value: Any) {
            val col = columnNameFromMetadataField(field)
            return update(col, acmId, value)
        }

        fun update(column: String, acmId: String, value: Any) {
            logger.log(Level.INFO, "Update '$column' of audo item '$acmId' to '$value'")

            ACMConfiguration.getInstance().currentDB.db.update(
                "UPDATE $TABLE SET $column = ?, updated_at = ? WHERE acm_id = ?",
                value,
                SqliteManager.now(),
                acmId
            )
        }

        fun insertMessageOrPlaylistPrompt(
            audioType: ItemType,
            audioTarget: AudioTarget
        ) {
            val msg = audioTarget.messageSpec
            val audio = audioTarget.item
            val playlistQuery = if (audioType != ItemType.SystemPrompt) {
                "(SELECT p.id FROM playlists p" +
                        " INNER JOIN deployments d ON d.id = p.deployment_id " +
                        " AND d.deployment_number = ${msg?.deploymentNumber ?: audioTarget.playlistSpec?.deploymentNumber} " +
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

        fun insertSystemPrompt(audio: AudioItem) {
            ACMConfiguration.getInstance().currentDB.db.update(
                "INSERT OR IGNORE INTO audio_items(title, language, duration, file_path, position," +
                        " format, created_at, status, " +
                        " volume, keywords,timing, primary_speaker, acm_id, related_id, transcription, " +
                        " note, beneficiary, category, type, committed, source)" +
                        " VALUES(?, ?, ?, null, 0, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                audio.title,
                audio?.languageCode,
                audio?.duration,
                audio?.metadata?.get(MetadataSpecification.LB_MESSAGE_FORMAT),
                SqliteManager.now(),
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
                ItemType.SystemPrompt.name,
                false,
                audio?.metadata?.get(MetadataSpecification.DC_SOURCE),
            )
        }

        fun delete(
            audioItem: AudioItem
        ) {
            ACMConfiguration.getInstance().currentDB.db.update("DELETE FROM $TABLE WHERE acm_id = ?", audioItem.id)
        }
    }
}