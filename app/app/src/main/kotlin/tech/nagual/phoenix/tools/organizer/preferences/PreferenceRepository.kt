package tech.nagual.phoenix.tools.organizer.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import tech.nagual.common.preferences.datastore.EnumPreference
import tech.nagual.common.preferences.datastore.getEnum
import tech.nagual.common.preferences.datastore.setEnum
import tech.nagual.common.preferences.flow.FlowSharedPreferences

@OptIn(ExperimentalCoroutinesApi::class)
class PreferenceRepository(
    val dataStore: DataStore<Preferences>,
    private val sharedPreferences: FlowSharedPreferences,
) {
    inline fun <reified T> get(): Flow<T> where T : Enum<T>, T : EnumPreference {
        return dataStore.getEnum()
    }

    suspend fun <T> set(preference: T) where T : Enum<T>, T : EnumPreference {
        dataStore.setEnum(preference)
    }

    fun getEncryptedString(key: String): Flow<String> {
        return sharedPreferences.getString(key, "").asFlow()
    }

    suspend fun putEncryptedStrings(vararg pairs: Pair<String, String>) {
        pairs.forEach { (key, value) ->
            sharedPreferences.getString(key).setAndCommit(value)
        }
    }
}
