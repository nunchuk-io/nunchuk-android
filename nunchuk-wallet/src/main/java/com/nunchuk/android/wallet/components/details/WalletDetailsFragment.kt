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
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withResumed
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.constants.RoomAction
import com.nunchuk.android.core.groupchathistory.GroupChatHistoryArgs
import com.nunchuk.android.core.groupchathistory.GroupChatHistoryFragment
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.qr.convertToQRCode
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.CHOOSE_FILE_REQUEST_CODE
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.RENEW_ACCOUNT_LINK
import com.nunchuk.android.core.util.TextUtils
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.core.util.getFileFromUri
import com.nunchuk.android.core.util.hideLoading
import com.nunchuk.android.core.util.makeTextLink
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.core.util.openSelectFileChooser
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.core.util.setUnderline
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.util.showSuccess
import com.nunchuk.android.model.HistoryPeriod
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.wallet.WalletStatus
import com.nunchuk.android.share.wallet.bindWalletConfiguration
import com.nunchuk.android.utils.Utils
import com.nunchuk.android.utils.consumeEdgeToEdge
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.serializable
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.config.WalletConfigAction
import com.nunchuk.android.wallet.components.config.WalletConfigActivity
import com.nunchuk.android.wallet.components.details.WalletDetailsEvent.ImportPSBTSuccess
import com.nunchuk.android.wallet.components.details.WalletDetailsEvent.Loading
import com.nunchuk.android.wallet.components.details.WalletDetailsEvent.PaginationTransactions
import com.nunchuk.android.wallet.components.details.WalletDetailsEvent.SendMoneyEvent
import com.nunchuk.android.wallet.components.details.WalletDetailsEvent.UpdateUnusedAddress
import com.nunchuk.android.wallet.components.details.WalletDetailsEvent.WalletDetailsError
import com.nunchuk.android.wallet.databinding.FragmentWalletDetailBinding
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WalletDetailsFragment : BaseFragment<FragmentWalletDetailBinding>(),
    BottomSheetOptionListener {

    @Inject
    lateinit var textUtils: TextUtils

    @Inject
    lateinit var sessionHolder: SessionHolder

    @Inject
    lateinit var pushEventManager: PushEventManager

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data
            if (it.resultCode == Activity.RESULT_OK && data != null) {
                when (data.serializable<WalletConfigAction>(WalletConfigActivity.EXTRA_WALLET_ACTION)) {
                    WalletConfigAction.DELETE -> {
                        closeScreen()
                    }

                    WalletConfigAction.UPDATE_NAME -> viewModel.getWalletDetails(false)
                    WalletConfigAction.FORCE_REFRESH -> viewModel.setForceRefreshWalletProcessing(
                        true
                    )

                    null -> {}
                }
            }
        }

    private fun closeScreen() {
        if (requireActivity() is WalletDetailsActivity) requireActivity().finish()
        else findNavController().popBackStack()
    }

    private val controller: IntentSharingController by lazy {
        IntentSharingController.from(
            requireActivity()
        )
    }

    override fun onDestroy() {
        requireActivity().window.statusBarColor =
            ContextCompat.getColor(requireContext(), R.color.nc_primary_color)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        with(viewModel) {
            syncServerTransaction()
            syncData(loadingSilent = true)
            getGroupWalletMessageUnreadCount()
        }
    }

    private val viewModel: WalletDetailsViewModel by viewModels()

    private val adapter: TransactionAdapter = TransactionAdapter {
        navigator.openTransactionDetailsScreen(
            activityContext = requireActivity(),
            walletId = args.walletId,
            txId = it.txId,
            roomId = viewModel.getRoomWallet()?.roomId.orEmpty()
        )
    }

    private val args: WalletDetailsFragmentArgs by navArgs()

    override fun initializeBinding(
        inflater: LayoutInflater, container: ViewGroup?,
    ): FragmentWalletDetailBinding {
        return FragmentWalletDetailBinding.inflate(inflater, container, false)
    }

    private var updateDataJob: Job? = null
    private var animateLayoutJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeEvent()

        requireActivity().supportFragmentManager.setFragmentResultListener(
            GroupChatHistoryFragment.REQUEST_KEY, this
        ) { _: String?, bundle: Bundle ->
            val historyPeriod =
                bundle.parcelable<HistoryPeriod>(GroupChatHistoryFragment.EXTRA_HISTORY_PERIOD)
                    ?: return@setFragmentResultListener
            viewModel.updateGroupChatHistoryPeriod(historyPeriod)
            showSuccess(message = getString(R.string.nc_chat_setting_updated))
            clearFragmentResult(GroupChatHistoryFragment.REQUEST_KEY)
        }

        binding.chatView.setContent {
            val state by viewModel.state.observeAsState()
            if (state == null) return@setContent
            GroupWalletChatView(messages = state!!.groupChatMessages,
                unreadCount = state!!.unreadMessagesCount,
                onSendMessage = {
                    viewModel.sendMessage(it)
                }, onOpenChat = {
                    navigator.openGroupChatScreen(
                        activityContext = requireActivity(),
                        walletId = args.walletId,
                    )
                })
        }
    }

    private fun configureToolbar(state: WalletDetailsState) {
        val searchMenu = binding.toolbar.menu.findItem(R.id.menu_search)
        searchMenu.isVisible =
            state.walletExtended.wallet.name.isNotEmpty() && state.isFreeGroupWallet.not()
        if (state.groupId.isNullOrEmpty()
                .not() && state.walletStatus != WalletStatus.REPLACED.name
        ) {
            searchMenu.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_groups_menu)
        } else if (state.isAssistedWallet) {
            searchMenu.icon =
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_health_check_menu)
        } else {
            searchMenu.icon =
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_search_white)
        }
        binding.toolbar.menu.findItem(R.id.menu_more).isVisible =
            state.walletStatus != WalletStatus.LOCKED.name && viewModel.isFacilitatorAdmin()
                .not() && viewModel.isEmptyTransaction().not()
                    || state.isFreeGroupWallet
    }

    override fun onOptionClicked(option: SheetOption) {
        when (option.type) {
            SheetOptionType.TYPE_IMPORT_TX -> showImportTransactionOption()
            SheetOptionType.TYPE_IMPORT_PSBT -> handleImportPSBT()
            SheetOptionType.TYPE_IMPORT_PSBT_QR -> openImportTransactionScreen()
            SheetOptionType.TYPE_SEARCH_TX -> openSearchTransaction()
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
                        groupId = ""
                    )
                )
            }

            else -> {}
        }
    }

    private fun openSearchTransaction() {
        if (viewModel.isHideWalletDetailLocal) return
        navigator.openSearchTransaction(
            requireContext(), walletId = args.walletId,
            roomId = viewModel.getRoomWallet()?.roomId.orEmpty(),
        )
    }

    private fun setupPaginationAdapter() {
        binding.transactionList.adapter = adapter.withLoadStateFooter(LoadStateAdapter())
        adapter.addLoadStateListener {
            when (it.refresh) {
                is LoadState.Loading -> {}
                is LoadState.Error -> hideLoading()
                is LoadState.NotLoading -> hideLoading()
            }
        }
        lifecycleScope.launch {
            adapter.loadStateFlow.distinctUntilChangedBy(CombinedLoadStates::refresh)
                .filter { it.refresh is LoadState.NotLoading }
                .collect { binding.transactionList.scrollToPosition(0) }
        }
    }

    private fun paginateTransactions() {
        adapter.submitData(lifecycle, PagingData.empty())
        updateDataJob?.cancel()
        updateDataJob = lifecycleScope.launch {
            viewModel.paginateTransactions().catch { hideLoading() }
                .collectLatest(adapter::submitData)
        }
    }

    private fun observeEvent() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                // skip calling syncData here in-case forceRefreshWallet since it will show the an empty state for a while
                if (viewModel.isForceRefreshProcessing.not()) {
                    viewModel.syncData()
                } else {
                    viewModel.setForceRefreshWalletProcessing(false)
                }
            }
        }
        viewModel.state.observe(viewLifecycleOwner, ::handleState)
        viewModel.event.observe(viewLifecycleOwner, ::handleEvent)
        viewLifecycleOwner.lifecycleScope.launch {
            pushEventManager.event.collectLatest {
                if (it is PushEvent.CloseWalletDetail) {
                    viewLifecycleOwner.lifecycle.withResumed {
                        closeScreen()
                    }
                }
            }
        }
    }

    private fun handleEvent(event: WalletDetailsEvent) {
        when (event) {
            is WalletDetailsError -> onGetWalletError(event)
            is SendMoneyEvent -> openInputAmountScreen(event)
            is UpdateUnusedAddress -> bindUnusedAddress(event.address)
            is Loading -> showOrHideLoading(event.loading)
            ImportPSBTSuccess -> onPSBTImported()
            is PaginationTransactions -> startPagination(event.hasTransactions)
            is WalletDetailsEvent.OpenSetupGroupWallet -> navigator.openFreeGroupWalletScreen(
                activityContext = requireActivity(),
                groupId = event.groupId
            )
        }
    }

    private fun startPagination(hasTx: Boolean) {
        hideLoading()
        emptyTxVisibility(!hasTx)
        binding.transactionList.isVisible = hasTx
        if (hasTx) {
            paginateTransactions()
        }
    }

    private fun onPSBTImported() {
        viewModel.syncData()
        hideLoading()
        NCToastMessage(requireActivity()).showMessage(getString(R.string.nc_wallet_psbt_imported))
    }

    private fun onGetWalletError(event: WalletDetailsError) {
        hideLoading()
        NCToastMessage(requireActivity()).showError(event.message)
    }

    private fun openInputAmountScreen(event: SendMoneyEvent) {
        if (event.walletExtended.isShared) {
            val roomWallet = event.walletExtended.roomWallet!!
            if (viewModel.isLeaveRoom) {
                sessionHolder.setActiveRoom(roomWallet.roomId, true)
                navigator.openInputAmountScreen(
                    activityContext = requireActivity(),
                    roomId = roomWallet.roomId,
                    walletId = roomWallet.walletId,
                    availableAmount = event.walletExtended.wallet.balance.pureBTC(),
                )
            } else {
                navigator.openRoomDetailActivity(
                    activityContext = requireActivity(),
                    roomId = event.walletExtended.roomWallet!!.roomId,
                    roomAction = RoomAction.SEND
                )
            }
        } else {
            navigator.openInputAmountScreen(
                activityContext = requireActivity(),
                walletId = args.walletId,
                availableAmount = event.walletExtended.wallet.balance.pureBTC(),
            )
        }
    }

    private fun bindUnusedAddress(address: String) {
        hideLoading()
        if (address.isEmpty()) {
            emptyTxVisibility(false)
        } else {
            emptyTxVisibility(true)
            binding.addressQR.setImageBitmap(address.convertToQRCode())
            binding.addressText.text = address
        }
    }

    private fun emptyTxVisibility(isVisible: Boolean) {
        binding.emptyTxFacilitatorAdmin.isVisible = isVisible && viewModel.isFacilitatorAdmin()
        binding.emptyTxContainer.isVisible = isVisible && viewModel.isFacilitatorAdmin().not()
    }

    private fun handleState(state: WalletDetailsState) {
        binding.fab.isGone = state.role == AssistedWalletRole.FACILITATOR_ADMIN
        val wallet = state.walletExtended.wallet
        adapter.setHideWalletDetail(state.hideWalletDetailLocal)
        binding.toolbarTitle.text = wallet.name
        configureToolbar(state)
        binding.configuration.bindWalletConfiguration(
            wallet,
            hideWalletDetail = state.hideWalletDetailLocal
        )

        binding.btcAmount.text = Utils.maskValue(wallet.getBTCAmount(), state.hideWalletDetailLocal)
        binding.cashAmount.text =
            Utils.maskValue(wallet.getCurrencyAmount(), state.hideWalletDetailLocal)

        binding.shareIcon.isVisible =
            state.walletExtended.isShared || state.isAssistedWallet || state.walletStatus == WalletStatus.REPLACED.name
        handleWalletBackground(state)
        updateFabIcon(state.hideWalletDetailLocal)
        binding.ivSendBtc.isEnabled = state.walletStatus != WalletStatus.LOCKED.name
        binding.ivSendBtc.alpha = if (binding.ivSendBtc.isEnabled) 1.0f else 0.7f
        binding.ivViewCoin.isEnabled = state.isHasCoin && viewModel.isFacilitatorAdmin().not()
        binding.ivViewCoin.alpha =
            if (state.isHasCoin && viewModel.isFacilitatorAdmin().not()) 1.0f else 0.7f
        binding.tvWalletWarning.isVisible = state.walletExtended.wallet.needBackup
        if (state.walletExtended.wallet.needBackup) {
            handleNeedBackupWallet()
        }
        binding.chatView.isVisible = state.isFreeGroupWallet

        if (state.isFreeGroupWallet) {
            val layoutParams = binding.addressQR.layoutParams
            layoutParams.width = dpToPx(120)
            layoutParams.height = dpToPx(120)
            binding.addressQR.layoutParams = layoutParams

            binding.emptyTransactionTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            binding.addressText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun handleWalletBackground(state: WalletDetailsState) {
        if (state.walletExtended.wallet.needBackup) {
            binding.statusBarBackground.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.nc_beeswax_dark
                )
            )
            requireActivity().window.statusBarColor =
                ContextCompat.getColor(requireContext(), R.color.nc_beeswax_dark)
            binding.cashAmount.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.nc_beeswax_tint
                )
            )
        } else if (state.walletStatus == WalletStatus.REPLACED.name || state.walletStatus == WalletStatus.LOCKED.name) {
            val color = ContextCompat.getColor(requireContext(), R.color.nc_grey_dark_color)
            binding.statusBarBackground.setBackgroundColor(color)
            requireActivity().window.statusBarColor = color
            binding.shareIcon.text = getString(R.string.nc_deactivated)
            binding.shareIcon.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
        } else if (state.isAssistedWallet) {
            binding.statusBarBackground.setBackgroundResource(R.drawable.nc_header_membership_gradient_background)
            requireActivity().window.statusBarColor =
                ContextCompat.getColor(requireContext(), R.color.nc_wallet_premium_bg)
            binding.shareIcon.text =
                Utils.maskValue(getString(R.string.nc_assisted), state.hideWalletDetailLocal)
            binding.cashAmount.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.nc_denim_tint_color
                )
            )
        } else if (state.isFreeGroupWallet) {
            binding.statusBarBackground.setBackgroundResource(R.drawable.nc_header_free_group_wallet_background)
            requireActivity().window.statusBarColor =
                ContextCompat.getColor(requireContext(), R.color.cl_2B74A9)
            binding.cashAmount.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.nc_white_color
                )
            )
        } else {
            binding.statusBarBackground.setBackgroundResource(R.drawable.nc_header_gradient_background)
            requireActivity().window.statusBarColor =
                ContextCompat.getColor(requireContext(), R.color.nc_primary_color)
            binding.cashAmount.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.nc_denim_tint_color
                )
            )
        }
    }

    private fun setupViews() {
        binding.toolbar.consumeEdgeToEdge()
        binding.transactionList.layoutManager =
            LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding.tvWalletWarning.isVisible =
            viewModel.isInactiveAssistedWallet()
        if (binding.tvWalletWarning.isVisible) {
            handleInactiveAssistedWallet()
        }
        binding.transactionList.isNestedScrollingEnabled = false
        binding.transactionList.setHasFixedSize(false)
        binding.transactionList.adapter = adapter

        binding.viewWalletConfig.setUnderline()
        binding.viewWalletConfig.setOnClickListener {
            navigator.openWalletConfigScreen(
                launcher = launcher,
                activityContext = requireActivity(),
                walletId = args.walletId,
                keyPolicy = args.keyPolicy
            )
        }
        binding.ivReceiveBtc.setOnClickListener {
            navigator.openReceiveTransactionScreen(
                requireActivity(), args.walletId
            )
        }
        binding.ivSendBtc.setOnClickListener { viewModel.handleSendMoneyEvent() }
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.toolbar.setOnMenuItemClickListener { menu ->
            when (menu.itemId) {
                R.id.menu_search -> {
                    if (viewModel.isAssistedWallet || viewModel.isLockedAssistedWallet) {
                        navigator.openGroupDashboardScreen(
                            groupId = viewModel.groupId,
                            walletId = args.walletId,
                            activityContext = requireActivity()
                        )
                    } else {
                        openSearchTransaction()
                    }
                    true
                }

                R.id.menu_more -> {
                    onMoreClicked()
                    true
                }

                else -> false
            }
        }

        binding.copyAddressLayout.setOnClickListener { copyAddress(binding.addressText.text.toString()) }
        binding.shareLayout.setOnClickListener { controller.shareText(binding.addressText.text.toString()) }
        binding.ivViewCoin.setOnDebounceClickListener {
            navigator.openCoinList(context = requireContext(), walletId = args.walletId)
        }
        binding.fab.setOnClickListener {
            viewModel.updateHideWalletDetailLocal()
        }
        binding.container.setTransitionDuration(150)
        binding.transactionList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (!recyclerView.canScrollVertically(-1)) {
                        animateLayout(false)
                    } else {
                        animateLayout(true)
                    }
                } else {
                    animateLayout(true)
                }
            }
        })
        setupPaginationAdapter()
        binding.replaceWalletView.setContent {
            val replacedGroups by viewModel.state.asFlow().map { it.replaceGroups }
                .collectAsStateWithLifecycle(emptyMap())

            if (replacedGroups.isNotEmpty()) {
                ReplacedGroupView(
                    replacedGroups = replacedGroups,
                    onAcceptOrDeny = { groupId, isAccept ->
                        viewModel.acceptOrDenyReplaceGroup(groupId, isAccept)
                    },
                    onOpenSetupGroupWallet = { groupId ->
                        navigator.openFreeGroupWalletScreen(
                            activityContext = requireActivity(),
                            groupId = groupId
                        )
                    },
                )
            }
        }
    }

    private fun animateLayout(isEnd: Boolean) {
        animateLayoutJob?.cancel()
        animateLayoutJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(100L)
            if (isEnd) binding.container.transitionToEnd() else binding.container.transitionToStart()
        }
    }

    private fun updateFabIcon(hideWalletDetail: Boolean) {
        val icon = if (hideWalletDetail) ContextCompat.getDrawable(
            requireContext(),
            R.drawable.ic_visibility
        ) else ContextCompat.getDrawable(requireContext(), R.drawable.ic_hide_pass)
        binding.fab.setImageDrawable(icon)
    }

    private fun handleInactiveAssistedWallet() {
        binding.tvWalletWarning.makeTextLink(
            getString(R.string.nc_assisted_wallet_downgrade_hint),
            ClickAbleText(content = "renew your subscription", onClick = {
                requireActivity().openExternalLink(RENEW_ACCOUNT_LINK)
            })
        )
        binding.tvWalletWarning.setCompoundDrawablesRelativeWithIntrinsicBounds(
            R.drawable.ic_info, 0, 0, 0
        )
        binding.tvWalletWarning.setBackgroundResource(R.drawable.nc_rounded_whisper_background)
    }

    private fun handleNeedBackupWallet() {
        binding.tvWalletWarning.makeTextLink(
            getString(R.string.nc_write_down_the_seed_pharse_warning),
            ClickAbleText(content = getString(R.string.nc_do_it_now), onClick = {
                lifecycleScope.launch {
                    viewModel.hasSigner(viewModel.getWallet().signers.first())
                        .onSuccess {
                            if (it) {
                                showConfirmBackupDialog()
                            } else {
                                showHotKeyDeleted()
                            }
                        }
                }
            })
        )
        binding.tvWalletWarning.setCompoundDrawablesRelativeWithIntrinsicBounds(
            R.drawable.ic_warning_outline, 0, 0, 0
        )
        binding.tvWalletWarning.setBackgroundResource(R.drawable.nc_rounded_beeswax_background)
    }

    private fun showHotKeyDeleted() {
        NCInfoDialog(requireActivity()).showDialog(
            title = getString(R.string.nc_confirmation),
            message = getString(R.string.nc_hot_key_deleted),
        )
    }

    private fun showConfirmBackupDialog() {
        NCInfoDialog(requireActivity())
            .showDialog(
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

    private fun onMoreClicked() {
        val options = mutableListOf<SheetOption>()
        options.add(
            SheetOption(
                SheetOptionType.TYPE_IMPORT_TX,
                R.drawable.ic_import,
                R.string.nc_import_transaction
            )
        )
        if (viewModel.isAssistedWallet || viewModel.isFreeGroupWallet()) {
            options.add(
                SheetOption(
                    SheetOptionType.TYPE_SEARCH_TX,
                    R.drawable.ic_search_dark,
                    R.string.nc_search_transactions
                )
            )
        }
        if (viewModel.isFreeGroupWallet()) {
            options.add(
                SheetOption(
                    SheetOptionType.TYPE_GROUP_CHAT_HISTORY,
                    R.drawable.ic_clock,
                    R.string.nc_manage_group_chat_history
                )
            )
        }
        val bottomSheet = BottomSheetOption.newInstance(options)
        bottomSheet.show(childFragmentManager, "BottomSheetOption")
    }

    private fun showImportTransactionOption() {
        BottomSheetOption.newInstance(
            title = getString(R.string.nc_select_import_method),
            options = listOf(
                SheetOption(
                    SheetOptionType.TYPE_IMPORT_PSBT_QR,
                    R.drawable.ic_qr,
                    R.string.nc_import_via_qr
                ),
                SheetOption(
                    SheetOptionType.TYPE_IMPORT_PSBT,
                    R.drawable.ic_import,
                    R.string.nc_import_via_file
                )
            )
        ).show(childFragmentManager, "BottomSheetOption")
    }

    private fun openImportTransactionScreen() {
        navigator.openImportTransactionScreen(
            activityContext = requireActivity(),
            walletId = args.walletId
        )
    }

    private fun handleImportPSBT() {
        openSelectFileChooser()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == CHOOSE_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            intent?.data?.let {
                getFileFromUri(requireActivity().contentResolver, it, requireActivity().cacheDir)
            }?.absolutePath?.let(viewModel::handleImportPSBT)
        }
    }

    private fun copyAddress(address: String) {
        textUtils.copyText(text = address)
        NCToastMessage(requireActivity()).showMessage(getString(R.string.nc_address_copy_to_clipboard))
    }
}