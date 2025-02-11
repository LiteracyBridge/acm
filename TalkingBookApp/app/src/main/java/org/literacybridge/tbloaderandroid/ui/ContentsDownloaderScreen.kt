package org.literacybridge.tbloaderandroid.ui

import Screen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import org.literacybridge.tbloaderandroid.ui.components.AppScaffold
import org.literacybridge.tbloaderandroid.ui.home.StoragePermissionDialog
import org.literacybridge.tbloaderandroid.util.Constants.Companion.SCREEN_MARGIN
import org.literacybridge.tbloaderandroid.view_models.ContentDownloaderViewModel
import org.literacybridge.tbloaderandroid.view_models.ContentDownloaderViewModel.SyncState
import org.literacybridge.tbloaderandroid.view_models.UserViewModel

@Composable
fun ContentDownloaderScreen(
    navController: NavController, viewModel: ContentDownloaderViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {
    val syncState by viewModel.syncState.collectAsState()

    LaunchedEffect(key1 = "content-download") {
        viewModel.program = userViewModel.program.value!!
        viewModel.deployment = userViewModel.deployment.value!!
        viewModel.syncProgramContent()
    }

    LaunchedEffect(syncState) {
        if (syncState == SyncState.SUCCESS) {
            navController.navigate(Screen.HOME.name);
        }
    }

    // TODO: show error for no_content
    AppScaffold(
        title = "Downloading Program",
        navController = navController
    ) { innerPadding ->
        // Mass storage permission check
        StoragePermissionDialog()

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = SCREEN_MARGIN)
                .fillMaxSize(),
        ) {
            if (syncState == SyncState.DOWNLOADING) {
                CircularProgressIndicator(
                    progress = {
                        viewModel.downloadProgress.floatValue
                    },
                    modifier = Modifier.width(70.dp),
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.width(70.dp))
            }

            Text(
                modifier = Modifier
                    .padding(top = 50.dp, end = 2.dp, start = 2.dp)
                    .align(Alignment.CenterHorizontally),
                text = viewModel.displayText.value
            )
        }
    }
}
