package com.nunchuk.android.transaction.components.send.receipt

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcBadgePrimary
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.miniscript.MiniscriptTaproot
import com.nunchuk.android.compose.miniscript.ScriptMode
import com.nunchuk.android.compose.miniscript.ScriptNodeData
import com.nunchuk.android.compose.miniscript.ScriptNodeTree
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.model.ScriptNode
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.transaction.R

@Composable
fun ChooseSigningPathScreen(
    wallet: Wallet,
    signers: List<SignerModel>,
    scriptNode: ScriptNode,
    onContinue: () -> Unit,
) {
    NunchukTheme {
        Scaffold(
            topBar = {
                NcTopAppBar(
                    title = stringResource(R.string.nc_choose_signing_path),
                    textStyle = NunchukTheme.typography.titleLarge,
                )
            },
            bottomBar = {
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Continue")
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    val keyPath = wallet.signers.firstOrNull()?.name
                    MiniscriptTaproot(
                        keyPath = keyPath.orEmpty(),
                        data = ScriptNodeData(
                            mode = ScriptMode.VIEW,
                            signers = signers.associateBy { it.fingerPrint },
                            showBip32Path = true
                        ),
                        signer = wallet.signers.firstOrNull()?.toModel(),
                        onChangeBip32Path = { _, _ -> },
                        onActionKey = { _, _ -> }
                    )
                }
                item {
                    NcBadgePrimary(
                        modifier = Modifier.padding(
                            top = 16.dp,
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
                            signers = signers.associateBy { it.fingerPrint },
                            showBip32Path = true
                        )
                    )
                }
            }
        }
    }
} 