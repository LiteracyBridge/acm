package org.literacybridge.acm.config

import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.ResultSetHandler
import org.apache.commons.dbutils.handlers.BeanListHandler
import org.literacybridge.acm.gui.util.UIUtils
import org.literacybridge.acm.store.MetadataSpecification
import org.literacybridge.acm.store.MetadataStore
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.properties.Delegates

class Deployment {
    var id by Delegates.notNull<Int>()
}

class AudioItemDto {
    var id by Delegates.notNull<Int>()
    lateinit var title: String
    lateinit var language: String
    lateinit var acm_id: String
    lateinit var type: String
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

//    fun query(sql: String): PreparedStatement {
//        return connection.prepareStatement(sql);
//    }

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

        println("Messages count: ${store.audioItems.size}")

        spec.deployments.forEach { deployment ->
            update(
                "INSERT OR IGNORE INTO deployments(name, deployment_number, committed) VALUES(?,?,?)",
                deployment.deploymentname,
                deployment.deploymentnumber,
                false
            )

            spec.contentSpec.deploymentSpecs.find { it.deploymentNumber == deployment.deploymentnumber }
                ?.playlistSpecs?.forEach { playlist ->
//                    val test = (select<Deployment>(
//                        "SELECT id FROM deployments WHERE deployment_number = ?",
//                        deployment.deploymentnumber
//                    )?.get(0))
//                    println(test?.id)
//                    println(deployment.deploymentnumber)
                    update(
                        "INSERT OR IGNORE INTO playlists(title, deployment_id) VALUES(?,"
                                + "(SELECT id FROM deployments WHERE deployment_number = ? LIMIT 1))",
                        playlist.playlistTitle,
                        deployment.deploymentnumber
                    )

                    val prommptsTracker: ArrayList<String> = ArrayList()

                    // Insert for playlist prompts & messages
                    for (audio in store.audioItems) {
//                    store.audioItems.forEach { audio ->
                        val msg = playlist.messageSpecs.find { it.title == audio.title }
                        val title = audio?.title ?: msg?.title;

                        println(UIUtils.getCategoryNamesAsString(audio))
                        var index = 0;
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
                        if (audioType == "SystemPrompt"){
                            val key = "$title-${audio?.languageCode}"
                            if (prommptsTracker.contains(key)) {
                                continue
                            }
                            prommptsTracker.add(key)
                            playlistQuery="null"
                        }


                        update(
                            "INSERT OR IGNORE INTO audio_items(title, language, duration, file_path, position," +
                                    " format, default_category_code, variant, sdg_goal_id, key_points, created_at, status, " +
                                    " volume, keywords,timing, primary_speaker, acm_id, related_id, transcription, " +
                                    " note, beneficiary, category, type, committed, source, playlist_id)" +
                                    " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
                                    +  "$playlistQuery)",
                            title,
                            audio?.languageCode ?: msg?.languagecode,
                            audio?.duration,
                            null,
                            ++index,
                            audio?.metadata?.get(MetadataSpecification.LB_MESSAGE_FORMAT) ?: msg?.format,
                            msg?.variant,
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
                }
        }

        commit()
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
//        for (q in file.readText().split(";")) {
//            connection.createStatement().executeUpdate("$q;")
//        }

        update(file.readText())
        update(
            "INSERT INTO migrations(timestamp, name) VALUES(?,?)",
            Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT),
            file.name
        )

        println("Database migration executed successfully.")
    }
}