package io.github.alxiw.reactivecurrencies.presentation.recycler

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import io.github.alxiw.reactivecurrencies.R
import io.github.alxiw.reactivecurrencies.databinding.ItemBaseCurrencyBinding
import io.github.alxiw.reactivecurrencies.databinding.ItemCurrencyBinding
import io.github.alxiw.reactivecurrencies.data.model.Currency
import io.github.alxiw.reactivecurrencies.presentation.util.CurrencyUtil
import io.github.alxiw.reactivecurrencies.presentation.listeners.OnItemClickListener
import io.github.alxiw.reactivecurrencies.presentation.listeners.OnValueChangeListener
import io.github.alxiw.reactivecurrencies.presentation.view.CurrencyTextWatcher
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

class CurrenciesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private enum class CurrencyViewType {
            BASE,
            COMMON
        }
    }

    private val disposable = CompositeDisposable()

    private var currencies: MutableList<Currency> = mutableListOf()

    private lateinit var bindingBase: ItemBaseCurrencyBinding
    private lateinit var bindingCommon: ItemCurrencyBinding

    var itemClickListener: OnItemClickListener<Currency>? = null
    var valueChangeLister: OnValueChangeListener<Currency, BigDecimal>? = null

    override fun getItemCount() = currencies.size

    override fun getItemViewType(position: Int): Int {
        return if (currencies[position].isBase) {
            CurrencyViewType.BASE.ordinal
        } else {
            CurrencyViewType.COMMON.ordinal
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            CurrencyViewType.BASE.ordinal -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_base_currency, parent, false)
                    .also { bindingBase = ItemBaseCurrencyBinding.bind(it) }

                val viewHolder = BaseCurrencyViewHolder(view, bindingBase)

                return viewHolder
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_currency, parent, false)
                    .also { bindingCommon = ItemCurrencyBinding.bind(it) }

                val viewHolder = CurrencyViewHolder(view, bindingCommon)

                view.setOnClickListener {
                    val adapterPosition = viewHolder.getBindingAdapterPosition()
                    if (adapterPosition != NO_POSITION) {
                        itemClickListener?.onItemClick(currencies[adapterPosition], adapterPosition)
                    }
                }

                return viewHolder
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val isBase = holder.itemViewType == CurrencyViewType.BASE.ordinal
        if (isBase) {
            val currencyViewHolder = holder as BaseCurrencyViewHolder
            currencyViewHolder.bind(currencies[position])
        } else {
            val currencyViewHolder = holder as CurrencyViewHolder
            currencyViewHolder.bind(currencies[position])
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
            return
        }

        val newValue = payloads[0] as BigDecimal
        if (holder is CurrencyViewHolder) {
            holder.bindWithPayload(newValue)
        } else if (holder is BaseCurrencyViewHolder) {
            holder.bindWithPayload(newValue)
        }
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
        private val binding: ItemBaseCurrencyBinding
    ) : RecyclerView.ViewHolder(itemView) {

        fun bind(item: Currency) {
            binding.baseCurrencyItem.apply {
                setLongName(CurrencyUtil.getCurrencyFullName(item.code))
                setShortName(item.code)
                setValue(item.value)
                setIcon(CurrencyUtil.getCurrencyIcon(item.code))
                setSign(CurrencyUtil.getCurrencySignBy(item.code))
            }

            disposable.clear()
            disposable.add(formEditTextDisposable())
        }

        fun bindWithPayload(value: BigDecimal) {
            binding.baseCurrencyItem.setValue(value)
        }

        private fun formEditTextDisposable(): Disposable {
            return CurrencyTextWatcher.fromView(binding.baseCurrencyItem.value)
                .debounce(1, TimeUnit.SECONDS)
                .distinctUntilChanged()
                .doOnNext {
                    valueChangeLister?.onValueChanged(currencies[0], it ?: BigDecimal.ONE, 0)
                }
                .subscribe()
        }
    }

    inner class CurrencyViewHolder(
        itemView: View,
        private val binding: ItemCurrencyBinding
    ) : RecyclerView.ViewHolder(itemView) {

        fun bind(item: Currency) {
            binding.currencyItem.apply {
                setLongName(CurrencyUtil.getCurrencyFullName(item.code))
                setShortName(item.code)
                setValue(item.value)
                setIcon(CurrencyUtil.getCurrencyIcon(item.code))
                setSign(CurrencyUtil.getCurrencySignBy(item.code))
            }
        }

        fun bindWithPayload(value: BigDecimal) {
            binding.currencyItem.setValue(value)
        }
    }
}
