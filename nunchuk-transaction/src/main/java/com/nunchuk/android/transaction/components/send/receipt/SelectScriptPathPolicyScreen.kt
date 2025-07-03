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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.fillDenim2
import com.nunchuk.android.compose.miniscript.ScriptMode
import com.nunchuk.android.compose.miniscript.ScriptNodeData
import com.nunchuk.android.compose.miniscript.ScriptNodeTree
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.ScriptNode
import com.nunchuk.android.model.SigningPath
import com.nunchuk.android.transaction.R

@Composable
fun SelectScriptPathPolicyScreen(
    scriptNode: ScriptNode,
    signers: Map<String, SignerModel>,
    signingPaths: List<Pair<SigningPath, Amount>>,
    onContinue: (Int) -> Unit = {},
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
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
                    onClick = { onContinue(selectedIndex) }
                ) {
                    Text(stringResource(R.string.nc_text_continue))
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
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
                                    showBip32Path = true,
                                    duplicateSignerKeys = emptySet(),
                                    signingPath = signingPath
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
                                    shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
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
        }
    }
} 