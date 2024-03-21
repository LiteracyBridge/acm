package org.literacybridge.talkingbookapp.ui

import Screen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import org.literacybridge.talkingbookapp.view_models.ContentDownloaderViewModel
import org.literacybridge.talkingbookapp.view_models.ContentDownloaderViewModel.SyncState
import org.literacybridge.talkingbookapp.view_models.UserViewModel

@Composable
fun ContentDownloaderScreen(
    navController: NavController, viewModel: ContentDownloaderViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {
    val syncState by viewModel.syncState.collectAsState()

    LaunchedEffect(key1 = "content-download") {
        viewModel.program = userViewModel.program.value!!
        viewModel.deployment = userViewModel.deployment.value!!
        viewModel.syncProgramContent(navController)
    }

    LaunchedEffect(syncState) {
        if (syncState == SyncState.SUCCESS) {
            navController.navigate(Screen.HOME.name);
        }
    }

    // TODO: show erorr for no_content
    return Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (syncState == SyncState.DOWNLOADING) {
            CircularProgressIndicator(
                modifier = Modifier.width(70.dp),
                progress = viewModel.downloadProgress.floatValue
            )
        } else {
            CircularProgressIndicator(modifier = Modifier.width(70.dp))
        }

        Text(
            modifier = Modifier.padding(top = 50.dp),
            text = viewModel.displayText.value
        )
    }
}