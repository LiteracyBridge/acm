package org.literacybridge.talkingbookapp.ui.talkingbook_update

import Screen
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import org.literacybridge.talkingbookapp.ui.components.AppScaffold
import org.literacybridge.talkingbookapp.ui.components.SearchableExpandedDropDownMenu
import org.literacybridge.talkingbookapp.util.Constants.Companion.SCREEN_MARGIN
import org.literacybridge.talkingbookapp.view_models.TalkingBookViewModel

@Composable
fun RecipientScreen(
    navController: NavController,
    viewModel: TalkingBookViewModel = viewModel(),
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect("load-recipients") {
        // Populates the recipients list if empty
        if (viewModel.recipients.isEmpty()) {
            viewModel.recipients.addAll(viewModel.app.programSpec!!.recipients)
            viewModel.districts.addAll(viewModel.app.programSpec!!.recipients.map { it.district }.distinct())
        }
    }

    AppScaffold(title = "Choose Recipient", navController = navController,
        bottomBar = {
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = viewModel.selectedRecipient.value != null,
                    onClick = {
                        if (viewModel.selectedRecipient.value == null) {
                            Toast.makeText(context, "No recipient selected!", Toast.LENGTH_LONG)
                                .show()
                            return@Button
                        }

                        navController.navigate(Screen.CONTENT_VERIFICATION.name)
                    }) {
                    Text("Next")
                }
            }
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(SCREEN_MARGIN)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = "Select District")
            SearchableExpandedDropDownMenu(
                listOfItems = viewModel.districts,
                modifier = Modifier.fillMaxWidth(),
                onDropDownItemSelected = { item ->
                    viewModel.selectedDistrict.value = item
                    viewModel.updateSelectedRecipient()
                },
                enable = true,
                placeholder = {
                    Text("Select district")
                },
                dropdownItem = { name ->
                    Text(name)
                },
                defaultItem = {
                    viewModel.districts.first()
                }
            ) {}

            Text(text = "Select Community")
            SearchableExpandedDropDownMenu(
                listOfItems = if (viewModel.selectedDistrict.value != null) {
                    viewModel.recipients.map { it.communityname }.distinct()
                } else {
                    viewModel.recipients.filter { it.district.equals(viewModel.selectedDistrict.value) }
                        .map { it.communityname }.distinct()
                },
                modifier = Modifier.fillMaxWidth(),
                onDropDownItemSelected = { item ->
                    viewModel.selectedCommunity.value = item
                    viewModel.updateSelectedRecipient()
                },
                enable = !viewModel.selectedDistrict.value.isNullOrEmpty(),
                placeholder = {
                    Text("Select community")
                },
                dropdownItem = { name ->
                    Text(name)
                },
                defaultItem = {
                    ""
                }
            ) {
            }

            Text(text = "Select Group")
            SearchableExpandedDropDownMenu(
                listOfItems = if (viewModel.selectedCommunity.value != null) {
                    viewModel.recipients.map { it.groupname }.distinct()
                } else {
                    viewModel.recipients.filter { it.communityname.equals(viewModel.selectedCommunity.value) }
                        .map { it.groupname }.distinct()
                },
                modifier = Modifier.fillMaxWidth(),
                onDropDownItemSelected = { item -> // Returns the item selected in the dropdown
                    viewModel.selectedGroup.value = item
                    viewModel.updateSelectedRecipient()
                },
                enable = !viewModel.selectedCommunity.value.isNullOrEmpty(),
                placeholder = {
                    Text("Select group")
                },
                dropdownItem = { name ->
                    Text(name)
                },
                defaultItem = {
                    ""
                }
            ) {
            }


        }
    }
}