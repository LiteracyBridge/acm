package org.literacybridge.androidtbloader.view_models

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.literacybridge.core.spec.ProgramSpec
import org.literacybridge.core.spec.Recipient
import org.literacybridge.core.tbdevice.TbDeviceInfo
import org.literacybridge.core.tbloader.TBLoaderUtils
import org.literacybridge.androidtbloader.App
import org.literacybridge.androidtbloader.util.PathsProvider
import javax.inject.Inject


data class ContentPackage(
    /**
     * Name of the content package. {deploymentName}-{languageCode}, eg. demo-2017-3-dga
     */
    val name: String,

    /**
     * User friendly name, eg. English (eng)
     */
    val label: String,

    /**
     * True if package has been selected by the user (checkbox)
     */
    var isSelected: Boolean = false
)

@HiltViewModel
class RecipientViewModel @Inject constructor() : ViewModel() {
    // These fields track the recipient of the talking book device. They are populated
    // only when the user is performing Tb device update, ie. on the recipient screen
    val recipients = MutableStateFlow<List<Recipient>>(emptyList())
    val districts = mutableStateListOf<String>() // District names

    var selectedDistrict = mutableStateOf<String?>(null)
    var selectedCommunity = mutableStateOf<String?>(null)
    var selectedGroup = mutableStateOf<String?>(null)
    val selectedRecipient = mutableStateOf<Recipient?>(null)

    /**
     * The connected talking book's recipient info
     */
    val defaultRecipient = mutableStateOf<Recipient?>(null)

    /**
     * List of content packages in the deployment
     * Items are arranged (by the user) in order to be deployed to a Talking Book
     */
    val packages =
        MutableStateFlow(emptyList<ContentPackage>().toMutableList())

    /**
     * Whether the selected packages should be discarded. If true, the data won't be used
     */
    val shouldDiscardPackages = MutableStateFlow(false)

    fun fromProgramSpec(spec: ProgramSpec) {
        recipients.value = spec.recipients
        districts.addAll(spec.recipients.map { it.district }.distinct())
    }

    fun fromTalkingBook(tb: TbDeviceInfo?) {
        if (tb == null) return

        val tbRecipient = recipients.value.find { it.recipientid == tb.recipientid }
            ?: // No recipient found, could be TB from a different program
            return

        defaultRecipient.value = tbRecipient

        if (!tbRecipient.district.isNullOrBlank()) {
            selectedDistrict.value = tbRecipient.district
        }
        if (!tbRecipient.communityname.isNullOrBlank()) {
            selectedCommunity.value = tbRecipient.communityname
        }
        if (!tbRecipient.groupname.isNullOrBlank()) {
            selectedGroup.value = tbRecipient.groupname
        }
        updateSelectedRecipient()
    }

    fun loadPackagesInDeployment() {
        // TODO: auto select default packages currently on the talking book

        val resp =
            TBLoaderUtils.getPackagesInDeployment(PathsProvider.getLocalDeploymentDirectory(App.getInstance().programContent!!))
        packages.value = resp.map { it -> // {deploymentName}-{lang}
            val code = it.split("-").last() // select the language code
            val lang =
                App.getInstance().programSpec!!.languages.find { it.code.lowercase() == code }

            ContentPackage(name = it, label = "${lang?.name} ($code)")
        }.toMutableList()
    }

    /**
     * Returns list of packages selected by the user, arranged in order to be deployed to a Talking Book
     */
    fun getSelectedPackages(): List<ContentPackage> {
        if (shouldDiscardPackages.value) {
            return emptyList()
        }
        return packages.value.filter { it.isSelected }
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
