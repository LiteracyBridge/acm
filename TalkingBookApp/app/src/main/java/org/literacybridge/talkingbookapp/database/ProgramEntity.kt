package org.literacybridge.talkingbookapp.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import java.time.LocalDateTime


@Entity(tableName = "program_content")
data class ProgramContentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "program_id") val programId: String,
    @ColumnInfo(name = "deployment_name") val deploymentName: String,
    @ColumnInfo(name = "latest_revision") val latestRevision: String,
    @ColumnInfo(name = "local_path") val localPath: String,
    @ColumnInfo(name = "s3_path") val s3Path: String,
    @ColumnInfo(name = "last_sync") val lastSync: LocalDateTime?,
    @ColumnInfo(name = "status") val status: ProgramContentDao.ProgramEntityStatus?, // TODO: use enum -> SYNCED, NOT_SYNCED, OUTDATED
)

@Dao
interface ProgramContentDao {
    enum class ProgramEntityStatus {
        SYNCED,
        OUTDATED
    }

    @Query("SELECT * FROM program_content")
    fun getAll(): List<ProgramContentEntity>

    @Query("SELECT * FROM program_content WHERE program_id = :programId")
    fun findByProgramId(programId: String): List<ProgramContentEntity>

    @Query(
        "SELECT * FROM program_content WHERE deployment_name = :deploymentName ORDER BY latest_revision DESC LIMIT 1"
    )
    fun findLatestRevision(deploymentName: String): ProgramContentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg content: ProgramContentEntity)

    @Delete
    fun delete(user: ProgramContentEntity)
}

