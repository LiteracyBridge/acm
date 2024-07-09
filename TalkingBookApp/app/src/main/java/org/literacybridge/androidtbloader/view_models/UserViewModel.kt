package org.literacybridge.androidtbloader.view_models

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
import kotlinx.coroutines.launch
import org.literacybridge.androidtbloader.App
import org.literacybridge.androidtbloader.api_services.NetworkModule
import org.literacybridge.androidtbloader.models.Deployment
import org.literacybridge.androidtbloader.models.Program
import org.literacybridge.androidtbloader.models.UserModel
import org.literacybridge.androidtbloader.util.Constants.Companion.LOG_TAG
import org.literacybridge.androidtbloader.util.dataStoreManager
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor() : ViewModel() {
    val isLoading = mutableStateOf(false)
    val deployment = mutableStateOf<Deployment?>(null)
    val program = mutableStateOf<Program?>(null)


    private val _user = MutableStateFlow(UserModel())
    val user: StateFlow<UserModel> = _user.asStateFlow()

    val dataStore get() = dataStoreManager.data

    fun getPrograms(): List<Program> {
        return _user.value.programs.map { it ->
            it.program
        }
    }

    fun setActiveProgram(program: Program, deployment: Deployment, navController: NavController) {
        viewModelScope.launch {
            dataStoreManager.setProgramAndDeployment(
                program,
                deployment
            )
        }
        this.program.value = program
        this.deployment.value = deployment

        navController.navigate(Screen.CONTENT_DOWNLOADER.name)
    }

    fun setToken(token: String, cognitoSubId: String, navController: NavController) {
        isLoading.value = true

        viewModelScope.launch {
            dataStoreManager.setAccessToken(token, cognitoSubId)
        }.invokeOnCompletion {
            viewModelScope.launch {

                // if no network fallback to cache
                if (!App().isNetworkAvailable()) {
                    if (dataStoreManager.currentUser != null) {
                        _user.value = dataStoreManager.currentUser!!
                        navigateToNextScreen(navController)
                    } else {
                        // TODO: navigate to error page
                    }

                } else {
                    // Network available, re-fetch data from server
                    try {
                        val response = NetworkModule().instance().getUser()

                        // Cache user object
                        dataStoreManager.updateUser(response.data[0])
                        _user.value = response.data[0]

                        navigateToNextScreen(navController)
                    } catch (e: Exception) {
                        // TODO: navigate to error page
                        Log.d(LOG_TAG, "$e")
                    } finally {
                        isLoading.value = false
                    }

                }

            }.invokeOnCompletion { isLoading.value = false }
        }
    }

    private fun navigateToNextScreen(navController: NavController) {
        // The user has already selected a program/deployment, skip to home screen
        if (dataStoreManager.program != null && dataStoreManager.deployment != null) {
            this.deployment.value = dataStoreManager.deployment
            this.program.value = dataStoreManager.program

            return if (App().isNetworkAvailable()) {
                navController.navigate(Screen.CONTENT_DOWNLOADER.name);
            } else {
                // We're offline, skip content sync
                navController.navigate(Screen.HOME.name);
            }
        }

        return navController.navigate(Screen.PROGRAM_SELECTION.name)
    }
}