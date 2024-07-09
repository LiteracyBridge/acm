package org.literacybridge.talkingbookapp.ui.talkingbook_update

import Screen
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import org.literacybridge.core.spec.Recipient
import org.literacybridge.talkingbookapp.ui.components.AppScaffold
import org.literacybridge.talkingbookapp.util.Constants
import org.literacybridge.talkingbookapp.view_models.RecipientViewModel
import org.literacybridge.talkingbookapp.view_models.UserViewModel

@Composable
fun ContentVariantScreen(
    navController: NavController,
    userViewModel: UserViewModel = viewModel(),
    recipientViewModel: RecipientViewModel = viewModel(),
) {
//    val data by remember { mutableListOf("Amam", "Ghana") }
    val packages = recipientViewModel.packages.collectAsState()
    val pubCsv = createPropertiesMap()
    val recipient = Recipient(pubCsv)
    val defaultRecipient = recipientViewModel.defaultRecipient
    var checked by remember { mutableStateOf(true) }

    AppScaffold(title = "Available Content Packages", navController = navController, bottomBar = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constants.SCREEN_MARGIN),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    //navController.popBackStack()
                    defaultRecipient.value = recipient
                    navController.navigate(Screen.CONTENT_VERIFICATION.name)
                }
            ) {
                Text("Continue")
            }
        }

    }) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(Constants.SCREEN_MARGIN)
//                .verticalScroll(rememberScrollState())
        ) {
            // TODO: change to packages list
            items(packages.value.size) { index ->
                ListItem(
                    headlineContent = { Text(packages.value.get(index).label) },
                    trailingContent = {
                        Row {
                            if (index == packages.value.size - 1) { // Last item, hide "down" arrow
                                Box {}
                            } else {
                                IconButton(
                                    onClick = {
                                        val temp = packages.value.map { it }.toMutableList()
                                        val current = temp[index]
                                        val next = temp[index + 1]

                                        temp[index + 1] = current
                                        temp[index] = next
                                        recipientViewModel.packages.value = temp
                                    },
                                    content = {
                                        Icon(
                                            Icons.Filled.KeyboardArrowDown,
                                            contentDescription = "Move package down"
                                        )
                                    }
                                )
                            }

                            if (index == 0) { // First item, don't show arrow up
                                Box {}
                            } else {
                                IconButton(
                                    onClick = {
                                        val temp = packages.value.map { it }.toMutableList()
                                        val current = temp[index]
                                        val next = temp[index - 1]

                                        temp[index - 1] = current
                                        temp[index] = next
                                        recipientViewModel.packages.value = temp

                                    },
                                    content = {
                                        Icon(
                                            Icons.Filled.KeyboardArrowUp,
                                            contentDescription = "Move package up"
                                        )
                                    }
                                )
                            }
                        }
                    },
                    leadingContent = {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { checked = it }
                        )
                    }
                )
                HorizontalDivider()
            }
        }

    }
}

fun createPropertiesMap(): Map<String, String> {
    val pubCsv = hashMapOf<String, String>()
    pubCsv["recipientid"] = "12"
    pubCsv["district"] = "Default District"
    pubCsv["communityname"] = "WA"
    pubCsv["groupname"] = "Default Group"
    pubCsv["numtbs"] = "10"
    pubCsv["agent"] = "Agent"
    pubCsv["language"] = "en"
    pubCsv["variant"] = "variant"
    return pubCsv
}
