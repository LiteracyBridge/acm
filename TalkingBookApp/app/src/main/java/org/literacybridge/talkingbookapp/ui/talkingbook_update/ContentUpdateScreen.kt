package org.literacybridge.talkingbookapp.ui.talkingbook_update

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import org.literacybridge.talkingbookapp.ui.components.AppScaffold
import org.literacybridge.talkingbookapp.ui.components.TalkingBookOperationProgress
import org.literacybridge.talkingbookapp.ui.theme.Green40
import org.literacybridge.talkingbookapp.util.Constants.Companion.SCREEN_MARGIN
import org.literacybridge.talkingbookapp.view_models.RecipientViewModel
import org.literacybridge.talkingbookapp.view_models.TalkingBookViewModel
import org.literacybridge.talkingbookapp.view_models.UserViewModel

@Composable
fun ContentUpdateScreen(
    navController: NavController,
    viewModel: TalkingBookViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel(),
    recipientViewModel: RecipientViewModel = viewModel()
) {
    LaunchedEffect("init-tb-content-update") {
        viewModel.updateDevice(
            deployment = userViewModel.deployment.value!!,
            user = userViewModel.user.value,
            recipient = recipientViewModel.selectedRecipient.value!!
        )
    }

    AppScaffold(title = "Updating Contenting", navController = navController, bottomBar = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SCREEN_MARGIN),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                enabled = !viewModel.isOperationInProgress.value,
                onClick = {
                    // Disconnect device and wait for new connection
//                    Usb.getInstance().forceDisconnectDevice()
//                    showDialog.value = true
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
                border = BorderStroke(1.dp, Green40),
                enabled = !viewModel.isOperationInProgress.value,

                ) {
                Text("I'm Finished")
            }
        }
    }) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(SCREEN_MARGIN),
//                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            if (viewModel.isOperationInProgress.value) {
                TalkingBookOperationProgress(
                    operationStep = viewModel.operationStep.value,
                    operationStepDetail = viewModel.operationStepDetail.value,
                    isOperationInProgress = viewModel.isOperationInProgress.value,
                )
            } else {
                Text("Talking book updated successfully!")
//                OperationCompleted(result = viewModel.operationResult.value)
            }
        }
    }
}