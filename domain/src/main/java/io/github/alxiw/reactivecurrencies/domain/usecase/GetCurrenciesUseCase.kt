package io.github.alxiw.reactivecurrencies.domain.usecase

import io.github.alxiw.reactivecurrencies.domain.model.Currency
import io.github.alxiw.reactivecurrencies.domain.repository.CurrenciesRepository
import io.reactivex.rxjava3.core.Single

class GetCurrenciesUseCase(private val repository: CurrenciesRepository) {
    operator fun invoke(): Single<List<Currency>> =
        repository.getAllCurrencies()
}
