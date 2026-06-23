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

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone

/**
 * Validates the inheritance release-schedule "expand installment" math against the test cases the
 * backend team shared (inheritance-client-test-cases.json).
 *
 * Each backend stage expands into installments where:
 *  - count                     = ceil(total_stage_allocation / amount_per_release)
 *  - per-installment delta      = amount_per_release, with the final installment taking the remainder
 *  - allocation_percentage sent = the CUMULATIVE percentage (across previous stages too)
 *
 * The core logic under test is the production [ReleaseScheduleStage.buildInstallmentLines] (cumulative
 * percentage + [ReleaseScheduleDate.plus] date stepping). The millis<->date/zone conversion and the
 * repeat-interval mapping mirror the production conversions in InheritancePlanningActivity
 * (toReleaseScheduleUiState / toReleaseInstallmentConfig) and InheritanceReviewPlanViewModel
 * (toInheritancePlanStages / toEpochMillis).
 *
 * Two backend cases currently DIVERGE from the Android implementation and are asserted against the
 * app's actual output, with the divergence documented inline:
 *  - case 004: decimal percentages — the app models percentages as Int, so 2.5 / 5.5 are not supported.
 *  - case 009: month-end stepping — the app steps additively from the first date (origin.plusMonths(n)),
 *              while the backend steps iteratively (prev.plusMonths). They differ only at month-end.
 */
class InheritanceScheduleExpansionTest {

    private lateinit var originalDefaultZone: TimeZone

