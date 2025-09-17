package com.nunchuk.android.transaction.components.send.receipt

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.fillDenim2
import com.nunchuk.android.compose.miniscript.ScriptMode
import com.nunchuk.android.compose.miniscript.ScriptNodeData
import com.nunchuk.android.compose.miniscript.ScriptNodeTree
import com.nunchuk.android.compose.miniscript.filterMatchingSigningPaths
import com.nunchuk.android.compose.miniscript.updateWithSmartSelection
import com.nunchuk.android.compose.provider.SignersModelProvider
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.core.miniscript.ScriptNodeType
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.ScriptNode
import com.nunchuk.android.model.SigningPath
import com.nunchuk.android.model.isNotEmpty
import com.nunchuk.android.transaction.R

@Composable
fun SelectScriptPathPolicyScreen(
    isSelectingPathEnabled: Boolean,
    scriptNode: ScriptNode,
    signers: Map<String, SignerModel>,
    signingPaths: List<Pair<SigningPath, Amount>>,
    onContinue: (List<SigningPath>) -> Unit = {},
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    var currentPath by rememberSaveable { mutableStateOf(SigningPath(emptyList())) }
    var disabledPaths by rememberSaveable { mutableStateOf(emptySet<List<Int>>()) }
    var subNodeFollowParents by rememberSaveable { mutableStateOf(emptySet<List<Int>>()) }
    val isSelectingMode = signingPaths.size > 7 && isSelectingPathEnabled

    NunchukTheme {
        NcScaffold(
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxSize(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(R.string.nc_select_signing_policy),
                    textStyle = NunchukTheme.typography.titleLarge,
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = if (isSelectingMode) currentPath.isNotEmpty() else selectedIndex in signingPaths.indices,
                    onClick = {
                        if (isSelectingMode) {
                            // Filter signing paths based on current selected path
                            val availableSigningPaths = signingPaths.map { it.first }
                            val matchingPaths = filterMatchingSigningPaths(
                                currentPath = currentPath,
                                availableSigningPaths = availableSigningPaths
                            )
                            onContinue(matchingPaths)
                        } else {
                            onContinue(listOf(signingPaths[selectedIndex].first))
                        }
                    }
                ) {
                    Text(stringResource(R.string.nc_text_continue))
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Text(
                    text = stringResource(R.string.nc_select_signing_policy_desc),
                    style = NunchukTheme.typography.body,
                    color = MaterialTheme.colorScheme.textPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                if (!isSelectingMode) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        itemsIndexed(signingPaths) { index, (signingPath, fee) ->
                            val isSelected = index == selectedIndex
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .padding(top = 16.dp)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.textPrimary else MaterialTheme.colorScheme.strokePrimary,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .background(MaterialTheme.colorScheme.background)
                                    .clickable {
                                        selectedIndex = index
                                    }
                            ) {
                                // Display the script tree for the selected path
                                Box(
                                    modifier = Modifier.padding(
                                        top = 16.dp,
                                        start = 16.dp,
                                        end = 16.dp,
                                    )
                                ) {
                                    ScriptNodeTree(
                                        node = scriptNode,
                                        data = ScriptNodeData(
                                            mode = ScriptMode.VIEW,
                                            signers = signers,
                                            duplicateSignerKeys = emptySet(),
                                            signingPath = signingPath,
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.fillDenim2.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(
                                                bottomStart = 8.dp,
                                                bottomEnd = 8.dp
                                            )
                                        )
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.nc_transaction_estimate_fee),
                                            style = NunchukTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.textPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = fee.getBTCAmount(),
                                                style = NunchukTheme.typography.body,
                                                fontWeight = FontWeight.W600,
                                                color = MaterialTheme.colorScheme.textPrimary
                                            )
                                            Text(
                                                text = fee.getCurrencyAmount(),
                                                style = NunchukTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.textPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .padding(16.dp)
                    ) {
                        item {
                            ScriptNodeTree(
                                node = scriptNode,
                                data = ScriptNodeData(
                                    mode = ScriptMode.SELECTING,
                                    signingPath = currentPath,
                                    disabledPaths = disabledPaths,
                                    subNodeFollowParents = subNodeFollowParents,
                                    signers = signers,
                                    onPathSelectionChange = { nodeId, isSelected ->
                                        val result = currentPath.updateWithSmartSelection(
                                            nodeId = nodeId,
                                            isSelected = isSelected,
                                            rootNode = scriptNode,
                                            currentDisabledPaths = disabledPaths
                                        )
                                        currentPath = result.signingPath
                                        disabledPaths = result.disabledPaths
                                        subNodeFollowParents =
                                            subNodeFollowParents.toMutableSet().apply {
                                                addAll(result.subNodeFollowParent)
                                            }
                                    }
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun WalletConfigViewMiniscriptPreview(
    @PreviewParameter(SignersModelProvider::class) sampleSigners: List<SignerModel>,
) {
    // Create sample script node with miniscript structure
    val sampleScriptNode = ScriptNode(
        id = emptyList(),
        data = byteArrayOf(),
        type = ScriptNodeType.ANDOR.name,
        keys = listOf(),
        k = 0,
        timeLock = null,
        subs = listOf(
            ScriptNode(
                type = ScriptNodeType.THRESH.name,
                keys = listOf("key_0_0", "key_1_0"),
                subs = emptyList(),
                k = 2,
                id = emptyList(),
                data = byteArrayOf(),
                timeLock = null
            ),
            ScriptNode(
                type = ScriptNodeType.OLDER.name,
                keys = listOf("key_0_1"),
                subs = emptyList(),
                k = 0,
                id = emptyList(),
                data = byteArrayOf(),
                timeLock = null
            )
        )
    )

    // Create signer map that maps keys to signers
    val sampleSignerMap = mapOf(
        "key_0_0" to sampleSigners[0],
        "key_1_0" to sampleSigners[1],
        "key_0_1" to sampleSigners[2]
    )

    SelectScriptPathPolicyScreen(
        isSelectingPathEnabled = true,
        scriptNode = sampleScriptNode,
        signers = sampleSignerMap,
        signingPaths = listOf(
            Pair(
                SigningPath(
                    path = listOf(listOf(1, 1)),
                ),
                Amount(1000)
            ),
            Pair(
                SigningPath(
                    path = listOf(listOf(2, 2)),
                ),
                Amount(2000)
            )
        ),
        onContinue = {}
    )
}