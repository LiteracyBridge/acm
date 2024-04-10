package org.literacybridge.talkingbookapp.ui.talkingbook_update

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
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
    val context = LocalContext.current
    val sports =
        mutableListOf("Basketball", "Rugby", "Football", "MMA", "Motorsport", "Snooker", "Tennis")

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
                listOfItems = sports,
                modifier = Modifier.fillMaxWidth(),
                onDropDownItemSelected = { item -> // Returns the item selected in the dropdown
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
        }
    }
}