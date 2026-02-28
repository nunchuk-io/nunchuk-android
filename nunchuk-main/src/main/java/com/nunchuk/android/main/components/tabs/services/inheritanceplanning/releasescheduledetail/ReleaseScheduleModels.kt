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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail

import java.time.LocalDate
import java.util.TimeZone

data class ReleaseScheduleDate(
    val month: Int,
    val day: Int,
    val year: Int,
) {
    fun display(): String = "%02d/%02d/%04d".format(month, day, year)

    fun plusYears(years: Int): ReleaseScheduleDate = copy(year = year + years)

    fun plus(
        frequency: ReleaseInstallmentFrequency,
        repeatEvery: Int,
        times: Int,
    ): ReleaseScheduleDate {
        val safeRepeatEvery = repeatEvery.coerceAtLeast(1)
        val safeTimes = times.coerceAtLeast(0)
        val amount = safeRepeatEvery.toLong() * safeTimes
        val date = LocalDate.of(year, month, day)
        val updated = when (frequency) {
            ReleaseInstallmentFrequency.DAILY -> date.plusDays(amount)
            ReleaseInstallmentFrequency.WEEKLY -> date.plusWeeks(amount)
            ReleaseInstallmentFrequency.MONTHLY -> date.plusMonths(amount)
            ReleaseInstallmentFrequency.ANNUALLY -> date.plusYears(amount)
        }
        return ReleaseScheduleDate(
            month = updated.monthValue,
            day = updated.dayOfMonth,
            year = updated.year
        )
    }
}

enum class ReleaseInstallmentFrequency(
) {
    DAILY,
    WEEKLY,
    MONTHLY,
    ANNUALLY,
}

data class ReleaseScheduleTime(
    val hour: Int,
    val minute: Int,
) {
    fun display(): String = "%02d:%02d".format(hour, minute)
}

data class ReleaseInstallmentConfig(
    val installmentPercent: Int,
    val repeatEvery: Int = 1,
    val frequency: ReleaseInstallmentFrequency = ReleaseInstallmentFrequency.ANNUALLY,
) {
    fun installmentCount(totalAllocationPercent: Int): Int {
        if (installmentPercent <= 0 || totalAllocationPercent <= 0) return 0
        val fullInstallmentCount = totalAllocationPercent / installmentPercent
        val remainder = totalAllocationPercent % installmentPercent
        return fullInstallmentCount + if (remainder > 0) 1 else 0
    }
}

data class ReleaseScheduleInstallmentLine(
    val order: Int,
    val availablePercent: Int,
    val availableBy: ReleaseScheduleDate,
) {
    val orderLabel: String
        get() = when (order % 100) {
            11, 12, 13 -> "${order}th"
            else -> when (order % 10) {
                1 -> "${order}st"
                2 -> "${order}nd"
                3 -> "${order}rd"
                else -> "${order}th"
            }
        }
}

data class ReleaseScheduleStage(
    val id: Int,
    val stageNumber: Int,
    val allocationPercent: Int,
    val firstWithdrawalDate: ReleaseScheduleDate,
    val firstWithdrawalTime: ReleaseScheduleTime = ReleaseScheduleTime(hour = 0, minute = 0),
    val timeZoneId: String = TimeZone.getDefault().id,
    val installmentConfig: ReleaseInstallmentConfig,
    val isExpanded: Boolean = false,
) {
    val installmentCount: Int
        get() = installmentConfig.installmentCount(allocationPercent)

    fun buildInstallmentLines(baseAllocatedPercent: Int = 0): List<ReleaseScheduleInstallmentLine> {
        val installmentPercent = installmentConfig.installmentPercent
        if (installmentPercent <= 0 || allocationPercent <= 0) return emptyList()

        val fullInstallmentCount = allocationPercent / installmentPercent
        val remainder = allocationPercent % installmentPercent
        val totalInstallmentCount = fullInstallmentCount + if (remainder > 0) 1 else 0

        return (1..totalInstallmentCount).map { order ->
            val releasedInStage = if (order <= fullInstallmentCount) {
                installmentPercent * order
            } else {
                allocationPercent
            }
            ReleaseScheduleInstallmentLine(
                order = order,
                availablePercent = baseAllocatedPercent + releasedInStage,
                availableBy = firstWithdrawalDate.plus(
                    frequency = installmentConfig.frequency,
                    repeatEvery = installmentConfig.repeatEvery,
                    times = order - 1,
                )
            )
        }
    }
}

