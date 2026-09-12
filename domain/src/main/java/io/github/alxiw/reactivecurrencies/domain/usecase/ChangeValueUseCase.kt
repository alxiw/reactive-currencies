package io.github.alxiw.reactivecurrencies.domain.usecase

import io.github.alxiw.reactivecurrencies.domain.model.Currency
import io.github.alxiw.reactivecurrencies.domain.repository.CurrenciesRepository
import io.reactivex.rxjava3.core.Single

class ChangeValueUseCase(private val repository: CurrenciesRepository) {
    operator fun invoke(baseCurrency: Currency): Single<List<Currency>> =
        repository.changeValue(baseCurrency)
}
