package org.literacybridge.talkingbookapp.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import org.literacybridge.talkingbookapp.ui.components.AppScaffold
import org.literacybridge.talkingbookapp.ui.theme.Green40
import org.literacybridge.talkingbookapp.util.device_manager.Usb
import org.literacybridge.talkingbookapp.view_models.TalkingBookViewModel
import org.literacybridge.talkingbookapp.view_models.UserViewModel

@Composable
fun CollectStatisticsScreen(
    navController: NavController,
    viewModel: TalkingBookViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel(),
) {
    val showDialog = remember { mutableStateOf(false) }
    val device by viewModel.usbDevice.collectAsStateWithLifecycle()

    // Initialize talking book data collection when the screen is open
    LaunchedEffect("init-data-collection") {
        viewModel.operationType.value =
            TalkingBookViewModel.TalkingBookOperation.COLLECT_STATS_ONLY
        viewModel.collectUsageStatistics(
            user = userViewModel.user.value,
            deployment = userViewModel.deployment.value!!,
        )
    }

    // If new device is detected while the dialog is open, we assume a different talking book has
    // been connected. Proceed to collect stats
    if (showDialog.value && device?.deviceName != null) {
        showDialog.value = false
        Toast.makeText(
            LocalContext.current,
            "New talking book detected, statistics collection in progress...",
            Toast.LENGTH_LONG
        ).show()

        LaunchedEffect("new-device-connection") {
            viewModel.operationType.value =
                TalkingBookViewModel.TalkingBookOperation.COLLECT_STATS_ONLY
            viewModel.collectUsageStatistics(
                user = userViewModel.user.value,
                deployment = userViewModel.deployment.value!!,
            )
        }
    }

    AppScaffold(title = "Statistics Collection", navController = navController) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (viewModel.isOperationInProgress.value) {
                OperationProgress(
                    operationStep = viewModel.operationStep.value,
                    operationStepDetail = viewModel.operationStepDetail.value,
                    isOperationInProgress = viewModel.isOperationInProgress.value,
                )
            } else {
                OperationCompleted(result = viewModel.operationResult.value)
            }

            if (!viewModel.isOperationInProgress.value) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp, horizontal = 15.dp)
                ) {
                    Button(
                        onClick = {
                            // Disconnect device and wait for new connection
                            Usb.getInstance().forceDisconnectDevice()
                            showDialog.value = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 5.dp)
                    ) {
                        Text("Update another Talking Book")
                    }

                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Green40)
                    ) {
                        Text("I'm Finished")
                    }
                }
            }

        }
    }

    if (showDialog.value) {
        ConnectDeviceDialog(
            onDismissRequest = {
                showDialog.value = false
                navController.popBackStack()
            }
        )
    }
}

@Composable
fun OperationProgress(
    operationStep: String,
    operationStepDetail: String,
    isOperationInProgress: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 15.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .width(80.dp)
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(top = 30.dp, bottom = 60.dp),
            strokeWidth = 5.dp,
        )

        Text(
            text = operationStep,
            style = TextStyle(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(bottom = 15.dp)
        )
        Text(
            text = operationStepDetail,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(bottom = 15.dp)
        )

        if (isOperationInProgress) {
            Text(
                text = "DO NOT DISCONNECT THE DEVICE!",
                style = TextStyle(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Red,
                    fontSize = 17.sp
                ),
                modifier = Modifier.padding(top = 70.dp, bottom = 20.dp)
            )
        }
    }
}

@Composable
fun OperationCompleted(result: TalkingBookViewModel.OperationResult) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 15.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        CircularProgressIndicator(
            modifier = Modifier
                .width(80.dp)
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(top = 100.dp, bottom = 60.dp),
            strokeWidth = 5.dp,
            progress = 1.0f
        )

        if (result == TalkingBookViewModel.OperationResult.Success) {
            Text(
                text = "Statistics collected successfully!",
                style = TextStyle(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp
                ),
                modifier = Modifier.padding(top = 20.dp)
            )
        } else {
            Text(
                text = "An error occurred while collecting statistics from the talking book.\nPlease reconnect the device and try again",
                style = TextStyle(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Red,
                    fontSize = 17.sp
                ),
                modifier = Modifier.padding(top = 20.dp)
            )
        }
    }
}

@Composable
fun ConnectDeviceDialog(
    onDismissRequest: () -> Unit,
) {
    val selectedOption = remember { mutableStateOf("") }

    AlertDialog(
//        title = { Text(text = "Connect Another Talking Book") },
        text = {
            Column(
                modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(strokeWidth = 40.dp)

                Text(
                    text = "No device detected, waiting for connection...",
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        },
        onDismissRequest = { onDismissRequest() },
        confirmButton = {},
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