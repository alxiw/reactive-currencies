package io.github.alxiw.reactivecurrencies.data.local

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/** Name of the DataStore file, stored in the `files/datastore/` directory. */
private const val DATA_STORE_NAME = "currencies_prefs"

/** Name of the legacy SharedPreferences file used before the DataStore migration. */
internal const val LEGACY_SHARED_PREFERENCES_NAME = "currencies_prefs"

val Context.currencyDataStore: DataStore<Preferences> by preferencesDataStore(
    name = DATA_STORE_NAME,
    produceMigrations = ::currencyDataStoreMigrations,
)

/**
 * One-shot migration from the legacy SharedPreferences file into DataStore. It runs lazily on the
 * first access: values are copied over, already-present DataStore keys are never overwritten, and
 * the legacy file is deleted once it becomes empty, so the migration effectively runs only once.
 */
internal fun currencyDataStoreMigrations(context: Context): List<DataMigration<Preferences>> =
    listOf(SharedPreferencesMigration(context, LEGACY_SHARED_PREFERENCES_NAME))

class CurrencyDataStore(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val KEY_BASE_CURRENCY = stringPreferencesKey("base_currency")
        private val KEY_BASE_VALUE = stringPreferencesKey("base_value")
        private val KEY_UPDATE_DATE = stringPreferencesKey("update_date")

        private const val DEFAULT_BASE_CURRENCY = "RUB"
        private const val DEFAULT_BASE_VALUE = "100"
    }

    suspend fun saveBaseCurrency(code: String, value: String) {
        dataStore.edit { preferences ->
            preferences[KEY_BASE_CURRENCY] = code
            preferences[KEY_BASE_VALUE] = value
        }
    }

    suspend fun loadBaseCurrency(): Pair<String, String> {
        val preferences = dataStore.data.first()
        val name = preferences[KEY_BASE_CURRENCY] ?: DEFAULT_BASE_CURRENCY
        val value = preferences[KEY_BASE_VALUE] ?: DEFAULT_BASE_VALUE
        return name to value
    }

    suspend fun saveUpdateDate(date: String) {
        dataStore.edit { preferences ->
            preferences[KEY_UPDATE_DATE] = date
        }
    }

    suspend fun loadUpdateDate(): String? {
        return dataStore.data.first()[KEY_UPDATE_DATE]
    }
}
