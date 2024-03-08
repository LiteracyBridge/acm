package org.literacybridge.talkingbookapp.helpers

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.literacybridge.talkingbookapp.App

// Datastore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")


class DataStoreManager() {
    companion object {
        var accessToken: String? = null;

        private val KE_ACCESS_TOKEN = stringPreferencesKey("access_token");
    }

    suspend fun setAccessToken(token: String) {
        accessToken = token;
        App.context.dataStore.edit {
            it[KE_ACCESS_TOKEN] = token;
        }
    }

    fun getAccessToken(): Flow<String?> {
        if (accessToken != null) {
            return flowOf(accessToken)
        }

        return App.context.dataStore.data.map { pref ->
            pref[KE_ACCESS_TOKEN];
        }
    }


    suspend fun clear() = App.context.dataStore.edit {
        it.clear()
    }
}

val dataStoreManager = DataStoreManager();

