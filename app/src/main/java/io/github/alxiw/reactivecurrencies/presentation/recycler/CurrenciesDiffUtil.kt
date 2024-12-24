package io.github.alxiw.reactivecurrencies.presentation.recycler

import androidx.recyclerview.widget.DiffUtil
import io.github.alxiw.reactivecurrencies.data.model.Currency

class CurrenciesDiffUtil(
    private val oldCurrenciesList: List<Currency>,
    private val newCurrenciesList: List<Currency>
) : DiffUtil.Callback() {

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldCurrenciesList[oldItemPosition].code == newCurrenciesList[newItemPosition].code
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldCurrenciesList[oldItemPosition].value == newCurrenciesList[newItemPosition].value
    }

    override fun getOldListSize(): Int {
        return oldCurrenciesList.size
    }

    override fun getNewListSize(): Int {
        return newCurrenciesList.size
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        return newCurrenciesList[newItemPosition].value
    }
}
