package org.literacybridge.talkingbookapp.view_models

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.literacybridge.core.spec.ProgramSpec
import org.literacybridge.core.spec.Recipient
import org.literacybridge.core.tbdevice.TbDeviceInfo
import javax.inject.Inject


@HiltViewModel
class RecipientViewModel @Inject constructor() : ViewModel() {
    // These fields track the recipient of the talking book device. They are populated
    // only when the user is performing Tb device update, ie. on the recipient screen
//    val recipients = mutableStateListOf<Recipient>()
    val recipients = MutableStateFlow<List<Recipient>>(emptyList())
    val districts = mutableStateListOf<String>() // District names

    val selectedDistrict = mutableStateOf<String?>(null)
    val selectedCommunity = mutableStateOf<String?>(null)
    val selectedGroup = mutableStateOf<String?>(null)

    val selectedRecipient = mutableStateOf<Recipient?>(null)


    fun fromProgramSpec(spec: ProgramSpec) {
        recipients.value = spec.recipients
        districts.addAll(spec.recipients.map { it.district }.distinct())
    }

    fun fromTalkingBook(tb: TbDeviceInfo?) {
        if (tb == null) return

        val tbRecipient = recipients.value.find { it.recipientid == tb.recipientid }
            ?: // No recipient found, could be TB from a different program
            return

        selectedDistrict.value = tbRecipient.district
        selectedCommunity.value = tbRecipient.communityname
        selectedGroup.value = tbRecipient.groupname
        selectedRecipient.value = tbRecipient
    }

    /**
     * Updates the value of selectedRecipient state based on the value of
     * the selected district/community/group
     * NB: This function called from the recipients screen
     */
    fun updateSelectedRecipient() {
        selectedRecipient.value = if (!selectedGroup.value.isNullOrBlank()) {
            recipients.value.find {
                it.groupname.equals(
                    selectedGroup.value,
                    true
                ) && it.communityname.equals(
                    selectedCommunity.value,
                    true
                ) && it.district.equals(
                    selectedDistrict.value,
                    true
                )
            }
        } else if (!selectedCommunity.value.isNullOrBlank()) {
            recipients.value.find {
                it.communityname.equals(
                    selectedCommunity.value,
                    true
                ) && it.district.equals(selectedDistrict.value, true)
            }
        } else if (!selectedDistrict.value.isNullOrBlank()) {
            recipients.value.find {
                it.district.equals(
                    selectedDistrict.value,
                    true
                )
            }
        } else {
            null
        }

    }

}
