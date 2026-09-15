package io.github.alxiw.reactivecurrencies.data.remote

import io.github.alxiw.reactivecurrencies.data.remote.model.CbrCurrenciesResponse
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET

internal interface CbrApiService {

    @GET("daily_eng_utf8.xml")
    fun getCbrCurrencies(): Single<CbrCurrenciesResponse>
}
