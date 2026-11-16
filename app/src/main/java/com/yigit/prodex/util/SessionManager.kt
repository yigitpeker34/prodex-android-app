package com.yigit.prodex.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

class SessionManager(context: Context) {

    private val dataStore = context.dataStore

    companion object {
        val ACTIVE_USER_ID = intPreferencesKey("active_user_id")
    }

    val activeUserId: Flow<Int?> = dataStore.data.map {
        it[ACTIVE_USER_ID]
    }

    suspend fun setActiveUserId(userId: Int?) {
        dataStore.edit {
            if (userId == null) {
                it.remove(ACTIVE_USER_ID)
            } else {
                it[ACTIVE_USER_ID] = userId
            }
        }
    }
}
