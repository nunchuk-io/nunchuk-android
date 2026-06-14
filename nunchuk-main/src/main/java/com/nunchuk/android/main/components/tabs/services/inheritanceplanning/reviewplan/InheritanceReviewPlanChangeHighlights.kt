package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan

import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritanceBeneficiaryAllocation
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritanceBufferPeriodApplyType
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningParam
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.beneficiaryschedules.InheritanceBeneficiaryScheduleConfig
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleAllocationSegment
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleDate
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleUiState
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.withTimezone
import com.nunchuk.android.model.inheritance.InheritanceNotificationSettings

/**
 * Canonical time zone used to compare schedules. Converting every schedule to the same zone
 * before comparison ensures a pure time-zone change (which only shifts the displayed wall-clock
 * dates while keeping the same moment in time) is not mistaken for a schedule edit.
 */
private const val COMPARISON_ZONE_ID = "UTC"

data class InheritanceReviewPlanChangeHighlights(
    val assetAllocationChangedEmails: Set<String> = emptySet(),
    val releaseMethodChanged: Boolean = false,
    val sharedScheduleChanges: ScheduleChangeHighlights = ScheduleChangeHighlights(),
    val individualScheduleChangesByEmail: Map<String, ScheduleChangeHighlights> = emptyMap(),
    val timezoneChanged: Boolean = false,
    val fallbackSettingsChanged: Boolean = false,
    val globalNoteChanged: Boolean = false,
    val noteChangedEmails: Set<String> = emptySet(),
    val notificationPreferencesChanged: Boolean = false,
) {
    val isAssetAllocationChanged: Boolean
        get() = assetAllocationChangedEmails.isNotEmpty()

    val isBeneficiarySchedulesChanged: Boolean
        get() = sharedScheduleChanges.hasAny ||
            individualScheduleChangesByEmail.values.any { it.hasAny }

    fun isAssetAllocationChangedForEmail(email: String): Boolean {
        return assetAllocationChangedEmails.contains(email.toEmailKey())
    }

    fun isNoteChangedForEmail(email: String): Boolean {
        return noteChangedEmails.contains(email.toEmailKey())
    }
}

data class ScheduleChangeHighlights(
    val firstWithdrawalChanged: Boolean = false,
    val bufferPeriodChanged: Boolean = false,
    val changedStageLabelNumbers: Set<Int> = emptySet(),
    val changedStageDateNumbers: Set<Int> = emptySet(),
) {
    val hasAny: Boolean
        get() = firstWithdrawalChanged ||
            bufferPeriodChanged ||
            changedStageLabelNumbers.isNotEmpty() ||
            changedStageDateNumbers.isNotEmpty()
}

internal fun calculateReviewPlanChangeHighlights(
    initial: InheritancePlanningParam.SetupOrReview?,
    current: InheritancePlanningParam.SetupOrReview,
): InheritanceReviewPlanChangeHighlights {
    if (initial == null) return InheritanceReviewPlanChangeHighlights()

    val initialAllocations = initial.beneficiaryAllocations.toAllocationPercentByEmail()
    val currentAllocations = current.beneficiaryAllocations.toAllocationPercentByEmail()
    val assetAllocationChangedEmails = changedEmailKeysByValue(initialAllocations, currentAllocations)

    val initialScheduleConfigs = normalizeScheduleConfigs(initial.individualScheduleConfigs)
    val currentScheduleConfigs = normalizeScheduleConfigs(current.individualScheduleConfigs)
    val individualScheduleChangesByEmail =
        (initialScheduleConfigs.keys + currentScheduleConfigs.keys).associateWith { emailKey ->
            calculateScheduleChangeHighlights(
                initialScheduleConfigs[emailKey],
                currentScheduleConfigs[emailKey]
            )
        }.filterValues { it.hasAny }

    val initialNotes = initial.beneficiaryAllocations.toNotesByEmail()
    val currentNotes = current.beneficiaryAllocations.toNotesByEmail()
    val noteChangedEmails = changedEmailKeysByValue(initialNotes, currentNotes)

    return InheritanceReviewPlanChangeHighlights(
        assetAllocationChangedEmails = assetAllocationChangedEmails,
        releaseMethodChanged = initial.releaseMethodType != current.releaseMethodType,
        sharedScheduleChanges = calculateScheduleChangeHighlights(
            initial.sharedScheduleConfig,
            current.sharedScheduleConfig
        ),
        individualScheduleChangesByEmail = individualScheduleChangesByEmail,
        timezoneChanged = initial.selectedZoneId != current.selectedZoneId,
        fallbackSettingsChanged = initial.fallbackSettings != current.fallbackSettings,
        globalNoteChanged = initial.note != current.note,
        noteChangedEmails = noteChangedEmails,
        notificationPreferencesChanged = !notificationSettingsEqual(
            first = initial.notificationSettings,
            second = current.notificationSettings
        ) ||
            initial.emails != current.emails ||
            initial.isNotify != current.isNotify
    )
}

