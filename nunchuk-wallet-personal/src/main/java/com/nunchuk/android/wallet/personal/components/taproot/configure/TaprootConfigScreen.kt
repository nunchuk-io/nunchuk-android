package com.nunchuk.android.wallet.personal.components.taproot.configure

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcSnackbarVisuals
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.backgroundMidGray
import com.nunchuk.android.compose.provider.SignersModelProvider
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.wallet.ConfigureWalletState
import com.nunchuk.android.wallet.ConfigureWalletViewModel
import com.nunchuk.android.wallet.personal.R
import kotlinx.coroutines.launch

const val TaprootConfigScreenRoute = "taproot_config_screen"

const val MAX_SELECTED_SIGNERS = 5

fun NavGraphBuilder.taprootConfigScreen(
    viewModel: ConfigureWalletViewModel,
    onContinue: () -> Unit,
    onSelectSigner: (SignerModel, Boolean) -> Unit,
    onEditPath: (SignerModel) -> Unit,
    onToggleShowPath: () -> Unit,
) {
    composable(TaprootConfigScreenRoute) {
        val state by viewModel.state.collectAsStateWithLifecycle()
        TaprootConfigScreen(
            state = state,
            onContinue = onContinue,
            onSelectSigner = onSelectSigner,
            onEditPath = onEditPath,
            onUpdateRequiredKey = {
                if (it) {
                    viewModel.handleIncreaseRequiredSigners()
                } else {
                    viewModel.handleDecreaseRequiredSigners()
                }
            },
            onToggleShowPath = onToggleShowPath,
        )
    }
}

fun NavHostController.navigateTaprootConfigScreen() {
    navigate(TaprootConfigScreenRoute)
}


@Composable
fun TaprootConfigScreen(
    modifier: Modifier = Modifier,
    state: ConfigureWalletState = ConfigureWalletState(),
    onContinue: () -> Unit = {},
    onSelectSigner: (SignerModel, Boolean) -> Unit = { _, _ -> },
    onEditPath: (SignerModel) -> Unit = {},
    onUpdateRequiredKey: (Boolean) -> Unit = {},
    onToggleShowPath: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val walletType =
        if (state.selectedSigners.size > 1) WalletType.MULTI_SIG else WalletType.SINGLE_SIG
    val partition =
        remember(state.allSigners, state.supportedSigners, state.selectedSigners, walletType) {
            state.allSigners.partition { signer ->
                state.selectedSigners.contains(signer) || isSupportedSigner(
                    state.supportedSigners,
                    signer,
                    walletType
                )
            }
        }
    NunchukTheme {
        NcScaffold(
            modifier = modifier.systemBarsPadding(),
            snackState = snackbarHostState,
            topBar = {
                NcTopAppBar(
                    title = stringResource(R.string.nc_wallet_configure_title),
                    textStyle = NunchukTheme.typography.titleLarge,
                    actions = {
                        IconButton(onClick = onToggleShowPath) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_more),
                                contentDescription = "More icon"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.strokePrimary,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.nc_wallet_required_signers),
                                style = NunchukTheme.typography.body,
                            )

                            Text(
                                modifier = Modifier.padding(top = 4.dp),
                                text = stringResource(R.string.nc_number_of_signatures_required_to_unlock_funds),
                                style = NunchukTheme.typography.caption.copy(
                                    color = MaterialTheme.colorScheme.textSecondary
                                ),
                            )
                        }

                        CircleButton(icon = R.drawable.ic_minus) {
                            onUpdateRequiredKey(false)
                        }
                        Box(
                            Modifier
                                .size(50.dp)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.strokePrimary,
                                    shape = RoundedCornerShape(8.dp),
                                )
                        ) {
                            Text(
                                modifier = Modifier.align(Alignment.Center),
                                text = "${state.totalRequireSigns}",
                                style = NunchukTheme.typography.title,
                            )
                        }
                        CircleButton(icon = R.drawable.ic_plus) {
                            onUpdateRequiredKey(true)
                        }
                    }
                    Row(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.nc_wallet_your_current_config),
                            style = NunchukTheme.typography.bodySmall,
                        )

                        val walletTypeString = if (state.selectedSigners.size > 1) {
                            stringResource(R.string.nc_wallet_multisig)
                        } else {
                            stringResource(R.string.nc_wallet_single_sig)
                        }

                        Text(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.backgroundMidGray,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            text = "${state.totalRequireSigns}/${state.selectedSigners.size} $walletTypeString",
                            style = NunchukTheme.typography.caption,
                        )
                    }
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        enabled = state.selectedSigners.isNotEmpty(),
                        onClick = {
                            val invalidSigner = state.selectedSigners.find { signer ->
                                !isSupportedSigner(state.supportedSigners, signer, walletType)
                            }
                            if (invalidSigner != null) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        NcSnackbarVisuals(
                                            type = NcToastType.ERROR,
                                            message = context.getString(
                                                R.string.nc_error_not_supported_signer,
                                                invalidSigner.getXfpOrCardIdLabel()
                                            ),
                                        )
                                    )
                                }
                            } else {
                                onContinue()
                            }
                        },
                    ) {
                        Text(text = stringResource(R.string.nc_text_continue))
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item("title assign key") {
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = stringResource(R.string.nc_assign_at_least_1_key_to_the_wallet_up_to_5),
                        style = NunchukTheme.typography.titleSmall,
                    )
                }

                partition.first.forEach { signer ->
                    val isSelected = state.selectedSigners.contains(signer)
                    val checkable = state.selectedSigners.size < MAX_SELECTED_SIGNERS || isSelected
                    item {
                        ConfigSignerItem(
                            signer = signer,
                            checkable = checkable,
                            isChecked = isSelected,
                            isShowPath = state.isShowPath,
                            onSelectSigner = onSelectSigner,
                            onEditPath = onEditPath,
                        )
                    }
                }

                item("divider") {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.strokePrimary,
                    )
                }

                item("title can not assign key") {
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = stringResource(R.string.nc_keys_not_yet_supporting_taproot),
                        style = NunchukTheme.typography.titleSmall,
                    )
                }

                partition.second.forEach { signer ->
                    item {
                        ConfigSignerItem(
                            signer = signer,
                            checkable = false,
                            isChecked = false,
                            onSelectSigner = onSelectSigner,
                        )
                    }
                }
            }
        }
    }
}

private fun isSupportedSigner(
    supportedSigners: List<SupportedSigner>,
    signer: SignerModel,
    walletType: WalletType
) = supportedSigners.isEmpty() || supportedSigners.any { supportedSigner ->
    supportedSigner.type == signer.type
            && (supportedSigner.tag == null || signer.tags.contains(supportedSigner.tag))
            && (supportedSigner.walletType == null || supportedSigner.walletType == walletType)
}

@Composable
private fun CircleButton(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .size(36.dp)
            .border(
                width = 2.dp,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.textPrimary
            )
    ) {
        NcIcon(
            modifier = Modifier
                .size(18.dp)
                .align(Alignment.Center),
            painter = painterResource(id = icon),
            contentDescription = null,
        )
    }
}

@PreviewLightDark
@Composable
private fun TaprootConfigScreenPreview(
    @PreviewParameter(SignersModelProvider::class) signers: List<SignerModel>,
) {
    TaprootConfigScreen(
        state = ConfigureWalletState(allSigners = signers),
    )
}