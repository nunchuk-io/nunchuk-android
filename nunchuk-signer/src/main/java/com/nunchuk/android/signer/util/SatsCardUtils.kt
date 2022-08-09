package com.nunchuk.android.signer.util

import androidx.fragment.app.Fragment
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.nav.NunchukNavigator

internal fun Fragment.openSweepRecipeScreen(navigator: NunchukNavigator, slots: List<SatsCardSlot>, isSweepActiveSlot: Boolean) {
    val sweepType = if (isSweepActiveSlot) SweepType.UNSEAL_SWEEP_TO_EXTERNAL_ADDRESS else SweepType.SWEEP_TO_EXTERNAL_ADDRESS
    val totalBalance = slots.sumOf { it.balance.value }
    val totalInBtc = Amount(value = totalBalance).pureBTC()
    navigator.openAddReceiptScreen(
        activityContext = requireActivity(),
        walletId = "",
        outputAmount = totalInBtc,
        availableAmount = totalInBtc,
        subtractFeeFromAmount = true,
        slots = slots,
        sweepType = sweepType
    )
}