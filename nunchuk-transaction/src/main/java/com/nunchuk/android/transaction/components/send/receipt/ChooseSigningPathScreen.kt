package com.nunchuk.android.transaction.components.send.receipt

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcBadgePrimary
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.SelectableContainer
import com.nunchuk.android.compose.miniscript.MiniscriptTaproot
import com.nunchuk.android.compose.miniscript.PolicyHeader
import com.nunchuk.android.compose.miniscript.ScriptMode
import com.nunchuk.android.compose.miniscript.ScriptNodeData
import com.nunchuk.android.compose.miniscript.ScriptNodeTree
import com.nunchuk.android.compose.provider.SignersModelProvider
import com.nunchuk.android.compose.provider.WalletExtendedProvider
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.core.miniscript.MiniscriptUtil
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.model.ScriptNode
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.transaction.R

@Composable
fun ChooseSigningPathScreen(
    state: AddReceiptState,
    onContinue: (isKeyPathSelected: Boolean) -> Unit = {},
) {
    var isKeyPathSelected by rememberSaveable { mutableStateOf(true) }
    val wallet: Wallet = state.wallet
    val signers: Map<String, SignerModel> = state.signers
    val scriptNode: ScriptNode = state.scriptNode!!

    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxSize(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(R.string.nc_choose_signing_path),
                    textStyle = NunchukTheme.typography.titleLarge,
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    onClick = { onContinue(isKeyPathSelected) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Continue")
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                Text(
                    text = stringResource(R.string.nc_select_signing_policy_desc),
                    style = NunchukTheme.typography.body,
                    color = MaterialTheme.colorScheme.textPrimary,
                    modifier = Modifier.padding(16.dp)
                )

                PolicyHeader(modifier = Modifier.padding(horizontal = 16.dp,))

                LazyColumn(
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    item {
                        val keyPath = wallet.signers.firstOrNull()?.name
                        SelectableContainer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            paddingValues = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 16.dp,
                                bottom = 4.dp
                            ),
                            isSelected = isKeyPathSelected,
                            onClick = { isKeyPathSelected = true }
                        ) {
                            if (wallet.totalRequireSigns > 1) {
                                Column {
                                    NcBadgePrimary(
                                        modifier = Modifier.padding(bottom = 8.dp),
                                        text = stringResource(R.string.nc_key_path),
                                        enabled = true,
                                    )
                                    ScriptNodeTree(
                                        node = MiniscriptUtil.buildMusigNode(wallet.totalRequireSigns),
                                        data = ScriptNodeData(
                                            mode = ScriptMode.VIEW,
                                            signers = state.signers,
                                        ),
                                        onChangeBip32Path = { _, _ -> },
                                        onActionKey = { _, _ -> }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                            } else {
                                MiniscriptTaproot(
                                    keyPath = keyPath.orEmpty(),
                                    data = ScriptNodeData(
                                        mode = ScriptMode.VIEW,
                                        signers = signers,
                                    ),
                                    signer = wallet.signers.firstOrNull()?.toModel(),
                                    onChangeBip32Path = { _, _ -> },
                                    onActionKey = { _, _ -> },
                                    divider = {}
                                )
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        SelectableContainer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            paddingValues = PaddingValues(16.dp),
                            isSelected = !isKeyPathSelected,
                            onClick = { isKeyPathSelected = false }
                        ) {
                            Column {
                                NcBadgePrimary(
                                    modifier = Modifier.padding(
                                        bottom = 8.dp,
                                        end = 16.dp
                                    ),
                                    text = "Script path",
                                    enabled = true
                                )
                                ScriptNodeTree(
                                    node = scriptNode,
                                    data = ScriptNodeData(
                                        mode = ScriptMode.VIEW,
                                        signers = signers,
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun ChooseSigningPathScreenPreview() {
    val wallet = WalletExtendedProvider().values.first().wallet
    val signers = SignersModelProvider().values.first().associateBy { it.fingerPrint }
    val scriptNode = ScriptNode(
        id = listOf(1),
        type = "ANDOR",
        keys = emptyList(),
        subs = emptyList(),
        k = 0,
        data = byteArrayOf(),
        timeLock = null
    )
    ChooseSigningPathScreen(
        state = AddReceiptState(
            wallet = wallet,
            signers = signers,
            scriptNode = scriptNode,
            isValueKeySetDisable = false
        )
    )
}

