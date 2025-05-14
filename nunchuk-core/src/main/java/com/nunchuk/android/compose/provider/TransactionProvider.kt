package com.nunchuk.android.compose.provider

import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.KeySetStatus
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.TxInput
import com.nunchuk.android.model.TxOutput
import com.nunchuk.android.type.TransactionStatus

class TransactionProvider : CollectionPreviewParameterProvider<Transaction>(
    listOf(
        Transaction(
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
                KeySetStatus(TransactionStatus.CONFIRMED, mapOf(
                    "79EB35F4" to true,
                    "79EB35F5" to false
                )),
                KeySetStatus(TransactionStatus.CONFIRMED, mapOf(
                    "79EB35F4" to true,
                    "79EB35F5" to false
                )),
            ),
            scriptPathFee = Amount(200)
        )
    )
)