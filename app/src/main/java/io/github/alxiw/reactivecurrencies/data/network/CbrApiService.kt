package io.github.alxiw.reactivecurrencies.data.network

import io.github.alxiw.reactivecurrencies.data.network.model.CbrCurrenciesResponse
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET

interface CbrApiService {

    @GET("daily_eng_utf8.xml")
    fun getCbrCurrencies(): Single<CbrCurrenciesResponse>
}