data class ReleaseScheduleAllocationSegment(
    val stageNumber: Int,
    val allocationPercent: Int,
    val firstWithdrawalDate: ReleaseScheduleDate,
    val startPercent: Int,
    val endPercent: Int,
)

data class ReleaseScheduleUiState(
    val stages: List<ReleaseScheduleStage> = defaultStages(),
) {
    val totalAllocatedPercent: Int
        get() = stages.sumOf { it.allocationPercent }

    val isOverAllocated: Boolean
        get() = totalAllocatedPercent > 100

    val summaryScalePercent: Int
        get() = if (isOverAllocated) {
            totalAllocatedPercent.coerceAtLeast(100)
        } else {
            100
        }

    val remainingSummaryPercent: Int
        get() = (summaryScalePercent - allocationSegments.sumOf { it.allocationPercent })
            .coerceAtLeast(0)

    val allocationSegments: List<ReleaseScheduleAllocationSegment>
        get() {
            var runningPercent = 0
            return stages.mapNotNull { stage ->
                val startPercent = runningPercent.coerceAtLeast(0)
                val segmentPercent = stage.allocationPercent.coerceAtLeast(0)
                runningPercent += segmentPercent
                if (segmentPercent <= 0) {
                    null
                } else {
                    ReleaseScheduleAllocationSegment(
                        stageNumber = stage.stageNumber,
                        allocationPercent = segmentPercent,
                        firstWithdrawalDate = stage.firstWithdrawalDate,
                        startPercent = startPercent,
                        endPercent = startPercent + segmentPercent,
                    )
                }
            }
        }

    fun allocatedBeforeStage(stageId: Int): Int {
        var total = 0
        stages.forEach { stage ->
            if (stage.id == stageId) return total.coerceAtLeast(0)
            total += stage.allocationPercent
        }
        return total.coerceAtLeast(0)
    }

    fun getStage(stageId: Int): ReleaseScheduleStage? {
        return stages.firstOrNull { it.id == stageId }
    }

    fun updateStage(updatedStage: ReleaseScheduleStage): ReleaseScheduleUiState {
        return copy(
            stages = stages.map { stage ->
                if (stage.id == updatedStage.id) updatedStage else stage
            }
        )
    }

    fun deleteStage(stageId: Int): ReleaseScheduleUiState {
        val remainingStages = stages.filterNot { it.id == stageId }
        if (remainingStages.size == stages.size) return this
        return copy(
            stages = remainingStages.mapIndexed { index, stage ->
                stage.copy(stageNumber = index + 1)
            }
        )
    }

    fun toggleExpand(stageId: Int): ReleaseScheduleUiState {
        return copy(
            stages = stages.map { stage ->
                if (stage.id == stageId) {
                    stage.copy(isExpanded = !stage.isExpanded)
                } else {
                    stage
                }
            }
        )
    }

    fun addStage(): ReleaseScheduleUiState {
        return appendStage(buildNewStage())
    }

    fun appendStage(newStage: ReleaseScheduleStage): ReleaseScheduleUiState {
        return copy(stages = stages + newStage)
    }

    fun previousStageDate(stageNumber: Int): ReleaseScheduleDate? {
        if (stageNumber <= 1) return null
        return stages.firstOrNull { it.stageNumber == stageNumber - 1 }?.firstWithdrawalDate
    }

    fun buildNewStage(): ReleaseScheduleStage {
        val nextStageNumber = (stages.maxOfOrNull { it.stageNumber } ?: 0) + 1
        val previousDate = stages.lastOrNull()?.firstWithdrawalDate
            ?: ReleaseScheduleDate(month = 5, day = 29, year = 2028)
        return ReleaseScheduleStage(
            id = (stages.maxOfOrNull { it.id } ?: 0) + 1,
            stageNumber = nextStageNumber,
            allocationPercent = 0,
            firstWithdrawalDate = previousDate.plusYears(1),
            installmentConfig = ReleaseInstallmentConfig(
                installmentPercent = 20,
                repeatEvery = 1,
                frequency = ReleaseInstallmentFrequency.ANNUALLY,
            ),
            isExpanded = false,
        )
    }

    companion object {
        fun defaultStages(): List<ReleaseScheduleStage> {
            return listOf(
                ReleaseScheduleStage(
                    id = 1,
                    stageNumber = 1,
                    allocationPercent = 100,
                    firstWithdrawalDate = ReleaseScheduleDate(month = 5, day = 29, year = 2028),
                    installmentConfig = ReleaseInstallmentConfig(
                        installmentPercent = 20,
                        repeatEvery = 1,
                        frequency = ReleaseInstallmentFrequency.ANNUALLY,
                    ),
                    isExpanded = false,
                )
            )
        }

        fun largeDataPreviewStages(): List<ReleaseScheduleStage> {
            return listOf(
                ReleaseScheduleStage(
                    id = 1,
                    stageNumber = 1,
                    allocationPercent = 20,
                    firstWithdrawalDate = ReleaseScheduleDate(month = 5, day = 29, year = 2028),
                    installmentConfig = ReleaseInstallmentConfig(
                        installmentPercent = 5,
                        repeatEvery = 1,
                        frequency = ReleaseInstallmentFrequency.ANNUALLY,
                    ),
                    isExpanded = true,
                ),
                ReleaseScheduleStage(
                    id = 2,
                    stageNumber = 2,
                    allocationPercent = 60,
                    firstWithdrawalDate = ReleaseScheduleDate(month = 5, day = 29, year = 2032),
                    installmentConfig = ReleaseInstallmentConfig(
                        installmentPercent = 10,
                        repeatEvery = 2,
                        frequency = ReleaseInstallmentFrequency.ANNUALLY,
                    ),
                    isExpanded = true,
                ),
                ReleaseScheduleStage(
                    id = 3,
                    stageNumber = 3,
                    allocationPercent = 20,
                    firstWithdrawalDate = ReleaseScheduleDate(month = 5, day = 29, year = 2045),
                    installmentConfig = ReleaseInstallmentConfig(
                        installmentPercent = 10,
                        repeatEvery = 1,
                        frequency = ReleaseInstallmentFrequency.ANNUALLY,
                    ),
                    isExpanded = true,
                )
            )
        }

        fun errorPreviewStages(): List<ReleaseScheduleStage> {
            return listOf(
                ReleaseScheduleStage(
                    id = 1,
                    stageNumber = 1,
                    allocationPercent = 100,
                    firstWithdrawalDate = ReleaseScheduleDate(month = 5, day = 29, year = 2028),
                    installmentConfig = ReleaseInstallmentConfig(
                        installmentPercent = 5,
                        repeatEvery = 1,
                        frequency = ReleaseInstallmentFrequency.ANNUALLY,
                    ),
                    isExpanded = false,
                ),
                ReleaseScheduleStage(
                    id = 2,
                    stageNumber = 2,
                    allocationPercent = 80,
                    firstWithdrawalDate = ReleaseScheduleDate(month = 5, day = 29, year = 2032),
                    installmentConfig = ReleaseInstallmentConfig(
                        installmentPercent = 10,
                        repeatEvery = 2,
                        frequency = ReleaseInstallmentFrequency.ANNUALLY,
                    ),
                    isExpanded = false,
                )
            )
        }
    }
}