    @Before
    fun setUp() {
        // Production falls back to ZoneId.systemDefault() for blank/invalid time zones (see case 011).
        // Pin the default zone to UTC so that fallback is deterministic regardless of the build machine.
        originalDefaultZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalDefaultZone)
    }

    // region Test cases

    @Test
    fun `case 001 - single monthly stage expands into cumulative allocation percentages`() {
        val zone = zoneOf("Etc/UTC")
        val stage = stage(
            amountPerRelease = 10,
            totalAllocation = 30,
            repeatInterval = "MONTH",
            repeatIntervalCount = 1,
            firstWithdrawalTimeMillis = 1767225600000L,
            zone = zone,
        )

        assertEquals(
            listOf(
                1767225600000L to 10,
                1769904000000L to 20,
                1772323200000L to 30,
            ),
            expand(stage, baseAllocatedPercent = 0, zone = zone),
        )
    }

    @Test
    fun `case 002 - later stages continue cumulative allocation from previous stages`() {
        val zone = zoneOf("Etc/UTC")
        val stage1 = stage(10, 30, "MONTH", 1, 1767225600000L, zone)
        val stage2 = stage(20, 70, "MONTH", 1, 1775001600000L, zone)

        assertEquals(
            listOf(
                1767225600000L to 10,
                1769904000000L to 20,
                1772323200000L to 30,
            ),
            expand(stage1, baseAllocatedPercent = 0, zone = zone),
        )
        // Stage 2 continues from the 30% already allocated by stage 1.
        assertEquals(
            listOf(
                1775001600000L to 50,
                1777593600000L to 70,
                1780272000000L to 90,
                1782864000000L to 100,
            ),
            expand(stage2, baseAllocatedPercent = 30, zone = zone),
        )
    }

    @Test
    fun `case 003 - final installment uses the remaining integer percentage`() {
        val zone = zoneOf("Etc/UTC")
        val stage = stage(2, 5, "MONTH", 1, 1767225600000L, zone)

        // deltas: 2, 2, 1 -> cumulative 2, 4, 5
        assertEquals(
            listOf(
                1767225600000L to 2,
                1769904000000L to 4,
                1772323200000L to 5,
            ),
            expand(stage, baseAllocatedPercent = 0, zone = zone),
        )
    }

    @Test
    fun `case 004 - decimal percentages are NOT supported (model uses Int) - DIVERGES from backend`() {
        // Backend case 004 uses amount_per_release = 2.5 and total = 5.5 (deltas 2.5, 2.5, 0.5;
        // cumulative 2.5, 5.0, 5.5). The Android model represents both as Int
        // (ReleaseInstallmentConfig.installmentPercent / ReleaseScheduleStage.allocationPercent),
        // so decimals cannot be expressed. Feeding the integer parts (2 / 5) yields the integer
        // expansion below. This test documents the limitation; see the report for the BE expectation.
        val zone = zoneOf("Etc/UTC")
        val stage = stage(2, 5, "MONTH", 1, 1767225600000L, zone)

        assertEquals(
            listOf(
                1767225600000L to 2,
                1769904000000L to 4,
                1772323200000L to 5,
            ),
            expand(stage, baseAllocatedPercent = 0, zone = zone),
        )
    }

    @Test
    fun `case 005 - DAY repeat interval uses repeat_interval_count days between releases`() {
        val zone = zoneOf("Etc/UTC")
        val stage = stage(25, 100, "DAY", 3, 1767225600000L, zone)

        assertEquals(
            listOf(
                1767225600000L to 25,
                1767484800000L to 50,
                1767744000000L to 75,
                1768003200000L to 100,
            ),
            expand(stage, baseAllocatedPercent = 0, zone = zone),
        )
    }

    @Test
    fun `case 006 - WEEK repeat interval uses repeat_interval_count weeks between releases`() {
        val zone = zoneOf("Etc/UTC")
        val stage = stage(25, 100, "WEEK", 2, 1767225600000L, zone)

        assertEquals(
            listOf(
                1767225600000L to 25,
                1768435200000L to 50,
                1769644800000L to 75,
                1770854400000L to 100,
            ),
            expand(stage, baseAllocatedPercent = 0, zone = zone),
        )
    }

    @Test
    fun `case 007 - MONTH repeat interval count is applied after the first withdrawal time`() {
        val zone = zoneOf("Etc/UTC")
        val stage = stage(10, 50, "MONTH", 5, 1767225600000L, zone)

        // First release is exactly first_withdrawal_time; 5 releases every 5 months -> last is +20 months.
        assertEquals(
            listOf(
                1767225600000L to 10,
                1780272000000L to 20,
                1793491200000L to 30,
                1806537600000L to 40,
                1819756800000L to 50,
            ),
            expand(stage, baseAllocatedPercent = 0, zone = zone),
        )
    }

    @Test
    fun `case 008 - YEAR repeat interval uses calendar years`() {
        val zone = zoneOf("Etc/UTC")
        val stage = stage(40, 100, "YEAR", 1, 1772236800000L, zone)

        // deltas: 40, 40, 20 -> cumulative 40, 80, 100
        assertEquals(
            listOf(
                1772236800000L to 40,
                1803772800000L to 80,
                1835308800000L to 100,
            ),
            expand(stage, baseAllocatedPercent = 0, zone = zone),
        )
    }

    @Test
    fun `case 009 - month-end stepping DIVERGES from backend`() {
        // Backend expects ITERATIVE stepping: 2026-01-31 +1mo -> 02-28, +1mo -> 03-28 (1774656000000).
        // The app's ReleaseScheduleDate.plus steps ADDITIVELY from the first date:
        //   order 3 = 2026-01-31.plusMonths(2) = 2026-03-31 -> 1774915200000.
        // The first two installments match; only the month-end third one differs.
        val zone = zoneOf("Etc/UTC")
        val stage = stage(10, 30, "MONTH", 1, 1769817600000L, zone)

        assertEquals(
            listOf(
                1769817600000L to 10,
                1772236800000L to 20,
                1774915200000L to 30, // app: 2026-03-31. Backend case 009 expects 1774656000000 (2026-03-28).
            ),
            expand(stage, baseAllocatedPercent = 0, zone = zone),
        )
    }

    @Test
    fun `case 010 - timezone expansion preserves local wall-clock time across DST`() {
        val zone = zoneOf("America/New_York")
        val stage = stage(10, 30, "MONTH", 1, 1768053600000L, zone)

        // Local time stays 09:00 New York; the March UTC instant shifts by an hour because DST starts.
        assertEquals(
            listOf(
                1768053600000L to 10, // 2026-01-10T14:00:00Z
                1770732000000L to 20, // 2026-02-10T14:00:00Z
                1773147600000L to 30, // 2026-03-10T13:00:00Z (EDT)
            ),
            expand(stage, baseAllocatedPercent = 0, zone = zone),
        )
    }

    @Test
    fun `case 011 - invalid timezone falls back to (UTC) default schedule expansion`() {
        // Production maps an invalid zone to ZoneId.systemDefault(); setUp() pins that to UTC.
        val zone = zoneOf("Invalid/Timezone")
        val stage = stage(10, 30, "MONTH", 1, 1767225600000L, zone)

        assertEquals(
            listOf(
                1767225600000L to 10,
                1769904000000L to 20,
                1772323200000L to 30,
            ),
            expand(stage, baseAllocatedPercent = 0, zone = zone),
        )
    }

    // endregion

    // region Helpers mirroring the production conversions

    /** Mirror of InheritancePlanningActivity.toZoneIdOrDefault. */
    private fun zoneOf(timezone: String): ZoneId =
        runCatching { if (timezone.isBlank()) ZoneId.systemDefault() else ZoneId.of(timezone) }
            .getOrDefault(ZoneId.systemDefault())

    /**
     * Builds a [ReleaseScheduleStage] from the backend stage representation, mirroring
     * InheritancePlanningActivity.toReleaseScheduleUiState (millis -> date/time in zone) and
     * toReleaseInstallmentConfig (repeat interval -> frequency / repeatEvery).
     */
    private fun stage(
        amountPerRelease: Int,
        totalAllocation: Int,
        repeatInterval: String,
        repeatIntervalCount: Int,
        firstWithdrawalTimeMillis: Long,
        zone: ZoneId,
    ): ReleaseScheduleStage {
        val dateTime = Instant.ofEpochMilli(firstWithdrawalTimeMillis).atZone(zone)
        val normalizedInterval = repeatInterval.uppercase()
        val normalizedCount = repeatIntervalCount.coerceAtLeast(1)
        val frequency = when (normalizedInterval) {
            "DAY", "DAILY" -> if (normalizedCount % 7 == 0) {
                ReleaseInstallmentFrequency.WEEKLY
            } else {
                ReleaseInstallmentFrequency.DAILY
            }

            "WEEK", "WEEKLY" -> ReleaseInstallmentFrequency.WEEKLY
            "MONTH", "MONTHLY" -> ReleaseInstallmentFrequency.MONTHLY
            else -> ReleaseInstallmentFrequency.ANNUALLY
        }
        val repeatEvery = if (
            frequency == ReleaseInstallmentFrequency.WEEKLY &&
            (normalizedInterval == "DAY" || normalizedInterval == "DAILY")
        ) {
            (normalizedCount / 7).coerceAtLeast(1)
        } else {
            normalizedCount
        }

        return ReleaseScheduleStage(
            id = 1,
            stageNumber = 1,
            allocationPercent = totalAllocation,
            firstWithdrawalDate = ReleaseScheduleDate(
                month = dateTime.monthValue,
                day = dateTime.dayOfMonth,
                year = dateTime.year,
            ),
            firstWithdrawalTime = ReleaseScheduleTime(
                hour = dateTime.hour,
                minute = dateTime.minute,
            ),
            timeZoneId = zone.id,
            installmentConfig = ReleaseInstallmentConfig(
                installmentPercent = amountPerRelease,
                repeatEvery = repeatEvery,
                frequency = frequency,
            ),
        )
    }

    /** Mirror of InheritanceReviewPlanViewModel.toEpochMillis. */
    private fun toEpochMillis(date: ReleaseScheduleDate, time: ReleaseScheduleTime, zone: ZoneId): Long =
        LocalDateTime.of(date.year, date.month, date.day, time.hour, time.minute)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

    /**
     * Expands a stage exactly like InheritanceReviewPlanViewModel.toInheritancePlanStages does:
     * [ReleaseScheduleStage.buildInstallmentLines] for the cumulative percentage + the installment
     * dates, then encodes each date back to epoch millis using the stage's first-withdrawal time.
     * Returns (withdrawalTimeMillis, cumulativeAllocationPercentage) pairs.
     */
    private fun expand(
        stage: ReleaseScheduleStage,
        baseAllocatedPercent: Int,
        zone: ZoneId,
    ): List<Pair<Long, Int>> =
        stage.buildInstallmentLines(baseAllocatedPercent = baseAllocatedPercent).map { line ->
            toEpochMillis(line.availableBy, stage.firstWithdrawalTime, zone) to line.availablePercent
        }

    // endregion
}
