package org.literacybridge.tbloaderandroid.ui.components

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import org.literacybridge.tbloaderandroid.App
import org.literacybridge.tbloaderandroid.util.Constants.Companion.LOG_TAG
import org.literacybridge.tbloaderandroid.view_models.UpdatesDownloaderViewModel

@Composable
fun AppUpdateDialog(
    viewModel: UpdatesDownloaderViewModel
) {
    val showDialog by viewModel.showDialog.collectAsState()

    LaunchedEffect("app-updates-check") {
        viewModel.checkUpdate()
    }

    if (showDialog) {
        AlertDialog(
            icon = { Icon(imageVector = Icons.Filled.Info, contentDescription = "Info") },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            title = { Text(text = "New Version Available") },
            text = {
                Text(
                    text = "Update [x.x.x] is available to download",
                    fontSize = 17.sp
                )
            },
            onDismissRequest = {},
            confirmButton = {
                TextButton(onClick = {
                    Toast.makeText(
                        App.context,
                        "Downloading new version, please wait",
                        Toast.LENGTH_LONG
                    ).show()
                    viewModel.downloadUpdate()
                }) {
                    Text("DOWNLOAD")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.showDialog.value = false
                }) {
                    Text("CANCEL")
                }
            }
        )
    }
}