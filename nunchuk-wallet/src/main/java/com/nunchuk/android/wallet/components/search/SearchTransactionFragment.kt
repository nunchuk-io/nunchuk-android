package com.nunchuk.android.wallet.components.search

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.PagingData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.utils.textChanges
import com.nunchuk.android.wallet.components.coin.filter.CoinFilterFragment
import com.nunchuk.android.wallet.components.coin.filter.CoinFilterFragmentArgs
import com.nunchuk.android.wallet.components.details.TransactionAdapter
import com.nunchuk.android.wallet.databinding.FragmentSearchTransactionBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchTransactionFragment : BaseFragment<FragmentSearchTransactionBinding>() {

    private val viewModel: SearchTransactionViewModel by viewModels()
    private val args: SearchTransactionFragmentArgs by navArgs()

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSearchTransactionBinding {
        return FragmentSearchTransactionBinding.inflate(inflater, container, false)
    }

    private val adapter: TransactionAdapter = TransactionAdapter {
        navigator.openTransactionDetailsScreen(
            activityContext = requireActivity(),
            walletId = args.walletId,
            txId = it.txId,
            roomId = args.roomId
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()

        setFragmentResultListener(CoinFilterFragment.REQUEST_KEY) { _, bundle ->
            val filter = CoinFilterFragmentArgs.fromBundle(bundle)
            viewModel.updateFilter(filter.filter)
            clearFragmentResult(CoinFilterFragment.REQUEST_KEY)
        }
    }

    private fun setupViews() {
        binding.input.requestFocus()

        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(binding.input, InputMethodManager.SHOW_IMPLICIT)

        binding.transactionList.layoutManager =
            LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding.transactionList.isNestedScrollingEnabled = false
        binding.transactionList.setHasFixedSize(false)
        binding.transactionList.adapter = adapter
        binding.input.doAfterTextChanged {
            viewModel.search(it.toString())
            binding.ivClear.isVisible = it.toString().isNotBlank()
        }
        binding.ivClear.setOnClickListener {
            binding.input.setText("")
        }
        binding.ivFilter.setOnClickListener {
            findNavController().navigate(
                SearchTransactionFragmentDirections.actionSearchTransactionFragmentToCoinFilterFragment(
                    filter = viewModel.filter.value,
                    isSearchTransaction = true
                )
            )
        }
        binding.ivBack.setOnClickListener {
            requireActivity().finish()
        }

        flowObserver(viewModel.state) {
            adapter.submitData(PagingData.from(it.transactions))
        }
    }
}