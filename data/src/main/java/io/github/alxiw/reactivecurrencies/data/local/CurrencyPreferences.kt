package io.github.alxiw.reactivecurrencies.data.local

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.alxiw.reactivecurrencies.domain.repository.CurrencyPreferences as DomainPreferences
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.rx3.rxCompletable
import kotlinx.coroutines.rx3.rxMaybe
import kotlinx.coroutines.rx3.rxSingle

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

class CurrencyPreferences(private val dataStore: DataStore<Preferences>) : DomainPreferences {

    companion object {
        private val KEY_BASE_CURRENCY = stringPreferencesKey("base_currency")
        private val KEY_BASE_VALUE = stringPreferencesKey("base_value")
        private val KEY_UPDATE_DATE = stringPreferencesKey("update_date")
        private val KEY_CONVERT_FROM = stringPreferencesKey("convert_from")
        private val KEY_CONVERT_TO = stringPreferencesKey("convert_to")

        private const val DEFAULT_BASE_CURRENCY = "RUB"
        private const val DEFAULT_BASE_VALUE = "100"
    }

    override val fromCurrency: Maybe<String> = rxMaybe { dataStore.data.first()[KEY_CONVERT_FROM] }
    override val toCurrency: Maybe<String> = rxMaybe { dataStore.data.first()[KEY_CONVERT_TO] }

    override fun saveFrom(code: String): Completable = rxCompletable {
        dataStore.edit { it[KEY_CONVERT_FROM] = code }
    }

    override fun saveTo(code: String): Completable = rxCompletable {
        dataStore.edit { it[KEY_CONVERT_TO] = code }
    }

    override fun saveBaseCurrency(code: String, value: String): Completable = rxCompletable {
        dataStore.edit { preferences ->
            preferences[KEY_BASE_CURRENCY] = code
            preferences[KEY_BASE_VALUE] = value
        }
    }

    override fun loadBaseCurrency(): Single<Pair<String, String>> = rxSingle {
        val preferences = dataStore.data.first()
        val name = preferences[KEY_BASE_CURRENCY] ?: DEFAULT_BASE_CURRENCY
        val value = preferences[KEY_BASE_VALUE] ?: DEFAULT_BASE_VALUE
        name to value
    }

    override fun saveUpdateDate(date: String): Completable = rxCompletable {
        dataStore.edit { preferences ->
            preferences[KEY_UPDATE_DATE] = date
        }
    }

    override fun loadUpdateDate(): Maybe<String> = rxMaybe {
        dataStore.data.first()[KEY_UPDATE_DATE]
    }
}
