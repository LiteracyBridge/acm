package org.literacybridge.talkingbookapp.ui.upload_status

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import org.literacybridge.talkingbookapp.database.S3SyncEntity
import org.literacybridge.talkingbookapp.database.S3SyncEntityDao
import org.literacybridge.talkingbookapp.ui.components.AppScaffold
import org.literacybridge.talkingbookapp.ui.theme.Green40
import org.literacybridge.talkingbookapp.util.Constants.Companion.SCREEN_MARGIN
import org.literacybridge.talkingbookapp.util.TimeAgo2
import org.literacybridge.talkingbookapp.view_models.UploadStatusViewModel
import java.time.format.DateTimeFormatter

@Composable
fun UploadStatusScreen(
    navController: NavController,
    statusModel: UploadStatusViewModel = viewModel()
) {
    val files by statusModel.files.observeAsState()

    // Query database for s3 files
    LaunchedEffect("query-db") {
        statusModel.queryFiles()
    }

    AppScaffold(title = "Upload Status", navController = navController) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(SCREEN_MARGIN)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "${statusModel.inProgressFiles.value?.size} files uploading...",
                modifier = Modifier.padding(bottom = 15.dp),
                style = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            )

            // TODO: Implement UI for empty files
            files?.forEach { file ->
                BuildCard(file = file)
            }

        }

    }
}

@Composable
fun BuildCard(file: S3SyncEntity) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .height(80.dp)
            .background(color = Color.White),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(vertical = 5.dp)
        ) {
            Icon(
                Icons.Outlined.Send,
                modifier = Modifier
                    .size(50.dp)
                    .padding(10.dp)
                    .align(Alignment.CenterVertically),
                contentDescription = "View progress of statistics and contents upload or download"
            )
            Column {
                if (file.status != S3SyncEntityDao.S3SyncStatus.Completed) {
                    Text(
                        file.status.toString().replaceFirstChar { it.uppercase() },
                        style = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (file.status != S3SyncEntityDao.S3SyncStatus.Completed) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 2.dp),
                            tint = Green40,
                            contentDescription = "View progress of statistics and contents upload or download"
                        )
                    }

                    Text(
                        text = file.fileName,
                        modifier = Modifier.padding(vertical = 5.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Show progress indicator based on upload state
                when (file.status) {
                    S3SyncEntityDao.S3SyncStatus.Completed -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 5.dp, top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Size: ${file.size/1000}kb", fontSize = 12.sp)
                            Text(
                                text = "Uploaded: ${
                                    TimeAgo2().toText(
                                        file.updatedAt.format(
                                            DateTimeFormatter.ISO_DATE_TIME
                                        )
                                    )
                                }",
                                fontSize = 12.sp
                            )
                        }

                    }

                    S3SyncEntityDao.S3SyncStatus.Failed -> {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .height(10.dp)
                                .fillMaxWidth()
                                .padding(end = 5.dp)
                                .clip(CircleShape),
                            progress = 1.0f,
                            trackColor = MaterialTheme.colorScheme.error,
                        )
                    }

                    else -> {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .height(10.dp)
                                .fillMaxWidth()
                                .padding(end = 5.dp)
                                .clip(CircleShape),
                            progress = (file.uploaded / file.size).toFloat()
                        )
                    }
                }
            }
        }
    }
}
