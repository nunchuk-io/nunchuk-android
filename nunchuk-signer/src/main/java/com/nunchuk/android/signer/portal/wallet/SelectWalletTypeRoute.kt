package com.nunchuk.android.signer.portal.wallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioOption
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcSwitch
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.signer.R
import com.nunchuk.android.type.AddressType

const val selectWalletTypeRoute = "select_wallet_type"

fun NavGraphBuilder.selectWalletType(
    onSelectWalletType: (Boolean, AddressType) -> Unit = { _, _ -> },
) {
    composable(selectWalletTypeRoute) {
        SelectWalletTypeScreen(
            onSelectWalletType = onSelectWalletType,
        )
    }
}

fun NavController.navigateToSelectWalletType(navOptions: NavOptions? = null) {
    navigate(selectWalletTypeRoute, navOptions)
}

@Composable
fun SelectWalletTypeScreen(
    modifier: Modifier = Modifier,
    onSelectWalletType: (Boolean, AddressType) -> Unit = { _, _ -> },
) {
    var isSingleSig by rememberSaveable { mutableStateOf(true) }
    var isCustomize by rememberSaveable { mutableStateOf(false) }
    var addressType by remember { mutableStateOf(AddressType.NATIVE_SEGWIT) }
    NcScaffold(
        modifier = modifier.navigationBarsPadding(),
        topBar = {
            NcImageAppBar(backgroundRes = R.drawable.nc_bg_multisig)
        },
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = { onSelectWalletType(isSingleSig, addressType) }) {
                Text(text = stringResource(id = R.string.nc_text_continue))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.nc_select_wallet_address_type),
                style = NunchukTheme.typography.heading,
            )

            Text(
                text = stringResource(R.string.nc_select_the_type_of_wallet_you_want_to_use_portal_in),
                style = NunchukTheme.typography.body,
            )

            Text(
                modifier = Modifier.padding(top = 24.dp),
                text = stringResource(R.string.nc_select_wallet_type),
                style = NunchukTheme.typography.titleLarge,
            )

            NcRadioOption(
                modifier = Modifier.fillMaxWidth(),
                isSelected = isSingleSig,
                onClick = { isSingleSig = true }
            ) {
                Text(
                    text = stringResource(R.string.nc_single_sig),
                    style = NunchukTheme.typography.title,
                )
            }

            NcRadioOption(
                modifier = Modifier.fillMaxWidth(),
                isSelected = !isSingleSig,
                onClick = { isSingleSig = false }
            ) {
                Text(
                    text = stringResource(R.string.nc_multisig),
                    style = NunchukTheme.typography.title,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.nc_wallet_customize_address_type),
                        style = NunchukTheme.typography.titleLarge,
                    )

                    if (!isCustomize) {
                        Text(
                            text = stringResource(R.string.nc_default_native_segwit),
                            style = NunchukTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.textSecondary
                        )
                    }
                }

                NcSwitch(
                    checked = isCustomize,
                    onCheckedChange = { enabled ->
                        isCustomize = enabled
                        if (!enabled) {
                            addressType = AddressType.NATIVE_SEGWIT
                        }
                    },
                )
            }
            
            if (isCustomize) {
                NcRadioOption(
                    modifier = Modifier.fillMaxWidth(),
                    isSelected = addressType == AddressType.NATIVE_SEGWIT,
                    onClick = { addressType = AddressType.NATIVE_SEGWIT }
                ) {
                    Text(
                        text = stringResource(R.string.nc_native_segwit),
                        style = NunchukTheme.typography.title,
                    )

                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = stringResource(R.string.nc_high_fee_saving),
                        style = NunchukTheme.typography.body,
                    )
                }
                NcRadioOption(
                    modifier = Modifier.fillMaxWidth(),
                    isSelected = addressType == AddressType.NESTED_SEGWIT,
                    onClick = { addressType = AddressType.NESTED_SEGWIT }
                ) {
                    Text(
                        text = stringResource(R.string.nc_nested_segwit),
                        style = NunchukTheme.typography.title,
                    )

                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = stringResource(R.string.nc_medium_fee_saving),
                        style = NunchukTheme.typography.body,
                    )
                }
                NcRadioOption(
                    modifier = Modifier.fillMaxWidth(),
                    isSelected = addressType == AddressType.LEGACY,
                    onClick = { addressType = AddressType.LEGACY }
                ) {
                    Text(
                        text = stringResource(R.string.nc_legacy),
                        style = NunchukTheme.typography.title,
                    )

                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = stringResource(R.string.nc_no_fee_saving),
                        style = NunchukTheme.typography.body,
                    )
                }
                NcRadioOption(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isSingleSig,
                    isSelected = addressType == AddressType.TAPROOT,
                    onClick = { addressType = AddressType.TAPROOT }
                ) {
                    Text(
                        text = stringResource(R.string.nc_taproot),
                        style = NunchukTheme.typography.title,
                    )

                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = stringResource(R.string.nc_bech32m_address),
                        style = NunchukTheme.typography.body,
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun SelectWalletTypeScreenPreview() {
    NunchukTheme {
        SelectWalletTypeScreen()
    }
}