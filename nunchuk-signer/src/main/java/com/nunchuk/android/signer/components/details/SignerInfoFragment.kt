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

import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewbinding.ViewBinding
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.greyDark
import com.nunchuk.android.compose.latoBold
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.core.base.BaseShareSaveFileFragment
import com.nunchuk.android.core.domain.data.CheckFirmwareVersion
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_GENERATE_HEAL_CHECK_MSG
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_MK4_IMPORT_SIGNATURE
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_NFC_HEALTH_CHECK
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_NFC_TOPUP_XPUBS
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_NFC_VIEW_BACKUP_KEY
import com.nunchuk.android.core.nfc.BasePortalActivity
import com.nunchuk.android.core.nfc.NfcActionListener
import com.nunchuk.android.core.nfc.NfcScanInfo
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.formatMMMddyyyyDate
import com.nunchuk.android.core.util.hideLoading
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.util.showOrHideNfcLoading
import com.nunchuk.android.core.util.showWarning
import com.nunchuk.android.core.util.toReadableDrawableResId
import com.nunchuk.android.core.util.toReadableString
import com.nunchuk.android.core.wallet.WalletBottomSheetResult
import com.nunchuk.android.core.wallet.WalletComposeBottomSheet
import com.nunchuk.android.model.HealthCheckHistory
import com.nunchuk.android.model.KeyHealthType
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.VerificationType
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.components.details.model.SingerOption
import com.nunchuk.android.signer.tapsigner.NfcSetupActivity
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.utils.healthCheckLabel
import com.nunchuk.android.utils.healthCheckTimeColor
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter

@AndroidEntryPoint
class SignerInfoFragment : BaseShareSaveFileFragment<ViewBinding>(), SingerInfoOptionBottomSheet.OptionClickListener {

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding {
        TODO("Not yet implemented")
    }

    private val viewModel: SignerInfoViewModel by viewModels()
    private val nfcViewModel: NfcViewModel by activityViewModels()

