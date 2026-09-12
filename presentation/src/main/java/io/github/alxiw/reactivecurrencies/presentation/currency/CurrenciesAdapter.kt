package io.github.alxiw.reactivecurrencies.presentation.currency

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import io.github.alxiw.reactivecurrencies.domain.model.Currency
import io.github.alxiw.reactivecurrencies.presentation.databinding.ItemBaseCurrencyBinding
import io.github.alxiw.reactivecurrencies.presentation.databinding.ItemCurrencyBinding
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

private enum class CurrencyViewType { BASE, COMMON }

class CurrenciesAdapter(
    private val onItemClick: (Currency, Int) -> Unit,
    private val onValueChanged: (Currency, BigDecimal, Int) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var currencies: MutableList<Currency> = mutableListOf()

    override fun getItemCount() = currencies.size

    override fun getItemViewType(position: Int): Int =
        if (currencies[position].isBase) CurrencyViewType.BASE.ordinal else CurrencyViewType.COMMON.ordinal

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            CurrencyViewType.BASE.ordinal -> {
                val binding = ItemBaseCurrencyBinding.inflate(inflater, parent, false)
                BaseCurrencyViewHolder(binding.root, binding.baseCurrencyItem)
            }
            else -> {
                val binding = ItemCurrencyBinding.inflate(inflater, parent, false)
                val viewHolder = CurrencyViewHolder(binding.root, binding.currencyItem)
                binding.root.setOnClickListener {
                    val adapterPosition = viewHolder.bindingAdapterPosition
                    if (adapterPosition != NO_POSITION) {
                        onItemClick(currencies[adapterPosition], adapterPosition)
                    }
                }
                viewHolder
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is BaseCurrencyViewHolder -> holder.bind(currencies[position], position)
            is CurrencyViewHolder -> holder.bind(currencies[position])
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>,
    ) {
        val newValue = (payloads.firstOrNull() as? BigDecimal)
            ?: return onBindViewHolder(holder, position)
        when (holder) {
            is BaseCurrencyViewHolder -> holder.bindWithPayload(newValue)
            is CurrencyViewHolder -> holder.bindWithPayload(newValue)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        (holder as? BaseCurrencyViewHolder)?.release()
    }

    fun updateCurrencies(newCurrencies: List<Currency>) {
        val diffUtilsCallback = CurrenciesDiffUtil(currencies, newCurrencies)
        val diffUtilsResult = DiffUtil.calculateDiff(diffUtilsCallback, false)
        currencies.clear()
        currencies.addAll(newCurrencies)
        diffUtilsResult.dispatchUpdatesTo(this)
    }

    inner class BaseCurrencyViewHolder(
        itemView: View,
        private val card: CurrencyCardView,
    ) : RecyclerView.ViewHolder(itemView) {

        private val disposable = CompositeDisposable()

        fun bind(item: Currency, position: Int) {
            card.bind(item)
            disposable.clear()
            disposable.add(formEditTextDisposable(position))
        }

        fun bindWithPayload(value: BigDecimal) {
            card.setValue(value)
        }

        fun release() {
            disposable.clear()
        }

        private fun formEditTextDisposable(position: Int): Disposable =
            CurrencyTextWatcher.fromView(card.value)
                .debounce(1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .distinctUntilChanged()
                .doOnNext { onValueChanged(currencies[position], it, position) }
                .subscribe()
    }

    class CurrencyViewHolder(
        itemView: View,
        private val card: CurrencyCardView,
    ) : RecyclerView.ViewHolder(itemView) {

        fun bind(item: Currency) {
            card.bind(item)
        }

        fun bindWithPayload(value: BigDecimal) {
            card.setValue(value)
        }
    }
}

private fun CurrencyCardView.bind(item: Currency) {
    setLongName(CurrencyUtil.getCurrencyFullName(item.code))
    setShortName(item.code)
    setValue(item.value)
    setIcon(CurrencyUtil.getCurrencyIcon(item.code))
    setSign(CurrencyUtil.getCurrencySignBy(item.code))
}
