package org.literacybridge.talkingbookapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import org.literacybridge.talkingbookapp.view_models.ContentDownloaderViewModel
import org.literacybridge.talkingbookapp.view_models.UserViewModel

@Composable
fun ContentDownloaderScreen(
    navController: NavController, viewModel: ContentDownloaderViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {
    LaunchedEffect(key1 = "content-download") {
        viewModel.syncProgramContent(
            userViewModel.program.value!!,
            userViewModel.deployment.value!!,
            navController
        )
    }

    return Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (viewModel.syncState.value == "comparing") {
            CircularProgressIndicator(modifier = Modifier.width(100.dp))
        } else {
            CircularProgressIndicator(
                modifier = Modifier.width(100.dp),
                progress = viewModel.downloadProgress.value
            )
        }

        Text("Downloading program content, please wait...")
    }
}