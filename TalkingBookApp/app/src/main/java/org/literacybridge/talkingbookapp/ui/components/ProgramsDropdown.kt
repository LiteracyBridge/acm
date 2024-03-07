package org.literacybridge.talkingbookapp.ui.components
//
//import android.util.Log
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Info
//import androidx.compose.material3.AlertDialog
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.Icon
//import androidx.compose.material3.Text
//import androidx.compose.material3.TextButton
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.ui.graphics.vector.ImageVector
//import org.literacybridge.talkingbookapp.helpers.LOG_TAG
//
//@Composable
//fun ProgramsDropdown(visible: Boolean) {
//    // ...
//    val openAlertDialog = remember { mutableStateOf(true) }
//
//    Log.d(LOG_TAG, "Re-rendered dialog")
//    // ...
//    when {
//        // ...
//        openAlertDialog.value -> {
//            AlertDialogExample(
//                onDismissRequest = { openAlertDialog.value = false },
//                onConfirmation = {
//                    openAlertDialog.value = false
//                    println("Confirmation registered") // Add logic here to handle confirmation.
//                },
//                dialogTitle = "Alert dialog example",
//                dialogText = "This is an example of an alert dialog with buttons.",
//                icon = Icons.Default.Info
//            )
//        }
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun AlertDialogExample(
//    onDismissRequest: () -> Unit,
//    onConfirmation: () -> Unit,
//) {
//    AlertDialog(
//        icon = {
//            Icon(Icons.Default.Info, contentDescription = "Example Icon")
//        },
//        title = {
//            Text(text = "Select Program")
//        },
//        text = {
//            Text(text =
//             "This is an example of an alert dialog with buttons.",
//            )
//        },
//        onDismissRequest = {
//            onDismissRequest()
//        },
//        confirmButton = {
//            TextButton(
//                onClick = {
//                    onConfirmation()
//                }
//            ) {
//                Text("Confirm")
//            }
//        },
//        dismissButton = {
//            TextButton(
//                onClick = {
//                    onDismissRequest()
//                }
//            ) {
//                Text("Dismiss")
//            }
//        }
//    )
//}