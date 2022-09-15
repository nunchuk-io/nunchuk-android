package com.nunchuk.android.persistence

import androidx.room.TypeConverter
import com.nunchuk.android.model.MembershipStep

class Converters {
  @TypeConverter
  fun fromTimestamp(value: Int): MembershipStep {
    return MembershipStep.values()[value]
  }

  @TypeConverter
  fun dateToTimestamp(value: MembershipStep): Int {
    return value.ordinal
  }
}
