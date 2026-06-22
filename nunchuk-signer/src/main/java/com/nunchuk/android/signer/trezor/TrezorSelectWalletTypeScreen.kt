package com.nunchuk.android.signer.trezor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioButton
import com.nunchuk.android.compose.NcRadioOption
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcSpannedClickableText
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.SpanIndicator
import com.nunchuk.android.compose.dialog.NcConfirmationDialog
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.signer.R
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType

const val trezorSelectWalletTypeRoute = "trezor_select_wallet_type_route"

private data class AddressTypeOption(
    val type: AddressType,
    val titleRes: Int,
    val subtitleRes: Int
)

private val addressTypeOptions = listOf(
    AddressTypeOption(
        type = AddressType.NATIVE_SEGWIT,
        titleRes = R.string.nc_native_segwit,
        subtitleRes = R.string.nc_high_fee_saving
    ),
    AddressTypeOption(
        type = AddressType.NESTED_SEGWIT,
        titleRes = R.string.nc_nested_segwit,
        subtitleRes = R.string.nc_medium_fee_saving
    ),
    AddressTypeOption(
        type = AddressType.LEGACY,
        titleRes = R.string.nc_legacy,
        subtitleRes = R.string.nc_no_fee_saving
    ),
    AddressTypeOption(
        type = AddressType.TAPROOT,
        titleRes = R.string.nc_taproot,
        subtitleRes = R.string.nc_bech32m_address
    )
)

fun NavGraphBuilder.trezorSelectWalletType(
    onBack: () -> Unit = {},
    taprootSupportState: TrezorTaprootSupportState = TrezorTaprootSupportState(),
    onContinue: (Boolean, AddressType, Int) -> Unit = { _, _, _ -> }
) {
    composable(trezorSelectWalletTypeRoute) {
        TrezorSelectWalletTypeScreen(
            onBack = onBack,
            taprootSupportState = taprootSupportState,
            onContinue = onContinue
        )
    }
}

fun NavHostController.navigateToTrezorSelectWalletType() {
    navigate(trezorSelectWalletTypeRoute)
}

