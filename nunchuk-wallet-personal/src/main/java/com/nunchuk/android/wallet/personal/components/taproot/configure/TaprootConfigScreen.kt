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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.backgroundMidGray
import com.nunchuk.android.compose.provider.SignersModelProvider
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.wallet.ConfigureWalletState
import com.nunchuk.android.wallet.ConfigureWalletViewModel
import com.nunchuk.android.wallet.personal.R

const val TaprootConfigScreenRoute = "taproot_config_screen"

fun NavGraphBuilder.taprootConfigScreen(
    viewModel: ConfigureWalletViewModel,
    onContinue: () -> Unit,
    onSelectSigner: (SignerModel, Boolean) -> Unit,
    onEditPath: (SignerModel) -> Unit,
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
            }
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
) {
    val partition = state.allSigners.partition { state.supportedSignerTypes.contains(it.type) }
    NunchukTheme {
        NcScaffold(
            modifier = modifier.systemBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(R.string.nc_wallet_configure_title),
                    textStyle = NunchukTheme.typography.titleLarge,
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
                                style = NunchukTheme.typography.caption,
                                color = MaterialTheme.colorScheme.textSecondary,
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

                        Text(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.backgroundMidGray,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            text = "${state.totalRequireSigns}/${state.selectedSigners.size} ${
                                stringResource(
                                    R.string.nc_multisig
                                )
                            }",
                            style = NunchukTheme.typography.caption,
                        )
                    }
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        onClick = onContinue,
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
                    val checkable = state.selectedSigners.size < 5 || isSelected
                    item {
                        ConfigSignerItem(
                            signer = signer,
                            checkable = checkable,
                            isChecked = isSelected,
                            onSelectSigner = onSelectSigner,
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