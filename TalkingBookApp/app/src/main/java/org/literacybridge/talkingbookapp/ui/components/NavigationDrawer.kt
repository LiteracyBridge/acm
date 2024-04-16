package org.literacybridge.talkingbookapp.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun NavigationDrawer(
    drawerState: DrawerState,
    navController: NavController,
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Account Info Here!", modifier = Modifier.padding(16.dp))
                Divider()
                NavigationDrawerItem(
                    label = { Text(text = "Manage Program Content") },
                    icon = {
                        Icon(
                            Icons.Outlined.Build,
                            contentDescription = "Manage program audio content"
                        )
                    },
                    selected = false,
                    onClick = { /*TODO*/ }
                )
                NavigationDrawerItem(
                    label = { Text(text = "Change Program") },
                    icon = {
                        Icon(
                            Icons.Outlined.Build,
                            contentDescription = "Change currently selected program"
                        )
                    },
                    selected = false,
                    onClick = { navController.navigate(Screen.PROGRAM_SELECTION.name, ) }
                )
                NavigationDrawerItem(
                    label = { Text(text = "Upload Status") },
                    icon = {
                        Icon(
                            Icons.Outlined.Send,
                            contentDescription = "View progress of statistics and contents upload or download"
                        )
                    },
                    selected = false,
                    onClick = { /*TODO*/ }
                )
                Divider()
                NavigationDrawerItem(
                    label = { Text(text = "Settings") },
                    icon = {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = "Explore advance features of the Talking Book Loader"
                        )
                    },
                    selected = false,
                    onClick = { /*TODO*/ }
                )
                NavigationDrawerItem(
                    label = {
                        Text(text = "About")
                    },
                    icon = {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = "More information about the Amplio Talking Book loader app"
                        )
                    },
                    selected = false,
                    onClick = { /*TODO*/ }
                )
                Divider()
                NavigationDrawerItem(
                    label = { Text(text = "Logout") },
                    icon = {
                        Icon(
                            Icons.Outlined.Lock,
                            contentDescription = "Logout of the application. All stored data will be cleared"
                        )
                    },
                    selected = false,
                    onClick = { /*TODO*/ }
                )
                // ...other drawer items
            }
        }
    ) {
        content()
    }
}