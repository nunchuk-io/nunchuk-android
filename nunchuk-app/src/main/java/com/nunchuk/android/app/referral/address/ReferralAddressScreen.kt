package com.nunchuk.android.app.referral.address

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.distinctUntilChanged
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.journeyapps.barcodescanner.ScanContract
import com.nunchuk.android.app.referral.ConfirmationCodeResultData
import com.nunchuk.android.app.referral.ReferralAction
import com.nunchuk.android.app.referral.confirmationcode.REFERRAL_CONFIRMATION_CODE_RESULT
import com.nunchuk.android.app.referral.simplifyAddress
import com.nunchuk.android.compose.HighlightMessageType
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.core.qr.startQRCodeScan
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.signer.R

const val referralAddressRoute = "referral_address_route/{address}/{walletId}"
internal const val DEFAULT_ADDRESS = "DEFAULT_ADDRESS"
internal const val DEFAULT_WALLET_ID = "DEFAULT_WALLET_ID"

const val REFERRAL_ADDRESS_RESULT = "referral_address_result"

fun NavGraphBuilder.referralAddress(
    navController: NavController,
    onSaveChange: (String) -> Unit
) {
    composable(referralAddressRoute, arguments = listOf(
        navArgument("address") {
            type = NavType.StringType
        }
    )) {

        val viewModel = hiltViewModel<ReferralAddressViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

        val launcher = rememberLauncherForActivityResult(
            contract = ScanContract()
        ) { result ->
            result.contents?.let { content ->
                viewModel.parseBtcUri(content)
            }
        }

        LaunchedEffect(state.checkAddressSuccess) {
            if (state.checkAddressSuccess) {
                viewModel.consumeCheckAddressSuccess()
                onSaveChange(state.enteredAddress)
            }
        }

        ReferralAddressScreen(
            modifier = Modifier,
            navController = navController,
            state = state,
            isEnableButton = viewModel.isEnableButton(),
            onScan = {
                startQRCodeScan(launcher)
            },
            onEnterAddress = {
                viewModel.updateEnteredAddress(it)
            },
            onSaveChange = { isOtherAddress, address ->
                if (isOtherAddress) {
                    viewModel.checkAddressValid(address)
                } else {
                    onSaveChange(address)
                }
            },
            onSelectedWallet = {
                viewModel.updateSelectedWalletAddress(it)
            },
            onUpdateReceiveAddress = { data ->
                var result = data
                if (data.action == ReferralAction.CHANGE.value) {
                    val walletId = viewModel.getWalletIdByAddress(data.address.orEmpty())
                    result = data.copy(walletId = walletId)
                }
                navController.previousBackStackEntry?.savedStateHandle?.set(
                    REFERRAL_ADDRESS_RESULT,
                    result
                )
                navController.popBackStack()
            },
            onShowOtherAddress = {
                viewModel.updateShowOtherAddress(it)
            }
        )
    }
}

fun NavController.navigateToReferralAddress(
    navOptions: NavOptions? = null,
    address: String,
    walletId: String
) {
    navigate("referral_address_route/$address/$walletId", navOptions)
}

