package io.github.alxiw.reactivecurrencies.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "currency")
internal data class CurrencyDto(
    @PrimaryKey
    @ColumnInfo(name = "code")
    val code: String,
    @ColumnInfo(name = "value")
    val value: String,
    @ColumnInfo(name = "nominal")
    val nominal: Int,
    @ColumnInfo(name = "name")
    val name: String?
)
