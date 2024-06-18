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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    val selected = remember { mutableStateOf(false) }

    AppScaffold(title = "Verify Deployment", navController = navController, bottomBar = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SCREEN_MARGIN),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
//            OutlinedButton(
//                modifier = Modifier.fillMaxWidth(),
//                onClick = { navController.popBackStack() }
//            ) {
//                Text("Change Recipient")
//            }
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
//            Tab(selected.value, onClick = {
//                Log.d(LOG_TAG, "Tab Clicked")
//            }) {
//                Column(
//                    Modifier.padding(10.dp).height(50.dp).fillMaxWidth(),
//                    verticalArrangement = Arrangement.SpaceBetween
//                ) {
//                    Box(
//                        Modifier.size(10.dp)
//                            .align(Alignment.CenterHorizontally)
//                            .background(
//                                color =
//                                if (selected.value) MaterialTheme.colorScheme.primary
//                                else MaterialTheme.colorScheme.background
//                            )
//                    )
//                    Text(
//                        text = "Current",
//                        style = MaterialTheme.typography.bodyLarge,
//                        modifier = Modifier.align(Alignment.CenterHorizontally)
//                    )
//                }
//            }

//            Tab(selected.value, onClick = {
//                Log.d(LOG_TAG, "Tab Clicked")
//            }) {
//                Column(
//                    Modifier.padding(10.dp).height(50.dp).fillMaxWidth(),
//                    verticalArrangement = Arrangement.SpaceBetween
//                ) {
//                    Box(
//                        Modifier.size(10.dp)
//                            .align(Alignment.CenterHorizontally)
//                            .background(
//                                color =
//                                if (selected.value) MaterialTheme.colorScheme.primary
//                                else MaterialTheme.colorScheme.background
//                            )
//                    )
//                    Text(
//                        text = "Current",
//                        style = MaterialTheme.typography.bodyLarge,
//                        modifier = Modifier.align(Alignment.CenterHorizontally)
//                    )
//                }
//            }

//            Text("Verify the content deployment")

            Text(
                text = "After Update",
                modifier = Modifier
                    .padding(16.dp),
                textAlign = TextAlign.Left,
                style = TextStyle(fontWeight = FontWeight.Bold)
            )
            Divider()

            ElevatedCard(
//                elevation = CardDefaults.cardElevation(
//                    defaultElevation = 6.dp
//                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
//                    .padding(5.dp)
//                    .fillMaxHeight()
//                    .size(width = Modifier.fillMaxWidth(), height = 100.dp)
            ) {

//
//                ListItem(
//                    headlineContent = { Text("One line list item with 24x24 icon") },
//                    leadingContent = {
//                        Icon(
//                            Icons.Filled.Favorite,
//                            contentDescription = "Localized description",
//                        )
//                    }
//                )
//                Divider()

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
//                    append("\n")

//                    append("\n")
//                    append(viewModel.talkingBookDeviceInfo.value?.deploymentPropertiesString)

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
                        // TODO: change to get community
                        append(recipientViewModel.defaultRecipient.value?.groupname ?: "N/A")
                    })
            }

            Text(
                text = "Current",
                modifier = Modifier
                    .padding(16.dp)
                    .padding(top = 15.dp),
                textAlign = TextAlign.Center,
                style = TextStyle(fontWeight = FontWeight.Bold)
            )
            Divider()

            ElevatedCard(
//                elevation = CardDefaults.cardElevation(
//                    defaultElevation = 6.dp
//                ),
                modifier = Modifier
                    .fillMaxWidth()
//                    .size(height = 100.dp)
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


        }
    }
}