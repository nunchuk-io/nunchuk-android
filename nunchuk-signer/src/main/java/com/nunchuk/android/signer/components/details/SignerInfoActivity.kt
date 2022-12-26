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

package com.nunchuk.android.signer.components.details

import android.content.Context
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcScanInfo
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.core.util.*
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.components.details.model.SingerOption
import com.nunchuk.android.signer.databinding.ActivitySignerInfoBinding
import com.nunchuk.android.signer.tapsigner.NfcSetupActivity
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter

@AndroidEntryPoint
class SignerInfoActivity : BaseNfcActivity<ActivitySignerInfoBinding>(),
    SingerInfoOptionBottomSheet.OptionClickListener {

    private val viewModel: SignerInfoViewModel by viewModels()

    override fun initializeBinding() = ActivitySignerInfoBinding.inflate(layoutInflater)

    private val args: SignerInfoArgs by lazy { SignerInfoArgs.deserializeFrom(intent) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupViews()
        observeEvent()
        viewModel.init(args)
    }

    override fun onOptionClickListener(option: SingerOption) {
        when (option) {
            SingerOption.TOP_UP -> startNfcFlow(REQUEST_NFC_TOPUP_XPUBS)
            SingerOption.CHANGE_CVC -> onChangeCvcOptionClicked()
            SingerOption.BACKUP_KEY -> startNfcFlow(REQUEST_NFC_VIEW_BACKUP_KEY)
            SingerOption.REMOVE_KEY -> handleRemoveKey()
        }
    }

    private fun onChangeCvcOptionClicked() {
        viewModel.state.value?.masterSigner?.id?.let { masterSignerId ->
            NfcSetupActivity.navigate(
                activity = this,
                setUpAction = NfcSetupActivity.CHANGE_CVC,
                masterSignerId = masterSignerId
            )
        }
    }

    private fun handleRemoveKey() {
        if (args.isInWallet) {
            NCInfoDialog(this).showDialog(
                message = getString(R.string.nc_warning_key_use_in_wallet),
            )
        } else if (args.signerType == SignerType.FOREIGN_SOFTWARE) {
            NCInfoDialog(this).showDialog(
                message = getString(R.string.nc_please_remove_on_added_device),
            )
        } else {
            NCWarningDialog(this).showDialog(
                title = getString(R.string.nc_confirmation),
                message = getString(R.string.nc_delete_key_msg),
                onYesClick = {
                    viewModel.handleRemoveSigner()
                }
            )
        }
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)

        lifecycleScope.launchWhenStarted {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_NFC_VIEW_BACKUP_KEY }
                    .collect {
                        requestViewBackupKey(it)
                        nfcViewModel.clearScanInfo()
                    }
            }
        }

        lifecycleScope.launchWhenStarted {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_NFC_HEALTH_CHECK }
                    .collect {
                        val isoDep = IsoDep.get(it.tag) ?: return@collect
                        viewModel.healthCheckTapSigner(
                            isoDep,
                            nfcViewModel.inputCvc.orEmpty(),
                            viewModel.state.value?.masterSigner ?: return@collect
                        )
                        nfcViewModel.clearScanInfo()
                    }
            }
        }

        lifecycleScope.launchWhenStarted {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_NFC_TOPUP_XPUBS }
                    .collect {
                        topUpXPubs(it)
                        nfcViewModel.clearScanInfo()
                    }
            }
        }

        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_GENERATE_HEAL_CHECK_MSG }) { scanInfo ->
            viewModel.state.value?.remoteSigner?.let { signer ->
                viewModel.generateColdcardHealthMessages(
                    Ndef.get(scanInfo.tag) ?: return@flowObserver,
                    signer.derivationPath
                )
            }
            nfcViewModel.clearScanInfo()
        }

        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_MK4_IMPORT_SIGNATURE }) {
            viewModel.state.value?.remoteSigner?.let { signer ->
                viewModel.healthCheckColdCard(signer, it.records)
            }
            nfcViewModel.clearScanInfo()
        }
    }

    private fun requestViewBackupKey(nfcScanInfo: NfcScanInfo) {
        viewModel.getTapSignerBackup(
            IsoDep.get(nfcScanInfo.tag) ?: return,
            nfcViewModel.inputCvc.orEmpty()
        )
    }

    private fun topUpXPubs(nfcScanInfo: NfcScanInfo) {
        viewModel.topUpXpubTapSigner(
            IsoDep.get(nfcScanInfo.tag) ?: return,
            nfcViewModel.inputCvc.orEmpty(),
            args.id
        )
    }

    private fun handleState(state: SignerInfoState) {
        state.remoteSigner?.let(::bindRemoteSigner)
        state.masterSigner?.let(::bindMasterSigner)
        binding.tvCardId.isVisible = state.nfcCardId != null
        binding.tvCardIdLabel.isVisible = binding.tvCardId.isVisible
        state.nfcCardId?.let { cardId ->
            binding.tvCardId.text = cardId
        }
    }

    private fun bindMasterSigner(signer: MasterSigner) {
        val isPrimaryKey = isPrimaryKey(signer.device.masterFingerprint)
        binding.signerName.text = signer.name
        binding.signerTypeIcon.setImageDrawable(signer.type.toReadableDrawable(this, isPrimaryKey))
        binding.fingerprint.isVisible = true
        binding.fingerprint.text = signer.device.masterFingerprint
        binding.signerSpec.isVisible = false
        binding.signerType.text = signer.type.toReadableString(this, false)
        binding.signerPrimaryKeyType.isVisible = isPrimaryKey
    }

    private fun bindRemoteSigner(signer: SingleSigner) {
        binding.signerName.text = signer.name
        binding.signerTypeIcon.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.ic_air_signer_big
            )
        )
        binding.signerSpec.isVisible = true
        binding.signerSpec.text = signer.descriptor
        binding.fingerprint.isVisible = false
        binding.signerType.text =
            signer.type.toReadableString(this, isPrimaryKey(signer.masterSignerId))
    }

    private fun handleEvent(event: SignerInfoEvent) {
        showOrHideNfcLoading(event is SignerInfoEvent.NfcLoading)
        when (event) {
            is SignerInfoEvent.UpdateNameSuccessEvent -> {
                binding.signerName.text = event.signerName
                showEditSignerNameSuccess()
            }
            SignerInfoEvent.RemoveSignerCompletedEvent -> finish()
            is SignerInfoEvent.RemoveSignerErrorEvent -> showToast(event.message)
            is SignerInfoEvent.UpdateNameErrorEvent -> showToast(event.message)
            is SignerInfoEvent.HealthCheckErrorEvent -> {
                if (nfcViewModel.handleNfcError(event.e).not()) showHealthCheckError(event)
            }
            SignerInfoEvent.HealthCheckSuccessEvent -> NCToastMessage(this).showMessage(
                message = getString(
                    R.string.nc_txt_run_health_check_success_event,
                    binding.signerName.text
                ),
                icon = R.drawable.ic_check_circle_outline
            )
            is SignerInfoEvent.GetTapSignerBackupKeyEvent -> IntentSharingController.from(this)
                .shareFile(event.backupKeyPath)
            is SignerInfoEvent.NfcError -> {
                if (nfcViewModel.handleNfcError(event.e).not()) {
                    val message = event.e?.message.orUnknownError()
                    NCToastMessage(this).showError(message)
                }
            }
            SignerInfoEvent.TopUpXpubSuccess -> NCToastMessage(this).showMessage(
                message = getString(R.string.nc_xpub_topped_up),
                icon = R.drawable.ic_check_circle_outline
            )
            is SignerInfoEvent.TopUpXpubFailed -> {
                val message = event.e?.message ?: getString(R.string.nc_topup_xpub_failed)
                NCToastMessage(this).showError(message)
            }
            SignerInfoEvent.GenerateColdcardHealthMessagesSuccess -> startNfcFlow(
                REQUEST_MK4_IMPORT_SIGNATURE
            )
            SignerInfoEvent.NfcLoading -> showOrHideNfcLoading(true)
        }
    }

    private fun showHealthCheckError(event: SignerInfoEvent.HealthCheckErrorEvent) {
        if (event.message.isNullOrEmpty()) {
            val errorMessage = if (event.e?.message.isNullOrEmpty()) {
                getString(
                    R.string.nc_txt_run_health_check_error_event,
                    binding.signerName.text
                )
            } else {
                event.e?.message.orEmpty()
            }
            NCToastMessage(this).showError(errorMessage)
        } else {
            NCToastMessage(this).showWarning(event.message)
        }
    }

    private fun setupViews() {
        binding.signerName.text = args.name
        if (args.customMessage.isNotBlank()) {
            NCToastMessage(this).showMessage(
                message = args.customMessage,
                icon = R.drawable.ic_check_circle_outline
            )
        } else if (args.isReplacePrimaryKey) {
            NCToastMessage(this).showMessage(
                message = getString(R.string.nc_replace_primary_key_success),
                icon = R.drawable.ic_check_circle_outline
            )
        } else if (args.justAdded) {
            NCToastMessage(this).showMessage(
                message = getString(R.string.nc_text_add_signer_success, args.name),
                icon = R.drawable.ic_check_circle_outline
            )
            if (args.setPassphrase) {
                NCToastMessage(this).showMessage(
                    message = getString(R.string.nc_text_set_passphrase_success),
                    offset = R.dimen.nc_padding_44,
                    dismissTime = 4000L
                )
            }
        }
        binding.btnDone.isVisible = args.justAdded
        binding.toolbar.setNavigationOnClickListener { openMainScreen() }
        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.menu_more) {
                val type = viewModel.state.value?.masterSigner?.type
                    ?: viewModel.state.value?.remoteSigner?.type
                type?.let { signerType ->
                    SingerInfoOptionBottomSheet.newInstance(signerType)
                        .show(supportFragmentManager, "SingerInfoOptionBottomSheet")
                }
            }
            false
        }
        binding.btnDone.setOnClickListener { openMainScreen() }
        binding.signerName.setOnClickListener { onEditClicked() }
        binding.btnHealthCheck.setOnClickListener { handleRunHealthCheck() }
    }

    private fun handleRunHealthCheck() {
        val masterSigner = viewModel.state.value?.masterSigner
        val remoteSigner = viewModel.state.value?.remoteSigner
        if (masterSigner != null) {
            if (args.signerType == SignerType.NFC) {
                startNfcFlow(REQUEST_NFC_HEALTH_CHECK)
            } else if (masterSigner.software) {
                if (masterSigner.device.needPassPhraseSent) {
                    NCInputDialog(this).showDialog(
                        title = getString(R.string.nc_transaction_enter_passphrase),
                        onConfirmed = { viewModel.handleHealthCheck(masterSigner, it) }
                    )
                } else {
                    viewModel.handleHealthCheck(masterSigner)
                }
            }
        } else if (remoteSigner != null) {
            if (args.signerType == SignerType.COLDCARD_NFC) {
                startNfcFlow(REQUEST_GENERATE_HEAL_CHECK_MSG)
            }
        }
    }

    private fun openMainScreen() {
        navigator.returnToMainScreen()
    }

    private fun onEditClicked() {
        val bottomSheet = SignerUpdateBottomSheet.show(
            fragmentManager = supportFragmentManager,
            signerName = binding.signerName.text.toString()
        )
        bottomSheet.setListener(viewModel::handleEditCompletedEvent)
    }

    private fun showEditSignerNameSuccess() {
        binding.signerName.post {
            NCToastMessage(this).showMessage(
                message = getString(R.string.nc_text_change_signer_success),
                icon = R.drawable.ic_check_circle_outline
            )
        }
    }

    private fun isPrimaryKey(xfp: String): Boolean {
        val accountInfo = accountManager.getAccount()
        return accountInfo.loginType == SignInMode.PRIMARY_KEY.value && accountInfo.primaryKeyInfo?.xfp == xfp
    }

    companion object {

        fun start(
            activityContext: Context,
            id: String,
            masterFingerprint: String,
            name: String,
            type: SignerType,
            derivationPath: String,
            justAdded: Boolean = false,
            setPassphrase: Boolean = false,
            isInWallet: Boolean,
            isReplacePrimaryKey: Boolean = false,
            customMessage: String
        ) {
            activityContext.startActivity(
                SignerInfoArgs(
                    id = id,
                    name = name,
                    derivationPath = derivationPath,
                    justAdded = justAdded,
                    signerType = type,
                    setPassphrase = setPassphrase,
                    masterFingerprint = masterFingerprint,
                    isInWallet = isInWallet,
                    isReplacePrimaryKey = isReplacePrimaryKey,
                    customMessage = customMessage
                ).buildIntent(activityContext)
            )
        }
    }

}