package org.literacybridge.tbloaderandroid.ui.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import org.literacybridge.tbloaderandroid.util.Constants.Companion.LOG_TAG

@Composable
fun StoragePermissionDialog() {
    val context = LocalContext.current
    val show = remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(LOG_TAG, "STORAGE PERMISSION GRANTED!")
            show.value = false
        } else {
            Log.d(LOG_TAG, "STORAGE PERMISSION DENIED")
        }
    }

    val startForResult =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_CANCELED) {
                // The all files access activity doesn't return any data (could be that the user
                // has to press the back button) after enabling the permission
                // We again have to recheck if we indeed have been given the permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    Environment.isExternalStorageManager()
                ) { // Access has been given
                    show.value = false
                    Log.d(LOG_TAG, "STORAGE PERMISSION GRANTED!")
                } else { // We still don't have permission, remind the user
                    Toast.makeText(
                        context,
                        "Please allow the app to access all files",
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
            }
        }

    LaunchedEffect("storage-permission-check") {
        // Manage external storage is available on Android 11+, request user to enable it if we don't have permission yet
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                show.value = true
            }
        } else { // Below Android 11, we have to request for write external storage permission
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_DENIED
            ) {
                show.value = true
            }
        }
    }

    if (show.value) {
        AlertDialog(
            icon = { Icon(imageVector = Icons.Filled.Info, contentDescription = "Info") },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            title = { Text(text = "Allow Files Access") },
            text = {
                Text(
                    text = "In other to update Talking Books, the app needs permission to manage external storage",
                    fontSize = 17.sp
                )
            },
            onDismissRequest = {},
            confirmButton = {
                TextButton(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val intent = Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        startForResult.launch(intent)
                    } else {
                        launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                }) {
                    Text("ENABLE")
                }
            },
            dismissButton = { }
        )
    }
}