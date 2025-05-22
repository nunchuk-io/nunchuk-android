package com.nunchuk.android.transaction.components.send.receipt

import androidx.compose.foundation.background
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcSpannedClickableText
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.SpanIndicator
import com.nunchuk.android.compose.lightGray
import com.nunchuk.android.compose.provider.SignersModelProvider
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.KeySetStatus
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.TxInput
import com.nunchuk.android.model.TxOutput
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.details.KeySetFeeView
import com.nunchuk.android.transaction.components.send.confirmation.TaprootDraftTransaction
import com.nunchuk.android.type.TransactionStatus

@Composable
fun FeeSelectionScreen(
    isAutoFeeSelectionEnabled: Boolean,
    draftTx: TaprootDraftTransaction,
    signers: List<SignerModel>,
    onContinue: (Int) -> Unit = {},
    onFeeSettingsClick: () -> Unit = {},
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    NunchukTheme {
        NcScaffold(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.nc_estimated_fees_for_each_signing_policy),
                    style = NunchukTheme.typography.body,
                    color = MaterialTheme.colorScheme.textPrimary,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Info row with clickable Fee Settings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(
                            color = MaterialTheme.colorScheme.lightGray,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    NcSpannedClickableText(
                        modifier = Modifier,
                        text = if (isAutoFeeSelectionEnabled) stringResource(R.string.nc_update_automatic_fee_selection)
                        else stringResource(R.string.nc_configure_automatic_fee_selection),
                        baseStyle = NunchukTheme.typography.body,
                        styles = mapOf(
                            SpanIndicator('A') to SpanStyle(
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline
                            )
                        ),
                        onClick = { onFeeSettingsClick() }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(draftTx.draftTxKeyPath.keySetStatus) { keySetIndex, keySet ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedIndex = keySetIndex
                                }
                        ) {
                            KeySetFeeView(
                                modifier = Modifier.fillMaxWidth(),
                                keySetIndex = keySetIndex,
                                keySet = keySet,
                                signers = signers.associateBy { it.fingerPrint },
                                isValueKeySetDisable = false,
                                isSelected = keySetIndex == selectedIndex,
                                fee = if (keySetIndex == 0) {
                                    draftTx.draftTxKeyPath.fee
                                } else {
                                    draftTx.draftTxScriptPath.fee
                                },
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun FeeSelectionScreenPreview(
    @PreviewParameter(SignersModelProvider::class) signers: List<SignerModel>,
) {
    val transaction = Transaction(
        txId = "123abc",
        height = 1,
        inputs = listOf(
            TxInput("inputTxId1", 0),
            TxInput("inputTxId2", 1)
        ),
        outputs = listOf(
            TxOutput("address1", Amount(1000)),
            TxOutput("address2", Amount(2000))
        ),
        userOutputs = listOf(
            TxOutput("userAddress1", Amount(1500))
        ),
        receiveOutputs = listOf(
            TxOutput("receiveAddress1", Amount(500))
        ),
        changeIndex = 0,
        m = 2,
        signers = mapOf("signer1" to true, "signer2" to false),
        memo = "Sample transaction",
        status = TransactionStatus.CONFIRMED,
        replacedByTxid = "",
        replacedTxid = "",
        fee = Amount(100),
        feeRate = Amount(10),
        blockTime = 1670000000L,
        subtractFeeFromAmount = false,
        isReceive = true,
        subAmount = Amount(500),
        totalAmount = Amount(2500),
        psbt = "psbtData",
        cpfpFee = Amount(50),
        keySetStatus = listOf(
            KeySetStatus(
                TransactionStatus.CONFIRMED, mapOf(
                    "79EB35F4" to true,
                    "79EB35F5" to false
                )
            ),
            KeySetStatus(
                TransactionStatus.CONFIRMED, mapOf(
                    "79EB35F4" to true,
                    "79EB35F5" to false
                )
            ),
        ),
        scriptPathFee = Amount(200)
    )
    NunchukTheme {
        FeeSelectionScreen(
            draftTx = TaprootDraftTransaction(
                draftTxKeyPath = transaction,
                draftTxScriptPath = transaction
            ),
            signers = emptyList(),
            isAutoFeeSelectionEnabled = true
        )
    }
}