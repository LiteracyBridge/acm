package org.literacybridge.talkingbookapp.view_models

import Screen
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.literacybridge.talkingbookapp.App
import org.literacybridge.talkingbookapp.api_services.NetworkModule
import org.literacybridge.talkingbookapp.helpers.LOG_TAG
import org.literacybridge.talkingbookapp.helpers.dataStoreManager
import org.literacybridge.talkingbookapp.models.UserModel
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor() : ViewModel() {
    val isLoading = mutableStateOf(false)

    private val _user = MutableStateFlow(UserModel())
    val user: StateFlow<UserModel> = _user.asStateFlow()

    fun setToken(token: String, cognitoSubId: String, navController: NavController) {
        isLoading.value = true

        viewModelScope.launch {
            dataStoreManager.setAccessToken(token, cognitoSubId)
        }.invokeOnCompletion {
            // if no network fallback to cache
            if (!App().isNetworkAvailable()) {
                viewModelScope.launch {
                    val resp = dataStoreManager.getCurrentUser().firstOrNull()
                    if (resp != null) {
                        _user.value = resp
                        navController.navigate(Screen.HOME.name);
                    } else {
                        // TODO: navigate to error page
                    }
                }.invokeOnCompletion { isLoading.value = false }

            } else { // Network available, re-fetch data from server
                viewModelScope.launch {
                    try {
                        val response = NetworkModule().instance().getUser()

                        // Cache user object
                        _user.value = response.data[0]
                        dataStoreManager.setUser(_user.value)

                        navController.navigate(Screen.HOME.name);
                        Log.d(LOG_TAG, "${response.data}")
                    } catch (e: Exception) {
                        // TODO: navigate to error page
                        Log.d(LOG_TAG, "$e")
                    } finally {
                        isLoading.value = false
                    }
                }

            }

        }
    }
}