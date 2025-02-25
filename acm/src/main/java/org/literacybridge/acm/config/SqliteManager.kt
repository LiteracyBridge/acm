package org.literacybridge.acm.config

import java.io.File
import java.sql.*
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class SqliteManager(private val pathsProvider: PathsProvider) {
    private lateinit var connection: Connection
    private val dbPath = "${this.pathsProvider.programHomeDir}/database.sqlite"
    var isNewDb = false
        private set

    init {
        try {
            // Default to 'false' if the db file is yet to be created
            // Trick to check run initial migration scripts for all programs
            isNewDb = !File(dbPath).exists()

            println("Connecting to database... jdbc:sqlite:${dbPath}")
            this.connection = DriverManager.getConnection("jdbc:sqlite:${dbPath}")
            runMigrations()
        } catch (e: SQLException) {
            println(e.message)
        }
    }

    fun query(sql: String): PreparedStatement {
        return connection.prepareStatement(sql);
    }

    private fun runMigrations() {
        val migrationsDir = this::class.java.getResource("/db-migrations").toURI().path

        if (isNewDb) {
            val f = File("${migrationsDir}/1-initial-migration.sql")
            executeMigration(f)
        }
        val resultSet = this.query("SELECT name FROM migrations").executeQuery()
        val results = mutableListOf<String>()
        while (resultSet.next()) {
            results.add(resultSet.getString("name"))
        }

        File(migrationsDir).listFiles()?.forEach { f ->
            if (!results.any { it == f.name }) {
                executeMigration(f)
            }
        }
    }

    private fun executeMigration(file: File) {
        for (q in file.readText().split(";")) {
            connection.createStatement().executeUpdate("$q;")
        }

        val stmt = this.query("INSERT INTO migrations(timestamp, name) VALUES(?,?)")
        stmt.setString(1, Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT))
        stmt.setString(2, file.name)
        stmt.execute()

        println("Database migration executed successfully.")
    }
}