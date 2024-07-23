package org.literacybridge.tbloaderandroid.ui.components

import Screen
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import org.literacybridge.tbloaderandroid.view_models.TalkingBookViewModel

@Composable
fun TalkingBookOperationProgress(
    operationStep: String,
    operationStepDetail: String,
    isOperationInProgress: Boolean,
) {
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .fillMaxHeight()
//    ) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        if (isOperationInProgress) {
            Text(
                text = "DO NOT DISCONNECT THE DEVICE!",
                style = TextStyle(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Red,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(bottom = 15.dp, top = 5.dp)
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
        Text(
            text = operationStep,
            style = TextStyle(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                textAlign = TextAlign.Left
            ),
            modifier = Modifier.padding(bottom = 15.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState(0), reverseScrolling = true),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = operationStepDetail,
                style = TextStyle(
                    fontSize = 13.sp,
                    textAlign = TextAlign.Left
                ),
                modifier = Modifier.padding(bottom = 15.dp)
            )
        }
    }
}

@Composable
fun OperationCompleted(
    successText: String,
    result: TalkingBookViewModel.OperationResult,
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 15.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        CircularProgressIndicator(
            progress = {
                1.0f
            },
            modifier = Modifier
                .width(80.dp)
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(top = 100.dp, bottom = 60.dp),
            strokeWidth = 5.dp,
        )

        if (result == TalkingBookViewModel.OperationResult.Success) {
            Text(
                text = successText,
                style = TextStyle(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp
                ),
                modifier = Modifier.padding(top = 20.dp)
            )
            Button(
                modifier = Modifier.padding(vertical = 20.dp),
                onClick = {
                    navController.popBackStack(Screen.HOME.name, false)
                }
            ) {
                Text("Done")
            }
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