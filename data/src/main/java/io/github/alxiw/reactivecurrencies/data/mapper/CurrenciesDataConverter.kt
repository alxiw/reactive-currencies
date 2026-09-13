package io.github.alxiw.reactivecurrencies.data.mapper

import io.github.alxiw.reactivecurrencies.data.local.model.CurrenciesDataDto
import io.github.alxiw.reactivecurrencies.data.remote.model.CbrCurrenciesResponse
import io.github.alxiw.reactivecurrencies.data.local.model.CurrencyDto
import java.text.SimpleDateFormat
import java.util.*

object CurrenciesDataConverter {

    // RUB is not provided by the backend explicitly, but it is default base currency
    private const val REMOTE_BASE_CURRENCY = "RUB"
    private const val REMOTE_BASE_VALUE = "1.0"
    private const val REMOTE_BASE_NOMINAL = 100
    private const val REMOTE_BASE_NAME = "Russian Ruble"

    private val format = SimpleDateFormat("dd.MM.yyyy", Locale.ROOT)

    fun fromResponseToDto(input: CbrCurrenciesResponse): CurrenciesDataDto {
        val date = input.date ?: format.format(Date(System.currentTimeMillis()))
        val list = input.list ?: emptyList()
        val set = list
            .map { item ->
                CurrencyDto(
                    code = item.charCode!!,
                    value = formatRate(item.rate!!),
                    nominal = item.nominal?.toIntOrNull() ?: 1,
                    name = item.name
                )
            }
            .toMutableSet()
            .apply { add(CurrencyDto(REMOTE_BASE_CURRENCY, REMOTE_BASE_VALUE, REMOTE_BASE_NOMINAL, REMOTE_BASE_NAME)) }

        return CurrenciesDataDto(date, set)
    }

    private fun formatRate(rate: String): String = rate.replace(",", ".")
}
