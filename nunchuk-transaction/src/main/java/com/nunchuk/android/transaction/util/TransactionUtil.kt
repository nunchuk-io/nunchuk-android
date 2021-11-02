package com.nunchuk.android.transaction.util

import com.nunchuk.android.model.Transaction

fun Transaction.getConfirmations(chainTip: Int) = if (chainTip > 0 && height > 0 && chainTip >= height) (chainTip - height + 1) else 0

fun Transaction.hasChangeIndex() = outputs.isNotEmpty() && changeIndex >= 0 && changeIndex < outputs.size
