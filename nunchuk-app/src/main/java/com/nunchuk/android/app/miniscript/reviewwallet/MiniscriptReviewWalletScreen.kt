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
import com.nunchuk.android.compose.NcBadgePrimary
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.backgroundMidGray
import com.nunchuk.android.compose.miniscript.MiniscriptTaproot
import com.nunchuk.android.compose.miniscript.PolicyHeader
import com.nunchuk.android.compose.miniscript.ScriptMode
import com.nunchuk.android.compose.miniscript.ScriptNodeData
import com.nunchuk.android.compose.miniscript.ScriptNodeTree
import com.nunchuk.android.core.signer.SignerModel
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
                Timber.tag("miniscript-feature")
                    .d("Processing CreateWalletSuccess event with wallet: ${event.wallet}")
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
                    val hasDuplicateSigners =
                        getDuplicateSignerKeys(uiState.signers, uiState.taprootSigner).isNotEmpty()
                    NcPrimaryDarkButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            viewModel.createMiniscriptWallet()
                        },
                        enabled = uiState.areAllKeysAssigned && !hasDuplicateSigners
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

                val parentModifier = Modifier.padding(horizontal = 16.dp)

                PolicyHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                )

                TaprootAddressContent(uiState = uiState, parentModifier = parentModifier)

                Column(modifier = parentModifier) {
                    uiState.scriptNode?.let { scriptNode ->
                        ScriptNodeTree(
                            node = scriptNode,
                            data = ScriptNodeData(
                                mode = ScriptMode.VIEW,
                                signers = uiState.signers,
                                showBip32Path = true,
                                duplicateSignerKeys = getDuplicateSignerKeys(
                                    uiState.signers,
                                    uiState.taprootSigner
                                )
                            ),
                            onChangeBip32Path = { _, _ -> },
                            onActionKey = { _, _ -> }
                            // Using default behavior, no custom action button needed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaprootAddressContent(
    uiState: com.nunchuk.android.app.miniscript.MiniscriptSharedWalletState,
    parentModifier: Modifier = Modifier
) {
    if (uiState.addressType == AddressType.TAPROOT) {
        val scriptNodeMuSig = uiState.scriptNodeMuSig
        if (uiState.keyPath.size <= 1) {
            MiniscriptTaproot(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                keyPath = uiState.keyPath.firstOrNull().orEmpty(),
                data = ScriptNodeData(
                    mode = ScriptMode.VIEW,
                    signers = uiState.signers,
                    showBip32Path = true,
                    duplicateSignerKeys = getDuplicateSignerKeys(
                        uiState.signers,
                        uiState.taprootSigner
                    )
                ),
                signer = if (uiState.keyPath.isNotEmpty() && uiState.taprootSigner.isNotEmpty()) uiState.taprootSigner.first() else null,
                onChangeBip32Path = { _, _ -> },
                onActionKey = { _, _ -> }
            )
        } else if (scriptNodeMuSig != null) {
            NcBadgePrimary(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                text = "Key path",
                enabled = true,
            )

            Column(modifier = parentModifier) {
                ScriptNodeTree(
                    node = scriptNodeMuSig,
                    data = ScriptNodeData(
                        mode = ScriptMode.VIEW,
                        signers = uiState.signers,
                        showBip32Path = true,
                        duplicateSignerKeys = getDuplicateSignerKeys(
                            uiState.signers,
                            uiState.taprootSigner
                        )
                    ),
                    onChangeBip32Path = { _, _ -> },
                    onActionKey = { _, _ -> }
                    // Using default behavior, no custom action button needed
                )
            }
        }

        // Add Script path badge
        NcBadgePrimary(
            modifier = Modifier.padding(
                top = 16.dp,
                bottom = 8.dp,
                start = 16.dp,
                end = 16.dp
            ),
            text = "Script path",
            enabled = true
        )
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

private fun getDuplicateSignerKeys(
    signers: Map<String, SignerModel?>,
    taprootSigners: List<SignerModel>
): Set<String> {
    val signerKeyCounts = mutableMapOf<String, Int>()

    // Create a unique key for each signer combining fingerprint and derivation path
    signers.values.filterNotNull().forEach { signer ->
        val signerKey = "${signer.fingerPrint}:${signer.derivationPath}"
        signerKeyCounts[signerKey] = signerKeyCounts.getOrDefault(signerKey, 0) + 1
    }

    // Count taproot signers
    taprootSigners.forEach { signer ->
        val signerKey = "${signer.fingerPrint}:${signer.derivationPath}"
        signerKeyCounts[signerKey] = signerKeyCounts.getOrDefault(signerKey, 0) + 1
    }

    // Return signer keys that appear more than once
    return signerKeyCounts.filter { it.value > 1 }.keys.toSet()
}

fun NavGraphBuilder.miniscriptReviewWalletDestination(
    viewModel: MiniscriptSharedWalletViewModel,
    onNext: (Wallet) -> Unit = {}
) {
    composable<MiniscriptReviewWallet> {
        MiniscriptReviewWalletScreen(viewModel = viewModel, onNext = onNext)
    }
}