package com.nunchuk.android.wallet.personal.components.add

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
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
import com.nunchuk.android.compose.border
import com.nunchuk.android.compose.controlTextPrimary
import com.nunchuk.android.compose.fillDenim2
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.compose.whisper
import com.nunchuk.android.model.FreeGroupConfig
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.wallet.personal.R
import kotlinx.coroutines.delay

@Composable
fun AddWalletView(
    state: AddWalletState,
    isEditGroupWallet: Boolean,
    onSelectAddressType: (AddressType) -> Unit = {},
    onContinue: (String, AddressType, Int, Int) -> Unit = { _, _, _, _ -> }
) {
    var walletName by rememberSaveable { mutableStateOf("") }
    var viewAll by rememberSaveable { mutableStateOf(false) }
    var walletConfigType by rememberSaveable {
        mutableStateOf(
            getWalletConfigTypeBy(
                n = state.groupSandbox?.n ?: 2,
                m = state.groupSandbox?.m ?: 3
            )
        )
    }
    var keys by remember { mutableIntStateOf(0) }
    var requiredKeys by remember { mutableIntStateOf(0) }

    val options = if (!viewAll) listOf(
        AddressType.NATIVE_SEGWIT,
        AddressType.TAPROOT
    ) else listOf(
        AddressType.NATIVE_SEGWIT,
        AddressType.TAPROOT,
        AddressType.NESTED_SEGWIT,
        AddressType.LEGACY
    )

    val walletConfigOptions = remember { WalletConfigType.entries }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100L)
        focusRequester.requestFocus()
    }
    NunchukTheme {
        NcScaffold(
            modifier = Modifier
                .systemBarsPadding()
                .imePadding()
                .fillMaxSize(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(id = R.string.nc_config),
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
                        onContinue(walletName, state.addressTypeSelected, keys, requiredKeys)
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
                    onValueChange = { walletName = it.take(80) },
                    enableMaxLength = true,
                    maxLength = 80,
                    modifier = Modifier.focusRequester(focusRequester)
                )

                Text(
                    modifier = Modifier.padding(bottom = 8.dp, top = 16.dp),
                    text = stringResource(id = R.string.nc_wallet_address_type),
                    style = NunchukTheme.typography.titleSmall
                )

                options.forEach { type ->
                    TypeOption(
                        selected = state.addressTypeSelected == type,
                        name = getAddressType(type),
                        badge = getBadge(type),
                        onClick = {
                            onSelectAddressType(type)
                        }
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

                if (isEditGroupWallet && state.groupSandbox != null) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 24.dp),
                        color = MaterialTheme.colorScheme.backgroundMidGray
                    )

                    Text(
                        text = "Wallet config",
                        modifier = Modifier.padding(bottom = 6.dp),
                        style = NunchukTheme.typography.titleSmall
                    )

                    walletConfigOptions.forEach { option ->
                        TypeOption(
                            selected = walletConfigType == option,
                            name = option.toOptionName(),
                            badge = null,
                            isEndItem = option == WalletConfigType.CUSTOM,
                            onClick = {
                                walletConfigType = option
                            }
                        )
                    }
                }
                if (walletConfigType == WalletConfigType.CUSTOM) {
                    KeysAndRequiredKeysScreen(
                        state.freeGroupWalletConfig,
                        m = state.groupSandbox!!.m,
                        n = state.groupSandbox.n
                    ) { m, n ->
                        keys = m
                        requiredKeys = n
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
private fun TypeOption(
    selected: Boolean = true,
    name: String,
    badge: String?,
    isEndItem: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(
                color = if (selected) MaterialTheme.colorScheme.fillDenim2 else MaterialTheme.colorScheme.surface,
                shape = if (isEndItem.not()) RoundedCornerShape(8.dp) else RoundedCornerShape(
                    topEnd = 8.dp,
                    topStart = 8.dp
                )
            )
            .padding(horizontal = 12.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = name,
            style = NunchukTheme.typography.body
        )

        badge?.let {
            Text(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.whisper,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clip(shape = RoundedCornerShape(20.dp))
                    .background(color = MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp),
                text = badge,
                style = NunchukTheme.typography.bold.copy(fontSize = 10.sp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        NcRadioButton(selected = selected, onClick = onClick)
    }
}

@Composable
fun KeyManagementSection(
    title: String,
    description: String,
    value: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = NunchukTheme.typography.body,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = NunchukTheme.typography.caption.copy(
                    color = MaterialTheme.colorScheme.textSecondary
                )
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(1.dp, MaterialTheme.colorScheme.textPrimary, CircleShape)
                    .clickable {
                        onDecrement()
                    },
                contentAlignment = Alignment.Center
            ) {
                NcIcon(
                    painter = painterResource(id = R.drawable.ic_minus),
                    contentDescription = "Decrement",
                )
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .size(48.dp)
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value.toString(),
                    style = NunchukTheme.typography.body,
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(1.dp, MaterialTheme.colorScheme.textPrimary, CircleShape)
                    .clickable { onIncrement() },
                contentAlignment = Alignment.Center
            ) {
                NcIcon(
                    painter = painterResource(id = R.drawable.ic_plus),
                    contentDescription = "Increment",
                )
            }
        }
    }
}

@Composable
fun KeysAndRequiredKeysScreen(
    freeGroupWalletConfig: FreeGroupConfig,
    m: Int,
    n: Int,
    onNumberChange: (Int, Int) -> Unit = { _, _ -> }
) {
    Column(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.border,
                shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
            )
            .padding(16.dp)
    ) {
        var keys by remember { mutableIntStateOf(m) }
        var requiredKeys by remember { mutableIntStateOf(n) }

        LaunchedEffect(m, n) {
            onNumberChange(keys, requiredKeys)
        }

        KeyManagementSection(
            title = "Keys",
            description = "Number of keys assigned to the wallet (up to ${freeGroupWalletConfig.maxKey}).",
            value = keys,
            onIncrement = { if (keys < freeGroupWalletConfig.maxKey) keys++ },
            onDecrement = { if (keys > 1) keys-- }
        )

        Spacer(modifier = Modifier.height(16.dp))

        KeyManagementSection(
            title = "Required keys",
            description = "Number of signatures required to unlock funds.",
            value = requiredKeys,
            onIncrement = { if (requiredKeys < keys) requiredKeys++ },
            onDecrement = { if (requiredKeys > 1) requiredKeys-- }
        )
    }
}

@PreviewLightDark
@Composable
private fun AddWalletViewPreview() {
    AddWalletView(
        state = AddWalletState(),
        isEditGroupWallet = true
    )
}