package io.github.alxiw.reactivecurrencies.presentation

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.alxiw.reactivecurrencies.R
import io.github.alxiw.reactivecurrencies.presentation.recycler.CurrenciesAdapter
import io.github.alxiw.reactivecurrencies.data.model.Currency
import io.github.alxiw.reactivecurrencies.presentation.listeners.OnItemClickListener
import io.github.alxiw.reactivecurrencies.presentation.listeners.OnValueChangeListener
import com.google.android.material.snackbar.Snackbar
import io.github.alxiw.reactivecurrencies.Dependencies
import io.github.alxiw.reactivecurrencies.databinding.FragmentCurrenciesBinding
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import java.math.BigDecimal

class CurrenciesFragment : Fragment() {

    private val viewModel: CurrenciesViewModel by viewModels { Dependencies.viewModelFactory }

    private lateinit var binding: FragmentCurrenciesBinding

    private var adapter = CurrenciesAdapter()

    private var snackBar: Snackbar? = null

    private var disposable: Disposable? = null

    @SuppressLint("RestrictedApi")
    private var savedState: LinearLayoutManager.SavedState? = null

    private val onItemClickListener = object : OnItemClickListener<Currency> {
        override fun onItemClick(item: Currency, position: Int) {
            viewModel.onCurrencyClick(item)
            val looper = Looper.getMainLooper()
            Handler(looper).postDelayed(
                { binding.currenciesList.smoothScrollToPosition(0) },
                500
            )
        }
    }

    private val onValueChangeListener = object : OnValueChangeListener<Currency, BigDecimal> {
        override fun onValueChanged(
            item: Currency,
            value: BigDecimal,
            position: Int
        ) {
            viewModel.onValueChange(Currency(item.code, value, true))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val lm = binding.currenciesList.layoutManager as LinearLayoutManager
        outState.putParcelable(RECYCLER_VIEW_STATE_TAG, lm.onSaveInstanceState())
    }

    @SuppressLint("RestrictedApi")
    private fun restoreSavedInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState ?: return
        savedState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            savedInstanceState.getParcelable<LinearLayoutManager.SavedState>(
                RECYCLER_VIEW_STATE_TAG,
                LinearLayoutManager.SavedState::class.java
            )
        } else {
            savedInstanceState.getParcelable<LinearLayoutManager.SavedState>(
                RECYCLER_VIEW_STATE_TAG
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_currencies, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentCurrenciesBinding.bind(view)
        restoreSavedInstanceState(savedInstanceState)

        binding.currenciesProgressBar.isVisible = true
        binding.currenciesSwipeRefresh.isEnabled = false

        binding.currenciesList.also {
            it.setHasFixedSize(true)
            val lm = LinearLayoutManager(requireContext())
            it.layoutManager = lm
            it.addItemDecoration(DividerItemDecoration(activity, lm.orientation))
            it.itemAnimator = DefaultItemAnimator()
            it.adapter = adapter.apply {
                itemClickListener = onItemClickListener
                valueChangeLister = onValueChangeListener
            }
        }

        binding.currenciesSwipeRefresh.apply {
            setProgressBackgroundColorSchemeResource(
                R.color.swipeRefreshBackground
            )
            setColorSchemeResources(
                R.color.swipeRefreshProgressOne,
                R.color.swipeRefreshProgressTwo
            )
            setOnRefreshListener {
                snackBar?.dismiss()
                viewModel.updateAllCurrencies(fromUi = true)
            }
        }

        viewModel.init()

        disposable = viewModel.eventObservable
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { event ->
                handleEvent(event)
            }

        viewModel.getAllCurrencies(fromUi = true)
    }

    override fun onDestroyView() {
        disposable?.dispose()
        viewModel.clear()
        super.onDestroyView()
    }

    private fun handleEvent(event: CurrenciesViewModel.LoadEvent) {
        when (event) {
            is CurrenciesViewModel.LoadEvent.ShowStub -> {
                binding.currenciesSwipeRefresh.isEnabled = true
                binding.currenciesSwipeRefresh.isRefreshing = false

                binding.currenciesStub.inflate()

                binding.currenciesList.isVisible = false
                binding.currenciesProgressBar.isVisible = false
                binding.currenciesStub.isVisible = true
            }
            is CurrenciesViewModel.LoadEvent.ShowLoadingError -> {
                binding.currenciesSwipeRefresh.isEnabled = true
                binding.currenciesSwipeRefresh.isRefreshing = false

                snackBar = Snackbar.make(
                    binding.currenciesSwipeRefresh,
                    getString(R.string.error_loading),
                    Snackbar.LENGTH_INDEFINITE
                )
                snackBar?.view?.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.snackbarBackgroundColor)
                )
                snackBar?.setAction(getString(R.string.retry)) { viewModel.retryCall() }?.show()
            }
            is CurrenciesViewModel.LoadEvent.ShowLoadingSuccess -> {
                snackBar?.dismiss()
                snackBar = Snackbar.make(
                    binding.currenciesList,
                    String.format(getString(R.string.success_loading), event.info),
                    Snackbar.LENGTH_SHORT
                )
                snackBar?.view?.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.snackbarBackgroundColor)
                )
                snackBar?.show()
            }
            is CurrenciesViewModel.LoadEvent.ShowList -> {
                binding.currenciesSwipeRefresh.isEnabled = true
                binding.currenciesSwipeRefresh.isRefreshing = false

                binding.currenciesList.isVisible = true
                binding.currenciesProgressBar.isVisible = false
                binding.currenciesStub.isVisible = false

                adapter.updateCurrencies(event.list)

                if (event.useSavedState) {
                    binding.currenciesList.layoutManager?.onRestoreInstanceState(savedState)
                    savedState = null
                }
            }
            is CurrenciesViewModel.LoadEvent.ShowRefreshing -> {
                binding.currenciesSwipeRefresh.isEnabled = true
                binding.currenciesSwipeRefresh.isRefreshing = true

                snackBar?.dismiss()
            }
            is CurrenciesViewModel.LoadEvent.ShowUpdatingError -> {
                snackBar = Snackbar.make(
                    binding.currenciesList,
                    getString(R.string.error_updating),
                    Snackbar.LENGTH_SHORT
                )
                snackBar?.view?.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.snackbarBackgroundColor)
                )
                snackBar?.show()
            }
        }
    }

    companion object {
        private const val RECYCLER_VIEW_STATE_TAG = "recycler_view_state"

        @JvmStatic
        fun newInstance() = CurrenciesFragment()
    }
}
