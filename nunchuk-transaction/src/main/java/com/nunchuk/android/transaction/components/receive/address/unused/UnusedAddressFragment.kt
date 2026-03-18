/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.transaction.components.receive.address.unused

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.domain.data.VerifyAddress
import com.nunchuk.android.core.nfc.BasePortalActivity
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.TextUtils
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.receive.ReceiveTransactionActivity
import com.nunchuk.android.transaction.components.receive.TabCountChangeListener
import com.nunchuk.android.transaction.components.receive.address.AddressFragmentArgs
import com.nunchuk.android.transaction.components.receive.address.AddressTab
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningVerticalDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class UnusedAddressFragment : BaseFragment<ViewBinding>(),
    BottomSheetOptionListener {

    @Inject
    lateinit var textUtils: TextUtils

    private val controller: IntentSharingController by lazy {
        IntentSharingController.from(requireActivity())
    }

    private val args: AddressFragmentArgs by lazy { AddressFragmentArgs.deserializeFrom(arguments) }

    private val viewModel: UnusedAddressViewModel by viewModels()

    private var currentPage = 0

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): ViewBinding = ViewBinding {
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val state by viewModel.state.collectAsStateWithLifecycle()

                NunchukTheme {
                    UnusedAddressContent(
                        addresses = state.addresses,
                        onAddressClick = ::copyAddress,
                        onGenerateAddressClick = viewModel::generateAddress,
                        onShareClick = ::handleShareAddress,
                        onCopyClick = ::handleCopyAddress,
                        onCopyAddress = ::copyAddress,
                        onMoreClick = ::showMoreOptions,
                        onPageChanged = { page -> currentPage = page },
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeEvent()
        viewModel.init(args.walletId)
    }

    private fun observeEvent() {
        flowObserver(viewModel.event) { event ->
            when (event) {
                is UnusedAddressEvent.GetAddressPathSuccessEvent -> {
                    copyDerivationPath(event.address)
                }

                is UnusedAddressEvent.MarkAddressAsUsedSuccessEvent -> {
                    NCToastMessage(requireActivity()).showMessage(getString(R.string.nc_address_marked_as_used))
                    (requireActivity() as? ReceiveTransactionActivity)?.switchToUsedTab(AddressTab.USED)
                }

                is UnusedAddressEvent.GenerateAddressErrorEvent -> {
                    activity?.let { NCToastMessage(it).showError(event.message) }
                }
            }
        }

        flowObserver(viewModel.state) { state ->
            (requireActivity() as TabCountChangeListener).onChange(
                AddressTab.UNUSED,
                state.addresses.size,
            )
        }
    }

    private fun getCurrentAddress(): String? {
        val addresses = viewModel.state.value.addresses
        return addresses.getOrNull(currentPage)
    }

    private fun copyAddress(address: String) {
        textUtils.copyText(text = address)
        NCToastMessage(requireActivity()).showMessage(getString(R.string.nc_address_copy_to_clipboard))
    }

    private fun copyDerivationPath(address: String) {
        textUtils.copyText(text = address)
        NCToastMessage(requireActivity()).showMessage(getString(R.string.nc_address_derivation_path_have_been_copied))
    }

    private fun handleShareAddress() {
        getCurrentAddress()?.let(controller::shareText)
    }

    private fun handleCopyAddress() {
        getCurrentAddress()?.let(::copyAddress)
    }

    private fun showMoreOptions() {
        val options = mutableListOf(
            SheetOption(
                type = SheetOptionType.TYPE_MARK_ADDRESS_AS_USED,
                resId = R.drawable.ic_mark_address,
                label = getString(R.string.nc_mark_address_as_used),
            )
        )
        options.add(
            SheetOption(
                type = SheetOptionType.TYPE_VERIFY_ADDRESS_DEVICE,
                resId = R.drawable.ic_visibility,
                label = getString(R.string.nc_verify_address_via_portal),
            )
        )
        if (viewModel.isSingleSignWallet()) {
            options.add(
                SheetOption(
                    type = SheetOptionType.TYPE_ADDRESS_DERIVATION_PATH,
                    resId = R.drawable.ic_copy,
                    label = getString(R.string.nc_copy_address_derivation_path),
                )
            )
        }
        if (options.isEmpty()) return
        BottomSheetOption.newInstance(options).show(childFragmentManager, "BottomSheetOption")
    }

    override fun onOptionClicked(option: SheetOption) {
        when (option.type) {
            SheetOptionType.TYPE_VERIFY_ADDRESS_DEVICE -> {
                viewLifecycleOwner.lifecycleScope.launch {
                    val address = getCurrentAddress().orEmpty()
                    if (address.isNotBlank()) {
                        val index = viewModel.getAddressIndex(address)
                        if (index != -1) {
                            (requireActivity() as BasePortalActivity<*>).handlePortalAction(
                                VerifyAddress(
                                    address = address,
                                    index = index
                                )
                            )
                        }
                    }
                }
            }

            SheetOptionType.TYPE_ADDRESS_DERIVATION_PATH -> {
                viewModel.getAddressPath(getCurrentAddress().orEmpty())
            }

            SheetOptionType.TYPE_MARK_ADDRESS_AS_USED -> {
                showMarkAddressAsUsedDialog()
            }

            else -> {}
        }
    }

    private fun showMarkAddressAsUsedDialog() {
        NCWarningVerticalDialog(requireActivity()).showDialog(
            title = getString(R.string.nc_confirmation),
            message = getString(R.string.nc_mark_address_as_used_message),
            btnYes = getString(R.string.nc_text_continue),
            btnNo = getString(R.string.nc_text_cancel),
            onYesClick = {
                viewModel.markAddressAsUsed(getCurrentAddress().orEmpty())
            }
        )
    }

    companion object {
        fun newInstance(walletId: String) = UnusedAddressFragment().apply {
            arguments = AddressFragmentArgs(walletId = walletId).buildBundle()
        }
    }
}