@Composable
private fun TrezorSelectWalletTypeScreen(
    onBack: () -> Unit = {},
    taprootSupportState: TrezorTaprootSupportState = TrezorTaprootSupportState(),
    onContinue: (Boolean, AddressType, Int) -> Unit = { _, _, _ -> }
) {
    var isSingleSig by rememberSaveable { mutableStateOf(true) }
    var addressTypeName by rememberSaveable { mutableStateOf(AddressType.NATIVE_SEGWIT.name) }
    var accountIndex by rememberSaveable { mutableIntStateOf(0) }
    var showAddressTypeSheet by rememberSaveable { mutableStateOf(false) }
    var showAccountIndexSheet by rememberSaveable { mutableStateOf(false) }
    var showOpenSuiteConfirmation by rememberSaveable { mutableStateOf(false) }

    val addressType = addressTypeName.toAddressTypeOrDefault()
    val selectedWalletType = if (isSingleSig) WalletType.SINGLE_SIG else WalletType.MULTI_SIG
    val isTaprootSupported = taprootSupportState.isTaprootSupported(selectedWalletType)
    val addressTypeValue = stringResource(
        id = R.string.nc_default_value,
        walletAddressTypeText(addressType)
    )
    val accountIndexValue = stringResource(
        id = R.string.nc_default_number,
        accountIndex
    )

    NcScaffold(
        topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.nc_bg_multisig,
                onClosedClicked = onBack
            )
        },
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding()
                    .fillMaxWidth(),
                onClick = { showOpenSuiteConfirmation = true }
            ) {
                Text(text = stringResource(id = com.nunchuk.android.core.R.string.nc_text_continue))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.nc_select_wallet_address_type),
                style = NunchukTheme.typography.heading
            )

            Text(
                text = stringResource(id = R.string.nc_select_wallet_trezor_desc),
                style = NunchukTheme.typography.body
            )

            Text(
                modifier = Modifier.padding(top = 16.dp),
                text = stringResource(id = com.nunchuk.android.core.R.string.nc_select_wallet_type),
                style = NunchukTheme.typography.titleLarge
            )

            NcRadioOption(
                modifier = Modifier.fillMaxWidth(),
                isSelected = isSingleSig,
                onClick = { isSingleSig = true }
            ) {
                Text(
                    text = stringResource(id = R.string.nc_single_sig),
                    style = NunchukTheme.typography.title
                )
            }

            NcRadioOption(
                modifier = Modifier.fillMaxWidth(),
                isSelected = !isSingleSig,
                onClick = { isSingleSig = false }
            ) {
                Text(
                    text = stringResource(id = com.nunchuk.android.core.R.string.nc_multisig),
                    style = NunchukTheme.typography.title
                )
            }

            TrezorEditableField(
                title = stringResource(id = R.string.nc_address_type),
                value = addressTypeValue,
                onEdit = { showAddressTypeSheet = true }
            )

            TrezorEditableField(
                title = stringResource(id = com.nunchuk.android.core.R.string.nc_account_index),
                value = accountIndexValue,
                onEdit = { showAccountIndexSheet = true }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showAddressTypeSheet) {
        TrezorAddressTypeBottomSheet(
            isTaprootSupported = isTaprootSupported,
            selectedAddressType = addressType,
            onDismiss = { showAddressTypeSheet = false },
            onSave = { selectedType ->
                addressTypeName = selectedType.name
                showAddressTypeSheet = false
            }
        )
    }

    if (showAccountIndexSheet) {
        TrezorAccountIndexBottomSheet(
            initialAccountIndex = accountIndex,
            onDismiss = { showAccountIndexSheet = false },
            onSave = { selectedIndex ->
                accountIndex = selectedIndex
                showAccountIndexSheet = false
            }
        )
    }

    if (showOpenSuiteConfirmation) {
        NcConfirmationDialog(
            title = stringResource(id = com.nunchuk.android.core.R.string.nc_confirmation),
            message = stringResource(id = R.string.nc_open_trezor_suite_continue_message),
            positiveButtonText = stringResource(id = R.string.nc_open_trezor_suite),
            negativeButtonText = stringResource(id = com.nunchuk.android.core.R.string.nc_cancel),
            isPositiveButtonWrapContent = true,
            onDismiss = { showOpenSuiteConfirmation = false },
            onPositiveClick = {
                showOpenSuiteConfirmation = false
                onContinue(isSingleSig, addressType, accountIndex)
            }
        )
    }

    LaunchedEffect(isTaprootSupported, addressType) {
        if (!isTaprootSupported && addressType == AddressType.TAPROOT) {
            addressTypeName = AddressType.NATIVE_SEGWIT.name
        }
    }
}

@Composable
private fun TrezorEditableField(
    title: String,
    value: String,
    onEdit: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = NunchukTheme.typography.titleLarge
            )

            Text(
                text = stringResource(id = com.nunchuk.android.core.R.string.nc_edit),
                style = NunchukTheme.typography.textLink,
                modifier = Modifier.clickable(onClick = onEdit)
            )
        }

        Text(
            text = value,
            style = NunchukTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.textSecondary)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrezorAddressTypeBottomSheet(
    isTaprootSupported: Boolean,
    selectedAddressType: AddressType,
    onDismiss: () -> Unit = {},
    onSave: (AddressType) -> Unit = {}
) {
    var selectedAddressTypeName by rememberSaveable { mutableStateOf(selectedAddressType.name) }

    LaunchedEffect(isTaprootSupported) {
        if (!isTaprootSupported && selectedAddressTypeName == AddressType.TAPROOT.name) {
            selectedAddressTypeName = AddressType.NATIVE_SEGWIT.name
        }
    }

    ModalBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        onDismissRequest = onDismiss,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 16.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = stringResource(id = com.nunchuk.android.core.R.string.nc_wallet_customize_address_type),
                style = NunchukTheme.typography.title
            )

            Spacer(modifier = Modifier.height(12.dp))

            addressTypeOptions.forEach { option ->
                val isTaprootDisabled = !isTaprootSupported && option.type == AddressType.TAPROOT
                AddressTypeOptionItem(
                    title = stringResource(id = option.titleRes),
                    subtitle = stringResource(id = option.subtitleRes),
                    isSelected = selectedAddressTypeName == option.type.name,
                    enabled = !isTaprootDisabled,
                    onClick = { selectedAddressTypeName = option.type.name }
                )
            }

            NcPrimaryDarkButton(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth(),
                onClick = { onSave(selectedAddressTypeName.toAddressTypeOrDefault()) }
            ) {
                Text(text = stringResource(id = com.nunchuk.android.core.R.string.nc_text_save))
            }
        }
    }
}