    private val args: SignerInfoFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val uiState by viewModel.state.collectAsStateWithLifecycle()
                val isPrimaryKey =
                    uiState.masterSigner?.let { viewModel.isPrimaryKey(it.device.masterFingerprint) }
                        ?: false
                SignerInfoContent(
                    uiState = uiState, isPrimaryKey = isPrimaryKey,
                    justAdded = args.justAdded,
                    onBackClicked = ::openMainScreen,
                    onMoreClicked = {
                        val type = viewModel.state.value.masterSigner?.type
                            ?: viewModel.state.value.remoteSigner?.type
                        type?.let { signerType ->
                            SingerInfoOptionBottomSheet.newInstance(signerType)
                                .show(childFragmentManager, "SingerInfoOptionBottomSheet")
                        }
                    },
                    onDoneClicked = ::openMainScreen,
                    onEditClicked = { onEditClicked(uiState.signerName) },
                    onHealthCheckClicked = ::handleRunHealthCheck,
                    onHistoryItemClick = {
                        navigator.openTransactionDetailsScreen(
                            activityContext = requireActivity(),
                            walletId = it.walletLocalId,
                            txId = it.transactionId,
                            roomId = ""
                        )
                    }
                )
            }

            if (args.existingKey != null) {
                NCInfoDialog(requireActivity()).showDialog(
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
                        requireActivity().finish()
                    }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeEvent()

        childFragmentManager.setFragmentResultListener(
            WalletComposeBottomSheet.TAG,
            viewLifecycleOwner
        ) { _, bundle ->
            val result = bundle.parcelable<WalletBottomSheetResult>(WalletComposeBottomSheet.RESULT)
                ?: return@setFragmentResultListener
            val walletId = result.walletId ?: return@setFragmentResultListener
            if (walletId.isNotEmpty()) {
                viewModel.onHealthCheck(walletId)
            }
        }
    }

    override fun onOptionClickListener(option: SingerOption) {
        when (option) {
            SingerOption.TOP_UP -> (requireActivity() as NfcActionListener).startNfcFlow(
                REQUEST_NFC_TOPUP_XPUBS
            )

            SingerOption.CHANGE_CVC -> onChangeCvcOptionClicked()
            SingerOption.BACKUP_KEY -> (requireActivity() as NfcActionListener).startNfcFlow(
                REQUEST_NFC_VIEW_BACKUP_KEY
            )

            SingerOption.REMOVE_KEY -> handleRemoveKey()
            SingerOption.SIGN_MESSAGE -> findNavController().navigate(
                SignerInfoFragmentDirections.actionSignerInfoFragmentToSignMessageFragment(
                    masterSignerId = args.id,
                    signerType = args.signerType
                )
            )

            SingerOption.CHECK_FIRMWARE -> (requireActivity() as BasePortalActivity<*>).handlePortalAction(
                CheckFirmwareVersion
            )

            SingerOption.UPDATE_FIRMWARE -> (requireActivity() as BasePortalActivity<*>).selectFirmwareFile()
        }
    }

    private fun onChangeCvcOptionClicked() {
        viewModel.state.value.masterSigner?.id?.let { masterSignerId ->
            NfcSetupActivity.navigate(
                activity = requireActivity(),
                setUpAction = NfcSetupActivity.CHANGE_CVC,
                masterSignerId = masterSignerId
            )
        }
    }

    private fun handleRemoveKey() {
        if (viewModel.isInAssistedWallet()) {
            NCInfoDialog(requireActivity()).showDialog(
                title = getString(R.string.nc_confirmation),
                message = getString(R.string.nc_warning_key_use_in_assisted_wallet),
            )
        } else if (viewModel.isInHotWallet()) {
            NCWarningDialog(requireActivity()).showDialog(
                title = getString(R.string.nc_confirmation),
                message = getString(R.string.nc_warning_key_use_in_hot_wallet),
                btnYes = getString(R.string.nc_text_yes),
                btnNo = getString(R.string.nc_cancel),
                onYesClick = {
                    viewModel.handleRemoveSigner()
                }
            )
        } else if (viewModel.isInWallet()) {
            NCWarningDialog(requireActivity()).showDialog(
                title = getString(R.string.nc_confirmation),
                message = getString(R.string.nc_warning_key_use_in_wallet),
                btnYes = getString(R.string.nc_text_yes),
                btnNo = getString(R.string.nc_cancel),
                onYesClick = {
                    viewModel.handleRemoveSigner()
                }
            )
        } else if (args.signerType == SignerType.FOREIGN_SOFTWARE) {
            NCInfoDialog(requireActivity()).showDialog(
                message = getString(R.string.nc_please_remove_on_added_device),
            )
        } else {
            NCWarningDialog(requireActivity()).showDialog(
                title = getString(R.string.nc_confirmation),
                message = getString(R.string.nc_delete_key_msg),
                onYesClick = {
                    viewModel.handleRemoveSigner()
                }
            )
        }
    }

    private fun observeEvent() {
        flowObserver(viewModel.event, ::handleEvent)

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

            SignerInfoEvent.RemoveSignerCompletedEvent -> requireActivity().finish()
            is SignerInfoEvent.RemoveSignerErrorEvent -> showError(event.message)
            is SignerInfoEvent.UpdateNameErrorEvent -> showError(event.message)
            is SignerInfoEvent.HealthCheckErrorEvent -> {
                if (nfcViewModel.handleNfcError(event.e).not()) showHealthCheckError(event)
            }

            SignerInfoEvent.HealthCheckSuccessEvent -> NCToastMessage(requireActivity()).showMessage(
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
                    NCToastMessage(requireActivity()).showError(message)
                }
            }

            SignerInfoEvent.TopUpXpubSuccess -> NCToastMessage(requireActivity()).showMessage(
                message = getString(R.string.nc_xpub_topped_up),
                icon = R.drawable.ic_check_circle_outline
            )

            is SignerInfoEvent.TopUpXpubFailed -> {
                val message = event.e?.message ?: getString(R.string.nc_topup_xpub_failed)
                NCToastMessage(requireActivity()).showError(message)
            }

            SignerInfoEvent.GenerateColdcardHealthMessagesSuccess -> (requireActivity() as NfcActionListener).startNfcFlow(
                REQUEST_MK4_IMPORT_SIGNATURE
            )

            SignerInfoEvent.NfcLoading -> showOrHideNfcLoading(true)
            is SignerInfoEvent.DeleteExistingSignerSuccess -> {
                hideLoading()
                NCToastMessage(requireActivity()).showMessage(
                    message = String.format(
                        getString(R.string.nc_key_has_been_deleted),
                        event.keyName
                    ),
                    icon = R.drawable.ic_check_circle_outline
                )
                requireActivity().finish()
            }

            is SignerInfoEvent.Error -> showError(event.e.message.orUnknownError())
            is SignerInfoEvent.GetHealthCheckPayload -> {
                hideLoading()
                navigator.openWalletAuthentication(
                    activityContext = requireActivity(),
                    walletId = event.walletId,
                    requiredSignatures = event.payload.requiredSignatures,
                    type = VerificationType.SIGN_DUMMY_TX,
                    groupId = event.groupId,
                    dummyTransactionId = event.payload.dummyTransactionId,
                )
            }

            is SignerInfoEvent.Loading -> showOrHideLoading(event.loading)
            is SignerInfoEvent.SaveLocalFile -> showSaveFileState(event.isSuccess)
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
            NCToastMessage(requireActivity()).showError(errorMessage)
        } else {
            NCToastMessage(requireActivity()).showWarning(event.message)
        }
    }

    private fun setupViews() {
        if (args.customMessage.isNotBlank()) {
            NCToastMessage(requireActivity()).showMessage(
                message = args.customMessage,
                icon = R.drawable.ic_check_circle_outline
            )
        } else if (args.isReplacePrimaryKey) {
            NCToastMessage(requireActivity()).showMessage(
                message = getString(R.string.nc_replace_primary_key_success),
                icon = R.drawable.ic_check_circle_outline
            )
        } else if (args.justAdded) {
            NCToastMessage(requireActivity()).showMessage(
                message = getString(R.string.nc_text_add_signer_success, args.name),
                icon = R.drawable.ic_check_circle_outline
            )
            if (args.setPassphrase) {
                NCToastMessage(requireActivity()).showMessage(
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
                WalletComposeBottomSheet.show(
                    childFragmentManager,
                    assistedWalletIds = viewModel.getAssistedWalletIds(),
                    configArgs = WalletComposeBottomSheet.ConfigArgs()
                )
            }
            return
        }
        val masterSigner = viewModel.state.value.masterSigner
        val remoteSigner = viewModel.state.value.remoteSigner
        if (masterSigner != null) {
            if (args.signerType == SignerType.NFC) {
                (requireActivity() as NfcActionListener).startNfcFlow(REQUEST_NFC_HEALTH_CHECK)
            } else if (masterSigner.software) {
                if (masterSigner.device.needPassPhraseSent) {
                    NCInputDialog(requireActivity()).showDialog(
                        title = getString(R.string.nc_transaction_enter_passphrase),
                        onConfirmed = { viewModel.handleHealthCheck(masterSigner, it) }
                    )
                } else {
                    viewModel.handleHealthCheck(masterSigner)
                }
            }
        } else if (remoteSigner != null) {
            if (args.signerType == SignerType.COLDCARD_NFC) {
                (requireActivity() as NfcActionListener).startNfcFlow(
                    REQUEST_GENERATE_HEAL_CHECK_MSG
                )
            } else {
                showWarning(getString(R.string.nc_health_check_is_unavailable_for_this_key))
            }
        }
    }

    private fun openMainScreen() {
        navigator.returnToMainScreen(requireActivity())
    }

    private fun onEditClicked(signerName: String) {
        val bottomSheet = SignerUpdateBottomSheet.show(
            fragmentManager = childFragmentManager,
            signerName = signerName
        )
        bottomSheet.setListener(viewModel::handleEditCompletedEvent)
    }

    private fun showEditSignerNameSuccess() {
        NCToastMessage(requireActivity()).showMessage(
            message = getString(R.string.nc_text_change_signer_success),
            icon = R.drawable.ic_check_circle_outline
        )
    }
}

