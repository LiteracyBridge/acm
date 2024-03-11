package org.literacybridge.talkingbookapp.ui

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import org.literacybridge.talkingbookapp.helpers.LOG_TAG
import org.literacybridge.talkingbookapp.helpers.SCREEN_MARGIN
import org.literacybridge.talkingbookapp.view_models.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramSelectionScreen(
    navController: NavController, viewModel: UserViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val programs by viewModel.programsState.collectAsStateWithLifecycle()
//    val test= viewModel.test.observeAsState()
    val list  = viewModel.courseList.observeAsState()

    Log.d(LOG_TAG, "Updated user ${viewModel.user}")
    Log.d(LOG_TAG, "Updated program ${list.value}")

    Scaffold(
        topBar = {
            TopAppBar(colors = topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = Color.White,
            ), title = {
                Text("Select Program")
            })
        },
    ) { innerPadding ->
        if (user.programs?.isEmpty() == true) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = SCREEN_MARGIN)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No program has been assigned to you. \nPlease contact your IT administrator for assistance")
            }
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = SCREEN_MARGIN)
//            .size(100.dp)
                .verticalScroll(rememberScrollState())
        ) {
            user.programs?.map { it.program }?.forEach { it ->
                ListItem(
                    headlineContent = {
                        // TODO: show a confirm dialog when program is selected
                        TextButton(onClick = { viewModel.setActiveProgram(it, navController) },
                            content = { Text(it.project.name) })
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Info, contentDescription = "Info"
                        )
                    },

                    )
            }

        }
    }
}