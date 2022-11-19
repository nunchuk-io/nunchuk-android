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

package com.nunchuk.android.wallet.shared.components.assign

import android.content.Context
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.showOrHideNfcLoading
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.utils.viewModelProviderFactoryOf
import com.nunchuk.android.wallet.InputBipPathBottomSheet
import com.nunchuk.android.wallet.InputBipPathBottomSheetListener
import com.nunchuk.android.wallet.shared.R
import com.nunchuk.android.wallet.shared.components.assign.AssignSignerEvent.AssignSignerCompletedEvent
import com.nunchuk.android.wallet.shared.components.assign.AssignSignerEvent.ShowError
import com.nunchuk.android.wallet.shared.databinding.ActivityAssignSignerBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningDialog
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import javax.inject.Inject

@AndroidEntryPoint
class AssignSignerSharedWalletActivity : BaseNfcActivity<ActivityAssignSignerBinding>(),
    InputBipPathBottomSheetListener, BottomSheetOptionListener {

    @Inject
    internal lateinit var vmFactory: AssignSignerViewModel.Factory

    private val viewModel: AssignSignerViewModel by viewModels {
        viewModelProviderFactoryOf {
            vmFactory.create(args, it)
        }
    }

    private val args: AssignSignerArgs by lazy { AssignSignerArgs.deserializeFrom(intent) }

    private var emptyStateView: View? = null

    override fun initializeBinding() = ActivityAssignSignerBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        setupViews()
        observeEvent()

        if (args.signers.isNotEmpty()) {
            args.signers.forEach { viewModel.filterSigners(it) }
        } else {
            viewModel.init()
            viewModel.getSigners(args.walletType, args.addressType)
        }
    }

    override fun onInputDone(masterSignerId: String, newInput: String) {
        viewModel.changeBip32Path(masterSignerId, newInput)
    }

    override fun onOptionClicked(option: SheetOption) {
        viewModel.toggleShowPath()
    }

    private fun setEmptyState() {
        emptyStateView = binding.viewStubEmptyState.inflate()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_NFC_TOPUP_XPUBS }) {
            viewModel.cacheTapSignerXpub(
                IsoDep.get(it.tag),
                nfcViewModel.inputCvc.orEmpty(),
            )
            nfcViewModel.clearScanInfo()
        }
    }

    private fun handleEvent(event: AssignSignerEvent) {
        when (event) {
            is AssignSignerCompletedEvent -> openRoom(event.roomId)
            is ShowError -> NCToastMessage(this).showError(event.message)
            AssignSignerEvent.ChangeBip32Success -> NCToastMessage(this).show(getString(R.string.nc_bip_32_updated))
            is AssignSignerEvent.Loading -> showOrHideLoading(event.isLoading)
            is AssignSignerEvent.CacheTapSignerXpubError -> handleCacheXpubError(event)
            AssignSignerEvent.RequestCacheTapSignerXpub -> handleCacheXpub()
            is AssignSignerEvent.NfcLoading -> showOrHideNfcLoading(event.isLoading)
        }
    }

    private fun handleCacheXpub() {
        NCWarningDialog(this).showDialog(
            title = getString(R.string.nc_text_info),
            message = getString(R.string.nc_new_xpub_need),
            onYesClick = {
                startNfcFlow(REQUEST_NFC_TOPUP_XPUBS)
            }
        )
    }

    private fun handleCacheXpubError(event: AssignSignerEvent.CacheTapSignerXpubError) {
        if (nfcViewModel.handleNfcError(event.error).not()) {
            val message = event.error?.message.orUnknownError()
            NCToastMessage(this).showError(message)
        }
    }

    private fun openRoom(roomId: String) {
        ActivityManager.popUntilRoot()
        navigator.openRoomDetailActivity(this, roomId)
    }

    private fun handleState(state: AssignSignerState) {
        bindSigners(state.signers, state.selectedSigner, state.isShowPath)

        emptyStateView?.isVisible = state.signers.isEmpty()

        val slot = args.totalSigns - state.selectedSigner.size
        binding.slot.text = getString(R.string.nc_wallet_slots_left_in_the_wallet, slot)
    }

    private fun bindSigners(signers: List<SignerModel>, selectedPFXs: Set<SignerModel>, isShowPath: Boolean) {
        val canSelect = args.totalSigns - selectedPFXs.size
        SignersViewBinder(
            container = binding.signersContainer,
            signers = signers,
            canSelect = canSelect > 0,
            selectedXpfs = selectedPFXs,
            onItemSelectedListener = viewModel::updateSelectedXfps,
            onEditPath = ::handleEditDerivationPath,
            isShowPath = isShowPath
        ).bindItems()
    }

    private fun handleEditDerivationPath(signer: SignerModel) {
        InputBipPathBottomSheet.show(
            supportFragmentManager,
            signer.id,
            viewModel.getMasterSignerMap()[signer.id]?.derivationPath.orEmpty()
        )
    }

    private fun setupViews() {
        binding.toolbar.setOnMenuItemClickListener {
            BottomSheetOption.newInstance(
                options = listOf(
                    SheetOption(
                        0,
                        label = if (viewModel.isShowPath())
                            getString(R.string.nc_hide_bip_32_path)
                        else
                            getString(R.string.nc_show_bip_32_path)
                    ),
                )
            ).show(supportFragmentManager, "BottomSheetOption")
            true
        }
        binding.signersContainer.removeAllViews()
        binding.btnContinue.setOnClickListener {
            viewModel.handleContinueEvent()
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        setEmptyState()
        emptyStateView?.findViewById<View>(R.id.btnExit)?.setOnClickListener {
            navigator.returnRoomDetailScreen()
        }
        emptyStateView?.isVisible = true
    }

    companion object {

        fun start(
            activityContext: Context,
            walletName: String,
            walletType: WalletType,
            addressType: AddressType,
            totalSigns: Int,
            requireSigns: Int,
            signers: List<SingleSigner>
        ) {
            activityContext.startActivity(
                AssignSignerArgs(
                    walletName = walletName,
                    walletType = walletType,
                    addressType = addressType,
                    totalSigns = totalSigns,
                    requireSigns = requireSigns,
                    signers = signers
                ).buildIntent(activityContext)
            )
        }

    }

}