@Composable
private fun SignerInfoContent(
    uiState: SignerInfoState = SignerInfoState(),
    justAdded: Boolean = false,
    isPrimaryKey: Boolean = false,
    onBackClicked: () -> Unit = {},
    onMoreClicked: () -> Unit = {},
    onDoneClicked: () -> Unit = {},
    onEditClicked: () -> Unit = {},
    onHealthCheckClicked: () -> Unit = {},
    onHistoryItemClick: (HealthCheckHistory) -> Unit = {}
) {
    val context = LocalContext.current
    val label by remember(uiState.lastHealthCheckTimeMillis) {
        derivedStateOf {
            uiState.lastHealthCheckTimeMillis.healthCheckLabel(context)
        }
    }
    val color = uiState.lastHealthCheckTimeMillis.healthCheckTimeColor()
    val isMyKey = uiState.masterSigner?.isVisible ?: uiState.remoteSigner?.isVisible ?: false

    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .navigationBarsPadding(),
            topBar = {
                Column(
                    modifier = Modifier
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    colorResource(id = R.color.nc_primary_light_color),
                                    colorResource(id = R.color.nc_primary_dark_color)
                                ),
                                start = Offset(0f, 1000f),
                                end = Offset(0f, 0f)
                            ),
                            shape = RectangleShape
                        ),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NcTopAppBar(
                        textStyle = NunchukTheme.typography.titleLarge.copy(color = Color.White),
                        backgroundColor = Color.Transparent,
                        title = stringResource(id = R.string.nc_text_signer_info),
                        tintColor = Color.White,
                        onBackPress = onBackClicked,
                        actions = {
                            IconButton(onClick = onMoreClicked) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_more_horizontal),
                                    contentDescription = "More",
                                    tint = Color.White
                                )
                            }
                        }
                    )
                    val resId = if (uiState.masterSigner != null) {
                        uiState.masterSigner.toReadableDrawableResId(isPrimaryKey = isPrimaryKey)
                    } else if (uiState.remoteSigner != null) {
                        uiState.remoteSigner.toReadableDrawableResId()
                    } else {
                        null
                    }
                    resId?.let {
                        NcCircleImage(
                            modifier = Modifier.padding(top = 12.dp),
                            resId = resId,
                            size = 96.dp,
                            iconSize = 60.dp,
                            iconTintColor = colorResource(id = R.color.nc_grey_g7),
                            color = if (uiState.assistedWalletIds.isEmpty()) Color.White else color
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clickable { onEditClicked() }
                            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = uiState.signerName,
                            style = NunchukTheme.typography.heading.copy(color = Color.White)
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit),
                            contentDescription = "",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(18.dp)
                        )
                    }

                    Row(modifier = Modifier.padding(top = 8.dp)) {
                        if (isPrimaryKey) {
                            Text(
                                modifier = Modifier
                                    .background(
                                        color = colorResource(id = R.color.nc_fill_beewax),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 8.dp),
                                text = stringResource(id = R.string.nc_signer_type_primary_key),
                                style = TextStyle(
                                    color = MaterialTheme.colorScheme.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.W500,
                                    fontFamily = latoBold
                                )
                            )
                        }
                        val signerType =
                            uiState.masterSigner?.type?.toReadableString(
                                LocalContext.current,
                                false
                            )
                                ?: uiState.remoteSigner?.type?.toReadableString(
                                    LocalContext.current,
                                    isPrimaryKey
                                )
                                ?: ""
                        Text(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .background(
                                    color = colorResource(id = R.color.nc_grey_g2),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            text = signerType,
                            style = TextStyle(
                                color = colorResource(R.color.nc_grey_g7),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.W500,
                                fontFamily = latoBold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            },
            bottomBar = {
                Column(modifier = Modifier.padding()) {
                    if (justAdded) {
                        NcPrimaryDarkButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            onClick = onDoneClicked
                        ) {
                            Text(text = stringResource(id = R.string.nc_text_done))
                        }
                    }
                    if (isMyKey) {
                        NcOutlineButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = if (justAdded) 0.dp else 16.dp,
                                    bottom = 16.dp
                                ),
                            onClick = onHealthCheckClicked
                        ) {
                            Text(text = stringResource(id = R.string.nc_txt_run_health_check))
                        }
                    }
                }
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                if (isMyKey) {
                    uiState.nfcCardId?.let {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.nc_card_id),
                                style = NunchukTheme.typography.titleSmall
                            )
                            Text(
                                modifier = Modifier.padding(top = 4.dp),
                                text = uiState.nfcCardId,
                                style = NunchukTheme.typography.body
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = stringResource(R.string.nc_text_signer_spec),
                            style = NunchukTheme.typography.titleSmall
                        )
                        val keySpec = if (uiState.masterSigner != null) {
                            uiState.masterSigner.device.masterFingerprint
                        } else if (uiState.remoteSigner != null) {
                            uiState.remoteSigner.descriptor
                        } else {
                            ""
                        }
                        Text(
                            modifier = Modifier.padding(top = 4.dp),
                            text = keySpec,
                            style = NunchukTheme.typography.body
                        )
                    }
                }
                if (uiState.assistedWalletIds.isNotEmpty()) {
                    if (isMyKey) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            HorizontalDivider()
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Column {
                            Row(
                                modifier = Modifier
                                    .background(
                                        color = color,
                                        shape = RoundedCornerShape(size = 20.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_health_check_dark),
                                    contentDescription = "",
                                    tint = colorResource(R.color.nc_grey_g7)
                                )
                                Text(
                                    text = label,
                                    style = NunchukTheme.typography.bodySmall.copy(color = colorResource(R.color.nc_grey_g7))
                                )
                            }

                            Text(
                                modifier = Modifier.padding(top = 16.dp),
                                text = stringResource(R.string.nc_health_check_history),
                                style = NunchukTheme.typography.title
                            )
                            if (uiState.healthCheckHistories.isNullOrEmpty()) {
                                Text(
                                    modifier = Modifier.padding(top = 16.dp),
                                    text = stringResource(R.string.nc_no_history),
                                    style = NunchukTheme.typography.body
                                )
                            }
                        }

                    }

                    items(uiState.healthCheckHistories.orEmpty()) {
                        HealthCheckHistoryItem(it, onHistoryItemClick = { onHistoryItemClick(it) })
                    }
                }
            }
        }
    }
}

