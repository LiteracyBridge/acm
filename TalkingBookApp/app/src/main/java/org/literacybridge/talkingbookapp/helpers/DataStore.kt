package org.literacybridge.talkingbookapp.helpers

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.literacybridge.talkingbookapp.App
import org.literacybridge.talkingbookapp.models.UserModel

// Datastore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")


class DataStoreManager() {
    var accessToken: String? = null
        private set
    var currentUser: UserModel? = null
        private set
    var cognitoSubjectId: String? = null
        private set
    var activeProgramId: String? = null
        private set

    companion object {

        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token");
        private val KEY_COGNITO_SUB_ID = stringPreferencesKey("cognito_sub_id")
        private val KEY_USER = stringPreferencesKey("user")
        private val KEY_ACTIVE_PROGRAM_ID = stringPreferencesKey("active_program")
    }

    init {
        App.context.dataStore.data.map { pref ->
            this.accessToken = pref[KEY_ACCESS_TOKEN] ?: null
            this.cognitoSubjectId = pref[KEY_COGNITO_SUB_ID]
            this.activeProgramId = pref[KEY_ACTIVE_PROGRAM_ID]

            this.currentUser =
                Gson().fromJson(pref[KEY_ACCESS_TOKEN] ?: "{}", UserModel::class.java)
            this.currentUser
        }
    }

    suspend fun setUser(user: UserModel) {
        this.currentUser = user
        App.context.dataStore.edit {
            it[KEY_USER] = Gson().toJson(user)
        }
    }

    suspend fun setAccessToken(token: String, subId: String): String {
        accessToken = token
        cognitoSubjectId = subId

        App.context.dataStore.edit {
            it[KEY_ACCESS_TOKEN] = token;
            it[KEY_COGNITO_SUB_ID] = subId;
        }

        return token
    }

    suspend fun setActiveProgramId(id: String): String {
        this.activeProgramId = id

        App.context.dataStore.edit {
            it[KEY_ACTIVE_PROGRAM_ID] = id;
        }

        return id
    }

    fun getCurrentUser(): Flow<UserModel?> {
        if (this.currentUser != null) {
            return flowOf(this.currentUser)
        }

        return App.context.dataStore.data.map { pref ->
            val json = Gson().fromJson(pref[KEY_USER] ?: "{}", UserModel::class.java)
            this.currentUser = json;
            json
        }
    }

    fun getAccessToken(): Flow<String?> {
        if (accessToken != null) {
            return flowOf(accessToken)
        }

        return App.context.dataStore.data.map { pref ->
            this.accessToken = pref[KEY_ACCESS_TOKEN];
            this.cognitoSubjectId = pref[KEY_COGNITO_SUB_ID];

            this.accessToken
        }
    }


    suspend fun clear() = App.context.dataStore.edit {
        it.clear()
    }
}

val dataStoreManager = DataStoreManager();

