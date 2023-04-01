package com.nunchuk.android.wallet.components.coin.collection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.orFalse
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.databinding.BottomSheetCoinCollectionBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CoinCollectionBottomSheetFragment : BaseBottomSheet<BottomSheetCoinCollectionBinding>() {

    private val viewModel: CoinCollectionBottomSheetViewModel by viewModels()
    private val args: CoinCollectionBottomSheetFragmentArgs by navArgs()

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): BottomSheetCoinCollectionBinding {
        return BottomSheetCoinCollectionBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setCanceledOnTouchOutside(false)
        setupViews()
    }

    private fun setupViews() {
        binding.collectionNameInput.getEditTextView().doAfterTextChanged {
            binding.createCollectionButton.isEnabled = it.toString().isNotBlank()
        }
        binding.collectionNameInput.getEditTextView().setText(args.coinCollection?.name.orEmpty())
        binding.createCollectionButton.setOnClickListener {
            viewModel.createCoinCollection(binding.collectionNameInput.getEditText().trim())
        }
        binding.switchButtonAutoLock.isChecked = args.coinCollection?.isAutoLock.orFalse()
        binding.switchButtonMoveNewCoins.isChecked = args.coinCollection?.isAddNewCoin.orFalse()

        binding.switchButtonAutoLock.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAutoLock(isChecked)
        }
        binding.switchButtonMoveNewCoins.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAddNewCoin(isChecked)
        }
        val buttonText =
            if (args.flow == CollectionFlow.ADD) getString(R.string.nc_create_collection) else getString(
                R.string.nc_update_collection_setting
            )
        binding.createCollectionButton.text = buttonText
        binding.avatarHolder.text = args.coinCollection?.name?.shorten()
        flowObserver(viewModel.event) { event ->
            when (event) {
                is CoinCollectionBottomSheetEvent.Error -> showError(message = event.message)
                CoinCollectionBottomSheetEvent.CreateOrUpdateCollectionSuccess -> {
                    setFragmentResult(
                        REQUEST_KEY,
                        bundleOf(EXTRA_COIN_COLLECTION to viewModel.getCoinCollection())
                    )
                    dismissAllowingStateLoss()
                }
            }
        }
    }

    companion object {
        const val REQUEST_KEY = "CoinCollectionBottomSheetFragment"
        const val EXTRA_COIN_COLLECTION = "EXTRA_COIN_COLLECTION"
    }
}