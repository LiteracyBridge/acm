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