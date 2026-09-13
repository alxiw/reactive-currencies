package io.github.alxiw.reactivecurrencies.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.alxiw.reactivecurrencies.data.local.model.CurrencyDto

@Database(
    entities = [CurrencyDto::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun currencyDao(): CurrencyDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE currency ADD COLUMN nominal INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE currency ADD COLUMN name TEXT")
    }
}
