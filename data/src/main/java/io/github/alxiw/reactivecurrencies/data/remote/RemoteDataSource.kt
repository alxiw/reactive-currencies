package io.github.alxiw.reactivecurrencies.data.remote

import io.github.alxiw.reactivecurrencies.data.local.model.CurrenciesDataDto
import io.github.alxiw.reactivecurrencies.data.mapper.CurrenciesDataConverter
import io.reactivex.rxjava3.core.Single

internal class RemoteDataSource(private val apiService: CbrApiService) {

    fun updateCurrenciesData(): Single<CurrenciesDataDto> {
        return apiService.getCbrCurrencies()
            .map { response ->
                val hasDate = !response.date.isNullOrBlank()
                val hasList = !response.list.isNullOrEmpty()
                val hasContent = response.list.orEmpty().all {
                    !it.charCode.isNullOrEmpty() && !it.rate.isNullOrEmpty()
                }
                check(hasDate && hasList && hasContent) { "missing required fields" }
                CurrenciesDataConverter.fromResponseToDto(response)
            }
    }
}
