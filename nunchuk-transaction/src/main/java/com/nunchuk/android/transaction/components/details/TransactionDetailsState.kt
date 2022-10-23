package com.nunchuk.android.transaction.components.details

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.Transaction

data class TransactionDetailsState(
    val viewMore: Boolean = false,
    val transaction: Transaction = Transaction(),
    val signers: List<SignerModel> = emptyList()
)