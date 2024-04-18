package org.literacybridge.talkingbookapp.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import org.literacybridge.core.spec.Recipient
import org.literacybridge.talkingbookapp.models.Deployment
import org.literacybridge.talkingbookapp.models.Message
import org.literacybridge.talkingbookapp.models.Program
import java.time.LocalDateTime


@Entity(
    tableName = "program_spec",
    indices = [Index(value = ["deployment_name"], unique = true)]
)
data class ProgramSpecEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "program_id") val programId: String,
    @ColumnInfo(name = "deployment_name") val deploymentName: String,
    @ColumnInfo(name = "recipients") val recipients: List<Recipient> = emptyList(),
    @ColumnInfo(name = "general") val general: Program,
    @ColumnInfo(name = "deployments") val deployments: List<Deployment> = emptyList(),
    @ColumnInfo(name = "contents") val contents: List<Message> = emptyList(),
    @ColumnInfo(name = "updated_at") val updatedAt: LocalDateTime?,
)

@Dao
interface ProgramSpecDao {
    @Query(
        "SELECT * FROM program_spec WHERE deployment_name = :deploymentName ORDER BY deployment_name DESC LIMIT 1"
    )
    fun findByDeployment(deploymentName: String): ProgramSpecEntity?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg content: ProgramSpecEntity)
}

