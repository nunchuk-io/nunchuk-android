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

package com.nunchuk.android.signer.components.details

import android.content.Context
import android.content.Intent
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcConfirmationDialog
import com.nunchuk.android.core.bitbox.BitBoxHealthCheckSheet
import com.nunchuk.android.core.domain.data.CheckFirmwareVersion
import com.nunchuk.android.core.domain.membership.WalletsExistingKey
import com.nunchuk.android.core.ledger.LedgerHealthCheckSheet
import com.nunchuk.android.core.nfc.BaseComposePortalActivity
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_GENERATE_HEAL_CHECK_MSG
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_MK4_IMPORT_SIGNATURE
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_NFC_HEALTH_CHECK
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_NFC_TOPUP_XPUBS
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_NFC_VIEW_BACKUP_KEY
import com.nunchuk.android.core.nfc.NfcScanInfo
import com.nunchuk.android.core.nfc.PortalDeviceEvent
import com.nunchuk.android.core.util.TrezorCallbackHolder
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.openTrezorSuiteLink
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.showOrHideNfcLoading
import com.nunchuk.android.core.wallet.AssistedWalletContent
import com.nunchuk.android.core.wallet.WalletComposeBottomSheet
import com.nunchuk.android.core.wallet.WalletsBottomSheetViewModel
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.VerificationType
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.components.details.exportxpub.ExportXpubActivity
import com.nunchuk.android.signer.components.details.exportxpub.exporter.XpubExporters
import com.nunchuk.android.signer.components.details.exportxpub.exporter.XpubKey
import com.nunchuk.android.signer.components.details.message.SignMessageActivity
import com.nunchuk.android.signer.components.details.message.SignMessageArgs
import com.nunchuk.android.signer.components.details.model.SingerOption
import com.nunchuk.android.signer.components.jade.JadeQrUnlockActivity
import com.nunchuk.android.signer.tapsigner.NfcSetupActivity
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject

@AndroidEntryPoint
class SignerInfoActivity : BaseComposePortalActivity() {
    @Inject
    lateinit var trezorCallbackHolder: TrezorCallbackHolder

    @Inject
    lateinit var xpubExporters: XpubExporters

    private var openTrezorSuiteDeeplink: String? by mutableStateOf(null)

    /**
     * Signer to run the in-app health check for; non-null shows that key type's sign-message
     * sheet. One field per key type because each sheet drives its own transport, and only one
     * can be up at a time.
     */
    private var ledgerHealthCheckSigner: SingleSigner? by mutableStateOf(null)
    private var bitBoxHealthCheckSigner: SingleSigner? by mutableStateOf(null)

