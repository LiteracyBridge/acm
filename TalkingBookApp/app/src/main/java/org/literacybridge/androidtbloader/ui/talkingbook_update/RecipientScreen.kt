package org.literacybridge.androidtbloader.ui.talkingbook_update

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import org.literacybridge.androidtbloader.ui.components.AppScaffold
import org.literacybridge.androidtbloader.ui.components.SearchableExpandedDropDownMenu
import org.literacybridge.androidtbloader.util.Constants.Companion.SCREEN_MARGIN
import org.literacybridge.androidtbloader.view_models.RecipientViewModel
import org.literacybridge.androidtbloader.view_models.TalkingBookViewModel

@Composable
fun RecipientScreen(
    navController: NavController,
    recipientViewModel: RecipientViewModel = viewModel(),
    viewModel: TalkingBookViewModel = viewModel(),
) {
    val recipients by recipientViewModel.recipients.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect("load-recipients") {
        recipientViewModel.fromProgramSpec(viewModel.app.programSpec!!)
        recipientViewModel.fromTalkingBook(viewModel.talkingBookDeviceInfo.value)
    }

    AppScaffold(title = "Choose Recipient", navController = navController,
        bottomBar = {
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = recipientViewModel.selectedRecipient.value != null,
                    onClick = {
                        if (recipientViewModel.selectedRecipient.value == null) {
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
            Text(
                text = "Select District",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            SearchableExpandedDropDownMenu(
                listOfItems = recipientViewModel.districts,
                modifier = Modifier.fillMaxWidth(),
                onDropDownItemSelected = { item ->
                    recipientViewModel.selectedDistrict.value = item
                    recipientViewModel.updateSelectedRecipient()
                },
                enable = true,
                placeholder = {
                    Text("Select district")
                },
                dropdownItem = { name ->
                    Text(name)
                },
                defaultItem = {
                    recipientViewModel.selectedRecipient.value?.district ?: recipientViewModel.districts.first()
                }
            ) {}

            Text(
                text = "Select Community",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp, top = 15.dp)
            )
            SearchableExpandedDropDownMenu(
                listOfItems = recipients.filter {
                    it.district.equals(
                        recipientViewModel.selectedDistrict.value ?: ""
                    )
                }.map { it.communityname }.distinct(),
                modifier = Modifier.fillMaxWidth(),
                onDropDownItemSelected = { item ->
                    recipientViewModel.selectedCommunity.value = item
                    recipientViewModel.updateSelectedRecipient()
                },
                enable = !recipientViewModel.selectedDistrict.value.isNullOrEmpty(),
                placeholder = {
                    Text("Select community")
                },
                dropdownItem = { name ->
                    Text(name)
                },
                defaultItem = {
                    recipientViewModel.selectedRecipient.value?.communityname ?: ""
                }
            ) {
            }

            Text(
                text = "Select Group",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp, top = 15.dp)
            )
            SearchableExpandedDropDownMenu(
                listOfItems = recipients.filter {
                    it.communityname.equals(
                        recipientViewModel.selectedCommunity.value ?: ""
                    )
                }.map { it.groupname }.distinct(),
                modifier = Modifier.fillMaxWidth(),
                onDropDownItemSelected = { item -> // Returns the item selected in the dropdown
                    recipientViewModel.selectedGroup.value = item
                    recipientViewModel.updateSelectedRecipient()
                },
                enable = !recipientViewModel.selectedCommunity.value.isNullOrEmpty(),
                placeholder = {
                    Text("Select group")
                },
                dropdownItem = { name ->
                    Text(name)
                },
                defaultItem = {
                    recipientViewModel.selectedRecipient.value?.groupname ?: ""
                }
            ) {
            }


        }
    }
}