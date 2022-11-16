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

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcActionListener
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.*
import com.nunchuk.android.model.RecoverWalletData
import com.nunchuk.android.model.RecoverWalletType
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.wallet.personal.R
import com.nunchuk.android.wallet.personal.components.recover.RecoverWalletActionBottomSheet
import com.nunchuk.android.wallet.personal.components.recover.RecoverWalletOption
import com.nunchuk.android.wallet.personal.databinding.FragmentWalletIntermediaryBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter

@AndroidEntryPoint
class WalletIntermediaryFragment : BaseFragment<FragmentWalletIntermediaryBinding>(),
    BottomSheetOptionListener {
    private val viewModel: WalletIntermediaryViewModel by viewModels()
    private val nfcViewModel: NfcViewModel by activityViewModels()
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

    override fun onOptionClicked(option: SheetOption) {
        when (option.type) {
            SheetOptionType.IMPORT_MULTI_SIG_COLD_CARD -> (activity as? NfcActionListener)?.startNfcFlow(
                BaseNfcActivity.REQUEST_IMPORT_MULTI_WALLET_FROM_MK4
            )
            SheetOptionType.IMPORT_SINGLE_SIG_COLD_CARD -> (activity as? NfcActionListener)?.startNfcFlow(
                BaseNfcActivity.REQUEST_IMPORT_SINGLE_WALLET_FROM_MK4
            )
            else -> {
                viewModel.createWallet(option.id.orEmpty())
            }
        }
    }

    private fun observer() {
        flowObserver(viewModel.event) {
            showOrHideNfcLoading(it is WalletIntermediaryEvent.NfcLoading)
            when (it) {
                is WalletIntermediaryEvent.OnLoadFileSuccess -> handleLoadFilePath(it)
                is WalletIntermediaryEvent.ImportWalletFromMk4Success -> openRecoverWalletName(it.walletId)
                is WalletIntermediaryEvent.ShowError -> showError(it.msg)
                is WalletIntermediaryEvent.ExtractWalletsFromColdCard -> showWallets(it.wallets)
                is WalletIntermediaryEvent.Loading -> showOrHideLoading(it.isLoading)
                is WalletIntermediaryEvent.NfcLoading -> showOrHideNfcLoading(it.isLoading)
            }
        }
        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_IMPORT_MULTI_WALLET_FROM_MK4 }) {
            viewModel.importWalletFromMk4(it.records)
        }
        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_IMPORT_SINGLE_WALLET_FROM_MK4 }) {
            viewModel.getWalletsFromColdCard(it.records)
        }
    }

    private fun showWallets(wallets: List<Wallet>) {
        BottomSheetOption.newInstance(wallets.mapIndexed { index, wallet ->
            SheetOption(
                type = index,
                label = wallet.name,
                id = wallet.id,
            )
        }, title = getString(R.string.nc_sellect_wallet_type))
            .show(childFragmentManager, "BottomSheetOption")
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

    private fun openRecoverWalletName(walletId: String) {
        navigator.openAddRecoverWalletScreen(
            requireActivity(), RecoverWalletData(
                type = RecoverWalletType.COLDCARD,
                walletId = walletId
            )
        )
        requireActivity().finish()
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
                RecoverWalletOption.QrCode -> handleOptionUsingQRCode()
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
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
    }

    private fun openWalletEmptySignerScreen() {
        navigator.openWalletEmptySignerScreen(requireActivity())
    }

    private fun handleOptionUsingQRCode() {
        if (requireActivity().isPermissionGranted(Manifest.permission.CAMERA)) {
            openScanQRCodeScreen()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                WalletIntermediaryActivity.REQUEST_PERMISSION_CAMERA
            )
        }
    }


    // TODO: refactor with registerForActivityResult later
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == WalletIntermediaryActivity.REQUEST_PERMISSION_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                handlePermissionGranted()
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                showAlertPermissionNotGranted()
            } else {
                showAlertPermissionDeniedPermanently()
            }
        }
    }

    private fun handlePermissionGranted() {
        openScanQRCodeScreen()
    }

    private fun showAlertPermissionNotGranted() {
        requireActivity().showAlertDialog(
            title = getString(R.string.nc_text_title_permission_denied),
            message = getString(R.string.nc_text_des_permission_denied),
            positiveButtonText = getString(android.R.string.ok),
            negativeButtonText = getString(android.R.string.cancel),
            positiveClick = {
                handleOptionUsingQRCode()
            },
            negativeClick = {
            }
        )
    }

    private fun showAlertPermissionDeniedPermanently() {
        requireActivity().showAlertDialog(
            title = getString(R.string.nc_text_title_permission_denied_permanently),
            message = getString(R.string.nc_text_des_permission_denied_permanently),
            positiveButtonText = getString(android.R.string.ok),
            negativeButtonText = getString(android.R.string.cancel),
            positiveClick = {
                requireActivity().startActivityAppSetting()
            },
            negativeClick = {
            }
        )
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