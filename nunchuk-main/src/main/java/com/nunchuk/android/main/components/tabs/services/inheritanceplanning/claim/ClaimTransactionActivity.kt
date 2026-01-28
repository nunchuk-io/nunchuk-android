package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim

import android.app.Activity
import android.nfc.tech.IsoDep
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.viewbinding.ViewBinding
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcActionListener
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.ClaimTransactionViewModel.LoadingType
import com.nunchuk.android.nav.args.ClaimTransactionArgs
import com.nunchuk.android.transaction.components.details.TransactionDetailView
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.widget.NCInputDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter

@AndroidEntryPoint
class ClaimTransactionActivity : BaseNfcActivity<ViewBinding>() {

    override fun initializeBinding(): ViewBinding = ViewBinding {
        val args = ClaimTransactionArgs.deserializeFrom(intent)

        ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ClaimTransactionScreen(
                    args = args,
                    activity = this@ClaimTransactionActivity
                )
            }
        }
    }.also {
        enableEdgeToEdge()
    }
}

@Composable
private fun ClaimTransactionScreen(
    args: ClaimTransactionArgs,
    activity: ClaimTransactionActivity
) {
    val viewModel = hiltViewModel<ClaimTransactionViewModel, ClaimTransactionViewModel.Factory> { factory ->
        factory.create(args)
    }

    val nfcViewModel = hiltViewModel<NfcViewModel>(viewModelStoreOwner = activity)

    val context = LocalContext.current

    val state by viewModel.state.collectAsStateWithLifecycle()
    val miniscriptState by viewModel.miniscriptState.collectAsStateWithLifecycle()
    val needPassphrase by viewModel.needPassphrase.collectAsStateWithLifecycle()
    val loadingType by viewModel.loadingType.collectAsStateWithLifecycle()

    // Handle NFC scanning for tap signer
    LaunchedEffect(Unit) {
        nfcViewModel.nfcScanInfo
            .filter { it.requestCode == BaseNfcActivity.REQUEST_NFC_SIGN_TRANSACTION }
            .collect { scanInfo ->
                val isoDep = IsoDep.get(scanInfo.tag) ?: return@collect
                viewModel.signTapSignerPsbt(
                    isoDep = isoDep,
                    cvc = nfcViewModel.inputCvc.orEmpty()
                )
                nfcViewModel.clearScanInfo()
            }
    }

    // Handle passphrase dialog
    LaunchedEffect(needPassphrase) {
        needPassphrase?.let { id ->
            NCInputDialog(context as Activity).showDialog(
                title = context.getString(com.nunchuk.android.transaction.R.string.nc_transaction_enter_passphrase),
                onConfirmed = { passphrase ->
                    viewModel.handlePassphrase(passphrase)
                }
            )
        }
    }

    val type = loadingType
    if (type != null) {
        when (type) {
            LoadingType.Normal -> NcLoadingDialog()
            LoadingType.Nfc -> NcLoadingDialog(
                title = context.getString(R.string.nc_please_wait),
                customMessage = context.getString(R.string.nc_keep_holding_nfc)
            )
        }
    }

    NunchukTheme {
        TransactionDetailView(
            isDummyTx = false,
            walletId = "",
            txId = args.transaction.txId,
            state = state,
            miniscriptUiState = miniscriptState,
            onShowMore = { /* Handle menu more */ },
            onSignClick = { signerModel ->
                if (signerModel.type == SignerType.NFC) {
                    (activity as NfcActionListener).startNfcFlow(
                        BaseNfcActivity.REQUEST_NFC_SIGN_TRANSACTION
                    )
                } else if (signerModel.type == SignerType.SOFTWARE) {
                    viewModel.checkSoftwarePassphrase(signerModel)
                }
            },
            onBroadcastClick = { /* Handle broadcast click */ },
            onViewOnBlockExplorer = { /* Handle view on block explorer */ },
            onManageCoinClick = { /* Handle manage coin click */ },
            onEditNote = { /* Handle edit note */ },
            onEditChangeCoin = { /* Handle edit change coin */ },
            onCopyText = { /* Handle copy text */ },
            onPreimageSuccess = { /* Handle preimage success */ },
            onSetPendingSignNodeId = { /* Handle set pending sign node id */ }
        )
    }
}
