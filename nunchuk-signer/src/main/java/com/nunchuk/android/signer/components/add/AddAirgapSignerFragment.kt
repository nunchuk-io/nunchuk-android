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

package com.nunchuk.android.signer.components.add

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.viewbinding.ViewBinding
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.base.BaseCameraFragment
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.signer.OnChainAddSignerParam
import com.nunchuk.android.core.util.BackUpSeedPhraseType
import com.nunchuk.android.core.util.hideLoading
import com.nunchuk.android.core.util.isRecommendedMultiSigPath
import com.nunchuk.android.core.util.isRecommendedSingleSigPath
import com.nunchuk.android.core.util.isTestNetSigner
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nav.args.BackUpSeedPhraseArgs
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.share.result.GlobalResult
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.components.add.AddAirgapSignerEvent.AddAirgapSignerErrorEvent
import com.nunchuk.android.signer.components.add.AddAirgapSignerEvent.AddAirgapSignerSuccessEvent
import com.nunchuk.android.signer.components.add.AddAirgapSignerEvent.AddSameKey
import com.nunchuk.android.signer.components.add.AddAirgapSignerEvent.ErrorMk4TestNet
import com.nunchuk.android.signer.components.add.AddAirgapSignerEvent.KeyVerifiedSuccess
import com.nunchuk.android.signer.components.add.AddAirgapSignerEvent.LoadingEventAirgap
import com.nunchuk.android.signer.components.add.AddAirgapSignerEvent.ParseKeystoneAirgapSignerSuccess
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.ResultExistingKey
import com.nunchuk.android.utils.MaxLengthTransformation
import com.nunchuk.android.utils.parcelableArrayList
import com.nunchuk.android.widget.NCInfoDialog
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class AddAirgapSignerFragment : BaseCameraFragment<ViewBinding>(),
    BottomSheetOptionListener {
    @Inject
    lateinit var membershipStepManager: MembershipStepManager

    private val viewModel: AddAirgapSignerViewModel by viewModels()

    private val importFileLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                viewModel.parseAirgapSigner(it)
            }
        }

    private val scanQrLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val keys = it.data?.parcelableArrayList<SingleSigner>(PASSPORT_EXTRA_KEYS).orEmpty()
                handleResult(viewModel.validateAndUpdateSigners(keys))
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                AddAirgapSignerContent(
                    remainTime = remainTime, uiState = uiState,
                    isMembershipFlow = (requireActivity() as AddAirgapSignerActivity).isMembershipFlow,
                    onKeyNameChange = { viewModel.updateKeyName(it) },
                    onKeySpecChange = { viewModel.updateKeySpec(it) },
                    onChainAddSignerParam = (requireActivity() as AddAirgapSignerActivity).onChainAddSignerParam,
                    onImportFile = {
                        importFileLauncher.launch("*/*")
                    },
                    onScanQr = {
                        requestCameraPermissionOrExecuteAction()
                    },
                    onAddSigner = { keyName, keySpec ->
                        viewModel.handleAddAirgapSigner(
                            signerName = keyName,
                            signerSpec = keySpec,
                            isMembershipFlow = (requireActivity() as AddAirgapSignerActivity).isMembershipFlow,
                            signerTag = (requireActivity() as AddAirgapSignerActivity).signerTag,
                            xfp = (requireActivity() as AddAirgapSignerActivity).xfp,
                            newIndex = (requireActivity() as AddAirgapSignerActivity).newIndex,
                        )
                    })
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        membershipStepManager.updateStep(true)
    }

    override fun onDestroy() {
        membershipStepManager.updateStep(false)
        super.onDestroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeEvent()
        viewModel.init(
            groupId = (activity as AddAirgapSignerActivity).groupId,
            isMembershipFlow = (activity as AddAirgapSignerActivity).isMembershipFlow,
            replacedXfp = (activity as AddAirgapSignerActivity).replacedXfp,
            walletId = (activity as AddAirgapSignerActivity).walletId,
            onChainAddSignerParam = (activity as AddAirgapSignerActivity).onChainAddSignerParam,
        )
    }

    override fun onOptionClicked(option: SheetOption) {
        when (option.type) {
            SheetOptionType.TYPE_ADD_BITBOX -> viewModel.changeKeyType(signerTag = SignerTag.BITBOX)
            SheetOptionType.TYPE_ADD_AIRGAP_JADE -> viewModel.changeKeyType(signerTag = SignerTag.JADE)
            SignerType.COLDCARD_NFC.ordinal -> viewModel.changeKeyType(signerTag = SignerTag.COLDCARD)
            SheetOptionType.TYPE_ADD_AIRGAP_PASSPORT -> viewModel.changeKeyType(signerTag = SignerTag.PASSPORT)
            SheetOptionType.TYPE_ADD_AIRGAP_OTHER -> viewModel.changeKeyType(signerTag = null)
            SheetOptionType.TYPE_ADD_AIRGAP_KEYSTONE -> viewModel.changeKeyType(signerTag = SignerTag.KEYSTONE)
            SheetOptionType.TYPE_ADD_LEDGER -> viewModel.changeKeyType(signerTag = SignerTag.LEDGER)
            SheetOptionType.TYPE_ADD_AIRGAP_SEEDSIGNER -> viewModel.changeKeyType(signerTag = SignerTag.SEEDSIGNER)
            SheetOptionType.TYPE_ADD_TREZOR -> viewModel.changeKeyType(signerTag = SignerTag.TREZOR)
            else -> viewModel.signers.getOrNull(option.type)?.let {
                val isMembershipFlow =
                    (requireActivity() as AddAirgapSignerActivity).isMembershipFlow
                if (isMembershipFlow && it.derivationPath.isTestNetSigner && viewModel.chain == Chain.MAIN) {
                    NCInfoDialog(requireActivity())
                        .showDialog(
                            title = getString(R.string.nc_error),
                            message = getString(R.string.nc_error_device_in_testnet_msg_v2)
                        )
                } else {
                    viewModel.updateKeySpec(it.descriptor)
                }
            }
        }
    }

    override fun onCameraPermissionGranted(fromUser: Boolean) {
        openScanDynamicQRScreen()
    }

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): ViewBinding {
        TODO("Not yet implemented")
    }

    private fun handleSignerSuccess(signer: SingleSigner) {
        val activity = requireActivity() as AddAirgapSignerActivity
        val onChainAddSignerParam = activity.onChainAddSignerParam

        if (onChainAddSignerParam?.isClaiming == true) {
            activity.finish()
            navigator.returnToClaimScreen(requireActivity())
        } else if (onChainAddSignerParam?.isVerifyBackupSeedPhrase() == true && onChainAddSignerParam.currentSigner?.fingerPrint?.isNotEmpty() == true) {
            if (signer.masterFingerprint == onChainAddSignerParam.currentSigner?.fingerPrint) {
                if (onChainAddSignerParam.isReplaceKeyFlow()) {
                    viewModel.setReplaceKeyVerified(
                        keyId = signer.masterSignerId,
                        groupId = activity.groupId,
                        walletId = activity.walletId
                    )
                } else {
                    viewModel.setKeyVerified(
                        groupId = activity.groupId,
                        masterSignerId = signer.masterFingerprint
                    )
                }
            } else {
                activity.setResult(Activity.RESULT_OK)
                navigator.returnMembershipScreen()
            }
        } else if (onChainAddSignerParam != null) {
            activity.setResult(Activity.RESULT_OK)
            navigator.returnMembershipScreen()
        } else {
            openSignerInfo(signer)
        }
    }

    private fun observeEvent() {
        viewModel.event.observe(viewLifecycleOwner) {
            when (it) {
                is AddAirgapSignerSuccessEvent -> handleSignerSuccess(it.singleSigner)
                is AddAirgapSignerErrorEvent -> onAddAirSignerError(it.message)
                is LoadingEventAirgap -> showOrHideLoading(it.isLoading)
                is ParseKeystoneAirgapSignerSuccess -> handleResult(it.signers)
                AddSameKey -> showError(getString(R.string.nc_error_add_same_key))
                ErrorMk4TestNet -> NCInfoDialog(requireActivity())
                    .showDialog(
                        title = getString(R.string.nc_error),
                        message = getString(R.string.nc_error_device_in_testnet_msg_v2)
                    )

                AddAirgapSignerEvent.NewIndexNotMatchException -> {
                    requireActivity().apply {
                        setResult(GlobalResult.RESULT_INDEX_NOT_MATCH)
                        finish()
                    }
                }

                AddAirgapSignerEvent.XfpNotMatchException -> showError(getString(R.string.nc_airgap_xfp_does_not_match))
                is AddAirgapSignerEvent.CheckExisting -> {
                    when (it.type) {
                        ResultExistingKey.Software -> NCInfoDialog(requireActivity())
                            .showDialog(
                                message = String.format(
                                    getString(R.string.nc_existing_key_is_software_key_delete_key),
                                    it.singleSigner.masterFingerprint.uppercase(Locale.getDefault())
                                ),
                                btnYes = getString(R.string.nc_text_yes),
                                btnInfo = getString(R.string.nc_text_no),
                                onYesClick = {
                                    openSelectHardwareOption()
                                },
                                onInfoClick = {}
                            )

                        ResultExistingKey.Hardware -> {
                            NCInfoDialog(requireActivity())
                                .showDialog(
                                    message = String.format(
                                        getString(R.string.nc_existing_key_change_key_type),
                                        it.singleSigner.masterFingerprint.uppercase(Locale.getDefault())
                                    ),
                                    btnYes = getString(R.string.nc_text_yes),
                                    btnInfo = getString(R.string.nc_text_no),
                                    onYesClick = {
                                        openSelectHardwareOption()
                                    },
                                    onInfoClick = {}
                                )
                        }

                        ResultExistingKey.None -> openSignerInfo(it.singleSigner)
                    }
                }

                KeyVerifiedSuccess -> {
                    val activity = requireActivity() as AddAirgapSignerActivity
                    activity.setResult(Activity.RESULT_OK)
                    navigator.openBackUpSeedPhraseActivity(
                        requireActivity(),
                        BackUpSeedPhraseArgs(
                            type = BackUpSeedPhraseType.SUCCESS,
                            signer = null,
                            groupId = activity.groupId,
                            walletId = activity.walletId
                        )
                    )
                }
            }
        }
    }

    private fun openSelectHardwareOption() {
        val options =
            listOfNotNull(
                SheetOption(
                    type = SheetOptionType.TYPE_ADD_AIRGAP_JADE,
                    label = getString(com.nunchuk.android.core.R.string.nc_blockstream_jade),
                ),
                SheetOption(
                    type = SignerType.COLDCARD_NFC.ordinal,
                    label = getString(com.nunchuk.android.core.R.string.nc_coldcard)
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_ADD_AIRGAP_PASSPORT,
                    label = getString(com.nunchuk.android.core.R.string.nc_foudation_passport),
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_ADD_AIRGAP_OTHER,
                    label = getString(com.nunchuk.android.core.R.string.nc_signer_generic_air_gapped)
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_ADD_AIRGAP_KEYSTONE,
                    label = getString(com.nunchuk.android.core.R.string.nc_keystone),
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_ADD_AIRGAP_SEEDSIGNER,
                    label = getString(com.nunchuk.android.core.R.string.nc_seedsigner),
                ),
            )
        BottomSheetOption.newInstance(
            options = options,
            title = getString(R.string.nc_what_type_of_hardware_want_to_add),
        ).show(childFragmentManager, "BottomSheetOption")
    }

    private fun onAddAirSignerError(message: String) {
        hideLoading()
        showError(message)
    }

    private fun openSignerInfo(singleSigner: SingleSigner) {
        hideLoading()
        requireActivity().setResult(Activity.RESULT_OK)
        requireActivity().finish()
        val replacingWalletId = (requireActivity() as AddAirgapSignerActivity).walletId
        val groupId = (requireActivity() as AddAirgapSignerActivity).groupId
        // if replace key in wallet flow, we don't open signer info screen
        if (replacingWalletId.isNotEmpty() || groupId.isNotEmpty()) return
        if ((requireActivity() as AddAirgapSignerActivity).isMembershipFlow.not()) {
            navigator.openSignerInfoScreen(
                requireContext(),
                isMasterSigner = singleSigner.hasMasterSigner,
                id = singleSigner.masterSignerId,
                masterFingerprint = singleSigner.masterFingerprint,
                name = singleSigner.name,
                type = singleSigner.type,
                derivationPath = singleSigner.derivationPath,
                justAdded = true
            )
        }
    }

    private fun openScanDynamicQRScreen() {
        scanQrLauncher.launch(ScanDynamicQRActivity.buildIntent(requireActivity()))
    }

    private fun handleResult(keys: List<SingleSigner>) {
        if (keys.isNotEmpty()) {
            if (keys.size == 1) {
                bindKey(keys.first())
            } else {
                showSelectKeysDialog(keys)
            }
        }
    }

    private fun bindKey(key: SingleSigner) {
        viewModel.updateKeySpec(key.descriptor)
    }

    private fun showSelectKeysDialog(
        signers: List<SingleSigner>
    ) {
        val fragment = BottomSheetOption.newInstance(signers.mapIndexed { index, singleSigner ->
            SheetOption(
                type = index,
                label = if (singleSigner.derivationPath.isTestNetSigner) {
                    "${singleSigner.derivationPath} (${getString(R.string.nc_testnet)})"
                } else if (singleSigner.derivationPath.isRecommendedMultiSigPath) {
                    "${singleSigner.derivationPath} (${
                        getString(
                            R.string.nc_recommended_for_multisig
                        )
                    })"
                } else if (singleSigner.derivationPath.isRecommendedSingleSigPath) {
                    "${singleSigner.derivationPath} (${
                        getString(
                            R.string.nc_recommended_for_single_sig
                        )
                    })"
                } else {
                    singleSigner.derivationPath
                }
            )
        }, title = getString(R.string.nc_signer_select_key_dialog_title))
        fragment.show(childFragmentManager, "BottomSheetOption")
    }
}

