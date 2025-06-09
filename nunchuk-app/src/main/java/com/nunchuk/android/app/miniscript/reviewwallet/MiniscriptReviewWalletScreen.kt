package com.nunchuk.android.app.miniscript.reviewwallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.app.miniscript.MiniscriptSharedWalletEvent
import com.nunchuk.android.app.miniscript.MiniscriptSharedWalletViewModel
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.backgroundMidGray
import com.nunchuk.android.compose.miniscript.PolicyHeader
import com.nunchuk.android.compose.miniscript.ScriptMode
import com.nunchuk.android.compose.miniscript.ScriptNodeData
import com.nunchuk.android.compose.miniscript.ScriptNodeTree
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.util.toReadableString
import kotlinx.serialization.Serializable
import timber.log.Timber

@Serializable
object MiniscriptReviewWallet

@Composable
fun MiniscriptReviewWalletScreen(
    viewModel: MiniscriptSharedWalletViewModel = hiltViewModel(),
    onNext: (Wallet) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.event) {
        Timber.tag("miniscript-feature").d("Event received in LaunchedEffect: ${uiState.event}")
        when (val event = uiState.event) {
            is MiniscriptSharedWalletEvent.CreateWalletSuccess -> {
                Timber.tag("miniscript-feature").d("Processing CreateWalletSuccess event with wallet: ${event.wallet}")
                onNext(event.wallet)
                Timber.tag("miniscript-feature").d("onNext called with wallet: ${event.wallet}")
            }

            is MiniscriptSharedWalletEvent.Error -> {
                Timber.tag("miniscript-feature").e("Error event received: ${event.message}")
            }

            null -> {
                Timber.tag("miniscript-feature").d("Null event received")
            }
            else -> {
                Timber.tag("miniscript-feature").d("Other event received: $event")
            }
        }
        Timber.tag("miniscript-feature").d("Calling onEventHandled()")
        viewModel.onEventHandled()
    }

    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            bottomBar = {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    NcPrimaryDarkButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            viewModel.createMiniscriptWallet()
                        },
                        enabled = uiState.areAllKeysAssigned
                    ) {
                        Text(text = "Create wallet")
                    }
                }
            },
            topBar = {
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.backgroundMidGray)
                        .systemBarsPadding()
                        .padding(bottom = 24.dp)
                ) {
                    NcTopAppBar(
                        title = stringResource(id = R.string.nc_wallet_review_wallet_title),
                        textStyle = NunchukTheme.typography.titleLarge,
                        backgroundColor = MaterialTheme.colorScheme.backgroundMidGray,
                    )

                    Text(
                        text = uiState.walletName,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp),
                        style = NunchukTheme.typography.heading
                    )

                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.background,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            text = stringResource(id = R.string.nc_miniscript),
                            style = NunchukTheme.typography.caption
                        )

                        if (uiState.addressType == AddressType.TAPROOT) {
                            Text(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.background,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                text = uiState.addressType.toReadableString(LocalContext.current),
                                style = NunchukTheme.typography.caption
                            )
                        }
                    }
                }
            }

        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .navigationBarsPadding()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.size(8.dp))
                PolicyHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                )
                val parentModifier = Modifier.padding(horizontal = 16.dp)
                Column(modifier = parentModifier) {
                    uiState.scriptNode?.let { scriptNode ->
                        ScriptNodeTree(
                            node = scriptNode,
                            data = ScriptNodeData(
                                mode = ScriptMode.VIEW,
                                signers = uiState.signers,
                                showBip32Path = true
                            ),
                            onChangeBip32Path = { _, _ -> },
                            onActionKey = { _, _ -> }
                        )
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
fun MiniscriptReviewWalletScreenPreview() {
    NunchukTheme {
        MiniscriptReviewWalletScreen(
            viewModel = hiltViewModel<MiniscriptSharedWalletViewModel>(),
        )
    }
}

fun NavGraphBuilder.miniscriptReviewWalletDestination(
    viewModel: MiniscriptSharedWalletViewModel,
    onNext: (Wallet) -> Unit = {}
) {
    composable<MiniscriptReviewWallet> {
        MiniscriptReviewWalletScreen(viewModel = viewModel, onNext = onNext)
    }
}