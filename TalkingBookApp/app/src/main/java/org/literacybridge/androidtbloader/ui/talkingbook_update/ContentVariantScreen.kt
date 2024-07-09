package org.literacybridge.androidtbloader.ui.talkingbook_update

import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import org.literacybridge.androidtbloader.ui.components.AppScaffold
import org.literacybridge.androidtbloader.util.Constants
import org.literacybridge.androidtbloader.view_models.RecipientViewModel

@Composable
fun ContentVariantScreen(
    navController: NavController,
    recipientViewModel: RecipientViewModel = viewModel(),
) {
    val packages = recipientViewModel.packages.collectAsState()
//    var checked by remember { mutableStateOf(true) }

    AppScaffold(title = "Available Content Packages", navController = navController, bottomBar = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constants.SCREEN_MARGIN),
//            verticalArrangement = Arrangement.SpaceEvenly,
//            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    navController.popBackStack()
                }
            ) {
                Text("Save Selection")
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    recipientViewModel.shouldDiscardPackages.value = true
                    navController.popBackStack()
                }
            ) {
                Text("Discard")
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
                            checked = packages.value.get(index).isSelected,
                            onCheckedChange = { packages.value.get(index).isSelected = it }
                        )
                    }
                )
                HorizontalDivider()
            }
        }

    }
}