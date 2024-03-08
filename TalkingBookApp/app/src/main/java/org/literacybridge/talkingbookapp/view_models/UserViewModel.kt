package org.literacybridge.talkingbookapp.view_models

import Screen
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.literacybridge.talkingbookapp.api_services.NetworkModule
import org.literacybridge.talkingbookapp.helpers.DataStoreManager
import org.literacybridge.talkingbookapp.helpers.LOG_TAG
import org.literacybridge.talkingbookapp.helpers.dataStoreManager
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor() : ViewModel() {
//    private val _user = mutableStateOf(UserModel())
//    val users: State<ApiState<List<User>>> = _users

    fun setToken(token: String, navController: NavController) {
        viewModelScope.launch {
            dataStoreManager.setAccessToken(token)
        }.invokeOnCompletion {
            viewModelScope.launch {
                try {
                    // TODO; If the user is set, don't trigger api call
                    // FIXME: the function is executed multiple times due to ui re-rendering
                    val response = NetworkModule().instance().getUser()
                    navController.navigate(Screen.HOME.name);
                    Log.d(LOG_TAG, "${response.data}")
                } catch (e: Exception) {
                    Log.d(LOG_TAG, "$e")
                }
            }

        }
    }
}