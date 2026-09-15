package io.github.alxiw.reactivecurrencies.data.local.model

internal data class CurrenciesDataDto(
    val date: String,
    val list: Set<CurrencyDto>
)
