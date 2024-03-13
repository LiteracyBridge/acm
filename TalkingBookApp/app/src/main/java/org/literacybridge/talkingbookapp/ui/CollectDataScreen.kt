package org.literacybridge.talkingbookapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import org.literacybridge.talkingbookapp.ui.components.AppScaffold
import org.literacybridge.talkingbookapp.view_models.TalkingBookViewModel
import org.literacybridge.talkingbookapp.view_models.UserViewModel

@Composable
fun CollectDataScreen(
    navController: NavController,
    viewModel: TalkingBookViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {
    AppScaffold(title = "Collect Data", navController = navController) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(1f, false)
            ) {
                Text("Connect the Talking Book")
            }

            Box {
                Column {
                    Button(onClick = { /*TODO*/ }) {
                        Text("Update another Talking Book")
                    }
                    Button(onClick = { /*TODO*/ }) {
                        Text("I'm Finished")
                    }
                }
            }

        }
    }
}