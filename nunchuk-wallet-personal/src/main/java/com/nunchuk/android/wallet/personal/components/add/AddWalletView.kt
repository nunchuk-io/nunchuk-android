package com.nunchuk.android.wallet.personal.components.add

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.backgroundMidGray
import com.nunchuk.android.compose.controlTextPrimary
import com.nunchuk.android.compose.whisper
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.wallet.personal.R

@Composable
fun AddWalletView(
    onContinue: (String, AddressType) -> Unit
) {
    var walletName by rememberSaveable { mutableStateOf("") }
    var viewAll by rememberSaveable { mutableStateOf(false) }
    var addressType by rememberSaveable { mutableStateOf(AddressType.NATIVE_SEGWIT) }
    val options = if (!viewAll) listOf(
        AddressType.NATIVE_SEGWIT,
        AddressType.TAPROOT
    ) else listOf(
        AddressType.NATIVE_SEGWIT,
        AddressType.TAPROOT,
        AddressType.NESTED_SEGWIT,
        AddressType.LEGACY
    )
    NunchukTheme {
        NcScaffold(
            modifier = Modifier
                .systemBarsPadding()
                .imePadding()
                .fillMaxSize(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(id = R.string.nc_text_add_wallet),
                    textStyle = NunchukTheme.typography.titleLarge
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = walletName.isNotBlank(),
                    onClick = {
                        onContinue(walletName, addressType)
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.nc_text_continue),
                        style = NunchukTheme.typography.title.copy(
                            color = MaterialTheme.colorScheme.controlTextPrimary
                        )
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .animateContentSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                NcTextField(
                    title = stringResource(id = R.string.nc_text_wallet_name),
                    value = walletName,
                    onValueChange = { walletName = it },
                    enableMaxLength = true,
                    maxLength = 20
                )

                HorizontalDivider(
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.backgroundMidGray
                )

                Text(
                    modifier = Modifier.padding(bottom = 8.dp),
                    text = stringResource(id = R.string.nc_wallet_address_type),
                    style = NunchukTheme.typography.titleSmall
                )

                options.forEach { type ->
                    AddressTypeOption(
                        selected = addressType == type,
                        name = getAddressType(type),
                        badge = getBadge(type),
                        onClick = { addressType = type }
                    )
                }

                if (!viewAll) {
                    Row(
                        modifier = Modifier
                            .clickable(onClick = { viewAll = !viewAll })
                            .align(Alignment.CenterHorizontally),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier,
                            text = stringResource(R.string.nc_view_all),
                            style = NunchukTheme.typography.titleSmall.copy(textDecoration = TextDecoration.Underline)
                        )

                        NcIcon(
                            modifier = Modifier.size(16.dp),
                            painter = painterResource(id = R.drawable.ic_dropdown_outline),
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun getAddressType(type: AddressType): String {
    return when (type) {
        AddressType.LEGACY -> stringResource(id = R.string.nc_wallet_legacy_wallet)
        AddressType.NATIVE_SEGWIT -> stringResource(id = R.string.nc_wallet_native_segwit_wallet)
        AddressType.NESTED_SEGWIT -> stringResource(id = R.string.nc_wallet_nested_segwit_wallet)
        AddressType.TAPROOT -> stringResource(id = R.string.nc_wallet_taproot_wallet)
        else -> ""
    }
}

@Composable
fun getBadge(type: AddressType): String? {
    return when (type) {
        AddressType.NATIVE_SEGWIT -> stringResource(id = R.string.nc_wallet_default)
        AddressType.TAPROOT -> stringResource(id = R.string.nc_wallet_new)
        else -> null
    }
}

@Composable
private fun AddressTypeOption(
    modifier: Modifier = Modifier,
    selected: Boolean,
    name: String,
    badge: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = name,
            style = NunchukTheme.typography.titleSmall
        )

        badge?.let {
            Text(
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.whisper)
                    .padding(horizontal = 8.dp),
                text = badge,
                style = NunchukTheme.typography.bodySmall.copy(fontSize = 10.sp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        NcRadioButton(selected = selected, onClick = onClick)
    }
}

@PreviewLightDark
@Composable
private fun AddWalletViewPreview() {
    AddWalletView { _, _ -> }
}