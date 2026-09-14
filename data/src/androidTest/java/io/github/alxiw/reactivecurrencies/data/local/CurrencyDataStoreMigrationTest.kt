package io.github.alxiw.reactivecurrencies.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Verifies the one-shot SharedPreferences -> DataStore migration wired up by
 * [currencyDataStoreMigrations].
 */
@RunWith(AndroidJUnit4::class)
class CurrencyDataStoreMigrationTest {

    private companion object {
        // Keys written by the legacy SharedPreferences-based implementation.
        const val LEGACY_KEY_BASE_CURRENCY = "base_currency"
        const val LEGACY_KEY_BASE_VALUE = "base_value"
        const val LEGACY_KEY_UPDATE_DATE = "update_date"
    }

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var scope: CoroutineScope
    private lateinit var dataStoreFile: File

    @Before
    fun setUp() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        // A dedicated file so the test never touches the production DataStore instance.
        dataStoreFile = File(context.cacheDir, "migration_${System.nanoTime()}.preferences_pb")
        context.deleteSharedPreferences(LEGACY_SHARED_PREFERENCES_NAME)
    }

    @After
    fun tearDown() {
        scope.cancel()
        dataStoreFile.delete()
        File(dataStoreFile.parentFile, "${dataStoreFile.name}.tmp").delete()
        context.deleteSharedPreferences(LEGACY_SHARED_PREFERENCES_NAME)
    }

    @Test
    fun migratesLegacySharedPreferencesIntoDataStore() = runBlocking {
        // given: values persisted by the old SharedPreferences-based implementation
        legacySharedPreferences()
            .edit()
            .putString(LEGACY_KEY_BASE_CURRENCY, "USD")
            .putString(LEGACY_KEY_BASE_VALUE, "42")
            .putString(LEGACY_KEY_UPDATE_DATE, "2024-01-01")
            .commit()

        val dataStore = PreferenceDataStoreFactory.create(
            migrations = currencyDataStoreMigrations(context),
            scope = scope,
        ) { dataStoreFile }

        // when: the first read triggers the migration
        val subject = CurrencyPreferences(dataStore)
        val (code, value) = subject.loadBaseCurrency().blockingGet()

        // then: legacy values are available through the DataStore-backed API
        assertEquals("USD", code)
        assertEquals("42", value)
        assertEquals("2024-01-01", subject.loadUpdateDate().blockingGet())

        // and the legacy SharedPreferences were consumed, so the migration won't run again
        assertTrue(legacySharedPreferences().all.isEmpty())
        assertFalse(legacySharedPreferencesFile().exists())
    }

    @Test
    fun keepsDefaultsWhenThereIsNothingToMigrate() = runBlocking {
        // given: a clean install with no legacy SharedPreferences
        val dataStore = PreferenceDataStoreFactory.create(
            migrations = currencyDataStoreMigrations(context),
            scope = scope,
        ) { dataStoreFile }

        // when
        val (code, value) = CurrencyPreferences(dataStore).loadBaseCurrency().blockingGet()

        // then: built-in defaults are used
        assertEquals("RUB", code)
        assertEquals("100", value)
    }

    private fun legacySharedPreferences(): SharedPreferences =
        context.getSharedPreferences(LEGACY_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)

    private fun legacySharedPreferencesFile(): File =
        File(context.applicationInfo.dataDir, "shared_prefs/$LEGACY_SHARED_PREFERENCES_NAME.xml")
}
