package org.literacybridge.talkingbookapp.helpers

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.literacybridge.talkingbookapp.App
import org.literacybridge.talkingbookapp.models.Deployment
import org.literacybridge.talkingbookapp.models.Program
import org.literacybridge.talkingbookapp.models.UserModel

// Datastore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class SettingsDataStore(
    var accessToken: String? = null,
    var cognitoSubjectId: String? = null,
    var user: UserModel? = null,
    var program: Program? = null,
    var deployment: Deployment? = null
)

class DataStoreManager() {
    var data: SettingsDataStore = SettingsDataStore()
        private set

    val accessToken get() = data.accessToken

    val currentUser get() = data.user

    val program get() = data.program

    val deployment get() = data.deployment

    companion object {
        private val KEY_DATA = stringPreferencesKey("data");
    }

    init {
        loadData()
    }

    fun loadData(): Flow<SettingsDataStore?> {
        // This check is not really effective, but at least the access token field must be set
        // if the user has logged in. Should do the trick
        if (this.data.accessToken != null) {
            return flowOf(this.data)
        }

        return App.context.dataStore.data.map { pref ->
            this.data =
                Gson().fromJson(pref[KEY_DATA] ?: "{}", SettingsDataStore::class.java)

            this.data
        }
    }

    suspend fun updateUser(user: UserModel) {
        this.data.user = user
        this.updateData()
    }

    suspend fun setAccessToken(token: String, subId: String) {
        loadData().first().run {
            data.accessToken = token
            data.cognitoSubjectId = subId
            updateData()
        }
    }

    suspend fun setProgramAndDeployment(program: Program, deployment: Deployment) {
        this.data.program = program
        this.data.deployment = deployment
        this.updateData()
    }

    private suspend fun updateData() {
        App.context.dataStore.edit {
            it[KEY_DATA] = Gson().toJson(this.data)
        }
    }

    suspend fun clear() = App.context.dataStore.edit {
        it.clear()
    }
}

val dataStoreManager = DataStoreManager();

