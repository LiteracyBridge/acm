package org.literacybridge.talkingbookapp.view_models

import android.hardware.usb.UsbDevice
import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
//import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import org.literacybridge.talkingbookapp.api_services.ApiService
import org.literacybridge.talkingbookapp.util.LOG_TAG
import javax.inject.Inject

data class DeviceState(
    var device: UsbDevice? = null,
)

@HiltViewModel
class TalkingBookViewModel @Inject constructor() : ViewModel() {
    private val _deviceState = MutableStateFlow(DeviceState())
    val deviceState: StateFlow<DeviceState> = _deviceState.asStateFlow()

    // Handle business logic
//    fun rollDice() {
//        _uiState.update { currentState ->
//            currentState.copy(
//                firstDieValue = Random.nextInt(from = 1, until = 7),
//                secondDieValue = Random.nextInt(from = 1, until = 7),
//                numberOfRolls = currentState.numberOfRolls + 1,
//            )
//        }
//    }

    fun getDevice(): UsbDevice? {
        return deviceState.value.device
    }

    fun setDevice(device: UsbDevice?) {
        Log.d(LOG_TAG, "Device has been set $device");

        _deviceState.updateAndGet { state ->
            state.copy(
                device = device
            )
        }
    }

    fun disconnected() {
        _deviceState.update { state ->
            state.copy(device = null)
        }
    }
}
