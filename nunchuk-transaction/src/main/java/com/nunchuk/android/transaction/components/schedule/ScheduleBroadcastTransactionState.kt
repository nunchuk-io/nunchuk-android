package com.nunchuk.android.transaction.components.schedule

import com.nunchuk.android.core.util.ONE_HOUR_TO_SECONDS
import com.nunchuk.android.transaction.components.schedule.timezone.TimeZoneDetail
import com.nunchuk.android.transaction.components.schedule.timezone.toTimeZoneDetail
import java.util.*

data class ScheduleBroadcastTransactionState(
    val time: Long = Calendar.getInstance().timeInMillis + ONE_HOUR_TO_SECONDS * 1000L,
    val timeZone: TimeZoneDetail = TimeZone.getDefault().id.toTimeZoneDetail()!!,
)