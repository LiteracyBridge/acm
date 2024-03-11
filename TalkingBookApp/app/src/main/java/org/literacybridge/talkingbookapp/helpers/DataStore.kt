package org.literacybridge.talkingbookapp.helpers

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
//
//    var cognitoSubjectId: String? = null
//        private set
//    var activeProgramId: String? = null
//        private set
//    var deploymentNumber: Int? = null
//        private set;

    companion object {
        private val KEY_DATA = stringPreferencesKey("data");
//
//        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token");
//        private val KEY_COGNITO_SUB_ID = stringPreferencesKey("cognito_sub_id")
//        private val KEY_USER = stringPreferencesKey("user")
//        private val KEY_PROGRAM_ID = stringPreferencesKey("program")
//        private val KEY_DEPLOYMENT = intPreferencesKey("deployment")
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

    /// FIXME: methods below are deprecated

//    suspend fun setUser(user: UserModel) {
//        this.currentUser = user
//        App.context.dataStore.edit {
//            it[KEY_USER] = Gson().toJson(user)
//        }
//    }
//
//    suspend fun setAccessToken(token: String, subId: String): String {
//        accessToken = token
//        cognitoSubjectId = subId
//
//        App.context.dataStore.edit {
//            it[KEY_ACCESS_TOKEN] = token;
//            it[KEY_COGNITO_SUB_ID] = subId;
//        }
//
//        return token
//    }


//    fun getCurrentUser(): Flow<UserModel?> {
//        if (this.currentUser != null) {
//            return flowOf(this.currentUser)
//        }
//
//        return App.context.dataStore.data.map { pref ->
//            val json = Gson().fromJson(pref[KEY_USER] ?: "{}", UserModel::class.java)
//            this.currentUser = json;
//            json
//        }
//    }

//    fun getAccessToken(): Flow<String?> {
//        if (accessToken != null) {
//            return flowOf(accessToken)
//        }
//
//        return App.context.dataStore.data.map { pref ->
//            this.accessToken = pref[KEY_ACCESS_TOKEN];
//            this.cognitoSubjectId = pref[KEY_COGNITO_SUB_ID];
//
//            this.accessToken
//        }
//    }


    suspend fun clear() = App.context.dataStore.edit {
        it.clear()
    }
}

val dataStoreManager = DataStoreManager();

