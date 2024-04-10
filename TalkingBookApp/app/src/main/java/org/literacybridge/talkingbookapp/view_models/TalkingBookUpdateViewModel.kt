package org.literacybridge.talkingbookapp.view_models

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import org.literacybridge.core.spec.RecipientList
import org.literacybridge.talkingbookapp.App
import javax.inject.Inject

class TalkingBookUpdateViewModel @Inject constructor() : ViewModel() {
    private val programSpec = App.getInstance().programSpec

    val selectedGroup = mutableStateOf<String?>(null)
    val selectedDistrict = mutableStateOf<String?>(null)
    val selectedCommunity = mutableStateOf<String?>(null)

    fun recipients(): RecipientList? {
        return programSpec!!.recipients
    }

    fun districts(): List<String> {
        return recipients()?.map { it.district }?.distinct() ?: emptyList()
    }

    fun communities(): List<String> {
        if (selectedDistrict.value != null) {
            return recipients()?.map { it.communityname }?.distinct() ?: emptyList()
        }
        return recipients()?.filter { it.district.equals(selectedDistrict.value) }
            ?.map { it.communityname }?.distinct() ?: emptyList()
    }

    fun groups(): List<String> {
        if (selectedCommunity.value != null) {
            return recipients()?.map { it.groupname }?.distinct() ?: emptyList()
        }
        return recipients()?.filter { it.communityname.equals(selectedCommunity.value) }
            ?.map { it.groupname }?.distinct() ?: emptyList()
    }
}