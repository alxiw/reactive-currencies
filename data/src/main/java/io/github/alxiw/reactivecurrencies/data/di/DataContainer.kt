package io.github.alxiw.reactivecurrencies.data.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import io.github.alxiw.reactivecurrencies.data.CurrenciesRepository
import io.github.alxiw.reactivecurrencies.data.local.AppDatabase
import io.github.alxiw.reactivecurrencies.data.local.CurrencyDataStore
import io.github.alxiw.reactivecurrencies.data.local.LocalDataSource
import io.github.alxiw.reactivecurrencies.data.local.currencyDataStore
import io.github.alxiw.reactivecurrencies.data.local.MIGRATION_1_2
import io.github.alxiw.reactivecurrencies.data.remote.CbrApiService
import io.github.alxiw.reactivecurrencies.data.remote.RemoteDataSource
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import java.util.concurrent.TimeUnit

private const val BASE_URL = "https://www.cbr-xml-daily.com/"
private const val DB_NAME = "currencies.db"

interface DataContainer {
    val currenciesRepository: CurrenciesRepository
}

class DefaultDataContainer(context: Context) : DataContainer {

    private val applicationContext: Context = context.applicationContext

    private val currencyDataStore: CurrencyDataStore by lazy {
        CurrencyDataStore(applicationContext.currencyDataStore)
    }

    private val apiService: CbrApiService by lazy {
        val httpLoggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d("HELLO", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
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
        Room.databaseBuilder(applicationContext, AppDatabase::class.java, DB_NAME)
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    private val localDataSource: LocalDataSource by lazy {
        LocalDataSource(appDatabase)
    }

    override val currenciesRepository: CurrenciesRepository by lazy {
        CurrenciesRepository(localDataSource, remoteDataSource, currencyDataStore)
    }
}
