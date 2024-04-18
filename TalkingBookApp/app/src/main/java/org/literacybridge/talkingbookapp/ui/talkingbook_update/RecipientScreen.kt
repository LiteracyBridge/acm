package org.literacybridge.talkingbookapp.ui.talkingbook_update

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.literacybridge.talkingbookapp.ui.components.AppScaffold
import org.literacybridge.talkingbookapp.ui.components.SearchableExpandedDropDownMenu
import org.literacybridge.talkingbookapp.util.Constants.Companion.SCREEN_MARGIN
import org.literacybridge.talkingbookapp.view_models.TalkingBookViewModel
import org.literacybridge.talkingbookapp.view_models.UserViewModel

@Composable
fun RecipientScreen(
    navController: NavController,
    viewModel: TalkingBookViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel(),
) {
//    val recipients = remember { viewModel.recipients() ?: emptyList() }
//    val districts = remember { mutableListOf<String>() }
//    val recipients = remember { mutableStateOf<RecipientList?>(null) }
    val selectedDistrict = remember { mutableStateOf<String?>(null) }
    val selectedCommunity = remember { mutableStateOf<String?>(null) }
    val selectedGroup = remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sports =
        mutableListOf("Basketball", "Rugby", "Football", "MMA", "Motorsport", "Snooker", "Tennis")

//    LaunchedEffect("load-recipients") {
//        recipients.value = viewModel.recipients()
//    }

//    Log.d(LOG_TAG, "${viewModel.recipients()}")

    AppScaffold(title = "Choose Recipient", navController = navController) { contentPadding ->
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
                    selectedDistrict.value = item
                    Toast.makeText(context, item, Toast.LENGTH_SHORT).show()
                },
                enable = true,// controls the enabled state of the OutlinedTextField
//                colors = TextFieldDefaults.textFieldColors(
//                    backgroundColor = MaterialTheme.colorScheme.color1,
//                ),
                placeholder = {
                    Text("Select district")
                },
                dropdownItem = { name ->
                    Text(name)
                },
                defaultItem = {
                    sports.first()
                },
                onSearchTextFieldClicked = {
//                    Log.d(LOG_TAG, "$it")
//                    return true
                }
            )

            Text(text = "Select Community")
            SearchableExpandedDropDownMenu(
                listOfItems = if (selectedDistrict.value != null) {
                    viewModel.recipients.map { it.communityname }.distinct()
                } else {
                    viewModel.recipients.filter { it.district.equals(selectedDistrict.value) }
                        .map { it.communityname }.distinct()
                },
                modifier = Modifier.fillMaxWidth(),
                onDropDownItemSelected = { item ->
                    selectedCommunity.value = item
                    Toast.makeText(context, item, Toast.LENGTH_SHORT).show()
                },
                enable = !selectedDistrict.value.isNullOrEmpty(),
                placeholder = {
                    Text("Select community")
                },
                dropdownItem = { name ->
                    Text(name)
                },
                defaultItem = {
                    sports.first()
                },
                onSearchTextFieldClicked = {
//                    Log.d(LOG_TAG, "$it")
//                    return true
                }
            )

            Text(text = "Select Group")
            SearchableExpandedDropDownMenu(
                listOfItems = if (selectedCommunity.value != null) {
                    viewModel.recipients.map { it.groupname }.distinct()
                } else {
                    viewModel.recipients.filter { it.communityname.equals(selectedCommunity.value) }
                        .map { it.groupname }.distinct()
                },
                modifier = Modifier.fillMaxWidth(),
                onDropDownItemSelected = { item -> // Returns the item selected in the dropdown
                    selectedGroup.value = item
                    Toast.makeText(context, item, Toast.LENGTH_SHORT).show()
                },
                enable = !selectedCommunity.value.isNullOrEmpty(),
                placeholder = {
                    Text("Select group")
                },
                dropdownItem = { name ->
                    Text(name)
                },
                defaultItem = {
                    sports.first()
                },
                onSearchTextFieldClicked = {
//                    Log.d(LOG_TAG, "$it")
//                    return true
                }
            )

            Button(
//                enabled = viewModel.getSelectedRecipient() != null,
                onClick = {
                    val selectedRecipient = if (!selectedGroup.value.isNullOrBlank()) {
                        viewModel.recipients.find {
                            it.groupname.equals(
                                selectedGroup.value,
                                true
                            ) && it.communityname.equals(
                                selectedCommunity.value,
                                true
                            ) && it.district.equals(
                                selectedDistrict.value,
                                true
                            )
                        }
                    } else if (!selectedCommunity.value.isNullOrBlank()) {
                        viewModel.recipients.find {
                            it.communityname.equals(
                                selectedCommunity.value,
                                true
                            ) && it.district.equals(selectedDistrict.value, true)
                        }
                    } else if (!selectedDistrict.value.isNullOrBlank()) {
                        viewModel.recipients.find {
                            it.district.equals(
                                selectedDistrict.value,
                                true
                            )
                        }
                    } else {
                        null
                    }

                    if (selectedRecipient == null) {
                        Toast.makeText(context, "No recipient selected!", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    scope.launch {
                        viewModel.updateDevice(
                            deployment = userViewModel.deployment.value!!,
                            user = userViewModel.user.value,
                            navController = navController
                        )
                    }
                }) {
                Text("Next")
            }
        }
    }
}