@Composable
private fun AddAirgapSignerContent(
    remainTime: Int = 0,
    uiState: AddAirgapSignerState = AddAirgapSignerState(),
    isMembershipFlow: Boolean = false,
    onChainAddSignerParam: OnChainAddSignerParam? = null,
    onAddSigner: (String, String) -> Unit = { _, _ -> },
    onScanQr: () -> Unit = {},
    onImportFile: () -> Unit = {},
    onKeyNameChange: (String) -> Unit = {},
    onKeySpecChange: (String) -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                val title = if (isMembershipFlow) {
                    stringResource(
                        id = R.string.nc_estimate_remain_time,
                        remainTime
                    )
                } else {
                    ""
                }
                NcTopAppBar(title = title, actions = {
                    Spacer(modifier = Modifier.size(LocalViewConfiguration.current.minimumTouchTargetSize))
                })
            }, bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    onClick = {
                        onAddSigner(uiState.keyName, uiState.keySpec)
                    }) {
                    Text(
                        text = if (onChainAddSignerParam?.isVerifyBackupSeedPhrase() == true) stringResource(
                            id = R.string.nc_text_continue
                        ) else stringResource(id = R.string.nc_text_add_signer)
                    )
                }
            }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {

                Text(
                    text = if (onChainAddSignerParam != null) "Add Blockstream Jade" else stringResource(
                        R.string.nc_add_an_airgapped_key
                    ),
                    style = NunchukTheme.typography.heading
                )

                if (isMembershipFlow.not()) {
                    NcTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        title = stringResource(id = R.string.nc_text_signer_name),
                        error = if (uiState.showKeyNameError) stringResource(id = R.string.nc_text_required) else null,
                        value = uiState.keyName,
                        maxLines = 1,
                        maxLength = MAX_LENGTH,
                        enableMaxLength = true,
                        visualTransformation = MaxLengthTransformation(maxLength = MAX_LENGTH),
                        onValueChange = {
                            if (it.length <= MAX_LENGTH) {
                                onKeyNameChange(it)
                            }
                        },
                    )
                }

                NcTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    error = if (uiState.showKeySpecError) stringResource(id = R.string.nc_error_invalid_signer_spec) else "",
                    title = stringResource(id = R.string.nc_text_signer_spec),
                    value = uiState.keySpec,
                    inputBoxHeight = 180.dp,
                    onValueChange = {
                        onKeySpecChange(it)
                    }
                )

                Row(Modifier.padding(top = 16.dp)) {
                    NcOutlineButton(
                        modifier = Modifier
                            .weight(1f, true)
                            .padding(end = 4.dp)
                            .height(36.dp),
                        onClick = { onImportFile() },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                modifier = Modifier.padding(start = 6.dp),
                                text = stringResource(id = R.string.nc_import_via_file),
                                style = NunchukTheme.typography.captionTitle
                            )
                            NcIcon(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(18.dp),
                                painter = painterResource(id = R.drawable.ic_import),
                                contentDescription = "Import",
                            )
                        }
                    }

                    NcOutlineButton(
                        modifier = Modifier
                            .weight(1f, true)
                            .padding(start = 4.dp)
                            .height(36.dp),
                        onClick = { onScanQr() },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                modifier = Modifier.padding(start = 6.dp),
                                text = stringResource(id = R.string.nc_scan_qr),
                                style = NunchukTheme.typography.captionTitle
                            )
                            NcIcon(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(18.dp),
                                painter = painterResource(id = R.drawable.ic_qr),
                                contentDescription = "QR",
                            )
                        }
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun AddAirgapSignerScreenPreview() {
    AddAirgapSignerContent()
}

internal const val PASSPORT_EXTRA_KEYS = "PASSPORT_EXTRA_KEYS"
internal const val MAX_LENGTH = 20