@Composable
fun HealthCheckHistoryItem(history: HealthCheckHistory, onHistoryItemClick: () -> Unit = {}) {
    Column {
        Row(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .clickable { onHistoryItemClick() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = history.createdTimeMillis.formatMMMddyyyyDate,
                    style = NunchukTheme.typography.body,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val status = when (history.type) {
                    KeyHealthType.HEALTH_CHECK.name, KeyHealthType.DUMMY_TRANSACTION.name -> {
                        "Health check succeeded"
                    }

                    KeyHealthType.TRANSACTION.name -> {
                        "Transaction signed"
                    }

                    else -> {
                        ""
                    }
                }
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = status,
                    style = NunchukTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.greyDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (history.type == KeyHealthType.TRANSACTION.name) {
                Image(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(id = R.drawable.ic_right_arrow_dark),
                    contentDescription = "Arrow"
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun SignerInfoScreenPreview() {
    SignerInfoContent(
        justAdded = true,
        uiState = SignerInfoState(
            signerName = "Key",
            masterSigner = MasterSigner(
                type = SignerType.NFC
            ),
        )
    )
}

@PreviewLightDark
@Composable
private fun SignerInfoAssistedScreenPreview() {
    SignerInfoContent(
        justAdded = true,
        uiState = SignerInfoState(
            signerName = "Key",
            masterSigner = MasterSigner(
                type = SignerType.NFC
            ),
            assistedWalletIds = listOf("abc")
        )
    )
}