package com.nunchuk.android.transaction.components.send.confirmation

import com.nunchuk.android.model.Transaction

class TaprootDraftTransaction(
    val draftTxKeyPath: Transaction,
    val draftTxScriptPath: Transaction,
)