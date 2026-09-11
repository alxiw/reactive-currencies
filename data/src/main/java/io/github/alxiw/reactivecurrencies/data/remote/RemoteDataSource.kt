package io.github.alxiw.reactivecurrencies.data.remote

import io.github.alxiw.reactivecurrencies.data.local.model.CurrenciesDataDto
import io.github.alxiw.reactivecurrencies.data.mapper.CurrenciesDataConverter
import io.reactivex.rxjava3.core.Single

class RemoteDataSource(private val apiService: CbrApiService) {

    fun updateCurrenciesData(): Single<CurrenciesDataDto> {
        return apiService.getCbrCurrencies()
            .flatMap { response ->
                val date = !response.date.isNullOrBlank()
                val list = !response.list.isNullOrEmpty()
                val content = !response.list!!.any {
                    it.charCode.isNullOrEmpty() || it.rate.isNullOrEmpty()
                }
                if (date && list && content) {
                    Single.just(response)
                } else {
                    Single.error(RuntimeException("missing required fields"))
                }
            }
            .map { response ->
                CurrenciesDataConverter.fromResponseToDto(response)
            }
    }
}
