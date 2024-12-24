package io.github.alxiw.reactivecurrencies.data.network

import io.github.alxiw.reactivecurrencies.data.network.model.CbrCurrenciesResponse
import io.github.alxiw.reactivecurrencies.data.model.util.CurrenciesDataConverter
import io.github.alxiw.reactivecurrencies.data.storage.model.CurrenciesDataDto
import io.reactivex.rxjava3.core.Single

class RemoteDataSource(private val apiService: CbrApiService) {

    fun updateCurrenciesData(): Single<CurrenciesDataDto> {
        return apiService.getCbrCurrencies()
            .flatMap<CbrCurrenciesResponse> { response ->
                val date = !response.date.isNullOrBlank()
                val list = !response.list.isNullOrEmpty()
                val content = !response.list!!.any {
                    it.charCode.isNullOrEmpty() || it.rate.isNullOrEmpty()
                }
                if (date && list && content) {
                    Single.just(response)
                } else {
                    val e = RuntimeException("missing required fields")
                    Single.error<CbrCurrenciesResponse>(e)
                }
            }
            .map { response ->
                CurrenciesDataConverter.fromResponseToRoom(response)
            }
    }
}
