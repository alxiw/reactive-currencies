package io.github.alxiw.reactivecurrencies.data.model.util

import io.github.alxiw.reactivecurrencies.data.network.model.CbrCurrenciesResponse
import io.github.alxiw.reactivecurrencies.data.storage.model.CurrencyDto
import io.github.alxiw.reactivecurrencies.data.storage.model.CurrenciesDataDto
import java.text.SimpleDateFormat
import java.util.*

private const val REMOTE_BASE_CURRENCY = "RUB"
private const val REMOTE_BASE_VALUE = "1.0"

object CurrenciesDataConverter {

    private val format = SimpleDateFormat("dd.MM.yyyy", Locale.ROOT)

    fun fromResponseToRoom(input: CbrCurrenciesResponse): CurrenciesDataDto {
        val date = input.date ?: format.format(Date(System.currentTimeMillis()))
        val list = input.list ?: emptyList<CbrCurrenciesResponse.Currency>()
        val set = list
            .map { item -> CurrencyDto(item.charCode!!, formatRate(item.rate!!)) }
            .toMutableSet()
            .apply { add(CurrencyDto(REMOTE_BASE_CURRENCY, REMOTE_BASE_VALUE)) }

        return CurrenciesDataDto(date, set)
    }

    private fun formatRate(rate: String): String = rate.replace(",", ".")
}
