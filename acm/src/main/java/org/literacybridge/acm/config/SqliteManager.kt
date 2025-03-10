package org.literacybridge.acm.config

import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.ResultSetHandler
import org.apache.commons.dbutils.handlers.BeanListHandler
import org.literacybridge.acm.gui.assistants.util.AudioUtils
import org.literacybridge.acm.gui.util.UIUtils
import org.literacybridge.acm.store.AudioItem
import org.literacybridge.acm.store.MetadataSpecification
import org.literacybridge.acm.store.MetadataStore
import org.literacybridge.acm.store.Playlist
import org.literacybridge.core.spec.ContentSpec
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import kotlin.properties.Delegates

class Deployment {
    var id by Delegates.notNull<Int>()
}


class SqliteManager(private val pathsProvider: PathsProvider) {
    @PublishedApi
    internal lateinit var connection: Connection
        private set

    private val dbPath = "${this.pathsProvider.programHomeDir}/database.sqlite"
    var isNewDb = false
        private set

    init {
        try {
            // Default to 'false' if the db file is yet to be created
            // Trick to check run initial migration scripts for all programs
            isNewDb = !File(dbPath).exists()

            println("Connecting to database... jdbc:sqlite:${dbPath}")
            connection = DriverManager.getConnection("jdbc:sqlite:${dbPath}")
            connection.autoCommit = false

            runMigrations()
        } catch (e: SQLException) {
            println(e.message)
        }
    }

    /**
     * Saves the changes made to the database
     */
    fun commit() {
        connection.commit()
    }

    /**
     * Discard changes made to the database
     */
    fun discard() {
        connection.rollback()
    }

    inline fun <reified T> query(sql: String, vararg params: Any): List<T>? {
        val h: ResultSetHandler<List<T>> = BeanListHandler<T>(T::class.java)
        return QueryRunner().query(connection, sql, h, *params)
    }

    fun update(sql: String, vararg params: Any?): Int {
        return QueryRunner().update(connection, sql, *params)
    }

    fun migratContentsToSqlite() {
        if (!isNewDb) return;

        val spec = ACMConfiguration.getInstance().currentDB.programSpec;
        val store: MetadataStore = ACMConfiguration.getInstance().currentDB.metadataStore;
        val migratedAudioItems: ArrayList<String> = ArrayList()

        println("Messages count: ${store.audioItems.size}")

        spec.deployments.forEach { deployment ->
            update(
                "INSERT OR IGNORE INTO deployments(name, deployment_number, committed) VALUES(?,?,?)",
                deployment.deploymentname,
                deployment.deploymentnumber,
                false
            )

            // Import playlist
            val acmPlaylists = getAcmDeploymentPlaylists(store, deployment.deploymentnumber)
            val specPlaylist =
                spec.contentSpec.deploymentSpecs.find { it.deploymentNumber == deployment.deploymentnumber }?.playlistSpecs
                    ?: emptyList()

            for (playlist in specPlaylist) {
                update(
                    "INSERT OR IGNORE INTO playlists(title, deployment_id) VALUES(?,"
                            + "(SELECT id FROM deployments WHERE deployment_number = ? LIMIT 1))",
                    playlist.playlistTitle,
                    deployment.deploymentnumber
                )

                val acmPl = acmPlaylists.find {
                    var name = AudioUtils.undecoratedPlaylistName(it.name)
                    if (name.endsWith("tlh")) {
                        name = name.replace("tlh", "", true).trim()
                    }
                    playlist.playlistTitle.trim().startsWith(name, true)
                }


                if (acmPl == null) {
                    println(acmPl)
                    println(playlist)
                    continue
                };

                val audioItems = store.audioItems.filter {
                    var audioType = ""
                    if (it?.categoryList?.isNotEmpty() == true) {
                        audioType = when (UIUtils.getCategoryNamesAsString(it)) {
                            "General Other" -> "Message"
                            "TB System" -> "SystemPrompt"
                            "TB Categories" -> "PlaylistPrompt"
                            "Survey" -> "Survey"
                            else -> "Message"
                        }

                        if (audioType == "PlaylistPrompt") {
                            if (it.title.endsWith(
                                    " - invitation",
                                    true
                                ) && playlist.playlistTitle.startsWith(it.title.replace(" - invitation", ""), true)
                            ) {
                                return@filter true;
                            }
                            if (it.title.endsWith(
                                    " : invitation",
                                    true
                                ) && playlist.playlistTitle.startsWith(it.title.replace(" : invitation", ""), true)
                            ) {
                                return@filter true;
                            }

                            if (playlist.playlistTitle.startsWith(it.title, true)) {
                                return@filter true;
                            }
                        }
                    }
                    acmPl.audioItemList.contains(it.id)

                }

                // Import audio items//messages
                val prommptsTracker: ArrayList<String> = ArrayList()

                // Insert for playlist prompts & messages
                for (audio in audioItems) {
                    val msg = playlist.messageSpecs.find { it.title == audio.title }
                    val title = audio?.title ?: msg?.title;

                    var audioType = "Message"
                    if (audio?.categoryList?.isNotEmpty() == true) {
                        audioType = when (UIUtils.getCategoryNamesAsString(audio)) {
                            "General Other" -> "Message"
                            "TB System" -> "SystemPrompt"
                            "TB Categories" -> "PlaylistPrompt"
                            "Survey" -> "Survey"
                            else -> "Message"
                        }
                    }

                    var playlistQuery = "(SELECT id FROM playlists WHERE title = '${playlist.playlistTitle}' LIMIT 1)"
                    if (audioType == "SystemPrompt") {
                        playlistQuery = "null"
                    }

                    migrateAudioItem(playlistQuery, title, audio, msg, 0, audioType)
                    migratedAudioItems.add(audio.id)
                }
            }
        }

        // TODO: implement system prompts
        // Insert for playlist prompts & messages
        for (audio in store.audioItems) {
            if (migratedAudioItems.contains(audio.id)) continue;

            // TODO: if name contains 'invitation' or matches a specific playlist name, then playlist prompt
            var audioType = "Message"
            if (audio.categoryList?.isNotEmpty() == true) {
                audioType = when (UIUtils.getCategoryNamesAsString(audio)) {
                    "General Other" -> "Message"
                    "TB System" -> "SystemPrompt"
                    "TB Categories" -> "PlaylistPrompt"
                    "Survey" -> "Survey"
                    else -> "Message"
                }
            }

            migrateAudioItem("null", audio.title, audio, null, 0, audioType)
            migratedAudioItems.add(audio.id)
        }
        commit()
    }

