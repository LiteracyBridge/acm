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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.literacybridge.core.tbdevice.TbDeviceInfo
import org.literacybridge.talkingbookapp.ui.components.NavigationDrawer
import org.literacybridge.talkingbookapp.util.Constants.Companion.SCREEN_MARGIN
import org.literacybridge.talkingbookapp.util.device_manager.Dfu
import org.literacybridge.talkingbookapp.util.device_manager.Usb
import org.literacybridge.talkingbookapp.view_models.TalkingBookViewModel
import org.literacybridge.talkingbookapp.view_models.UserViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: TalkingBookViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel(),
) {
    val deviceState by viewModel.usbDevice.collectAsStateWithLifecycle()
    val usbState by viewModel.usbState.collectAsStateWithLifecycle()
    val deviceInfo by viewModel.talkingBookDeviceInfo.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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
            bottomBar = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                ) {
                    Text(
                        text = "What would you like to do?",
                        modifier = Modifier.padding(bottom = 10.dp),
                        fontSize = 20.sp, fontWeight = FontWeight.SemiBold
                    )
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
                            enabled = viewModel.isMassStorageReady.value,
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
        ) { contentPadding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding)
                    .padding(SCREEN_MARGIN),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Connect the Talking Book",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
                BuildInstructions()

//                Text("${deviceState.device?.deviceName}")
//                Image(
//                    painter = painterResource(R.drawable.tb_table_image),
//                    contentDescription = "Tree"
////                contentDescription = stringResource(id = R.string.bus_content_description)
//                )

                Box(modifier = Modifier.padding(top = 50.dp)) {
                    if (deviceState == null && deviceInfo == null) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Please connect the Talking Book!",
                                modifier = Modifier.padding(top = 10.dp),
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (deviceInfo != null) { // We're in mass storage mode
                        BuildDeviceInfo(device = deviceInfo!!)
                    } else { // In DFU mode
                        UpdateFirmware(usb = usbState)
                    }
                }

            }
        }
    }

}

@Composable
fun UpdateFirmware(usb: Usb) {
    val showDialog = remember { mutableStateOf(false) }

    Button(
        onClick = {
            // TODO: check if device needs firmware updates, navigate to firmware update screen
            showDialog.value = true

            val dfu = Dfu(Usb.USB_VENDOR_ID, Usb.USB_PRODUCT_ID)
            dfu.setUsb(usb)
            dfu.program()
            // TODO: find a way to track if update is complete
        }) {
        Text("Update Firmware")
    }

    if (showDialog.value) {
        AlertDialog(
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            title = { Text(text = "Updating Firmware") },
            text = {
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                ) {
                    LinearProgressIndicator()
                    Text("please wait....", modifier = Modifier.padding(top = 8.dp))
                }
            },
            onDismissRequest = {},
            confirmButton = {},
            dismissButton = {}
        )
    }
}

@Composable
fun BuildDeviceInfo(device: TbDeviceInfo) {
    Box(
        modifier = Modifier
            .border(width = 1.dp, color = Color.Black)
            .fillMaxWidth(0.95f)
    ) {
        Text(buildAnnotatedString {
            withStyle(
                SpanStyle(fontWeight = FontWeight.Bold)
            ) {
                append("Serial No.: ")
            }
            append("${device.serialNumber}\n")
            withStyle(
                SpanStyle(fontWeight = FontWeight.Bold)
            ) {
                append("Device Version: ")
            }
            append("${device.deviceVersion}\n")
            withStyle(
                SpanStyle(fontWeight = FontWeight.Bold)
            ) {
                append("Deployment: ")
            }
            append("${device.deploymentName}\n")
            withStyle(
                SpanStyle(fontWeight = FontWeight.Bold)
            ) {
                append("Current recipient: ")
            }
            append(device.communityName)
        }, modifier = Modifier.padding(horizontal = 5.dp))
    }
}

@Composable
fun BuildInstructions() {
    return Row {
        Text(
            text = buildAnnotatedString {
                append("Press ")
                withStyle(
                    SpanStyle(fontWeight = FontWeight.Bold)
                ) {
                    append("\"Tree\"")
                }
                append(", ")
                withStyle(
                    SpanStyle(fontWeight = FontWeight.Bold)
                ) {
                    append("\"Table\"")
                }
                append(" & ")
                withStyle(
                    SpanStyle(fontWeight = FontWeight.Bold)
                ) {
                    append("\"Plus\"")
                }
                append(" buttons at the same time to connect.")
            },
            textAlign = TextAlign.Left,
            fontSize = 17.sp,
            fontStyle = FontStyle.Italic
        )
    }
}