package com.nunchuk.android.transaction.components.address.addoredit

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.navArgs
import com.journeyapps.barcodescanner.ScanContract
import com.nunchuk.android.compose.HighlightMessageType
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.core.qr.startQRCodeScan
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.SavedAddressFlow
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.model.SavedAddress
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.transaction.R
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AddOrEditAddressFragment : Fragment() {

    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: AddOrEditAddressViewModel by viewModels()
    private val args: AddOrEditAddressFragmentArgs by navArgs()

    private val launcher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let { content ->
            viewModel.parseBtcUri(content)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AddOrEditAddressScreen(viewModel,
                    flow = args.flow,
                    onScanClick = {
                        startQRCodeScan(launcher)
                    },
                    onRemoveClick = {
                        NCWarningDialog(requireActivity()).showDialog(
                            title = getString(R.string.nc_confirmation),
                            message = getString(
                                R.string.nc_delete_saved_address_dialog_desc,
                                it
                            ), onYesClick = {
                                viewModel.deleteAddress()
                            })
                    })
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is AddOrEditAddressEvent.Error -> {
                    showOrHideLoading(false)
                    showError(event.message)
                }

                is AddOrEditAddressEvent.Loading -> {
                    showOrHideLoading(event.loading)
                }

                is AddOrEditAddressEvent.Success -> {
                    requireActivity().setResult(Activity.RESULT_OK, Intent())
                    requireActivity().finish()
                }
            }
        }
    }
}

@Composable
fun AddOrEditAddressScreen(
    viewModel: AddOrEditAddressViewModel = viewModel(),
    flow: Int = SavedAddressFlow.NONE,
    onScanClick: () -> Unit = {},
    onRemoveClick: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AddOrEditAddressScreenContent(
        uiState = state,
        flow = flow,
        onScanClick = onScanClick,
        onInputLabelChange = { viewModel.updateLabel(it) },
        onInputAddressChange = { viewModel.updateAddress(it) },
        onActionClick = { viewModel.addOrUpdateAddress() },
        onRemoveClick = { onRemoveClick(state.label) }
    )
}

@Composable
fun AddOrEditAddressScreenContent(
    uiState: AddOrEditAddressState,
    flow: Int = SavedAddressFlow.NONE,
    onInputLabelChange: (String) -> Unit = {},
    onInputAddressChange: (String) -> Unit = {},
    onScanClick: () -> Unit = {},
    onActionClick: () -> Unit = {},
    onRemoveClick: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .navigationBarsPadding()
                .statusBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = "",
                    textStyle = NunchukTheme.typography.titleLarge,
                    actions = {
                        Spacer(modifier = Modifier.size(LocalViewConfiguration.current.minimumTouchTargetSize))
                    })
            },
            bottomBar = {
                Column {
                    if (flow == SavedAddressFlow.CREATE) {
                        NcHintMessage(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            messages = listOf(
                                ClickAbleText(
                                    content = stringResource(id = R.string.nc_saved_address_warning_desc)
                                )
                            ),
                            type = HighlightMessageType.WARNING,
                        )
                    }

                    val isButtonEnabled = if (flow == SavedAddressFlow.CREATE) {
                        uiState.address.isNotEmpty() && uiState.label.isNotEmpty()
                    } else {
                        uiState.label.isNotEmpty() && uiState.originSavedAddress != null
                                && (uiState.address != uiState.originSavedAddress.address
                                || uiState.label != uiState.originSavedAddress.label)
                    }
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        onClick = onActionClick,
                        enabled = isButtonEnabled,
                    ) {
                        Text(
                            text = if (flow == SavedAddressFlow.CREATE) {
                                stringResource(id = R.string.nc_add)
                            } else {
                                stringResource(id = R.string.nc_save)
                            }
                        )
                    }
                    if (flow == SavedAddressFlow.EDIT) {
                        TextButton(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            onClick = onRemoveClick
                        ) {
                            Text(
                                text = stringResource(id = R.string.nc_remove_saved_addresses),
                                style = NunchukTheme.typography.title
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxHeight()
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = if (flow == SavedAddressFlow.EDIT) stringResource(id = R.string.nc_saved_address) else stringResource(
                        id = R.string.nc_add_a_new_address
                    ),
                    style = NunchukTheme.typography.heading
                )

                if (flow == SavedAddressFlow.CREATE) {
                    Text(
                        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                        text = stringResource(id = R.string.nc_save_an_address_subsequent_uses),
                        style = NunchukTheme.typography.body,
                    )
                }

                NcTextField(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                    title = stringResource(id = R.string.nc_label),
                    value = uiState.label,
                    placeholder = {
                        Text(
                            text = stringResource(id = R.string.nc_enter_a_label),
                            style = NunchukTheme.typography.body.copy(
                                color = colorResource(
                                    id = com.nunchuk.android.core.R.color.nc_boulder_color
                                )
                            )
                        )
                    },
                    onValueChange = {
                        if (it.length <= 40) onInputLabelChange(it)
                    },
                    hint = "$MAX_INPUT_LENGTH/$MAX_INPUT_LENGTH".takeIf { uiState.label.length == MAX_INPUT_LENGTH }
                )

                if (flow == SavedAddressFlow.CREATE) {
                    NcTextField(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        title = stringResource(id = R.string.nc_address),
                        value = uiState.address,
                        error = if (uiState.invalidAddress) stringResource(id = R.string.nc_transaction_invalid_address) else null,
                        placeholder = {
                            Text(
                                text = "Enter an address",
                                style = NunchukTheme.typography.body.copy(
                                    color = colorResource(
                                        id = com.nunchuk.android.core.R.color.nc_boulder_color
                                    )
                                )
                            )
                        },
                        rightContent = {
                            Image(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .clickable {
                                        onScanClick()
                                    },
                                painter = painterResource(id = R.drawable.ic_qrcode_dark),
                                contentDescription = ""
                            )
                        },
                        onValueChange = onInputAddressChange
                    )
                } else {

                    Text(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                        text = "Address",
                        style = NunchukTheme.typography.titleSmall
                    )

                    Row(
                        modifier = Modifier
                            .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                            .background(
                                color = MaterialTheme.colorScheme.greyLight,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                            text = uiState.address, style = NunchukTheme.typography.body.copy(
                                color = colorResource(
                                    id = R.color.nc_grey_dark_color
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}

private const val MAX_INPUT_LENGTH = 40

@Preview
@Composable
private fun AddOrEditAddressScreenContentPreview() {
    AddOrEditAddressScreenContent(uiState = AddOrEditAddressState(
        label = "Label",
        address = "Address",
    ))
}