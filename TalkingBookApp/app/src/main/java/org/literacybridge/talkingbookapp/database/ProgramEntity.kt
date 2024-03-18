package org.literacybridge.talkingbookapp.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import java.util.Date

@Entity(tableName = "program_content")
data class ProgramContentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "program_id") val programId: String,
    @ColumnInfo(name = "deployment_name") val deploymentName: String,
    @ColumnInfo(name = "latest_version") val latestVersion: String?,
    @ColumnInfo(name = "local_path") val localPath: String?,
    @ColumnInfo(name = "s3_path") val s3Path: String?,
    @ColumnInfo(name = "last_sync") val lastSync: Date?,
    @ColumnInfo(name = "status") val status: String?, // TODO: use enum -> SYNCED, NOT_SYNCED, OUTDATED
)

@Dao
interface ProgramContentDao {
    @Query("SELECT * FROM program_content")
    fun getAll(): List<ProgramContentEntity>

//    @Query("SELECT * FROM program_content WHERE id IN (:userIds)")
//    fun loadAllByProgramIds(userIds: IntArray): List<ProgramContentEntity>

    @Query("SELECT * FROM program_content WHERE program_id = :programId")
    fun findByProgramId(programId: String): List<ProgramContentEntity>

    @Insert
    fun insertAll(vararg users: ProgramContentEntity)

    @Delete
    fun delete(user: ProgramContentEntity)
}

