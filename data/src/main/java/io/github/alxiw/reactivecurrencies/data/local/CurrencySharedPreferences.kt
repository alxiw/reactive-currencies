package io.github.alxiw.reactivecurrencies.data.local

import android.content.SharedPreferences
import androidx.core.content.edit

class CurrencySharedPreferences(private val sharedPreferences: SharedPreferences) {

    companion object {
        private const val PREF_KEY_BASE_CURRENCY = "base_currency"
        private const val PREF_KEY_BASE_VALUE = "base_value"
        private const val PREF_KEY_UPDATE_DATE = "update_date"

        private const val DEFAULT_BASE_CURRENCY = "RUB"
        private const val DEFAULT_BASE_VALUE = "1.00"
    }

    fun saveBaseCurrency(code: String, value: String) {
        sharedPreferences.edit {
            putString(PREF_KEY_BASE_CURRENCY, code)
            putString(PREF_KEY_BASE_VALUE, value)
        }
    }

    fun loadBaseCurrency(): Pair<String, String> {
        val name = sharedPreferences.getString(PREF_KEY_BASE_CURRENCY, DEFAULT_BASE_CURRENCY) ?: DEFAULT_BASE_CURRENCY
        val value = sharedPreferences.getString(PREF_KEY_BASE_VALUE, DEFAULT_BASE_VALUE) ?: DEFAULT_BASE_VALUE
        return Pair(name, value)
    }

    fun saveUpdateDate(date: String) {
        sharedPreferences.edit { putString(PREF_KEY_UPDATE_DATE, date) }
    }

    fun loadUpdateDate(): String? {
        return sharedPreferences.getString(PREF_KEY_UPDATE_DATE, null)
    }
}
