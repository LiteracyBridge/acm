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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.literacybridge.talkingbookapp.R
import org.literacybridge.talkingbookapp.ui.components.NavigationDrawer
import org.literacybridge.talkingbookapp.util.device_manager.Usb
import org.literacybridge.talkingbookapp.view_models.TalkingBookViewModel
import org.literacybridge.talkingbookapp.view_models.UserViewModel
import java.io.File
import java.lang.Thread.sleep


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: TalkingBookViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel(),
) {
    // Retrieve data from next screen
//    val msg =
//        navController.currentBackStackEntry?.savedStateHandle?.get<String>("msg")

    val uiState by viewModel.deviceState.collectAsStateWithLifecycle()


    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect("mass-device-listener") {
        // It takes a while (maybe 1s+ based on the device's performance) for connected
        // for mass storage to be ready. We check if the mass directory exists every 0.5s
        // This does the trick, FileObserver seems not to be working
        withContext(Dispatchers.IO) {
            val f = File(Usb.MASS_STORAGE_PATH)
            while (true) {
                if (f.exists()) {
                    viewModel.isMassStorageReady.value = true
                    break
                }
                delay(5000) // sleep for half a second
            }
        }
    }

    NavigationDrawer(drawerState, navController) {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = Color.White,
                    ),
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                drawerState.apply {
                                    if (isClosed) open() else close()
                                }
                            }
                        }) {
                            Icon(Icons.Filled.Menu, tint = Color.White, contentDescription = null)
                        }
                    },
                    title = {
                        userViewModel.program.value?.project?.name?.let { Text(it) }
                    })
            },
        ) { contentPadding ->

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding),
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
                        Button(
                            enabled = viewModel.isMassStorageReady.value,
                            onClick = { navController.navigate(Screen.COLLECT_DATA.name) }) {
                            Text("Collect Statistics")
                        }
                        Button(
                            enabled = true,
//                            enabled = viewModel.isDeviceConnected(),
                            onClick = {
                                // TODO: check if device needs firmware updates, navigate to firmware update screen
                                navController.navigate(Screen.RECIPIENT.name)
                            }) {
                            Text("Update Talking Book")
                        }
                    }
                }
            }
        }
    }


}
