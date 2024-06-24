package org.literacybridge.talkingbookapp.ui.talkingbook_update

import Screen
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import org.literacybridge.core.spec.Recipient
import org.literacybridge.talkingbookapp.ui.components.AppScaffold
import org.literacybridge.talkingbookapp.util.Constants
import org.literacybridge.talkingbookapp.view_models.RecipientViewModel
import org.literacybridge.talkingbookapp.view_models.UserViewModel

@Composable
fun ContentVariantScreen(
    navController: NavController,
    userViewModel: UserViewModel = viewModel(),
    recipientViewModel: RecipientViewModel = viewModel(),
) {
    val languages = remember { userViewModel.program.value?.languages }
    var optionselected = ""
    val pubCsv = createPropertiesMap()
    val recipient = Recipient(pubCsv)
    val defaultRecipient = recipientViewModel.defaultRecipient

    AppScaffold(title = "Available Content Packages", navController = navController, bottomBar = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constants.SCREEN_MARGIN),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    //navController.popBackStack()
                    Log.i("option selected", optionselected)
                    defaultRecipient.value = recipient
                    navController.navigate(Screen.CONTENT_VERIFICATION.name)
                }
            ) {
                Text("Select")
            }
        }

    }) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(Constants.SCREEN_MARGIN)
                .verticalScroll(rememberScrollState())
        ) {
            val (selectedOption, onOptionSelected) = remember { mutableStateOf(languages!![1] ) }
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
            ) {
                languages?.forEach { lang ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (lang == selectedOption),
                                onClick = {
                                    onOptionSelected(lang)
                                }
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (lang == selectedOption),
                            onClick = { onOptionSelected(lang) },
                        )
                        Text(
                            text = lang,
                            style = typography.bodyMedium.merge(),
                            modifier = Modifier.padding(start = 16.dp)
                        )
                        optionselected = selectedOption
                    }
                }
            }
        }

    }
}

@Composable
fun createPropertiesMap() : Map<String, String> {
    val pubCsv = hashMapOf<String, String>()
    pubCsv["recipientid"] = "12"
    pubCsv["district"] = "Default District"
    pubCsv["communityname"] = "WA"
    pubCsv["groupname"] = "Default Group"
    pubCsv["numtbs"] = "10"
    pubCsv["agent"] = "Agent"
    pubCsv["language"] = "en"
    pubCsv["variant"] = "variant"
    return pubCsv
}
