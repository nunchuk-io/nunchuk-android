package com.nunchuk.android.model

import com.nunchuk.android.model.signer.SignerServer

data class SignInDummyTransaction(
    val psbt: String,
    val pendingSignature: Int,
    val requiredSignatures: Int,
    val dummyTransactionId: String,
    val signerServers: List<SignerServer>,
    val signatures: List<DummySignature>
)