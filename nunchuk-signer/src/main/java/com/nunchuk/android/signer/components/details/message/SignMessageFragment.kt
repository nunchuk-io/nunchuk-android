package com.nunchuk.android.signer.components.details.message

import android.nfc.tech.IsoDep
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.*
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcActionListener
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.core.util.*
import com.nunchuk.android.model.SignedMessage
import com.nunchuk.android.signer.R
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.widget.NCInputDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalFoundationApi::class)
@AndroidEntryPoint
class SignMessageFragment : Fragment() {
    private val args: SignMessageFragmentArgs by navArgs()
    private val viewModel: SignMessageViewModel by viewModels()
    private val nfcViewModel: NfcViewModel by activityViewModels()
    private val controller: IntentSharingController by lazy {
        IntentSharingController.from(
            requireActivity()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val state by viewModel.state.collectAsStateWithLifecycle()
                SignMessageContent(
                    defaultPath = state.defaultPath,
                    signedMessage = state.signedMessage,
                    onValidatePath = viewModel::validatePath,
                    onSignMessage = { message, path ->
                        onSignMessage(message, path)
                    },
                    onCopyAddress = {
                        showSuccess(getString(R.string.nc_signer_address_copied_to_clipboard))
                    },
                    onCopySignature = {
                        showSuccess(getString(R.string.nc_signer_signature_copied_to_clipboard))
                    },
                    onExportSignature = viewModel::exportSignatureToFile,
                    onResetSignature = viewModel::resetSignature
                )
            }
        }
    }

    private fun onSignMessage(message: String, path: String) {
        viewModel.saveMessage(message, path)
        if (args.signerType == SignerType.NFC) {
            (requireActivity() as NfcActionListener).startNfcFlow(BaseNfcActivity.REQUEST_NFC_HEALTH_CHECK)
        } else if (viewModel.needPassphrase()) {
            NCInputDialog(requireActivity()).showDialog(
                title = getString(R.string.nc_transaction_enter_passphrase),
                onConfirmed = { viewModel.handleHealthCheck(it) }
            )
        } else {
            viewModel.signMessageBySoftware()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                SignMessageEvent.InvalidPath -> showError(getString(R.string.nc_signer_invalid_derivation_path))
                SignMessageEvent.NoSignatureDetected -> showError(getString(R.string.nc_signer_no_signature_detected))
                is SignMessageEvent.ShowError ->
                    if (nfcViewModel.handleNfcError(event.e).not()) {
                        showError(event.e.message.orUnknownError())
                    }

                SignMessageEvent.SignSuccess -> showSuccess(getString(R.string.nc_signer_the_message_has_been_signed))
                is SignMessageEvent.Loading -> showOrHideLoading(event.isLoading)
                is SignMessageEvent.NfcLoading -> showOrHideNfcLoading(event.isLoading)
                is SignMessageEvent.ShareFile -> controller.shareFile(event.path)
            }
        }

        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_NFC_HEALTH_CHECK }) {
            val isoDep = IsoDep.get(it.tag) ?: return@flowObserver
            viewModel.signMessageByTapSigner(isoDep, nfcViewModel.inputCvc.orEmpty())
            nfcViewModel.clearScanInfo()
        }
    }
}

@ExperimentalFoundationApi
@Composable
private fun SignMessageContent(
    defaultPath: String = "",
    signedMessage: SignedMessage? = null,
    onValidatePath: (path: String) -> Unit = {},
    onSignMessage: (message: String, path: String) -> Unit = { _, _ -> },
    onCopyAddress: () -> Unit = {},
    onCopySignature: () -> Unit = {},
    onExportSignature: () -> Unit = {},
    onResetSignature: () -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    var message by remember {
        mutableStateOf("")
    }
    var path by remember(defaultPath) {
        mutableStateOf(defaultPath)
    }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        delay(300L)
        focusRequester.requestFocus()
    }

    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(id = R.string.nc_signer_sign_message),
                    textStyle = NunchukTheme.typography.titleLarge,
                    isBack = false,
                    elevation = 0.dp
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                    enabled = message.isNotEmpty() && path.isNotEmpty(),
                    onClick = { onSignMessage(message, path) }) {
                    Text(text = stringResource(id = R.string.nc_transaction_sign))
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                NcTextField(
                    modifier = Modifier.focusRequester(focusRequester),
                    title = stringResource(R.string.nc_signer_message_to_sign),
                    value = message,
                    minLines = 3,
                    onValueChange = { value ->
                        message = value
                        onResetSignature()
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )

                NcTextField(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .onFocusChanged {
                            if (it.isFocused.not()) {
                                onValidatePath(path)
                            }
                        },
                    title = stringResource(R.string.nc_signer_derivation_path),
                    value = path,
                    onValueChange = { value ->
                        path = value
                        onResetSignature()
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )

                if (signedMessage != null) {
                    Spacer(
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                            .height(1.dp)
                            .fillMaxWidth()
                            .background(color = MaterialTheme.colors.whisper)
                    )

                    Text(
                        text = stringResource(R.string.nc_signer_address),
                        style = NunchukTheme.typography.titleSmall
                    )

                    Text(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .background(
                                color = MaterialTheme.colors.greyLight,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colors.border,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .combinedClickable(
                                onClick = {

                                },
                                onLongClick = {
                                    context.copyToClipboard(
                                        label = "Nunchuk",
                                        text = signedMessage.address
                                    )
                                    onCopyAddress()
                                })
                            .fillMaxWidth()
                            .padding(12.dp),
                        text = signedMessage.address,
                        style = NunchukTheme.typography.body,
                    )

                    Text(
                        modifier = Modifier.padding(top = 16.dp),
                        text = stringResource(R.string.nc_signer_signature),
                        style = NunchukTheme.typography.titleSmall
                    )

                    Text(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .background(
                                color = MaterialTheme.colors.greyLight,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colors.border,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {

                                },
                                onLongClick = {
                                    context.copyToClipboard(
                                        label = "Nunchuk",
                                        text = signedMessage.signature
                                    )
                                    onCopySignature()
                                })
                            .padding(12.dp),
                        text = signedMessage.signature,
                        style = NunchukTheme.typography.body,
                    )

                    Row(modifier = Modifier.padding(top = 16.dp)) {
                        NcOutlineButton(modifier = Modifier.weight(1f), onClick = {
                            context.copyToClipboard(
                                label = "Nunchuk",
                                text = signedMessage.signature
                            )
                            onCopySignature()
                        }) {
                            Icon(
                                modifier = Modifier.size(18.dp),
                                painter = painterResource(id = R.drawable.ic_copy),
                                contentDescription = ""
                            )
                            Text(
                                modifier = Modifier.padding(start = 6.dp),
                                text = stringResource(R.string.nc_signer_copy_signature),
                                style = NunchukTheme.typography.titleSmall
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        NcOutlineButton(
                            modifier = Modifier.weight(1f),
                            onClick = onExportSignature
                        ) {
                            Icon(
                                modifier = Modifier.size(18.dp),
                                painter = painterResource(id = R.drawable.ic_export),
                                contentDescription = ""
                            )
                            Text(
                                modifier = Modifier.padding(start = 6.dp),
                                text = stringResource(R.string.nc_signer_export_signature),
                                style = NunchukTheme.typography.titleSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
fun SignMessageContentPreview() {
    SignMessageContent(
        signedMessage = SignedMessage(
            address = "kfdjsfkjdsklfjsdkfjdsklfjksdlfjkdslf",
            signature = "kdfsjkfjdskfjkdsfhkjdsfhjksdhfjkdshfjkdshfjkdshfjks"
        )
    )
}