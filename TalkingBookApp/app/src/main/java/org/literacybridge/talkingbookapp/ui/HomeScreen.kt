import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import org.literacybridge.talkingbookapp.R
import org.literacybridge.talkingbookapp.helpers.LOG_TAG
import org.literacybridge.talkingbookapp.view_models.TalkingBookViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: TalkingBookViewModel = viewModel()
) {
    // Retrieve data from next screen
    val msg =
        navController.currentBackStackEntry?.savedStateHandle?.get<String>("msg")

    val uiState by viewModel.deviceState.collectAsStateWithLifecycle()
    val showProgramsDialog = remember { mutableStateOf(false) }

    Log.d(LOG_TAG, "Device updated/set ${uiState.device}");

    Column(
        Modifier.fillMaxSize(),
//        Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .weight(1f, false)
        ) {
            Text("Connect the Talking Book")
            Row {
                Text("Instructions", style = TextStyle(fontWeight = FontWeight.Bold))
                Text("Press 'tree', 'table' & “plus” buttons at the same time to connect")
            }
            Text("${uiState.device?.deviceName}")
            Image(
                painter = painterResource(R.drawable.tb_table_image),
                contentDescription = "Tree"
//                contentDescription = stringResource(id = R.string.bus_content_description)
            )
            Box(
                modifier = Modifier
                    .border(width = 2.dp, color = Color.Blue)
                    .align(Alignment.CenterHorizontally)
            ) {
                Text(
                    "[Display TB info once connected]\n" +
                            "TB ID\n" +
                            "Current recipient\n" +
                            "Current content deployment\n"
                )

            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("What would you like to do?", modifier = Modifier.padding(vertical = 4.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = {
                    showProgramsDialog.value = true;
                    /*TODO*/
                }) {
                    Text("Collect Data")
                }
                Button(onClick = { /*TODO*/ }) {
                    Text("Update Talking Book")
                }
            }
        }
//        Button(onClick = { navController.navigate("secondscreen") }) {
//            Text("Go to next screen")
//        }
//        Spacer(modifier = Modifier.height(8.dp))
//        msg?.let {
//            Text(it)
//        }
    }

    if (showProgramsDialog.value) {
        AlertDialogExample(
            onDismissRequest = { showProgramsDialog.value = false },
            onConfirmation = {
                showProgramsDialog.value = false
                println("Confirmation registered") // Add logic here to handle confirmation.
            },
        )
//        return ProgramsDropdown(visible = true);
    }
//    return ProgramsDropdown();
}

@Composable
fun AlertDialogExample(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
) {
    AlertDialog(
        icon = {
            Icon(Icons.Default.Info, contentDescription = "Example Icon")
        },
        title = {
            Text(text = "Select Program")
        },
        text = {
            Text(
                text =
                "This is an example of an alert dialog with buttons.",
            )
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("Dismiss")
            }
        }
    )
}