    private fun migrateAudioItem(
        playlistQuery: String,
        title: String?,
        audio: AudioItem,
        msg: ContentSpec.MessageSpec?,
        index: Int,
        audioType: String
    ): Int {
        var index1 = index
        val variant = if(msg?.variant == null){
             "null"
        } else if (msg.variant.isEmpty()){
            "null"
        } else {
            msg.variant
        }

        update(
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
            ++index1,
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
        return index1
    }

    private fun runMigrations() {
        val migrationsDir = this::class.java.getResource("/db-migrations").toURI().path

        if (isNewDb) {
            val f = File("${migrationsDir}/1-initial-migration.sql")
            executeMigration(f)
        }
        val resultSet = connection.prepareStatement("SELECT name FROM migrations").executeQuery()
        val results = mutableListOf<String>()
        while (resultSet.next()) {
            results.add(resultSet.getString("name"))
        }

        File(migrationsDir).listFiles()?.forEach { f ->
            if (!results.any { it == f.name }) {
                executeMigration(f)
            }
        }

        connection.commit()
    }

    private fun executeMigration(file: File) {
        update(file.readText())
        update(
            "INSERT INTO migrations(timestamp, name) VALUES(?,?)",
            Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT),
            file.name
        )

        println("Database migration executed successfully.")
    }

    /**
     * Gets the playlists defined in the ACM for a given Deployment. If all content was imported,
     * and playlists were not manually edited, these will completely match the programSpec playlists.
     * Additional playlists may be present, if there were any created with the pattern #-pl-lang.
     *
     * @param deploymentNo of the Deployment.
     * @param languages    of all the Recipients in the Deployment.
     * @return a map of { language : [ Playlist ] }
     */
    private fun getAcmDeploymentPlaylists(store: MetadataStore, deploymentNo: Int): ArrayList<Playlist> {
        val acmPlaylists: ArrayList<Playlist> = ArrayList()
        val playlists: Collection<Playlist> = store.getPlaylists()
//        introMessageCategoryName = store.getCategory(C
        // Look for anything matching the pattern, whether from the Program Spec or not.
        val pattern = Pattern.compile(String.format("%d-.*", deploymentNo))
        for (pl in playlists) {
            val plMatcher = pattern.matcher(pl.name)
            if (plMatcher.matches()) {
                acmPlaylists.add(pl)
//                    // Remember which playlists were "Intro Message" categories.
//                    if (introMessageCategoryName == AudioUtils.undecoratedPlaylistName(pl.name)) {
//                        introMessageCategories.add(pl)
//                    }
            }
//            }

        }
        return acmPlaylists

    }

}