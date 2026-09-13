package io.github.alxiw.reactivecurrencies.domain.model

import java.math.BigDecimal

data class Currency(
    val code: String,
    val value: BigDecimal,
    val isBase: Boolean = false,
    val nominal: Int = 1,
    val name: String? = null
)