    private val viewModel: SignerInfoViewModel by viewModels()
    private val args by lazy { SignerInfoArgs.fromBundle(requireNotNull(intent.extras)) }
    private var showWallets by mutableStateOf(false)
    private val walletsViewModel: WalletsBottomSheetViewModel by viewModels()
    private val dialogs = SignerDialogs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { Content() }
        if (savedInstanceState == null) setupViews()
        observeEvent()
        flowObserver(trezorCallbackHolder.callbackUri.filterNotNull()) { callbackUri ->
            if (viewModel.handleTrezorHealthCheckCallback(callbackUri)) trezorCallbackHolder.clear(callbackUri)
        }
        if (args.existingKey != null) {
            dialogs.info(
                message = String.format(
                    getString(R.string.nc_software_key_removed_from_device),
                    args.name
                ),
                btnYes = getString(R.string.nc_delete_key),
                btnInfo = getString(R.string.nc_text_cancel),
                onYesClick = {
                    args.existingKey?.let {
                        viewModel.updateExistingKey(it, true)
                    }
                },
                onInfoClick = {
                    this.finish()
                }
            )
        }
    }

    @Composable
    private fun Content() {
        var showOptions by rememberSaveable { mutableStateOf(false) }
        var editingName by rememberSaveable { mutableStateOf<String?>(null) }
        val uiState by viewModel.state.collectAsStateWithLifecycle()
        val isPrimaryKey =
            uiState.masterSigner?.let { viewModel.isPrimaryKey(it.device.masterFingerprint) } == true
        NunchukTheme {
            WalletSelectionSheet()
            dialogs.Content()
            SignerInfoContent(
                uiState = uiState,
                isPrimaryKey = isPrimaryKey,
                onBackClicked = ::openMainScreen,
                onMoreClicked = {
                    // wait until the signer is loaded, otherwise the options act on nothing
                    if ((uiState.masterSigner?.type ?: uiState.remoteSigner?.type) != null) showOptions = true
                },
                onEditClicked = { editingName = uiState.signerName },
                onHealthCheckClicked = ::handleRunHealthCheck,
                onHistoryItemClick = {
                    navigator.openTransactionDetailsScreen(
                        activityContext = this,
                        walletId = it.walletLocalId,
                        txId = it.transactionId,
                        roomId = ""
                    )
                },
                onBackupKeyClicked = {
                    navigator.openCreateNewSeedScreen(
                        activityContext = this,
                        walletId = "",
                        backupHotKeySignerId = args.id
                    )
                },
                onViewSeedPhraseClicked = { passphrase ->
                    if (uiState.seedPhraseViewTimestamp == null) {
                        viewModel.saveSeedPhraseViewTimestamp(args.id)
                    } else if (viewModel.isSeedPhraseWaitElapsed()) {
                        viewModel.removeSeedPhraseViewTimestamp(args.id)
                        if (uiState.hasXprv) {
                            navigator.openAddSoftwareSignerScreen(
                                activityContext = this,
                                masterSignerId = args.id,
                                passphrase = passphrase.orEmpty()
                            )
                        } else {
                            navigator.openCreateNewSeedScreen(
                                activityContext = this,
                                masterSignerId = args.id,
                                passphrase = passphrase.orEmpty()
                            )
                        }
                    }
                },
                onViewSeedPhraseRequested = viewModel::onViewSeedPhraseRequested,
                onSeedPhraseRequestHandled = viewModel::onSeedPhraseRequestHandled,
                onPassphraseSubmitted = { passphrase ->
                    viewModel.checkPassphrase(
                        masterSignerId = args.id,
                        passphrase = passphrase,
                    )
                },
                onPassphraseConsume = viewModel::onPassphraseConsumed
            )

            if (showOptions) {
                val type = uiState.masterSigner?.type ?: uiState.remoteSigner?.type
                SignerOptionsSheet(
                    signerType = type,
                    canSignMessage = type?.let(::canSignMessage) == true,
                    canQrUnlock = viewModel.isJadeSigner(),
                    canExportXpub = canExportXpub(uiState),
                    onOption = { option ->
                        showOptions = false
                        onOptionClickListener(option)
                    },
                    onDismiss = { showOptions = false },
                )
            }
            editingName?.let { name ->
                SignerNameSheet(
                    name = name,
                    onSave = { newName ->
                        editingName = null
                        if (newName != name) viewModel.handleEditCompletedEvent(newName)
                    },
                    onDismiss = { editingName = null },
                )
            }

            if (openTrezorSuiteDeeplink != null) {
                NcConfirmationDialog(
                    title = stringResource(id = com.nunchuk.android.core.R.string.nc_confirmation),
                    message = stringResource(id = R.string.nc_open_trezor_suite_continue_signing_message),
                    positiveButtonText = stringResource(id = R.string.nc_open_trezor_suite),
                    negativeButtonText = stringResource(id = com.nunchuk.android.core.R.string.nc_cancel),
                    isPositiveButtonWrapContent = true,
                    onPositiveClick = {
                        openTrezorSuiteDeeplink?.let(this@SignerInfoActivity::openTrezorSuiteLink)
                        openTrezorSuiteDeeplink = null
                    },
                    onDismiss = { openTrezorSuiteDeeplink = null }
                )
            }

            ledgerHealthCheckSigner?.let { signer ->
                LedgerHealthCheckSheet(
                    masterFingerprint = signer.masterFingerprint,
                    derivationPath = signer.derivationPath,
                    onDismiss = { ledgerHealthCheckSigner = null },
                    onResult = { isSuccess, errorMessage ->
                        viewModel.onHardwareHealthCheckResult(isSuccess, errorMessage)
                    },
                )
            }

            bitBoxHealthCheckSigner?.let { signer ->
                BitBoxHealthCheckSheet(
                    masterFingerprint = signer.masterFingerprint,
                    derivationPath = signer.derivationPath,
                    onDismiss = { bitBoxHealthCheckSigner = null },
                    onResult = { isSuccess, errorMessage ->
                        viewModel.onHardwareHealthCheckResult(isSuccess, errorMessage)
                    },
                )
            }
        }
    }

    private fun onOptionClickListener(option: SingerOption) {
        when (option) {
            SingerOption.TOP_UP -> startNfcFlow(
                REQUEST_NFC_TOPUP_XPUBS
            )

            SingerOption.CHANGE_CVC -> onChangeCvcOptionClicked()
            SingerOption.BACKUP_KEY -> startNfcFlow(
                REQUEST_NFC_VIEW_BACKUP_KEY
            )

            SingerOption.REMOVE_KEY -> handleRemoveKey()
            SingerOption.QR_UNLOCK -> startActivity(
                JadeQrUnlockActivity.buildIntent(this)
            )
            SingerOption.SIGN_MESSAGE -> {
                val remoteSigner = viewModel.state.value.remoteSigner
                startActivity(Intent(this, SignMessageActivity::class.java).putExtras(
                    SignMessageArgs(
                        masterSignerId = args.id,
                        signerType = args.signerType,
                        masterFingerprint = remoteSigner?.masterFingerprint.orEmpty(),
                        derivationPath = remoteSigner?.derivationPath.orEmpty()
                    ).toBundle()
                ))
            }

            SingerOption.CHECK_FIRMWARE -> handlePortalAction(
                CheckFirmwareVersion
            )

            SingerOption.UPDATE_FIRMWARE -> selectFirmwareFile()
            SingerOption.EXPORT_XPUB -> openExportXpub()
        }
    }

    /** Offered for any key that has an exporter; see [XpubExporters]. */
    private fun canExportXpub(state: SignerInfoState) = state.toXpubKey()?.let(xpubExporters::canExport) == true

    private fun SignerInfoState.toXpubKey(): XpubKey? = masterSigner?.let { XpubKey(it.type, it.tags, isMasterSigner = true) }
        ?: remoteSigner?.let { XpubKey(it.type, it.tags, isMasterSigner = false) }

    private fun openExportXpub() {
        val state = viewModel.state.value
        val masterSigner = state.masterSigner
        val remoteSigner = state.remoteSigner
        ExportXpubActivity.start(
            activityContext = this,
            isMasterSigner = masterSigner != null,
            masterSignerId = args.id,
            signerType = masterSigner?.type ?: remoteSigner?.type ?: args.signerType,
            signerTags = masterSigner?.tags ?: remoteSigner?.tags.orEmpty(),
            masterFingerprint = remoteSigner?.masterFingerprint.orEmpty(),
            derivationPath = remoteSigner?.derivationPath.orEmpty(),
            descriptor = remoteSigner?.descriptor.orEmpty(),
        )
    }

    /**
     * TapSigner and software keys sign a message in-app; among hardware keys only the ones with
     * an in-app flow (Trezor, Ledger, BitBox) can, so the rest don't offer the option.
     */
    private fun canSignMessage(signerType: SignerType) = signerType == SignerType.NFC ||
            signerType == SignerType.SOFTWARE ||
            viewModel.isInAppHardwareSigner()

    private fun onChangeCvcOptionClicked() {
        viewModel.state.value.masterSigner?.id?.let { masterSignerId ->
            NfcSetupActivity.navigate(
                activity = this,
                setUpAction = NfcSetupActivity.CHANGE_CVC,
                masterSignerId = masterSignerId
            )
        }
    }

    private fun handleRemoveKey() {
        if (viewModel.isInAssistedWallet()) {
            dialogs.info(
                title = getString(R.string.nc_confirmation),
                message = getString(R.string.nc_warning_key_use_in_assisted_wallet),
            )
        } else if (viewModel.isInHotWallet()) {
            dialogs.warning(
                title = getString(R.string.nc_confirmation),
                message = getString(R.string.nc_warning_key_use_in_hot_wallet),
                btnYes = getString(R.string.nc_text_yes),
                btnNo = getString(R.string.nc_cancel),
                onYesClick = {
                    viewModel.handleRemoveSigner()
                }
            )
        } else if (viewModel.isInWallet()) {
            dialogs.warning(
                title = getString(R.string.nc_confirmation),
                message = getString(R.string.nc_warning_key_use_in_wallet),
                btnYes = getString(R.string.nc_text_yes),
                btnNo = getString(R.string.nc_cancel),
                onYesClick = {
                    viewModel.handleRemoveSigner()
                }
            )
        } else if (args.signerType == SignerType.FOREIGN_SOFTWARE) {
            dialogs.info(
                message = getString(R.string.nc_please_remove_on_added_device),
            )
        } else {
            dialogs.warning(
                title = getString(R.string.nc_confirmation),
                message = getString(R.string.nc_delete_key_msg),
                onYesClick = {
                    viewModel.handleRemoveSigner()
                }
            )
        }
    }

    private fun observeEvent() {
        flowObserver(viewModel.event, collector = ::handleEvent)

        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_NFC_VIEW_BACKUP_KEY }) {
            requestViewBackupKey(it)
            nfcViewModel.clearScanInfo()
        }

        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_NFC_HEALTH_CHECK }) {
            val isoDep = IsoDep.get(it.tag) ?: return@flowObserver
            viewModel.healthCheckTapSigner(
                isoDep,
                nfcViewModel.inputCvc.orEmpty(),
                viewModel.state.value.masterSigner ?: return@flowObserver
            )
            nfcViewModel.clearScanInfo()
        }

        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_NFC_TOPUP_XPUBS }) {
            topUpXPubs(it)
            nfcViewModel.clearScanInfo()
        }

        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_GENERATE_HEAL_CHECK_MSG }) { scanInfo ->
            viewModel.state.value.remoteSigner?.let { signer ->
                viewModel.generateColdcardHealthMessages(
                    Ndef.get(scanInfo.tag) ?: return@flowObserver,
                    signer.derivationPath
                )
            }
            nfcViewModel.clearScanInfo()
        }

        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_MK4_IMPORT_SIGNATURE }) {
            viewModel.state.value.remoteSigner?.let { signer ->
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

    private fun handleEvent(event: SignerInfoEvent) {
        showOrHideNfcLoading(event is SignerInfoEvent.NfcLoading)
        when (event) {
            is SignerInfoEvent.UpdateNameSuccessEvent -> {
                showEditSignerNameSuccess()
            }

            SignerInfoEvent.RemoveSignerCompletedEvent -> this.finish()
            is SignerInfoEvent.RemoveSignerErrorEvent -> NCToastMessage(this).showError(event.message)
            is SignerInfoEvent.UpdateNameErrorEvent -> NCToastMessage(this).showError(event.message)
            is SignerInfoEvent.HealthCheckErrorEvent -> {
                if (nfcViewModel.handleNfcError(event.e).not()) showHealthCheckError(event)
            }

            SignerInfoEvent.HealthCheckSuccessEvent -> NCToastMessage(this).showMessage(
                message = getString(
                    R.string.nc_txt_run_health_check_success_event,
                    viewModel.getSignerName()
                ),
                icon = R.drawable.ic_check_circle_outline
            )

            is SignerInfoEvent.GetTapSignerBackupKeyEvent -> showSaveShareOption()

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
            is SignerInfoEvent.DeleteExistingSignerSuccess -> {
                hideLoading()
                NCToastMessage(this).showMessage(
                    message = String.format(
                        getString(R.string.nc_key_has_been_deleted),
                        event.keyName
                    ),
                    icon = R.drawable.ic_check_circle_outline
                )
                this.finish()
            }

            is SignerInfoEvent.Error -> NCToastMessage(this).showError(event.e.message.orUnknownError())
            is SignerInfoEvent.GetHealthCheckPayload -> {
                hideLoading()
                navigator.openWalletAuthentication(
                    activityContext = this,
                    walletId = event.walletId,
                    requiredSignatures = event.payload.requiredSignatures,
                    type = VerificationType.SIGN_DUMMY_TX,
                    groupId = event.groupId,
                    dummyTransactionId = event.payload.dummyTransactionId,
                )
            }

            is SignerInfoEvent.Loading -> showOrHideLoading(event.loading)
            is SignerInfoEvent.SaveLocalFile -> showSaveFileState(event.isSuccess)
            is SignerInfoEvent.OpenTrezorSuite -> {
                openTrezorSuiteDeeplink = event.deeplink
            }
        }
    }

    override fun shareFile() {
        super.shareFile()
        controller.shareFile(viewModel.state.value.backupKeyPath)
    }

    override fun saveFileToLocal() {
        super.saveFileToLocal()
        viewModel.saveLocalFile(viewModel.state.value.backupKeyPath)
    }

    private fun showHealthCheckError(event: SignerInfoEvent.HealthCheckErrorEvent) {
        if (event.message.isNullOrEmpty()) {
            val errorMessage = if (event.e?.message.isNullOrEmpty()) {
                getString(
                    R.string.nc_txt_run_health_check_error_event,
                    viewModel.getSignerName()
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
    }

    private fun handleRunHealthCheck() {
        if (viewModel.getAssistedWalletIds().isNotEmpty()) {
            if (viewModel.getAssistedWalletIds().size == 1) {
                viewModel.onHealthCheck(viewModel.getAssistedWalletIds().first())
            } else {
                walletsViewModel.init(WalletComposeBottomSheet.ConfigArgs(), viewModel.getAssistedWalletIds(), emptyList(), emptyList())
                showWallets = true
            }
            return
        }
        val masterSigner = viewModel.state.value.masterSigner
        val remoteSigner = viewModel.state.value.remoteSigner
        if (masterSigner != null) {
            if (args.signerType == SignerType.NFC) {
                startNfcFlow(REQUEST_NFC_HEALTH_CHECK)
            } else if (masterSigner.software) {
                if (masterSigner.device.needPassPhraseSent) {
                    dialogs.input(
                        title = getString(R.string.nc_transaction_enter_passphrase),
                        onConfirmed = { viewModel.handleHealthCheck(masterSigner, it) }
                    )
                } else {
                    viewModel.handleHealthCheck(masterSigner)
                }
            }
        } else if (remoteSigner != null) {
            if (args.signerType == SignerType.COLDCARD_NFC) {
                startNfcFlow(
                    REQUEST_GENERATE_HEAL_CHECK_MSG
                )
            } else if (viewModel.isTrezorSigner()) {
                viewModel.requestTrezorHealthCheck()
            } else if (viewModel.isLedgerSigner()) {
                // Ledger runs the health check (sign message + verify) over BLE/USB in-app,
                // shown inline as a bottom sheet on this screen.
                ledgerHealthCheckSigner = remoteSigner
            } else if (viewModel.isBitBoxSigner()) {
                // Same in-app sign-message + verify round trip as Ledger, over the BitBox
                // transport — Confluence "4. Sign message".
                bitBoxHealthCheckSigner = remoteSigner
            } else {
                NCToastMessage(this).showWarning(getString(R.string.nc_health_check_is_unavailable_for_this_key))
            }
        }
    }

    private fun openMainScreen() {
        navigator.returnToMainScreen(this)
    }

    private fun showEditSignerNameSuccess() {
        NCToastMessage(this).showMessage(
            message = getString(R.string.nc_text_change_signer_success),
            icon = R.drawable.ic_check_circle_outline
        )
    }
    @Composable
    private fun WalletSelectionSheet() {
        if (showWallets) {
            SignerInfoBottomSheet(onDismiss = { showWallets = false }) {
                val state by walletsViewModel.state.collectAsStateWithLifecycle()
                AssistedWalletContent(
                    uiState = state,
                    onWalletClick = { wallet ->
                        showWallets = false
                        viewModel.onHealthCheck(wallet.wallet.id)
                    },
                )
            }
        }
    }

    override fun onHandledPortalAction(event: PortalDeviceEvent) {
        when (event) {
            is PortalDeviceEvent.UpdateFirmwareSuccess -> NCToastMessage(this).show(
                getString(R.string.nc_firmware_successfully_updated_to_version, event.status.version.orEmpty())
            )
            is PortalDeviceEvent.CheckFirmwareVersionSuccess -> dialogs.info(
                message = getString(R.string.nc_current_firmware_version, event.status.version.orEmpty()),
                btnYes = getString(R.string.nc_text_got_it),
                btnInfo = getString(R.string.nc_text_update_firmware),
                onInfoClick = {
                    selectFirmwareFile()
                }
            )
            else -> Unit
        }
    }

    companion object {

        fun start(
            activityContext: Context,
            isMasterSigner: Boolean,
            id: String,
            masterFingerprint: String,
            name: String,
            type: SignerType,
            derivationPath: String,
            justAdded: Boolean = false,
            setPassphrase: Boolean = false,
            isReplacePrimaryKey: Boolean = false,
            customMessage: String,
            existingKey: WalletsExistingKey? = null,
        ) {
            activityContext.startActivity(
                Intent(
                    activityContext,
                    SignerInfoActivity::class.java
                ).apply {
                    putExtras(
                        SignerInfoArgs(
                            id = id,
                            name = name,
                            derivationPath = derivationPath,
                            justAdded = justAdded,
                            signerType = type,
                            setPassphrase = setPassphrase,
                            masterFingerprint = masterFingerprint,
                            isReplacePrimaryKey = isReplacePrimaryKey,
                            customMessage = customMessage,
                            isMasterSigner = isMasterSigner,
                            existingKey = existingKey
                        ).toBundle()
                    )
                })
        }
    }
}
