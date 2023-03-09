package com.nunchuk.android.main.components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.components.tabs.AssistedWalletViewModel
import com.nunchuk.android.main.databinding.BottomSheetAssistedWalletBinding
import com.nunchuk.android.share.result.GlobalResultKey
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AssistedWalletBottomSheet : BaseBottomSheet<BottomSheetAssistedWalletBinding>() {
    private val viewModel by viewModels<AssistedWalletViewModel>()

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): BottomSheetAssistedWalletBinding {
        return BottomSheetAssistedWalletBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val assistedWalletIds = requireArguments().getStringArrayList(EXTRA_WALLET_IDS).orEmpty()
        viewModel.loadWallets(assistedWalletIds)
        flowObserver(viewModel.state) { wallets ->
            WalletsViewBinder(
                container = binding.walletList,
                wallets = wallets,
                assistedWalletIds = assistedWalletIds.toSet(),
                callback = {
                    setFragmentResult(
                        TAG, bundleOf(GlobalResultKey.WALLET_ID to it)
                    )
                    dismissAllowingStateLoss()
                }
            ).bindItems()
        }
    }

    companion object {
        const val TAG = "AddContactsBottomSheet"
        private const val EXTRA_WALLET_IDS = "wallet_ids"

        fun show(fragmentManager: FragmentManager, assistedWalletIds: List<String>) = AssistedWalletBottomSheet().apply {
            arguments = Bundle().apply {
                putStringArrayList(EXTRA_WALLET_IDS, ArrayList(assistedWalletIds))
            }
            show(fragmentManager, TAG)
        }
    }
}