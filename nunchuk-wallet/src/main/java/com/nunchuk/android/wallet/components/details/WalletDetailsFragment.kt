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

package com.nunchuk.android.wallet.components.details

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.constants.RoomAction
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.qr.convertToQRCode
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.*
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.share.wallet.bindWalletConfiguration
import com.nunchuk.android.utils.Utils
import com.nunchuk.android.utils.serializable
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.coin.CoinActivity
import com.nunchuk.android.wallet.components.config.WalletConfigAction
import com.nunchuk.android.wallet.components.config.WalletConfigActivity
import com.nunchuk.android.wallet.components.details.WalletDetailsEvent.*
import com.nunchuk.android.wallet.databinding.FragmentWalletDetailBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WalletDetailsFragment : BaseFragment<FragmentWalletDetailBinding>(),
    BottomSheetOptionListener {

    @Inject
    lateinit var textUtils: TextUtils

    @Inject
    lateinit var sessionHolder: SessionHolder

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data
            if (it.resultCode == Activity.RESULT_OK && data != null) {
                when (data.serializable<WalletConfigAction>(WalletConfigActivity.EXTRA_WALLET_ACTION)) {
                    WalletConfigAction.DELETE -> requireActivity().onBackPressedDispatcher.onBackPressed()
                    WalletConfigAction.UPDATE_NAME -> viewModel.getWalletDetails(false)
                    WalletConfigAction.FORCE_REFRESH -> viewModel.setForceRefreshWalletProcessing(
                        true
                    )
                    null -> {}
                }
            }
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
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentWalletDetailBinding {
        return FragmentWalletDetailBinding.inflate(inflater, container, false)
    }

    private var job: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeEvent()
    }

    override fun onOptionClicked(option: SheetOption) {
        when (option.type) {
            SheetOptionType.TYPE_IMPORT_TX -> showImportTransactionOption()
            SheetOptionType.TYPE_IMPORT_PSBT -> handleImportPSBT()
            SheetOptionType.TYPE_IMPORT_PSBT_QR -> openImportTransactionScreen()
            SheetOptionType.SET_UP_INHERITANCE -> navigator.openMembershipActivity(
                activityContext = requireActivity(),
                groupStep = MembershipStage.SETUP_INHERITANCE,
                walletId = args.walletId
            )
        }
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
        job?.cancel()
        job = lifecycleScope.launch(Dispatchers.IO) {
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
    }

    private fun handleEvent(event: WalletDetailsEvent) {
        when (event) {
            is WalletDetailsError -> onGetWalletError(event)
            is SendMoneyEvent -> openInputAmountScreen(event)
            is UpdateUnusedAddress -> bindUnusedAddress(event.address)
            is Loading -> showOrHideLoading(event.loading)
            ImportPSBTSuccess -> onPSBTImported()
            is PaginationTransactions -> startPagination(event.hasTransactions)
        }
    }

    private fun startPagination(hasTx: Boolean) {
        hideLoading()
        binding.emptyTxContainer.isVisible = !hasTx
        binding.transactionTitle.isVisible = hasTx
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
                    availableAmount = event.walletExtended.wallet.balance.pureBTC()
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
                availableAmount = event.walletExtended.wallet.balance.pureBTC()
            )
        }
    }

    private fun bindUnusedAddress(address: String) {
        hideLoading()
        if (address.isEmpty()) {
            binding.emptyTxContainer.isVisible = false
        } else {
            binding.emptyTxContainer.isVisible = true
            binding.addressQR.setImageBitmap(address.convertToQRCode())
            binding.addressText.text = address
        }
    }

    private fun handleState(state: WalletDetailsState) {
        val wallet = state.walletExtended.wallet
        adapter.setHideWalletDetail(state.hideWalletDetailLocal)
        binding.toolbarTitle.text = wallet.name
        binding.configuration.bindWalletConfiguration(
            wallet,
            hideWalletDetail = state.hideWalletDetailLocal
        )

        binding.btcAmount.text = Utils.maskValue(wallet.getBTCAmount(), state.hideWalletDetailLocal)
        binding.cashAmount.text =
            Utils.maskValue(wallet.getCurrencyAmount(), state.hideWalletDetailLocal)
        binding.ivSendBtc.isClickable = wallet.balance.value > 0

        binding.shareIcon.isVisible = state.walletExtended.isShared || state.isAssistedWallet
        if (state.isAssistedWallet) {
            binding.container.setBackgroundResource(R.drawable.nc_header_membership_gradient_background)
            requireActivity().window.statusBarColor =
                ContextCompat.getColor(requireContext(), R.color.nc_wallet_premium_bg)
            binding.shareIcon.text =
                Utils.maskValue(getString(R.string.nc_assisted), state.hideWalletDetailLocal)
        }
        updateFabIcon(state.hideWalletDetailLocal)
    }

    private fun setupViews() {
        binding.transactionList.layoutManager =
            LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding.tvAssistedDowngradeHint.isVisible = viewModel.isInactiveAssistedWallet()
        if (binding.tvAssistedDowngradeHint.isVisible) {
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
                    Toast.makeText(requireActivity(), "Coming soon", Toast.LENGTH_SHORT).show()
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
        setupPaginationAdapter()
    }

    private fun updateFabIcon(hideWalletDetail: Boolean) {
        val icon = if (hideWalletDetail) ContextCompat.getDrawable(
            requireContext(),
            R.drawable.ic_visibility
        ) else ContextCompat.getDrawable(requireContext(), R.drawable.ic_hide_pass)
        binding.fab.setImageDrawable(icon)
    }

    private fun handleInactiveAssistedWallet() {
        binding.tvAssistedDowngradeHint.makeTextLink(
            getString(R.string.nc_assisted_wallet_downgrade_hint),
            ClickAbleText(content = "renew your subscription", onClick = {
                requireActivity().openExternalLink(RENEW_ACCOUNT_LINK)
            })
        )
    }

    private fun onMoreClicked() {
        val options = mutableListOf(
            SheetOption(
                SheetOptionType.TYPE_IMPORT_TX,
                R.drawable.ic_import,
                R.string.nc_import_transaction
            ),
        )
        if (viewModel.isShowSetupInheritance()) {
            options.add(
                0, SheetOption(
                    SheetOptionType.SET_UP_INHERITANCE,
                    R.drawable.ic_inheritance,
                    R.string.nc_setup_inheritance_for_this_wallet
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