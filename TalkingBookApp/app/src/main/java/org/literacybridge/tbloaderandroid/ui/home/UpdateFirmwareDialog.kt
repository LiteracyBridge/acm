package org.literacybridge.tbloaderandroid.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import org.literacybridge.tbloaderandroid.view_models.TalkingBookViewModel

@Composable
fun UpdateFirmware(
    status: TalkingBookViewModel.OperationResult,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        title = { Text(text = "Updating Firmware") },
        text = {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
            ) {
                if (status != TalkingBookViewModel.OperationResult.Success) {
                    LinearProgressIndicator()
                    Text(
                        "Update in progress, please wait....",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { 1f },
                    )
                    Text(
                        "Firmware updated successfully!",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        onDismissRequest = {},
        confirmButton = {},
        dismissButton = {
            if (status == TalkingBookViewModel.OperationResult.Success || status == TalkingBookViewModel.OperationResult.Failure) {
                TextButton(onClick = {
                    onDismissRequest()
                }) {
                    Text("Okay")
                }
            }
        }
    )
}