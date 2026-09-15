package io.github.alxiw.reactivecurrencies.data.mapper

import io.github.alxiw.reactivecurrencies.data.local.model.CurrenciesDataDto
import io.github.alxiw.reactivecurrencies.data.remote.model.CbrCurrenciesResponse
import io.github.alxiw.reactivecurrencies.data.local.model.CurrencyDto
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal object CurrenciesDataConverter {

    // immutable and thread-safe, safe to share across threads
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun fromResponseToDto(input: CbrCurrenciesResponse): CurrenciesDataDto {
        val date = input.date ?: LocalDate.now().format(dateFormatter)
        val list = input.list ?: emptyList()
        val set = list.mapNotNull { item ->
            val code = item.charCode ?: return@mapNotNull null
            val rate = item.rate ?: return@mapNotNull null
            CurrencyDto(
                code = code,
                value = formatRate(rate),
                nominal = item.nominal?.toIntOrNull() ?: 1,
                name = item.name
            )
        }
        .toMutableSet()
        .apply {
            add(CurrencyDto(
                code = REMOTE_BASE_CURRENCY_CODE,
                value = REMOTE_BASE_CURRENCY_VALUE,
                nominal = REMOTE_BASE_CURRENCY_NOMINAL,
                name = REMOTE_BASE_CURRENCY_NAME
            ))
        }

        return CurrenciesDataDto(date, set)
    }

    private fun formatRate(rate: String): String = rate.replace(",", ".")
}
