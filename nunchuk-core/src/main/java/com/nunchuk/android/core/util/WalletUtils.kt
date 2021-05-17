package com.nunchuk.android.core.util

import com.nunchuk.android.model.Wallet

fun Wallet.getBTCAmount() = "${balance.value} BTC"

fun Wallet.getUSDAmount() = "$${balance.value} USD"

fun Wallet.getConfiguration() = "${totalRequireSigns}/${signers.size}"