private fun calculateScheduleChangeHighlights(
    initial: InheritanceBeneficiaryScheduleConfig?,
    current: InheritanceBeneficiaryScheduleConfig?,
): ScheduleChangeHighlights {
    val initialSegmentsByStageNumber = initial.toSegmentsByStageNumber()
    val currentSegmentsByStageNumber = current.toSegmentsByStageNumber()
    val stageNumbers = initialSegmentsByStageNumber.keys + currentSegmentsByStageNumber.keys

    val changedStageLabelNumbers = stageNumbers.filterTo(mutableSetOf()) { stageNumber ->
        initialSegmentsByStageNumber[stageNumber]?.allocationPercent !=
            currentSegmentsByStageNumber[stageNumber]?.allocationPercent
    }
    val changedStageDateNumbers = stageNumbers.filterTo(mutableSetOf()) { stageNumber ->
        initialSegmentsByStageNumber[stageNumber]?.firstWithdrawalDate !=
            currentSegmentsByStageNumber[stageNumber]?.firstWithdrawalDate
    }

    return ScheduleChangeHighlights(
        firstWithdrawalChanged = initial.firstWithdrawalDateOrNull() != current.firstWithdrawalDateOrNull(),
        bufferPeriodChanged = initial.bufferSnapshot() != current.bufferSnapshot(),
        changedStageLabelNumbers = changedStageLabelNumbers,
        changedStageDateNumbers = changedStageDateNumbers,
    )
}

private data class BufferSnapshot(
    val periodId: String?,
    val applyType: InheritanceBufferPeriodApplyType?,
)

private fun InheritanceBeneficiaryScheduleConfig?.bufferSnapshot(): BufferSnapshot {
    return BufferSnapshot(
        periodId = this?.bufferPeriod?.id,
        applyType = this?.bufferPeriodApplyType,
    )
}

private fun InheritanceBeneficiaryScheduleConfig?.firstWithdrawalDateOrNull(): ReleaseScheduleDate? {
    return normalizedUiStateForComparison()?.stages?.firstOrNull()?.firstWithdrawalDate
}

private fun InheritanceBeneficiaryScheduleConfig?.toSegmentsByStageNumber(): Map<Int, ReleaseScheduleAllocationSegment> {
    return normalizedUiStateForComparison()
        ?.allocationSegments
        ?.associateBy { it.stageNumber }
        ?: emptyMap()
}

/**
 * Builds a canonical schedule for change detection: every stage is converted to a common time
 * zone so equivalent moments compare equal regardless of the display zone, then stages are
 * re-indexed. This prevents a pure time-zone change from being highlighted as a schedule edit.
 */
private fun InheritanceBeneficiaryScheduleConfig?.normalizedUiStateForComparison(): ReleaseScheduleUiState? {
    return this?.releaseScheduleUiState
        ?.withTimezone(newZoneId = COMPARISON_ZONE_ID, fallbackZoneId = COMPARISON_ZONE_ID)
        ?.normalizeForComparison()
}

private fun ReleaseScheduleUiState.normalizeForComparison(): ReleaseScheduleUiState {
    return copy(
        stages = stages
            .sortedBy { it.stageNumber }
            .mapIndexed { index, stage ->
                stage.copy(
                    id = index,
                    stageNumber = index + 1,
                    isExpanded = false,
                )
            }
    )
}

private fun List<InheritanceBeneficiaryAllocation>.toAllocationPercentByEmail(): Map<String, Int> {
    return associate { allocation ->
        allocation.email.toEmailKey() to allocation.allocationPercent
    }
}

private fun List<InheritanceBeneficiaryAllocation>.toNotesByEmail(): Map<String, String> {
    return associate { allocation ->
        allocation.email.toEmailKey() to allocation.note
    }
}

private fun normalizeScheduleConfigs(
    map: Map<String, InheritanceBeneficiaryScheduleConfig>,
): Map<String, InheritanceBeneficiaryScheduleConfig> {
    return map.mapKeys { (key, _) -> key.toEmailKey() }
}

private fun <T> changedEmailKeysByValue(
    initial: Map<String, T>,
    current: Map<String, T>,
): Set<String> {
    return (initial.keys + current.keys).filterTo(mutableSetOf()) { emailKey ->
        initial[emailKey] != current[emailKey]
    }
}

private fun notificationSettingsEqual(
    first: InheritanceNotificationSettings?,
    second: InheritanceNotificationSettings?
): Boolean {
    if (first == null && second == null) return true
    if (first == null || second == null) return false

    return first.emailMeWalletConfig == second.emailMeWalletConfig &&
        first.perEmailSettings == second.perEmailSettings
}

internal fun String.toEmailKey(): String = trim().lowercase()
