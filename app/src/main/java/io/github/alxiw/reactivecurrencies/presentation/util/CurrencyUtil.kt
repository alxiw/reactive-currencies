package io.github.alxiw.reactivecurrencies.presentation.util

import java.util.*

object CurrencyUtil {

    private const val ASCII_OFFSET = 0x41
    private const val UNICODE_FLAG_OFFSET = 0x1F1E6

    fun getCurrencyFullName(code: String): String = Currency.getInstance(code).getDisplayName(Locale.US)

    fun getCurrencySignBy(code: String): String = Currency.getInstance(code).symbol

    fun getCurrencyIcon(code: String): String {
        val firstChar = Character.codePointAt(code, 0) - ASCII_OFFSET + UNICODE_FLAG_OFFSET
        val secondChar = Character.codePointAt(code, 1) - ASCII_OFFSET + UNICODE_FLAG_OFFSET
        return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
    }

}
