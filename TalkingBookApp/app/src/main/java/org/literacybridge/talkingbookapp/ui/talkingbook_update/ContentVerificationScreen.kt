package org.literacybridge.talkingbookapp.ui.talkingbook_update

import Screen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import org.literacybridge.talkingbookapp.ui.components.AppScaffold
import org.literacybridge.talkingbookapp.util.Constants.Companion.SCREEN_MARGIN
import org.literacybridge.talkingbookapp.view_models.RecipientViewModel
import org.literacybridge.talkingbookapp.view_models.TalkingBookViewModel

@Composable
fun ContentVerificationScreen(
    navController: NavController,
    viewModel: TalkingBookViewModel = viewModel(),
    recipientViewModel: RecipientViewModel = viewModel()
) {
    AppScaffold(title = "Verify Deployment", navController = navController, bottomBar = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SCREEN_MARGIN),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = recipientViewModel.selectedRecipient.value != null,
                onClick = {
                    navController.navigate(Screen.CONTENT_UPDATE.name)
                }
            ) {
                Text("Update Device")
            }
        }
    }) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(SCREEN_MARGIN)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {

            Text(
                text = "Current Recipient",
                modifier = Modifier
                    .padding(16.dp),
                textAlign = TextAlign.Left,
                style = TextStyle(fontWeight = FontWeight.Bold)
            )
            Divider()

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
            ) {

                Text(
                    modifier = Modifier.padding(10.dp),
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(fontWeight = FontWeight.Bold)
                        ) {
                            append("District: \t")
                        }
                        // TODO: change to get district
                        append(recipientViewModel.defaultRecipient.value?.district ?: "N/A")

                        withStyle(
                            SpanStyle(fontWeight = FontWeight.Bold)
                        ) {
                            append("\nCommunity: \t")
                        }
                        append(recipientViewModel.defaultRecipient.value?.communityname ?: "N/A")

                        withStyle(
                            SpanStyle(fontWeight = FontWeight.Bold)
                        ) {
                            append("\nGroup: \t")
                        }
                        // TODO: change to get group
                        append(recipientViewModel.defaultRecipient.value?.groupname ?: "N/A")
                    })
            }

            Text(
                text = "Next Recipient",
                modifier = Modifier
                    .padding(16.dp)
                    .padding(top = 15.dp),
                textAlign = TextAlign.Center,
                style = TextStyle(fontWeight = FontWeight.Bold)
            )
            Divider()

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    modifier = Modifier.padding(10.dp),
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(fontWeight = FontWeight.Bold)
                        ) {
                            append("District: \t")
                        }
                        append(recipientViewModel.selectedRecipient.value?.district)

                        withStyle(
                            SpanStyle(fontWeight = FontWeight.Bold)
                        ) {
                            append("\nCommunity: \t")
                        }
                        append(recipientViewModel.selectedRecipient.value?.communityname)

                        withStyle(
                            SpanStyle(fontWeight = FontWeight.Bold)
                        ) {
                            append("\nGroup: \t")
                        }
                        append(recipientViewModel.selectedRecipient.value?.groupname)

                    })
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "",
                    modifier = Modifier.padding(50.dp),
                    textAlign = TextAlign.Left,
                    style = TextStyle(fontWeight = FontWeight.Bold)
                )

                Button(onClick = { navController.navigate(Screen.CONTENT_VARIANT.name) }) {
                    Text(text = "Select Content Package")
                }
            }
        }
    }
}