package org.literacybridge.talkingbookapp.view_models

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProgramsViewModel @Inject constructor() : ViewModel() {
    var showBottomSheet = mutableStateOf(false)
    var isLoading = mutableStateOf(false)

    // TODO: cache active program in datastore
    // TODO: cache selected program
}