@Composable
private fun AddressTypeOptionItem(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.textPrimary
            } else {
                MaterialTheme.colorScheme.strokePrimary
            }
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background,
            disabledContainerColor = MaterialTheme.colorScheme.background
        ),
        onClick = onClick,
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.4f)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = NunchukTheme.typography.title
                )
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = subtitle,
                    style = NunchukTheme.typography.body
                )
            }

            NcRadioButton(
                selected = isSelected,
                onClick = if (enabled) onClick else null,
                enabled = enabled
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrezorAccountIndexBottomSheet(
    initialAccountIndex: Int,
    onDismiss: () -> Unit = {},
    onSave: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    var accountIndexInput by rememberSaveable { mutableStateOf(initialAccountIndex.toString()) }

    ModalBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        onDismissRequest = onDismiss,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    NcIcon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = "Close",
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = stringResource(id = com.nunchuk.android.core.R.string.nc_text_save),
                    style = NunchukTheme.typography.textLink,
                    modifier = Modifier.clickable {
                        onSave(accountIndexInput.toIntOrNull()?.coerceAtLeast(0) ?: 0)
                    }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(id = R.string.nc_enter_account_index),
                    style = NunchukTheme.typography.titleLarge
                )

                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = stringResource(id = R.string.nc_portal_default_index),
                    style = NunchukTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.textSecondary)
                )
            }

            BasicTextField(
                value = accountIndexInput,
                onValueChange = { input ->
                    if (input.isEmpty() || input.all { it.isDigit() }) {
                        accountIndexInput = input
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                textStyle = NunchukTheme.typography.body.copy(color = MaterialTheme.colorScheme.textPrimary),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.textPrimary),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Number
                ),
                singleLine = true
            )

            HorizontalDivider(
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.strokePrimary
            )

            Spacer(modifier = Modifier.height(24.dp))

            NcHintMessage(modifier = Modifier.fillMaxWidth()) {
                NcSpannedClickableText(
                    text = stringResource(R.string.nc_select_index_hint),
                    baseStyle = NunchukTheme.typography.titleSmall,
                    styles = mapOf(
                        SpanIndicator('A') to SpanStyle(
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        )
                    ),
                    onClick = {
                        context.openExternalLink("https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki")
                    }
                )
            }
        }
    }
}

@Composable
private fun walletAddressTypeText(addressType: AddressType): String {
    return when (addressType) {
        AddressType.NATIVE_SEGWIT -> stringResource(id = R.string.nc_native_segwit)
        AddressType.NESTED_SEGWIT -> stringResource(id = R.string.nc_nested_segwit)
        AddressType.LEGACY -> stringResource(id = R.string.nc_legacy)
        AddressType.TAPROOT -> stringResource(id = R.string.nc_taproot)
        else -> stringResource(id = R.string.nc_native_segwit)
    }
}

private fun String.toAddressTypeOrDefault(): AddressType {
    return AddressType.entries.find { it.name == this } ?: AddressType.NATIVE_SEGWIT
}

@PreviewLightDark
@Composable
private fun TrezorSelectWalletTypeScreenPreview() {
    NunchukTheme {
        TrezorSelectWalletTypeScreen()
    }
}
