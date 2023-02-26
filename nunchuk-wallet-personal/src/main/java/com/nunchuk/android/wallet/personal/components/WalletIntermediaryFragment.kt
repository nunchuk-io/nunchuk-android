/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

package com.nunchuk.android.wallet.personal.components

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.core.base.BaseCameraFragment
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.*
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.RecoverWalletData
import com.nunchuk.android.model.RecoverWalletType
import com.nunchuk.android.share.ColdcardAction
import com.nunchuk.android.wallet.personal.R
import com.nunchuk.android.wallet.personal.components.recover.RecoverWalletActionBottomSheet
import com.nunchuk.android.wallet.personal.components.recover.RecoverWalletOption
import com.nunchuk.android.wallet.personal.databinding.FragmentWalletIntermediaryBinding
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WalletIntermediaryFragment : BaseCameraFragment<FragmentWalletIntermediaryBinding>(),
    BottomSheetOptionListener {
    private val viewModel: WalletIntermediaryViewModel by viewModels()
    private val args: WalletIntermediaryFragmentArgs by navArgs()

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentWalletIntermediaryBinding {
        return FragmentWalletIntermediaryBinding.inflate(inflater, container, false)
    }

    private val hasSigner
        get() = requireArguments().getBoolean(WalletIntermediaryActivity.EXTRA_HAS_SIGNER, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUi()
        setupViews()
        observer()
    }

    override fun onCameraPermissionGranted(fromUser: Boolean) {
        openScanQRCodeScreen()
    }

    override fun onOptionClicked(option: SheetOption) {
        when (option.type) {
            SheetOptionType.IMPORT_MULTI_SIG_COLD_CARD -> navigator.openSetupMk4(
                requireActivity(), false, ColdcardAction.RECOVER_MULTI_SIG_WALLET
            )
            SheetOptionType.IMPORT_SINGLE_SIG_COLD_CARD -> navigator.openSetupMk4(
                requireActivity(), false, ColdcardAction.RECOVER_SINGLE_SIG_WALLET
            )
        }
    }

    private fun observer() {
        flowObserver(viewModel.event) {
            when (it) {
                is WalletIntermediaryEvent.OnLoadFileSuccess -> handleLoadFilePath(it)
                is WalletIntermediaryEvent.ShowError -> showError(it.msg)
                is WalletIntermediaryEvent.Loading -> showOrHideLoading(it.isLoading)
            }
        }
        flowObserver(viewModel.state) {
            val isCreateAssistedWalletVisible =
                it.plan == MembershipPlan.HONEY_BADGER && it.assistedWallets.size < MAX_HONEY_BADGER_ASSISTED_WALLET_COUNT
            binding.btnCreateAssistedWallet.apply {
                isVisible = isCreateAssistedWalletVisible
                text = context.getString(
                    R.string.nc_create_assisted_wallet,
                    MAX_HONEY_BADGER_ASSISTED_WALLET_COUNT - it.assistedWallets.size
                )
            }
            binding.btnCreateNewWallet.setBackgroundResource(if (isCreateAssistedWalletVisible) R.drawable.nc_rounded_light_background else R.drawable.nc_rounded_dark_background)
            val textColor = ContextCompat.getColor(requireActivity(), if (isCreateAssistedWalletVisible) R.color.nc_primary_color else R.color.nc_white_color)
            binding.btnCreateNewWallet.setTextColor(textColor)
        }
    }

    private fun handleLoadFilePath(it: WalletIntermediaryEvent.OnLoadFileSuccess) {
        if (it.path.isNotEmpty()) {
            navigator.openAddRecoverWalletScreen(
                requireActivity(), RecoverWalletData(
                    type = RecoverWalletType.FILE,
                    filePath = it.path
                )
            )
            requireActivity().finish()
        }
    }

    private fun initUi() {
        if (args.isQuickWallet) {
            binding.title.isVisible = true
            binding.message.text = getString(R.string.nc_create_single_sig_for_sweep)
            binding.btnCreateNewWallet.text = getString(R.string.nc_text_continue)
            binding.btnRecoverWallet.text = getString(R.string.nc_create_my_own_wallet)
        }
    }

    private fun openCreateNewWalletScreen() {
        navigator.openAddWalletScreen(requireContext())
    }

    private fun openRecoverWalletScreen() {
        val recoverWalletBottomSheet = RecoverWalletActionBottomSheet.show(childFragmentManager)
        recoverWalletBottomSheet.listener = {
            when (it) {
                RecoverWalletOption.QrCode -> requestCameraPermissionOrExecuteAction()
                RecoverWalletOption.BSMSFile -> openSelectFileChooser(WalletIntermediaryActivity.REQUEST_CODE)
                RecoverWalletOption.ColdCard -> showOptionImportFromColdCard()
            }
        }
    }

    private fun openScanQRCodeScreen() {
        navigator.openRecoverWalletQRCodeScreen(requireContext())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == WalletIntermediaryActivity.REQUEST_CODE && resultCode == AppCompatActivity.RESULT_OK) {
            intent?.data?.let {
                viewModel.extractFilePath(it)
            }
        }
    }

    private fun setupViews() {
        binding.btnCreateNewWallet.setOnClickListener {
            if (args.isQuickWallet) {
                navigator.openCreateNewSeedScreen(this, true)
            } else if (hasSigner) {
                openCreateNewWalletScreen()
            } else {
                openWalletEmptySignerScreen()
            }
        }
        binding.btnRecoverWallet.setOnClickListener {
            if (args.isQuickWallet) {
                navigator.openWalletIntermediaryScreen(requireActivity(), viewModel.hasSigner)
                requireActivity().finish()
            } else {
                openRecoverWalletScreen()
            }
        }
        binding.btnCreateAssistedWallet.setOnDebounceClickListener {
            navigator.openMembershipActivity(
                requireActivity(),
                viewModel.getGroupStage()
            )
        }
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    private fun openWalletEmptySignerScreen() {
        navigator.openWalletEmptySignerScreen(requireActivity())
    }

    private fun showOptionImportFromColdCard() {
        BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    SheetOptionType.IMPORT_SINGLE_SIG_COLD_CARD,
                    stringId = R.string.nc_single_sig_wallet,
                ),
                SheetOption(
                    SheetOptionType.IMPORT_MULTI_SIG_COLD_CARD,
                    stringId = R.string.nc_multisig_wallet,
                ),
            ),
            title = getString(R.string.nc_which_type_wallet_you_want_import)
        ).show(childFragmentManager, "BottomSheetOption")
    }
}