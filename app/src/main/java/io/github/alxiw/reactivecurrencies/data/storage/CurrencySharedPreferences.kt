package io.github.alxiw.reactivecurrencies.data.storage

import android.content.SharedPreferences

class CurrencySharedPreferences(private val sharedPreferences: SharedPreferences) {

    companion object {
        private const val PREF_KEY_BASE_CURRENCY = "base_currency"
        private const val PREF_KEY_BASE_VALUE = "base_value"
        private const val PREF_KEY_UPDATE_DATE = "update_date"

        private const val DEFAULT_BASE_CURRENCY = "RUB"
        private const val DEFAULT_BASE_VALUE = "1.00"
    }

    fun saveBaseCurrency(code: String, value: String) {
        val editor = sharedPreferences.edit()
        editor.putString(PREF_KEY_BASE_CURRENCY, code)
        editor.putString(PREF_KEY_BASE_VALUE, value)
        editor.apply()
    }

    fun loadBaseCurrency(): Pair<String, String> {
        val name = sharedPreferences.getString(PREF_KEY_BASE_CURRENCY, DEFAULT_BASE_CURRENCY) ?: DEFAULT_BASE_CURRENCY
        val value = sharedPreferences.getString(PREF_KEY_BASE_VALUE, DEFAULT_BASE_VALUE) ?: DEFAULT_BASE_VALUE
        return Pair<String, String>(name, value)
    }

    fun saveUpdateDate(date: String) {
        sharedPreferences.edit().apply { putString(PREF_KEY_UPDATE_DATE, date) }.apply()
    }

    fun loadUpdateDate(): String? {
        return sharedPreferences.getString(PREF_KEY_UPDATE_DATE, null)
    }
}
