package org.literacybridge.tbloaderandroid.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import org.literacybridge.tbloaderandroid.ui.components.AppScaffold
import org.literacybridge.tbloaderandroid.ui.components.OperationCompleted
import org.literacybridge.tbloaderandroid.ui.components.TalkingBookOperationProgress
import org.literacybridge.tbloaderandroid.util.Constants
import org.literacybridge.tbloaderandroid.view_models.TalkingBookViewModel
import org.literacybridge.tbloaderandroid.view_models.UserViewModel

@Composable
fun CollectStatisticsScreen(
    navController: NavController,
    viewModel: TalkingBookViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel(),
) {
    // Initialize talking book data collection when the screen is open
    LaunchedEffect("init-data-collection") {
        viewModel.operationType.value =
            TalkingBookViewModel.TalkingBookOperation.COLLECT_STATS_ONLY
        viewModel.collectUsageStatistics(
            user = userViewModel.user.value,
            deployment = userViewModel.deployment.value!!,
        )
    }

    AppScaffold(title = "Statistics Collection", navController = navController) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(Constants.SCREEN_MARGIN),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
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
                    successText = "Statistics collected successfully!"
                )
            }
        }
    }
}

