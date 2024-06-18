package org.literacybridge.talkingbookapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
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
                modifier = Modifier.padding(bottom = 15.dp, top=5.dp)
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