package org.literacybridge.androidtbloader.ui.components

import Screen
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.List
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
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.8f),
            ) {
                Text("Account Info Here!", modifier = Modifier.padding(16.dp))
                Divider()
                NavigationDrawerItem(
                    label = { Text(text = "Home") },
                    icon = {
                        Icon(
                            Icons.Outlined.Home,
                            contentDescription = "Manage program audio content"
                        )
                    },
                    selected = false,
                    onClick = { navController.navigate(Screen.HOME.name) }
                )
                NavigationDrawerItem(
                    label = { Text(text = "Manage Program Content") },
                    icon = {
                        Icon(
                            Icons.Outlined.List,
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
                            Icons.Outlined.Edit,
                            contentDescription = "Change currently selected program"
                        )
                    },
                    selected = false,
                    onClick = { navController.navigate(Screen.PROGRAM_SELECTION.name) }
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
                    onClick = { navController.navigate(Screen.UPLOAD_STATUS.name) }
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
                    onClick = { navController.navigate(Screen.LOGOUT.name) }
                )
            }
        }
    ) {
        content()
    }
}