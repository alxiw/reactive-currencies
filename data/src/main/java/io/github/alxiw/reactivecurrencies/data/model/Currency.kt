package io.github.alxiw.reactivecurrencies.data.model

import java.math.BigDecimal

data class Currency(
    val code: String,
    val value: BigDecimal,
    val isBase: Boolean = false
)
