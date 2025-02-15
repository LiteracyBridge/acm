package org.literacybridge.tbloaderandroid.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import java.time.LocalDateTime


@Entity(
    tableName = "s3_sync",
    indices = [
        Index(value = ["aws_transfer_id"], unique = true)
    ]
)
data class S3SyncEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "program_id") val programId: String,
    @ColumnInfo(name = "aws_transfer_id") val awsTransferId: String,
    @ColumnInfo(name = "s3_key") val s3Key: String,
    @ColumnInfo(name = "file_path") val path: String,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "size") val size: Long,
    @ColumnInfo(name = "uploaded") val uploaded: Long = 0,
    @ColumnInfo(name = "created_at") val createdAt: LocalDateTime?,
    @ColumnInfo(name = "updated_at") val updatedAt: LocalDateTime,
    @ColumnInfo(name = "deleted_at") val deletedAt: LocalDateTime?,
    @ColumnInfo(name = "status") val status: S3SyncEntityDao.S3SyncStatus?,
)

@Dao
interface S3SyncEntityDao {
    enum class S3SyncStatus {
        Pending,
        Failed,
        Uploading,
        Cancelled,
        Completed,
    }

    @Query("SELECT * FROM s3_sync")
    suspend fun getAll(): List<S3SyncEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
   suspend fun insert(vararg content: S3SyncEntity)

    @Query(
        "UPDATE s3_sync SET status = :status WHERE aws_transfer_id = :transferId"
    )
    suspend fun updateStatus(transferId: String, status: S3SyncStatus)

    @Query(
        "UPDATE s3_sync SET status = :status, uploaded = size, updated_at = :time WHERE aws_transfer_id = :transferId"
    )
    suspend fun uploadCompleted(
        transferId: String,
        status: S3SyncStatus = S3SyncStatus.Completed,
        time: LocalDateTime = LocalDateTime.now()
    )

    @Query(
        "UPDATE s3_sync SET uploaded = :uploaded, updated_at = :time WHERE aws_transfer_id = :transferId"
    )
    suspend fun updateProgress(
        transferId: String,
        uploaded: Long,
        time: LocalDateTime = LocalDateTime.now()
    )


    @Delete
    suspend fun delete(user: S3SyncEntity)
}

