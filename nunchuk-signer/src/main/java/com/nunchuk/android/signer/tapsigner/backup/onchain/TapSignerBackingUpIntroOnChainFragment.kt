package com.nunchuk.android.signer.tapsigner.backup.onchain

import android.nfc.tech.IsoDep
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcActionListener
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideNfcLoading
import com.nunchuk.android.exception.NCNativeException
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.tapsigner.NfcSetupActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TapSignerBackingUpIntroOnChainFragment : MembershipFragment() {
    private val viewModel: TapSignerBackingUpIntroOnChainViewModel by viewModels()
    private val nfcViewModel: NfcViewModel by activityViewModels()
    private val args: TapSignerBackingUpIntroOnChainFragmentArgs by navArgs()
    private var isManualBackup = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                TapSignerBackingUpIntroOnChainScreen(
                    viewModel = viewModel,
                    membershipStepManager = membershipStepManager,
                    onManualBackup = ::onManualBackup
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle).collect { event ->
                when (event) {
                    is TapSignerBackingUpIntroOnChainEvent.GetTapSignerBackupKeyError -> if (nfcViewModel.handleNfcError(
                            event.e
                        ).not()
                    ) {
                        val message =
                            if (event.e is NCNativeException && event.e.message.contains("-6100")) {
                                getString(R.string.nc_card_id_does_not_match)
                            } else {
                                event.e?.message.orUnknownError()
                            }
                        showError(message)
                    }

                    is TapSignerBackingUpIntroOnChainEvent.GetTapSignerBackupKeyEvent -> {
                        val activity = requireActivity() as NfcSetupActivity
                        if (isManualBackup && activity.keyId.isNotEmpty()) {
                            // This is manual backup for replace key flow
                            viewModel.setReplaceKeyVerified(
                                keyId = activity.keyId,
                                filePath = event.filePath,
                                groupId = activity.groupId,
                                walletId = activity.walletId
                            )
                            isManualBackup = false
                        } else {
                            handleBackUpKeySuccess(event.filePath)
                        }
                    }

                    is TapSignerBackingUpIntroOnChainEvent.NfcLoading -> showOrHideNfcLoading(event.isLoading)
                    TapSignerBackingUpIntroOnChainEvent.OnContinueClicked -> (requireActivity() as NfcActionListener).startNfcFlow(
                        BaseNfcActivity.REQUEST_NFC_VIEW_BACKUP_KEY
                    )

                    TapSignerBackingUpIntroOnChainEvent.OnGetSingleWalletDone -> requireActivity().finish()
                    is TapSignerBackingUpIntroOnChainEvent.ReturnSignerModel -> {
                        val signerModel = event.singleSigner.toModel()
                        requireActivity().setResult(
                            android.app.Activity.RESULT_OK,
                            android.content.Intent().apply {
                                putExtra(GlobalResultKey.EXTRA_SIGNER, signerModel)
                            }
                        )
                        requireActivity().finish()
                    }

                    TapSignerBackingUpIntroOnChainEvent.KeyVerifiedSuccess -> {
                        requireActivity().finish()
                    }

                    is TapSignerBackingUpIntroOnChainEvent.ShowError -> {
                        showError(event.throwable?.message.orUnknownError())
                    }
                }
            }
        }

        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_NFC_VIEW_BACKUP_KEY }) {
            val onChainAddSignerParam =
                (requireActivity() as NfcSetupActivity).onChainAddSignerParam
            if (onChainAddSignerParam != null) {
                // OnChain flow: get signer and return to OnChainTimelockAddKeyListFragment
                viewModel.getSignerForOnChain(
                    isoDep = IsoDep.get(it.tag) ?: return@flowObserver,
                    cvc = nfcViewModel.inputCvc.orEmpty(),
                    index = (requireActivity() as NfcSetupActivity).signerIndex
                )
            } else {
                // Regular or manual backup flow - get backup file
                viewModel.getTapSignerBackup(
                    isoDep = IsoDep.get(it.tag) ?: return@flowObserver,
                    cvc = nfcViewModel.inputCvc.orEmpty(),
                    index = (requireActivity() as NfcSetupActivity).signerIndex
                )
            }
            nfcViewModel.clearScanInfo()
        }
    }

    private fun onManualBackup() {
        val activity = requireActivity() as NfcSetupActivity
        viewModel.setKeyVerified(
            groupId = activity.groupId,
            masterSignerId = args.masterSignerId
        )

    }

    private fun handleBackUpKeySuccess(filePath: String) {
        findNavController().navigate(
            TapSignerBackingUpIntroOnChainFragmentDirections.actionTapSignerBackingUpIntroOnChainFragmentToUploadBackUpTapSignerFragment(
                filePath = filePath,
                masterSignerId = args.masterSignerId,
                isOldKey = true
            )
        )
    }
}

@Composable
private fun TapSignerBackingUpIntroOnChainScreen(
    viewModel: TapSignerBackingUpIntroOnChainViewModel = viewModel(),
    membershipStepManager: MembershipStepManager,
    onManualBackup: () -> Unit = {},
) {
    val remainingTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()

    TapSignerBackingUpIntroOnChainContent(
        onContinueClicked = viewModel::onContinueClicked,
        onManualBackup = onManualBackup,
        remainingTime = remainingTime,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TapSignerBackingUpIntroOnChainContent(
    onContinueClicked: () -> Unit = {},
    onManualBackup: () -> Unit = {},
    remainingTime: Int = 0,
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    NunchukTheme {
        Scaffold(
            modifier = Modifier.imePadding(),
            topBar = {
                NcImageAppBar(
                    backgroundRes = R.drawable.nc_bg_tap_signer_explain,
                    title = stringResource(R.string.nc_estimate_remain_time, remainingTime),
                )
            }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .imePadding()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    text = "Backing up TAPSIGNER",
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = "If the TAPSIGNER card is lost or damaged, you can recover it using an encrypted backup file and the Backup Password printed on the back of the card.\n" +
                            "\n" +
                            "Would you like to store the encrypted backup on the Nunchuk server? You can also back it up manually.",
                    style = NunchukTheme.typography.body
                )

                Spacer(modifier = Modifier.weight(1.0f))

                NcHintMessage(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    messages = listOf(
                        ClickAbleText(
                            "We highly recommend that you keep a second copy of the Backup Password and store it somewhere safe."
                        )
                    )
                )

                NcPrimaryDarkButton(
                    modifier = Modifier
                        .padding(16.dp)
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .fillMaxWidth(),
                    onClick = onContinueClicked
                ) {
                    Text(text = "Back up on Nunchuk server")
                }

                NcOutlineButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    onClick = onManualBackup,
                ) {
                    Text(text = "I'll do it myself")
                }
            }
        }
    }
}

@Preview
@Composable
private fun CheckBackUpByAppScreenPreview() {
    TapSignerBackingUpIntroOnChainContent(

    )
}