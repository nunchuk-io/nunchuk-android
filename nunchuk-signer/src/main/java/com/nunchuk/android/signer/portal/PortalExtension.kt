package com.nunchuk.android.signer.portal

import com.nunchuk.android.type.Chain

fun Chain.toPortalNetwork(): String = when (this) {
    Chain.MAIN -> "bitcoin"
    Chain.TESTNET -> "testnet"
    Chain.SIGNET, Chain.REGTEST -> "signet"
}