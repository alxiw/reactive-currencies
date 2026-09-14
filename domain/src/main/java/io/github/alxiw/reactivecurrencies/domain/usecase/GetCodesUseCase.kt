package io.github.alxiw.reactivecurrencies.domain.usecase

import io.github.alxiw.reactivecurrencies.domain.repository.CurrenciesRepository
import io.reactivex.rxjava3.core.Single

class GetCodesUseCase(private val repository: CurrenciesRepository) {
    operator fun invoke(): Single<List<Pair<String, String>>> =
        repository.getCodes()
}
