package io.github.alxiw.reactivecurrencies.presentation

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
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
import io.github.alxiw.reactivecurrencies.App
import io.github.alxiw.reactivecurrencies.databinding.FragmentCurrenciesBinding
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import java.math.BigDecimal

class CurrenciesFragment : Fragment() {

    private val viewModel: CurrenciesViewModel by viewModels {
        (requireActivity().application as App).container.viewModelFactory
    }

    private lateinit var binding: FragmentCurrenciesBinding

    private var adapter = CurrenciesAdapter()

    private var snackBar: Snackbar? = null

    private var disposable: Disposable? = null

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_currencies, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentCurrenciesBinding.bind(view)

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

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            binding.currenciesList.updatePadding(
                left = systemBars.left,
                top = systemBars.top,
                right = systemBars.right,
                bottom = systemBars.bottom
            )

            val density = resources.displayMetrics.density
            val defaultOffset = (SWIPE_REFRESH_OFFSET_DEFAULT * density).toInt()
            binding.currenciesSwipeRefresh.setProgressViewOffset(
                false,
                systemBars.top,
                systemBars.top + defaultOffset
            )

            insets
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

    override fun onStop() {
        super.onStop()
        if (::binding.isInitialized) {
            (binding.currenciesList.layoutManager as? LinearLayoutManager)?.let { lm ->
                viewModel.saveScrollPosition(lm.findFirstVisibleItemPosition())
            }
        }
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
                    viewModel.restoreScrollPosition()?.let { position ->
                        binding.currenciesList.layoutManager?.scrollToPosition(position)
                    }
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
        private const val SWIPE_REFRESH_OFFSET_DEFAULT = 64

        @JvmStatic
        fun newInstance() = CurrenciesFragment()
    }
}
