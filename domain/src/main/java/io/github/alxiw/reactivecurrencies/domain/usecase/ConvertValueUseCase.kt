package io.github.alxiw.reactivecurrencies.domain.usecase

import io.github.alxiw.reactivecurrencies.domain.repository.CurrenciesRepository
import io.reactivex.rxjava3.core.Single
import java.math.BigDecimal
import java.util.Locale

class ConvertValueUseCase(private val repository: CurrenciesRepository) {

    operator fun invoke(from: String, to: String, value: BigDecimal?): Single<String> {
        return when {
            from.isBlank() || to.isBlank() -> Single.just("")
            value == null -> Single.just("")
            else -> repository.convertValue(from, to, value).map(::format)
        }
    }

    private fun format(value: BigDecimal) = String.format(Locale.ROOT, "%.2f", value)
}
