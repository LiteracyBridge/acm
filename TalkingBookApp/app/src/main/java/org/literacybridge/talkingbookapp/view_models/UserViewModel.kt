package org.literacybridge.talkingbookapp.view_models

import Screen
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import org.literacybridge.talkingbookapp.App
import org.literacybridge.talkingbookapp.api_services.NetworkModule
import org.literacybridge.talkingbookapp.helpers.LOG_TAG
import org.literacybridge.talkingbookapp.helpers.dataStoreManager
import org.literacybridge.talkingbookapp.models.Program
import org.literacybridge.talkingbookapp.models.UserModel
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor() : ViewModel() {
    val isLoading = mutableStateOf(false)

    val activeProgramId = mutableStateOf(dataStoreManager.activeProgramId)
    private val _programs = MutableStateFlow<List<Program>>(listOf())
    val programsState: StateFlow<List<Program>> = _programs.asStateFlow()

    private val _user = MutableStateFlow(UserModel())
    val user: StateFlow<UserModel> = _user.asStateFlow()
//    val test = _user.value

    private var _courseList = MutableLiveData<UserModel>()
    var courseList: LiveData<UserModel> = _courseList

//    val value: String by liveData.observeAsState("initial")



    fun getPrograms(): List<Program> {
        if(courseList.value == null) return emptyList()

        return courseList.value!!.programs.map { it ->
            it.program
        }
    }

    fun setActiveProgram(program: Program, navController: NavController) {
        activeProgramId.value = program.program_id

        viewModelScope.launch {
            dataStoreManager.setActiveProgramId(program.program_id)
        }

        navController.navigate(Screen.HOME.name)
    }

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
                        navigateToNextScreen(navController)
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
//                        _user.updateAndGet { state ->
//                            state.copy(
//                                programs = response.data[0].programs
//                            )
//                        }
                        _courseList.value = response.data[0]
//                        _user.value = _user.value.copy(programs = response.data[0].programs)
//                        _user.compareAndSet(_user.value, response.data[0])
                        _programs.compareAndSet(
                            _programs.value,
                            response.data[0].programs.map { it.program })
//                        _programs.updateAndGet { state ->
//                            state.set
//                        }
                        dataStoreManager.setUser(_user.value)

                        navigateToNextScreen(navController)
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

    private fun navigateToNextScreen(navController: NavController) {
        if (activeProgramId.value == null) {
            return navController.navigate(Screen.PROGRAM_SELECTION.name)
        }

        return navController.navigate(Screen.HOME.name);
    }
}