@Composable
fun ReferralAddressScreen(
    modifier: Modifier = Modifier,
    navController: NavController = NavController(LocalContext.current),
    state: ReferralAddressUiState,
    isEnableButton: Boolean = false,
    onSaveChange: (Boolean, String) -> Unit = { _, _ -> },
    onScan: () -> Unit = { },
    onEnterAddress: (String) -> Unit = { },
    onUpdateReceiveAddress: (ConfirmationCodeResultData) -> Unit = { },
    onShowOtherAddress: (Boolean) -> Unit = { },
    onSelectedWallet: (WalletAddressUi?) -> Unit = { },
) {
    val context = LocalLifecycleOwner.current
    var handledResult by remember { mutableStateOf(false) }

    LaunchedEffect(navController) {
        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<ConfirmationCodeResultData>(
            REFERRAL_CONFIRMATION_CODE_RESULT
        )?.distinctUntilChanged()
            ?.observe(context) { result ->
                if (handledResult.not() && result.action == ReferralAction.CHANGE.value) {
                    handledResult = true
                    onUpdateReceiveAddress(result)
                    Log.e("referral", "ReferralAddressScreen --- result-address: $result")
                }
            }
    }

    NcScaffold(
        modifier = modifier.systemBarsPadding(),
        topBar = {
            NcTopAppBar(
                title = "",
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                NcHintMessage(
                    messages = listOf(
                        ClickAbleText(
                            content = "We'll send you an email to confirm (or change) the reward address each time there's a successful referral."
                        )
                    ),
                    textStyle = NunchukTheme.typography.caption,
                    type = HighlightMessageType.HINT,
                )
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth(),
                    enabled = isEnableButton,
                    onClick = {
                        if (state.showOtherAddress) {
                            onSaveChange(true, state.enteredAddress)
                        } else {
                            onSaveChange(false, state.selectedWalletAddress?.address.orEmpty())
                        }
                    },
                ) {
                    Text(text = "Save change")
                }
            }

        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            LazyColumn {
                item {
                    Text(
                        text = "Select wallet or enter address to receive reward",
                        style = NunchukTheme.typography.heading,
                    )

                    Text(
                        text = "Select your wallet", style = NunchukTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
                    )
                }
                items(state.addressWalletUis) { addressWalletUi ->
                    OptionItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        isSelected = state.selectedWalletAddress?.address == addressWalletUi.address,
                        title = addressWalletUi.walletName,
                        desc = addressWalletUi.address,
                        disabled = state.showOtherAddress,
                        onClick = {
                            if (state.showOtherAddress.not()) {
                                onSelectedWallet(addressWalletUi)
                            }
                        }
                    )
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Other address",
                            style = NunchukTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f)
                        )

                        Switch(
                            checked = state.showOtherAddress,
                            onCheckedChange = {
                                onShowOtherAddress(it)
                                if (it) {
                                    onSelectedWallet(null)
                                }
                            },
                            colors = SwitchDefaults.colors()
                        )
                    }

                    if (state.showOtherAddress.not()) {
                        Row(
                            modifier = Modifier
                                .height(68.dp)
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.greyLight,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp)
                                .clickable { },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                modifier = Modifier
                                    .padding(end = 10.dp)
                                    .weight(1f),
                                text = "Enter or scan your address",
                                style = NunchukTheme.typography.body
                            )

                            Image(
                                modifier = Modifier
                                    .size(25.dp),
                                painter = painterResource(id = R.drawable.ic_qrcode_dark),
                                contentDescription = "",
                            )
                        }
                    } else {
                        NcTextField(
                            inputBoxHeight = 68.dp,
                            title = "",
                            value = state.enteredAddress,
                            placeholder = {
                                Text(
                                    modifier = Modifier.padding(top = 8.dp),
                                    text = "Enter or scan your address",
                                    style = NunchukTheme.typography.body
                                )
                            },
                            rightContent = {
                                Image(
                                    modifier = Modifier
                                        .size(25.dp)
                                        .clickable {
                                            onScan()
                                        },
                                    painter = painterResource(id = R.drawable.ic_qrcode_dark),
                                    contentDescription = "",
                                )
                            },
                            error = if (state.isAddressInvalid) stringResource(id = R.string.nc_transaction_invalid_address) else null,
                            onValueChange = onEnterAddress
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionItem(
    modifier: Modifier = Modifier,
    disabled: Boolean,
    isSelected: Boolean,
    title: String,
    desc: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier, onClick = onClick,
        border = BorderStroke(
            width = 2.dp,
            color = if (disabled) Color(0xFFDEDEDE) else if (isSelected) colorResource(id = R.color.nc_primary_color) else Color(
                0xFFDEDEDE
            )
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = isSelected, onClick = onClick, colors = RadioButtonDefaults.colors(
                    disabledUnselectedColor = colorResource(id = R.color.nc_color_031F2B_40),
                ),
                enabled = !disabled
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = title, style = NunchukTheme.typography.title.copy(
                        color = colorResource(id = R.color.nc_color_222222_40).takeIf { disabled }
                            ?: colorResource(id = R.color.nc_primary_color)
                    )
                )
                Text(
                    text = simplifyAddress(desc), style = NunchukTheme.typography.body.copy(
                        color = if (disabled) colorResource(id = R.color.nc_color_222222_40) else colorResource(
                            id = R.color.nc_primary_color
                        )
                    )
                )
            }
        }
    }
}

@Preview
@Composable
private fun ReferralAddressScreenPreview(
) {
    NunchukTheme {
        ReferralAddressScreen(
            state = ReferralAddressUiState(),
        )
    }
}