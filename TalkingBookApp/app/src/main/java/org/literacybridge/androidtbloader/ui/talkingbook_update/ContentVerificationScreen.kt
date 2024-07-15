package org.literacybridge.androidtbloader.ui.talkingbook_update

import Screen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import org.literacybridge.androidtbloader.ui.components.AppScaffold
import org.literacybridge.androidtbloader.util.Constants.Companion.SCREEN_MARGIN
import org.literacybridge.androidtbloader.view_models.RecipientViewModel
import org.literacybridge.androidtbloader.view_models.TalkingBookViewModel

@Composable
fun ContentVerificationScreen(
    navController: NavController,
    talkingBookViewModel: TalkingBookViewModel = viewModel(),
    recipientViewModel: RecipientViewModel = viewModel()
) {
    val isTestingDeployment by talkingBookViewModel.isTestingDeployment.collectAsState()

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
            HorizontalDivider()

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
            HorizontalDivider()

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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 15.dp)
            ) {
                Checkbox(
                    checked = isTestingDeployment,
                    onCheckedChange = { talkingBookViewModel.isTestingDeployment.value = it }
                )
                Text("Deploy for testing")
            }

            TextButton(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = {
                    recipientViewModel.loadPackagesInDeployment()
                    navController.navigate(Screen.CONTENT_VARIANT.name)
                }) {
                Text(text = "Select Content Package")
            }
        }
    }
}