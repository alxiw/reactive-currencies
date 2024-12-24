package io.github.alxiw.reactivecurrencies

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import io.github.alxiw.reactivecurrencies.data.CurrenciesRepository
import io.github.alxiw.reactivecurrencies.data.network.CbrApiService
import io.github.alxiw.reactivecurrencies.data.network.RemoteDataSource
import io.github.alxiw.reactivecurrencies.data.storage.AppDatabase
import io.github.alxiw.reactivecurrencies.data.storage.CurrencySharedPreferences
import io.github.alxiw.reactivecurrencies.data.storage.LocalDataSource
import io.github.alxiw.reactivecurrencies.presentation.CurrenciesViewModelFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import java.util.concurrent.TimeUnit

private const val BASE_URL = "https://cbr.ru/scripts/"

object Dependencies {

    private lateinit var applicationContext: Context

    private val sharedPreferences: SharedPreferences by lazy {
        applicationContext.getSharedPreferences("currencies_prefs", Context.MODE_PRIVATE)
    }

    private val currencySharedPreferences: CurrencySharedPreferences by lazy {
        CurrencySharedPreferences(sharedPreferences)
    }

    private val apiService: CbrApiService by lazy {
        val httpLoggingInterceptor =
            HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
                override fun log(message: String) {
                    Log.d("HELLO", message)
                }
            })
        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BASIC

        val client =  OkHttpClient.Builder()
            .readTimeout(15, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(httpLoggingInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .baseUrl(BASE_URL)
            .client(client)
            .build()

        retrofit.create(CbrApiService::class.java)
    }

    private val remoteDataSource: RemoteDataSource by lazy {
        RemoteDataSource(apiService)
    }

    private val appDatabase: AppDatabase by lazy {
        AppDatabase.getDatabase(applicationContext)
    }

    private val localDataSource: LocalDataSource by lazy {
        LocalDataSource(appDatabase)
    }

    val currenciesRepository: CurrenciesRepository by lazy {
        CurrenciesRepository(localDataSource, remoteDataSource, currencySharedPreferences)
    }

    val viewModelFactory by lazy { CurrenciesViewModelFactory(currenciesRepository) }

    fun init(context: Context) {
        applicationContext = context
    }
}
