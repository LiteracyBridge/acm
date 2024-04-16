package org.literacybridge.talkingbookapp.ui.talkingbook_update

import android.util.Log
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.literacybridge.talkingbookapp.ui.components.AppScaffold
import org.literacybridge.talkingbookapp.ui.components.SearchableExpandedDropDownMenu
import org.literacybridge.talkingbookapp.util.Constants.Companion.LOG_TAG
import org.literacybridge.talkingbookapp.util.Constants.Companion.SCREEN_MARGIN
import org.literacybridge.talkingbookapp.view_models.TalkingBookViewModel
import org.literacybridge.talkingbookapp.view_models.UserViewModel

@Composable
fun RecipientScreen(
    navController: NavController,
    viewModel: TalkingBookViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel(),
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sports =
        mutableListOf("Basketball", "Rugby", "Football", "MMA", "Motorsport", "Snooker", "Tennis")

    Log.d(LOG_TAG, "${viewModel.recipients()}")

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
                listOfItems = viewModel.districts(),
                modifier = Modifier.fillMaxWidth(),
                onDropDownItemSelected = { item ->
                    viewModel.selectedDistrict.value = item
                    Toast.makeText(context, item, Toast.LENGTH_SHORT).show()
                },
                enable = true,// controls the enabled state of the OutlinedTextField
//                colors = TextFieldDefaults.textFieldColors(
//                    backgroundColor = MaterialTheme.colorScheme.color1,
//                ),
                placeholder = {
                    Text("Select recipient")
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
                listOfItems = viewModel.communities(),
                modifier = Modifier.fillMaxWidth(),
                onDropDownItemSelected = { item ->
                    viewModel.selectedCommunity.value = item
                    Toast.makeText(context, item, Toast.LENGTH_SHORT).show()
                },
                enable = true,// controls the enabled state of the OutlinedTextField
//                colors = TextFieldDefaults.textFieldColors(
//                    backgroundColor = MaterialTheme.colorScheme.color1,
//                ),
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
                listOfItems = viewModel.groups(),
                modifier = Modifier.fillMaxWidth(),
                onDropDownItemSelected = { item -> // Returns the item selected in the dropdown
                    viewModel.selectedGroup.value = item
                    Toast.makeText(context, item, Toast.LENGTH_SHORT).show()
                },
                enable = true,// controls the enabled state of the OutlinedTextField
//                colors = TextFieldDefaults.textFieldColors(
//                    backgroundColor = MaterialTheme.colorScheme.color1,
//                ),
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