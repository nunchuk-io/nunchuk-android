package com.nunchuk.android.wallet.personal.components.taproot.configure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.provider.SignersModelProvider
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.wallet.ConfigureWalletState
import com.nunchuk.android.wallet.ConfigureWalletViewModel
import com.nunchuk.android.wallet.personal.R

const val ConfigureValueKeySetScreenRoute = "configureValueKeySet"

fun NavGraphBuilder.configureValueKeySetScreen(
    viewModel: ConfigureWalletViewModel,
    modifier: Modifier = Modifier,
    onContinueClick: () -> Unit = { }
) {
    composable(
        route = ConfigureValueKeySetScreenRoute,
    ) {
        val state by viewModel.state.collectAsStateWithLifecycle()
        ConfigureValueKeySetScreen(
            modifier = modifier,
            onContinueClick = onContinueClick,
            state = state,
            toggleSigner = viewModel::toggleSelectKeySet
        )
    }
}

fun NavHostController.navigateConfigureValueKeySet() {
    navigate(ConfigureValueKeySetScreenRoute)
}


@Composable
fun ConfigureValueKeySetScreen(
    modifier: Modifier = Modifier,
    state: ConfigureWalletState = ConfigureWalletState(),
    onContinueClick: () -> Unit = { },
    toggleSigner: (SignerModel) -> Unit = { },
) {
    val checkable = state.keySet.size < state.totalRequireSigns
    NunchukTheme {
        NcScaffold(
            modifier = modifier.systemBarsPadding(),
            topBar = {
                NcTopAppBar(title = "")
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = state.keySet.size == state.totalRequireSigns,
                    onClick = onContinueClick,
                    content = {
                        Text(text = stringResource(R.string.nc_text_continue))
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                NcCircleImage(
                    modifier = Modifier.align(CenterHorizontally),
                    size = 90.dp,
                    iconSize = 60.dp,
                    resId = R.drawable.ic_mulitsig_dark,
                )

                Text(
                    text = "Configure Value Key Set",
                    style = NunchukTheme.typography.heading,
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .padding(horizontal = 16.dp)
                )

                Text(
                    text = "Among the keys assigned to the wallet, please select the keys to create the Value Keyset. The Value Keyset will help reduce transaction fees and enhance security.",
                    style = NunchukTheme.typography.body,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .padding(horizontal = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(state.selectedSigners.toList()) { signer ->
                        val isSelected = state.keySet.contains(signer)
                        ConfigSignerItem(
                            signer = signer,
                            checkable = checkable || isSelected,
                            isChecked = isSelected,
                            onSelectSigner = { _, _ ->
                                toggleSigner(signer)
                            },
                        )
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ConfigureValueKeySetPreview(
    @PreviewParameter(SignersModelProvider::class) signers: List<SignerModel>,
) {
    ConfigureValueKeySetScreen(
        state = ConfigureWalletState(
            selectedSigners = signers.toSet(),
            keySet = signers.take(1).toSet(),
        )
    )
}