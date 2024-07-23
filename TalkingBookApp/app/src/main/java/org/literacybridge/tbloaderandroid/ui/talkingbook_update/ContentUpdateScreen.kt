package org.literacybridge.tbloaderandroid.ui.talkingbook_update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import org.literacybridge.tbloaderandroid.ui.components.AppScaffold
import org.literacybridge.tbloaderandroid.ui.components.OperationCompleted
import org.literacybridge.tbloaderandroid.ui.components.TalkingBookOperationProgress
import org.literacybridge.tbloaderandroid.util.Constants.Companion.SCREEN_MARGIN
import org.literacybridge.tbloaderandroid.view_models.RecipientViewModel
import org.literacybridge.tbloaderandroid.view_models.TalkingBookViewModel
import org.literacybridge.tbloaderandroid.view_models.UserViewModel

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
            recipient = recipientViewModel.selectedRecipient.value!!,
            packages = recipientViewModel.getSelectedPackages()
        )
    }

    AppScaffold(title = "Updating Contenting", navController = navController) { contentPadding ->
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
                OperationCompleted(
                    result = viewModel.operationResult.value,
                    navController = navController,
                    successText = "Talking book updated successfully!"
                )
            }
        }
    }
}