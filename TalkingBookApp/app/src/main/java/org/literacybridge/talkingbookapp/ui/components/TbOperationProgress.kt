package org.literacybridge.talkingbookapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.CircularProgressIndicator
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 15.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            modifier = Modifier
                .width(80.dp)
                .fillMaxWidth()
                .height(60.dp)
//                .wrapContentHeight()
                .padding(top = 30.dp, bottom = 60.dp),
//            strokeWidth = 5.dp,
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