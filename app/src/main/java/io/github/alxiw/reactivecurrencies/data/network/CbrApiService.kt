package io.github.alxiw.reactivecurrencies.data.network

import io.github.alxiw.reactivecurrencies.data.network.model.CbrCurrenciesResponse
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET

interface CbrApiService {

    @GET("XML_daily.asp")
    fun getCbrCurrencies(): Single<CbrCurrenciesResponse>
}
