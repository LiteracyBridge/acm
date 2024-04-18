package org.literacybridge.talkingbookapp.ui.talkingbook_update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.literacybridge.talkingbookapp.ui.components.AppScaffold
import org.literacybridge.talkingbookapp.util.Constants.Companion.SCREEN_MARGIN
import org.literacybridge.talkingbookapp.view_models.TalkingBookViewModel
import org.literacybridge.talkingbookapp.view_models.UserViewModel

@Composable
fun ContentVerificationScreen(
    navController: NavController,
    viewModel: TalkingBookViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel(),
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    AppScaffold(title = "Verify Content", navController = navController, bottomBar = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SCREEN_MARGIN),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navController.popBackStack() }
            ) {
                Text("Change Recipient")
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = viewModel.selectedRecipient.value != null,
                onClick = {
                    scope.launch {
                        viewModel.updateDevice(
                            deployment = userViewModel.deployment.value!!,
                            user = userViewModel.user.value,
                        )
                    }
                }
            ) {
                Text("Update Device")
            }
        }
    }) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(SCREEN_MARGIN)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            Text("Verify the content deployment")

            ElevatedCard(
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 6.dp
                ),
                modifier = Modifier
                    .size(width = 240.dp, height = 100.dp)
            ) {
                Text(
                    text = "After Update",
                    modifier = Modifier
                        .padding(16.dp),
                    textAlign = TextAlign.Center,
                    style = TextStyle(fontWeight = FontWeight.Bold)
                )
            }
            ElevatedCard(
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 6.dp
                ),
                modifier = Modifier
                    .size(width = 240.dp, height = 100.dp)
            ) {
                Text(
                    text = "Before Update",
                    modifier = Modifier
                        .padding(16.dp),
                    textAlign = TextAlign.Center,
                    style = TextStyle(fontWeight = FontWeight.Bold)
                )
            }


        }
    }
}