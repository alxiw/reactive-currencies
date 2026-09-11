package io.github.alxiw.reactivecurrencies.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.alxiw.reactivecurrencies.data.local.model.CurrencyDto

@Database(
    entities = [CurrencyDto::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun currencyDao(): CurrencyDao
}
