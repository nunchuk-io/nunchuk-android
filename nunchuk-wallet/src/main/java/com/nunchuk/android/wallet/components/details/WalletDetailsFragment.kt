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

package com.nunchuk.android.wallet.components.details

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withResumed
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.core.base.BaseShareSaveFileFragment
import com.nunchuk.android.core.constants.RoomAction
import com.nunchuk.android.core.groupchathistory.GroupChatHistoryArgs
import com.nunchuk.android.core.groupchathistory.GroupChatHistoryFragment
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.CHOOSE_FILE_REQUEST_CODE
import com.nunchuk.android.core.util.TextUtils
import com.nunchuk.android.core.util.getFileFromUri
import com.nunchuk.android.core.util.hideKeyboard
import com.nunchuk.android.core.util.hideLoading
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.core.util.openSelectFileChooser
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.util.showSuccess
import com.nunchuk.android.nav.args.BackUpWalletArgs
import com.nunchuk.android.nav.args.BackUpWalletType
import com.nunchuk.android.nav.args.ClaimArgs
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.serializable
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.config.WalletConfigAction
import com.nunchuk.android.wallet.components.config.WalletConfigActivity
import com.nunchuk.android.wallet.databinding.FragmentWalletDetailBinding
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.model.HistoryPeriod
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WalletDetailsFragment : BaseShareSaveFileFragment<FragmentWalletDetailBinding>(),
    BottomSheetOptionListener {

    @Inject
    lateinit var textUtils: TextUtils

    @Inject
    lateinit var sessionHolder: SessionHolder

    @Inject
    lateinit var pushEventManager: PushEventManager

    private val viewModel: WalletDetailsViewModel by viewModels()
    private val args: WalletDetailsFragmentArgs by navArgs()

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data
            if (it.resultCode == Activity.RESULT_OK && data != null) {
                when (data.serializable<WalletConfigAction>(WalletConfigActivity.EXTRA_WALLET_ACTION)) {
                    WalletConfigAction.DELETE -> closeScreen()
                    WalletConfigAction.UPDATE_NAME -> viewModel.getWalletDetails(false)
                    WalletConfigAction.FORCE_REFRESH ->
                        viewModel.setForceRefreshWalletProcessing(true)

                    null -> Unit
                }
            }
        }

    override fun initializeBinding(
        inflater: LayoutInflater, container: ViewGroup?,
    ): FragmentWalletDetailBinding =
        FragmentWalletDetailBinding.inflate(inflater, container, false)

    override fun onResume() {
        super.onResume()
        with(viewModel) {
            checkDeprecatedGroupWallet()
            syncServerTransaction()
            syncData(loadingSilent = true)
            getGroupWalletMessageUnreadCount()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.hideKeyboard()

        binding.composeRoot.setContent {
            WalletDetailsScreen(
                viewModel = viewModel,
                onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                onSearch = ::handleSearch,
                onMenu = ::onMoreClicked,
                onToggleMask = viewModel::updateHideWalletDetailLocal,
                onSend = viewModel::handleSendMoneyEvent,
                onReceive = {
                    navigator.openReceiveTransactionScreen(requireActivity(), args.walletId)
                },
                onViewCoin = {
                    navigator.openCoinList(context = requireContext(), walletId = args.walletId)
                },
                onWalletConfig = {
                    navigator.openWalletConfigScreen(
                        launcher = launcher,
                        activityContext = requireActivity(),
                        walletId = args.walletId,
                        keyPolicy = args.keyPolicy,
                    )
                },
                onSpendable = {
                    navigator.openCoinList(context = requireContext(), walletId = args.walletId)
                },
                onTransactionClick = { tx ->
                    navigator.openTransactionDetailsScreen(
                        activityContext = requireActivity(),
                        walletId = args.walletId,
                        txId = tx.txId,
                        roomId = viewModel.getRoomWallet()?.roomId.orEmpty(),
                    )
                },
                onClaimInheritance = ::openClaiInheritance,
                onNeedBackup = ::onWarningClick,
                onBannerBackupAndRegister = ::onBannerBackupAndRegisterClick,
                onBannerBackupOnly = ::onBannerBackupOnlyClick,
                onBannerRegisterOnly = ::onBannerRegisterOnlyClick,
                onOpenExternalLink = { url -> requireActivity().openExternalLink(url) },
                onCopyAddress = ::copyAddress,
                onShareAddress = { controller.shareText(it) },
                onAcceptOrDenyReplaceGroup = viewModel::acceptOrDenyReplaceGroup,
                onOpenReplacementSetup = { groupId ->
                    navigator.openFreeGroupWalletScreen(
                        activityContext = requireActivity(),
                        groupId = groupId,
                    )
                },
                onSendMessage = viewModel::sendMessage,
                onOpenChat = {
                    navigator.openGroupChatScreen(
                        activityContext = requireActivity(),
                        walletId = args.walletId,
                    )
                },
            )
        }

        observeEvents()
        setupFragmentResult()
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (!viewModel.isForceRefreshProcessing) {
                    viewModel.syncData()
                } else {
                    viewModel.setForceRefreshWalletProcessing(false)
                }
            }
        }
        viewModel.event.observe(viewLifecycleOwner, ::handleEvent)
        viewLifecycleOwner.lifecycleScope.launch {
            pushEventManager.event.collectLatest {
                if (it is PushEvent.CloseWalletDetail) {
                    viewLifecycleOwner.lifecycle.withResumed { closeScreen() }
                }
            }
        }
    }

    private fun setupFragmentResult() {
        requireActivity().supportFragmentManager.setFragmentResultListener(
            GroupChatHistoryFragment.REQUEST_KEY, this
        ) { _: String, bundle: Bundle ->
            val historyPeriod =
                bundle.parcelable<HistoryPeriod>(GroupChatHistoryFragment.EXTRA_HISTORY_PERIOD)
                    ?: return@setFragmentResultListener
            viewModel.updateGroupChatHistoryPeriod(historyPeriod)
            showSuccess(message = getString(R.string.nc_chat_setting_updated))
            clearFragmentResult(GroupChatHistoryFragment.REQUEST_KEY)
        }
    }

    private fun handleEvent(event: WalletDetailsEvent) {
        when (event) {
            is WalletDetailsEvent.WalletDetailsError -> {
                hideLoading()
                NCToastMessage(requireActivity()).showError(event.message)
            }

            is WalletDetailsEvent.SendMoneyEvent -> openInputAmountScreen(event)
            is WalletDetailsEvent.Loading -> showOrHideLoading(event.loading)
            WalletDetailsEvent.ImportPSBTSuccess -> {
                viewModel.syncData()
                hideLoading()
                NCToastMessage(requireActivity()).showMessage(getString(R.string.nc_wallet_psbt_imported))
            }

            is WalletDetailsEvent.OpenSetupGroupWallet -> navigator.openFreeGroupWalletScreen(
                activityContext = requireActivity(),
                groupId = event.groupId,
            )

            is WalletDetailsEvent.SaveLocalFile -> {
                val message = if (event.isSuccess) getString(R.string.nc_save_file_success)
                else getString(R.string.nc_save_file_failed)
                if (event.isSuccess) NCToastMessage(requireActivity()).showMessage(message)
                else NCToastMessage(requireActivity()).showError(message)
            }

            is WalletDetailsEvent.ShareBSMS -> controller.shareFile(event.filePath)
        }
    }

    private fun openInputAmountScreen(event: WalletDetailsEvent.SendMoneyEvent) {
        val isStablecoin =
            event.walletExtended.wallet.walletType == com.nunchuk.android.type.WalletType.LIQUID
        if (event.walletExtended.isShared) {
            val roomWallet = event.walletExtended.roomWallet!!
            if (viewModel.isLeaveRoom) {
                sessionHolder.setActiveRoom(roomWallet.roomId, true)
                navigator.openInputAmountScreen(
                    activityContext = requireActivity(),
                    walletId = roomWallet.walletId,
                    availableAmount = event.walletExtended.wallet.balance.pureBTC(),
                    isStablecoin = isStablecoin,
                )
            } else {
                navigator.openRoomDetailActivity(
                    activityContext = requireActivity(),
                    roomId = event.walletExtended.roomWallet!!.roomId,
                    roomAction = RoomAction.SEND,
                )
            }
        } else {
            navigator.openInputAmountScreen(
                activityContext = requireActivity(),
                walletId = args.walletId,
                availableAmount = event.walletExtended.wallet.balance.pureBTC(),
                isStablecoin = isStablecoin,
            )
        }
    }

    override fun onOptionClicked(option: SheetOption) {
        super.onOptionClicked(option)
        when (option.type) {
            SheetOptionType.TYPE_IMPORT_TX -> showImportTransactionOption()
            SheetOptionType.TYPE_IMPORT_PSBT -> openSelectFileChooser()
            SheetOptionType.TYPE_IMPORT_PSBT_QR -> openImportTransactionScreen()
            SheetOptionType.TYPE_SEARCH_TX -> openSearchTransaction()
            SheetOptionType.TYPE_VIEW_WALLET_CONFIG -> navigator.openWalletConfigScreen(
                launcher = launcher,
                activityContext = requireActivity(),
                walletId = args.walletId,
                keyPolicy = args.keyPolicy,
            )

            SheetOptionType.TYPE_GROUP_CHAT_HISTORY -> {
                GroupChatHistoryFragment.show(
                    childFragmentManager,
                    GroupChatHistoryArgs(
                        historyPeriods = viewModel.state.value?.historyPeriods.orEmpty()
                            .sortedBy { it.id.toInt() },
                        historyPeriodIdSelected = viewModel.state.value?.selectedHistoryPeriod?.id
                            ?: "7",
                        isFreeGroupWalletFlow = viewModel.isFreeGroupWallet(),
                        walletId = args.walletId,
                        roomId = "",
                        groupId = "",
                    )
                )
            }

            else -> Unit
        }
    }

    override fun shareFile() {
        viewModel.handleExportBSMS(true)
    }

    override fun saveFileToLocal() {
        viewModel.handleExportBSMS(false)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == CHOOSE_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            intent?.data?.let {
                getFileFromUri(requireActivity().contentResolver, it, requireActivity().cacheDir)
            }?.absolutePath?.let(viewModel::handleImportPSBT)
        }
    }

    private fun handleSearch() {
        if (viewModel.isAssistedWallet || viewModel.isLockedAssistedWallet) {
            navigator.openGroupDashboardScreen(
                groupId = viewModel.groupId,
                walletId = args.walletId,
                activityContext = requireActivity(),
            )
        } else {
            openSearchTransaction()
        }
    }

    private fun openSearchTransaction() {
        if (viewModel.isHideWalletDetailLocal) return
        navigator.openSearchTransaction(
            requireContext(),
            walletId = args.walletId,
            roomId = viewModel.getRoomWallet()?.roomId.orEmpty(),
        )
    }

    private fun copyAddress(address: String) {
        textUtils.copyText(text = address)
        NCToastMessage(requireActivity()).showMessage(getString(R.string.nc_address_copy_to_clipboard))
    }

    private fun onMoreClicked() {
        val options = mutableListOf<SheetOption>()
        options.add(
            SheetOption(
                SheetOptionType.TYPE_IMPORT_TX,
                R.drawable.ic_import,
                R.string.nc_import_transaction,
            )
        )
        if (viewModel.isAssistedWallet || viewModel.isFreeGroupWallet()) {
            options.add(
                SheetOption(
                    SheetOptionType.TYPE_SEARCH_TX,
                    R.drawable.ic_search_dark,
                    R.string.nc_search_transactions,
                )
            )
        }
        if (viewModel.isFreeGroupWallet()) {
            options.add(
                SheetOption(
                    SheetOptionType.TYPE_GROUP_CHAT_HISTORY,
                    R.drawable.ic_clock,
                    R.string.nc_manage_group_chat_history,
                )
            )
        }
        // Liquid/stablecoin wallets don't surface "View wallet config" inline in the
        // header (unlike regular wallets, which render it as a link below the balance),
        // so add it to the overflow menu here.
        if (viewModel.getWallet().walletType == com.nunchuk.android.type.WalletType.LIQUID) {
            options.add(
                SheetOption(
                    SheetOptionType.TYPE_VIEW_WALLET_CONFIG,
                    R.drawable.ic_settings_dark,
                    R.string.nc_wallet_view_wallet_config,
                )
            )
        }
        BottomSheetOption.newInstance(options).show(childFragmentManager, "BottomSheetOption")
    }

    private fun showImportTransactionOption() {
        BottomSheetOption.newInstance(
            title = getString(R.string.nc_select_import_method),
            options = listOf(
                SheetOption(
                    SheetOptionType.TYPE_IMPORT_PSBT_QR,
                    R.drawable.ic_qr,
                    R.string.nc_import_via_qr,
                ),
                SheetOption(
                    SheetOptionType.TYPE_IMPORT_PSBT,
                    R.drawable.ic_import,
                    R.string.nc_import_via_file,
                ),
            )
        ).show(childFragmentManager, "BottomSheetOption")
    }

    private fun openImportTransactionScreen() {
        navigator.openImportTransactionScreen(
            activityContext = requireActivity(),
            walletId = args.walletId,
        )
    }

    private fun openClaiInheritance() {
        lifecycleScope.launch {
            viewModel.getWalletBsms()?.let { bsms ->
                navigator.openClaimInheritanceScreen(
                    requireActivity(),
                    args = ClaimArgs(bsms = bsms),
                )
            }
        }
    }

    private fun onBannerBackupAndRegisterClick() {
        navigator.openBackupWalletScreen(
            requireActivity(),
            BackUpWalletArgs(
                wallet = viewModel.getWallet(),
                backUpWalletType = BackUpWalletType.NORMAL,
            )
        )
    }

    private fun onBannerBackupOnlyClick() {
        navigator.openBackupWalletScreen(
            requireActivity(),
            BackUpWalletArgs(
                wallet = viewModel.getWallet(),
                backUpWalletType = BackUpWalletType.NORMAL,
            )
        )
    }

    private fun onBannerRegisterOnlyClick() {
        navigator.openUploadConfigurationScreen(requireActivity(), args.walletId)
    }

    private fun onWarningClick() {
        if (viewModel.isFreeGroupWallet()) {
            showSaveShareOption()
        } else {
            lifecycleScope.launch {
                viewModel.hasSigner(viewModel.getWallet().signers.first()).onSuccess {
                    if (it) showConfirmBackupDialog() else showHotKeyDeleted()
                }
            }
        }
    }

    private fun showHotKeyDeleted() {
        NCInfoDialog(requireActivity()).showDialog(
            title = getString(R.string.nc_confirmation),
            message = getString(R.string.nc_hot_key_deleted),
        )
    }

    private fun showConfirmBackupDialog() {
        NCInfoDialog(requireActivity()).showDialog(
            message = getString(R.string.nc_back_up_seed_phrase_confirmation),
            btnYes = getString(R.string.nc_text_continue),
            btnInfo = getString(R.string.nc_text_do_this_later),
            onYesClick = {
                navigator.openCreateNewSeedScreen(
                    activityContext = requireActivity(),
                    walletId = args.walletId,
                )
            }
        )
    }

    private fun closeScreen() {
        if (requireActivity() is WalletDetailsActivity) requireActivity().finish()
        else findNavController().popBackStack()
    }
}
