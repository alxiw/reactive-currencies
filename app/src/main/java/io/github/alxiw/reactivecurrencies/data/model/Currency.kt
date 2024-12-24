package io.github.alxiw.reactivecurrencies.data.model

import java.math.BigDecimal

data class Currency(
    val code: String,
    val value: BigDecimal,
    var isBase: Boolean = false
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Currency) return false
        if (code != other.code) return false
        return true
    }

    override fun hashCode(): Int {
        var result = code.hashCode()
        result = 31 * result + value.hashCode()
        result = 31 * result + isBase.hashCode()
        return result
    }

}
