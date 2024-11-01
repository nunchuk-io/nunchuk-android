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

package com.nunchuk.android.wallet.personal.components

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.nunchuk.android.core.base.BaseCameraFragment
import com.nunchuk.android.core.portal.PortalDeviceArgs
import com.nunchuk.android.core.portal.PortalDeviceFlow
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.core.util.openSelectFileChooser
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.wallet.WalletSecurityArgs
import com.nunchuk.android.core.wallet.WalletSecurityType
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.RecoverWalletData
import com.nunchuk.android.model.RecoverWalletType
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.share.ColdcardAction
import com.nunchuk.android.wallet.personal.R
import com.nunchuk.android.wallet.personal.components.recover.RecoverWalletActionBottomSheet
import com.nunchuk.android.wallet.personal.components.recover.RecoverWalletOption
import com.nunchuk.android.wallet.personal.databinding.FragmentWalletIntermediaryBinding
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WalletIntermediaryFragment : BaseCameraFragment<FragmentWalletIntermediaryBinding>(),
    BottomSheetOptionListener {
    private val viewModel: WalletIntermediaryViewModel by viewModels()
    private val isQuickWallet: Boolean by lazy { requireActivity().intent.getBooleanExtra("is_quick_wallet", false) }

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                requireActivity().apply {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        }

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): FragmentWalletIntermediaryBinding {
        return FragmentWalletIntermediaryBinding.inflate(inflater, container, false)
    }

    private val hasSigner
        get() = requireArguments().getBoolean(WalletIntermediaryActivity.EXTRA_HAS_SIGNER, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.init(isQuickWallet)
        initUi()
        setupViews()
        observer()
    }

    override fun onResume() {
        super.onResume()
        viewModel.getAssistedWalletConfig()
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

            SheetOptionType.TYPE_GROUP_WALLET -> {
                if (viewModel.isGroupWalletAvailable()) {
                    openCreateGroupWallet()
                } else {
                    showRunOutWallet(false)
                }
            }
            SheetOptionType.TYPE_PERSONAL_WALLET -> {
                if (viewModel.isPersonalWalletAvailable()) {
                    openCreateAssistedWallet()
                } else {
                    showRunOutWallet(true)
                }
            }
            SheetOptionType.TYPE_CREATE_NEW_WALLET -> {
                if (isQuickWallet) {
                    navigator.openCreateNewSeedScreen(this, true)
                } else if (hasSigner) {
                    openCreateNewWalletScreen()
                } else {
                    openWalletEmptySignerScreen()
                }
            }
            SheetOptionType.TYPE_CREATE_HOT_WALLET -> {
                navigator.openHotWalletScreen(launcher, requireActivity(), isQuickWallet)
            }
            SheetOptionType.TYPE_CREATE_NEW_DECOY_WALLET -> {
                navigator.openWalletSecuritySettingScreen(
                    activityContext = requireContext(),
                    args = WalletSecurityArgs(type = WalletSecurityType.CREATE_DECOY_WALLET)
                )
            }
        }
    }

    private fun showRunOutWallet(isPersonalWallet: Boolean) {
        val message = if (isPersonalWallet) {
            getString(R.string.nc_run_out_of_personal_wallet)
        } else {
            getString(R.string.nc_run_out_of_group_wallet)
        }
        NCInfoDialog(requireActivity()).init(
            message = message,
            btnYes = getString(R.string.nc_take_me_there),
            btnInfo = getString(R.string.nc_text_got_it),
            onYesClick = {
                requireActivity().openExternalLink("https://nunchuk.io/my-plan")
            }
        ).show()
    }

    private fun observer() {
        flowObserver(viewModel.event) {
            when (it) {
                is WalletIntermediaryEvent.OnLoadFileSuccess -> handleLoadFilePath(it)
                is WalletIntermediaryEvent.ShowError -> showError(it.msg)
                is WalletIntermediaryEvent.Loading -> showOrHideLoading(it.isLoading)
                WalletIntermediaryEvent.NoSigner -> showNoSignerDialog()
            }
        }
        flowObserver(viewModel.state) {
            val isCreateAssistedWalletVisible = it.isMembership
            binding.btnCreateGroupWallet.apply {
                isVisible = isCreateAssistedWalletVisible
                text =
                    if (it.personalSteps.isNotEmpty()) {
                        getString(R.string.nc_continue_setting_your_wallet)
                    } else {
                        context.getString(
                            R.string.nc_create_assisted_wallet,
                            it.walletsCount.values.sum()
                        )
                    }
            }
            val assistedVisible = binding.btnCreateGroupWallet.isVisible
            binding.btnCreateNewWallet.setBackgroundResource(if (assistedVisible) R.drawable.nc_rounded_light_background else R.drawable.nc_rounded_dark_background)
            val textColor = ContextCompat.getColor(
                requireActivity(),
                if (assistedVisible) R.color.nc_fill_primary else R.color.nc_text_primary
            )
            binding.btnCreateNewWallet.setTextColor(textColor)
        }
    }

    private fun showNoSignerDialog() {
        NCInfoDialog(requireActivity()).init(
            message = getString(R.string.nc_no_signer_dialog_message),
            btnYes = getString(R.string.nc_add_key),
            btnInfo = getString(R.string.nc_cancel),
            onYesClick = {
                navigator.openSignerIntroScreen(requireActivity())
            }
        ).show()
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
        if (isQuickWallet) {
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
                RecoverWalletOption.HotWallet -> navigator.openRecoverSeedScreen(
                    activityContext = requireActivity(),
                    isRecoverHotWallet = true
                )

                RecoverWalletOption.PortalWallet -> navigator.openPortalScreen(
                    activity = requireActivity(),
                    args = PortalDeviceArgs(
                        type = PortalDeviceFlow.RECOVER
                    )
                )
            }
        }
    }

    private fun openScanQRCodeScreen() {
        navigator.openRecoverWalletQRCodeScreen(requireContext(), false)
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
            showCreateWalletOption()
        }
        binding.btnRecoverWallet.setOnClickListener {
            if (isQuickWallet) {
                navigator.openWalletIntermediaryScreen(requireActivity(), viewModel.hasSigner)
                requireActivity().finish()
            } else {
                openRecoverWalletScreen()
            }
        }
        binding.btnCreateGroupWallet.setOnDebounceClickListener {
            val state = viewModel.state.value
            val walletCount = state.walletsCount.values.sum()
            if (state.personalSteps.isNotEmpty()) {
                openCreateAssistedWallet()
            } else if (viewModel.isPersonalWalletAvailable() && walletCount == 1) {
                openCreateAssistedWallet()
            } else if (viewModel.isGroupWalletAvailable() && walletCount == 1) {
                openCreateGroupWallet()
            } else {
                showOptionGroupWalletType()
            }
        }
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    private fun openCreateGroupWallet() {
        navigator.openMembershipActivity(
            activityContext = requireActivity(),
            groupStep = MembershipStage.NONE,
            isPersonalWallet = false
        )
    }

    private fun openCreateAssistedWallet() {
        val walletType = when {
            viewModel.state.value.personalSteps.any { it.plan == MembershipPlan.IRON_HAND } -> GroupWalletType.TWO_OF_THREE_PLATFORM_KEY
            viewModel.state.value.personalSteps.any { it.plan == MembershipPlan.HONEY_BADGER } -> GroupWalletType.TWO_OF_FOUR_MULTISIG
            else -> null
        }
        when(walletType) {
            GroupWalletType.TWO_OF_THREE_PLATFORM_KEY -> viewModel.setLocalMembershipPlan(MembershipPlan.IRON_HAND)
            GroupWalletType.TWO_OF_FOUR_MULTISIG -> viewModel.setLocalMembershipPlan(MembershipPlan.HONEY_BADGER)
            else -> Unit
        }
        navigator.openMembershipActivity(
            activityContext = requireActivity(),
            groupStep = viewModel.getGroupStage(),
            isPersonalWallet = true,
            walletType = walletType
        )
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

    private fun showOptionGroupWalletType() {
        BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    SheetOptionType.TYPE_GROUP_WALLET,
                    stringId = R.string.nc_group_wallet,
                ),
                SheetOption(
                    SheetOptionType.TYPE_PERSONAL_WALLET,
                    stringId = R.string.nc_personal_wallet,
                ),
            ),
            title = getString(R.string.nc_type_of_assisted_wallet)
        ).show(childFragmentManager, "BottomSheetOption")
    }

    private fun showCreateWalletOption() {
        BottomSheetOption.newInstance(
            options = listOf(
                SheetOption(
                    type = SheetOptionType.TYPE_CREATE_NEW_WALLET,
                    resId = R.drawable.ic_circle_new_wallet,
                    stringId = R.string.nc_create_new_wallet,
                    subStringId = R.string.nc_create_new_wallet_desc
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_CREATE_HOT_WALLET,
                    resId = R.drawable.ic_circle_hot_wallet,
                    stringId = R.string.nc_create_hot_wallet,
                    subStringId = R.string.nc_create_hot_wallet_desc,
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_CREATE_NEW_DECOY_WALLET,
                    resId = R.drawable.ic_circle_decoy_wallet,
                    stringId = R.string.nc_create_new_decoy_wallet,
                    subStringId = R.string.nc_create_new_decoy_wallet_desc
                ),
            )
        ).show(childFragmentManager, "BottomSheetOption")
    }
}