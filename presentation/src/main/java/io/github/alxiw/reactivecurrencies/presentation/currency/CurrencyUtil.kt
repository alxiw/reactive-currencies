package io.github.alxiw.reactivecurrencies.presentation.currency

import java.util.Currency
import java.util.Locale

object CurrencyUtil {

    private const val ASCII_OFFSET = 0x41
    private const val UNICODE_FLAG_OFFSET = 0x1F1E6

    fun getCurrencyFullName(code: String, name: String? = null): String =
        currencyOf(code)?.getDisplayName(Locale.US) ?: name ?: code

    fun getCurrencySignBy(code: String): String =
        currencyOf(code)?.symbol ?: code

    fun getCurrencyIcon(code: String): String =
        code.take(2)
            .map { String(Character.toChars((it.code - ASCII_OFFSET) + UNICODE_FLAG_OFFSET)) }
            .joinToString("")

    private fun currencyOf(code: String): Currency? =
        runCatching { Currency.getInstance(code) }.getOrNull()
}
