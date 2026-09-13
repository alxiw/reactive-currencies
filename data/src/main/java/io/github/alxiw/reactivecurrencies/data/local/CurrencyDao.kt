package io.github.alxiw.reactivecurrencies.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.alxiw.reactivecurrencies.data.local.model.CurrencyDto
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

@Dao
interface CurrencyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(list: List<CurrencyDto>): Completable

    @Query("SELECT * FROM currency")
    fun load(): Single<List<CurrencyDto>>

    @Query("DELETE FROM currency")
    fun delete(): Completable
}
