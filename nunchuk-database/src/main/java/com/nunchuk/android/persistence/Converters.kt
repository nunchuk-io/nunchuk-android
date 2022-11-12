package com.nunchuk.android.persistence

import androidx.room.TypeConverter
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStep

class Converters {
    @TypeConverter
    fun fromMembershipStepInt(value: Int): MembershipStep {
        return MembershipStep.values()[value]
    }

    @TypeConverter
    fun membershipStepToInt(value: MembershipStep): Int {
        return value.ordinal
    }

    @TypeConverter
    fun fromMembershipPlanInt(value: Int): MembershipPlan {
        return MembershipPlan.values()[value]
    }

    @TypeConverter
    fun membershipPlanToInt(value: MembershipPlan): Int {
        return value.ordinal
    }
}
