package com.nunchuk.android.signer.satscard

import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.type.SatsCardSlotStatus

fun List<SatsCardSlot>.unSealBalanceSlots() = filter { it.isConfirmed && it.status == SatsCardSlotStatus.UNSEALED && it.balance.value > 0 }
fun Array<SatsCardSlot>.unSealBalanceSlots() = filter { it.isConfirmed && it.status == SatsCardSlotStatus.UNSEALED && it.balance.value > 0 }.toTypedArray()