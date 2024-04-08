package org.literacybridge.talkingbookapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import org.literacybridge.talkingbookapp.ui.components.AppScaffold
import org.literacybridge.talkingbookapp.ui.theme.Green40
import org.literacybridge.talkingbookapp.view_models.TalkingBookViewModel
import org.literacybridge.talkingbookapp.view_models.UserViewModel

@Composable
fun CollectStatisticsScreen(
    navController: NavController,
    viewModel: TalkingBookViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {

    // Initialize talking book data collection when the screen is open
    LaunchedEffect("init-data-collection") {
        viewModel.operationType.value =
            TalkingBookViewModel.TalkingBookOperation.COLLECT_STATS_ONLY
        viewModel.collectUsageStatistics(
            user = userViewModel.user.value,
            deployment = userViewModel.deployment.value!!,
            navController = navController
        )
    }

    AppScaffold(title = "Collect Statistics", navController = navController) { contentPadding ->
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp, horizontal = 15.dp)
            ) {
                if (!viewModel.isOperationInProgress.value) {
                    Button(
                        onClick = { /*TODO*/ },
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
}

@Composable
fun OperationProgress(
    operationStep: String,
    operationStepDetail: String,
    isOperationInProgress: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
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
            .fillMaxWidth()
            .fillMaxHeight()
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

