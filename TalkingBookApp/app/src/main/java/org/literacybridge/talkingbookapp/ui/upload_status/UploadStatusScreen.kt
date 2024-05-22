package org.literacybridge.talkingbookapp.ui.upload_status

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import org.literacybridge.talkingbookapp.ui.components.AppScaffold
import org.literacybridge.talkingbookapp.ui.theme.Green40
import org.literacybridge.talkingbookapp.util.Constants.Companion.SCREEN_MARGIN

@Composable
fun UploadStatusScreen(
    navController: NavController
) {
    AppScaffold(title = "Upload Status", navController = navController) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(SCREEN_MARGIN)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "2 files uploading...",
                modifier = Modifier.padding(bottom = 15.dp),
                style = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            )

            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth(),
//                    .background(color = Color.White)
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Icon(
                        Icons.Outlined.Send,
                        modifier = Modifier
                            .size(50.dp)
                            .padding(10.dp)
                            .align(Alignment.CenterVertically),
                        contentDescription = "View progress of statistics and contents upload or download"
                    )
                    Column {
                        Text(
                            "Uploading",
                            style = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(end = 2.dp),
                                tint = Green40,
                                contentDescription = "View progress of statistics and contents upload or download"
                            )
                            Text(
                                text = "sslsdlkdsldkssdlksdlk.zip",
                                modifier = Modifier.padding(vertical = 5.dp)
                            )
                        }

                        LinearProgressIndicator(
                            modifier = Modifier
                                .height(10.dp)
                                .width(IntrinsicSize.Max)
                                .clip(CircleShape)
                        )
                    }
                }
            }
        }
    }
}
