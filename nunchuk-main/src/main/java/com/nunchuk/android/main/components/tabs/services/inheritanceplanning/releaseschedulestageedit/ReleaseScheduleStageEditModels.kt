/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releaseschedulestageedit

import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseInstallmentFrequency
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleDate
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleStage
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleTime

internal enum class ReleaseRepeatUnit {
    DAY,
    WEEK,
    MONTH,
    YEAR,
}

internal enum class StageEditValidationError {
    AMOUNT_PER_RELEASE,
    TOTAL_STAGE_ALLOCATION,
}

internal enum class StageDateValidationError {
    FIRST_BEFORE_CURRENT,
    FIRST_BEFORE_PREVIOUS,
    LAST_AFTER_FOLLOWER,
}

/**
 * Validates the first withdrawal date of the stage being edited against:
 * - the current date (first withdrawal date must be later than today),
 * - the previous stage's final withdrawal date (no overlap with the prior stage),
 * - the follower stage's first withdrawal date (no overlap with the next stage).
 *
 * Returns the first failing rule, or null when the date range is valid.
 */
internal fun firstWithdrawalDateValidationError(
    firstWithdrawalDate: ReleaseScheduleDate,
    lastWithdrawalDate: ReleaseScheduleDate,
    currentDate: ReleaseScheduleDate,
    previousStageDate: ReleaseScheduleDate?,
    nextStageDate: ReleaseScheduleDate?,
): StageDateValidationError? {
    if (!firstWithdrawalDate.isAfter(currentDate)) {
        return StageDateValidationError.FIRST_BEFORE_CURRENT
    }
    if (previousStageDate != null && !firstWithdrawalDate.isAfter(previousStageDate)) {
        return StageDateValidationError.FIRST_BEFORE_PREVIOUS
    }
    if (nextStageDate != null && !lastWithdrawalDate.isBefore(nextStageDate)) {
        return StageDateValidationError.LAST_AFTER_FOLLOWER
    }
    return null
}

internal data class ReleaseScheduleStageDraft(
    val stageId: Int,
    val stageNumber: Int,
    val amountPerReleasePercent: String,
    val repeatEvery: String,
    val repeatUnit: ReleaseRepeatUnit,
    val totalStageAllocationPercent: String,
    val timeZoneId: String,
    val firstWithdrawalDate: ReleaseScheduleDate,
    val firstWithdrawalTime: ReleaseScheduleTime,
) {
    val amountPerReleaseValue: Int
        get() = amountPerReleasePercent.toIntOrNull() ?: 0

    val repeatEveryValue: Int
        get() = repeatEvery.toIntOrNull() ?: 1

    val totalStageAllocationValue: Int
        get() = totalStageAllocationPercent.toIntOrNull() ?: 0

    fun firstValidationError(): StageEditValidationError? {
        if (amountPerReleaseValue <= 0) return StageEditValidationError.AMOUNT_PER_RELEASE
        if (totalStageAllocationValue > 100) return StageEditValidationError.TOTAL_STAGE_ALLOCATION
        return null
    }

    val isStepOneValid: Boolean
        get() = firstValidationError() == null

    fun toUpdatedStage(currentStage: ReleaseScheduleStage): ReleaseScheduleStage {
        return currentStage.copy(
            allocationPercent = totalStageAllocationValue.coerceAtLeast(0),
            firstWithdrawalDate = firstWithdrawalDate,
            firstWithdrawalTime = firstWithdrawalTime,
            timeZoneId = timeZoneId,
            installmentConfig = currentStage.installmentConfig.copy(
                installmentPercent = amountPerReleaseValue.coerceAtLeast(0),
                repeatEvery = repeatEveryValue.coerceAtLeast(1),
                frequency = repeatUnit.toInstallmentFrequency(),
            )
        )
    }
}

internal fun ReleaseInstallmentFrequency.toRepeatUnit(): ReleaseRepeatUnit {
    return when (this) {
        ReleaseInstallmentFrequency.DAILY -> ReleaseRepeatUnit.DAY
        ReleaseInstallmentFrequency.WEEKLY -> ReleaseRepeatUnit.WEEK
        ReleaseInstallmentFrequency.MONTHLY -> ReleaseRepeatUnit.MONTH
        ReleaseInstallmentFrequency.ANNUALLY -> ReleaseRepeatUnit.YEAR
    }
}

internal fun ReleaseRepeatUnit.toInstallmentFrequency(): ReleaseInstallmentFrequency {
    return when (this) {
        ReleaseRepeatUnit.DAY -> ReleaseInstallmentFrequency.DAILY
        ReleaseRepeatUnit.WEEK -> ReleaseInstallmentFrequency.WEEKLY
        ReleaseRepeatUnit.MONTH -> ReleaseInstallmentFrequency.MONTHLY
        ReleaseRepeatUnit.YEAR -> ReleaseInstallmentFrequency.ANNUALLY
    }
}

internal fun ReleaseScheduleStage.toDraft(): ReleaseScheduleStageDraft {
    return ReleaseScheduleStageDraft(
        stageId = id,
        stageNumber = stageNumber,
        amountPerReleasePercent = installmentConfig.installmentPercent.toString(),
        repeatEvery = installmentConfig.repeatEvery.toString(),
        repeatUnit = installmentConfig.frequency.toRepeatUnit(),
        totalStageAllocationPercent = allocationPercent.toString(),
        timeZoneId = timeZoneId,
        firstWithdrawalDate = firstWithdrawalDate,
        firstWithdrawalTime = firstWithdrawalTime,
    )
}
