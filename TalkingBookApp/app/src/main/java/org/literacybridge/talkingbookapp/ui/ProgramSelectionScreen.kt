package org.literacybridge.talkingbookapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import org.literacybridge.talkingbookapp.models.Deployment
import org.literacybridge.talkingbookapp.models.Program
import org.literacybridge.talkingbookapp.ui.components.AppScaffold
import org.literacybridge.talkingbookapp.util.Constants.Companion.SCREEN_MARGIN
import org.literacybridge.talkingbookapp.view_models.UserViewModel

@Composable
fun ProgramSelectionScreen(
    navController: NavController, viewModel: UserViewModel = viewModel()
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val showDialog = remember { mutableStateOf(false) }
    val selectedProgram = remember { mutableStateOf<Program?>(null) }

    AppScaffold(title = "Select Program", navController = navController) { innerPadding ->
        if (user.programs.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = SCREEN_MARGIN)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No program has been assigned to you. \nPlease contact your IT administrator for assistance")
            }
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = SCREEN_MARGIN)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            viewModel.getPrograms().forEach { it ->
                ListItem(
                    headlineContent = {
                        // TODO: show a confirm dialog when program is selected
                        TextButton(onClick = {
                            selectedProgram.value = it
                            showDialog.value = true
                        },
                            content = { Text(it.project!!.name) })
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Info, contentDescription = "Info"
                        )
                    },
                )
            }
        }
    }

    if (showDialog.value && selectedProgram.value != null) {
        DeploymentDialog(
            onDismissRequest = {
                showDialog.value = false
                selectedProgram.value = null
            },
            onConfirmation = { deployment ->
                showDialog.value = false
                viewModel.setActiveProgram(selectedProgram.value!!, deployment, navController)
            },
            deployments = selectedProgram.value!!.project!!.deployments
        )
    }
}


@Composable
fun DeploymentDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: (deployment: Deployment) -> Unit,
    deployments: List<Deployment>
) {
    val selectedOption = remember { mutableStateOf("") }

    AlertDialog(
        title = { Text(text = "Choose Deployment") },
        text = {
            Column(
                modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                deployments.forEach { deployment ->
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (deployment.deploymentname == selectedOption.value),
                                onClick = {
                                    selectedOption.value = deployment.deploymentname
                                }
                            )
                            .padding(horizontal = 8.dp)

                    ) {
                        RadioButton(
                            selected = (deployment.deploymentname == selectedOption.value),
                            onClick = { selectedOption.value = deployment.deploymentname })
                        Text(
                            text = deployment.deploymentname,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        onDismissRequest = { onDismissRequest() },
        confirmButton = {
            TextButton(
                enabled = selectedOption.value.isNotEmpty(),
                onClick = {
                    onConfirmation(
                        deployments.find { d ->
                            d.deploymentname == selectedOption.value
                        }!!
                    )
                }) {
                Text("Open Program")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                selectedOption.value = ""
                onDismissRequest()
            }) {
                Text("Cancel")
            }
        }
    )
}
