package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.controlFillPrimary
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.whisper
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleAllocationSegment
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleDate
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleSummaryProgress
import com.nunchuk.android.model.Period
import com.nunchuk.android.model.inheritance.InheritanceNotificationSettings
import com.nunchuk.android.model.inheritance.InheritancePlanBeneficiary
import com.nunchuk.android.model.inheritance.InheritancePlanFallbackPolicy
import com.nunchuk.android.model.inheritance.InheritancePlanStage
import java.util.Calendar
import com.nunchuk.android.widget.R as WidgetR

// ─── Section header ─────────────────────────────────────────────────────────

@Composable
fun ReviewPlanSectionHeader(
    modifier: Modifier = Modifier,
    title: String,
    editable: Boolean = false,
    onEditClick: () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = NunchukTheme.typography.title,
        )
        Spacer(modifier = Modifier.weight(1f))
        if (editable) {
            Text(
                text = stringResource(id = R.string.nc_edit),
                style = NunchukTheme.typography.title,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable(onClick = onEditClick),
            )
        }
    }
}

// ─── Note display ───────────────────────────────────────────────────────────

@Composable
fun NoteDisplayBox(
    modifier: Modifier = Modifier,
    note: String,
    textColor: Color = MaterialTheme.colorScheme.controlFillPrimary,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.greyLight,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = note.ifBlank { stringResource(id = R.string.nc_no_note) },
            style = NunchukTheme.typography.body.copy(color = textColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}

// ─── Per-beneficiary expandable notes ───────────────────────────────────────

@Composable
fun BeneficiaryNotesSection(
    modifier: Modifier = Modifier,
    beneficiaries: List<InheritancePlanBeneficiary>,
    globalNote: String,
    forcePerBeneficiaryNotes: Boolean = false,
    itemSpacing: Dp = 8.dp,
    changedEmailKeys: Set<String> = emptySet(),
    globalNoteChanged: Boolean = false,
    textColor: @Composable (isChanged: Boolean) -> Color = { MaterialTheme.colorScheme.controlFillPrimary },
) {
    val expandedNotes = remember { mutableStateMapOf<String, Boolean>() }

    Column(modifier = modifier) {
        if (beneficiaries.isNotEmpty() && (forcePerBeneficiaryNotes || beneficiaries.any { it.note.isNotBlank() })) {
            beneficiaries.forEach { beneficiary ->
                val isExpanded = expandedNotes[beneficiary.email] ?: false
                val noteChanged = changedEmailKeys.contains(beneficiary.email.toEmailKey())

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = itemSpacing)
                        .background(
                            color = MaterialTheme.colorScheme.greyLight,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp)
                        .animateContentSize()
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = beneficiary.email,
                                style = NunchukTheme.typography.title,
                            )
                            Text(
                                modifier = Modifier.clickable {
                                    expandedNotes[beneficiary.email] = !isExpanded
                                },
                                text = stringResource(
                                    id = if (isExpanded) R.string.nc_collapse else R.string.nc_expand
                                ),
                                style = NunchukTheme.typography.title.copy(
                                    textDecoration = TextDecoration.Underline
                                ),
                            )
                            NcIcon(
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .size(16.dp)
                                    .clickable {
                                        expandedNotes[beneficiary.email] = !isExpanded
                                    },
                                painter = painterResource(
                                    id = if (isExpanded) WidgetR.drawable.ic_collapse
                                    else WidgetR.drawable.ic_expand
                                ),
                                contentDescription = null,
                            )
                        }
                        Text(
                            modifier = Modifier.padding(top = 8.dp),
                            text = beneficiary.note.ifBlank { stringResource(id = R.string.nc_no_note) },
                            style = NunchukTheme.typography.body.copy(
                                color = textColor(noteChanged)
                            ),
                            maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        } else {
            NoteDisplayBox(
                note = globalNote,
                textColor = textColor(globalNoteChanged),
            )
        }
    }
}

// ─── Simple notification card ───────────────────────────────────────────────

@Composable
fun SimpleNotificationCard(
    modifier: Modifier = Modifier,
    emails: List<String>,
    isNotifyToday: Boolean,
    emailTextColor: Color = MaterialTheme.colorScheme.controlFillPrimary,
    notifyTextColor: Color = MaterialTheme.colorScheme.controlFillPrimary,
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.greyLight,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(id = R.string.nc_beneficiary_trustee_email_address),
                    style = NunchukTheme.typography.body,
                    modifier = Modifier.fillMaxWidth(0.3f),
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = emails.joinToString("\n")
                        .ifEmpty { "(${stringResource(id = R.string.nc_none)})" },
                    style = NunchukTheme.typography.title.copy(color = emailTextColor),
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 24.dp,
                    bottom = 24.dp
                ),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.whisper
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(id = R.string.nc_notify_them_today),
                    style = NunchukTheme.typography.body,
                    modifier = Modifier.fillMaxWidth(0.3f),
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (isNotifyToday) stringResource(id = R.string.nc_text_yes)
                    else stringResource(id = R.string.nc_text_no),
                    style = NunchukTheme.typography.title.copy(color = notifyTextColor),
                )
            }
        }
    }
}

// ─── Notification preferences section ───────────────────────────────────────

@Composable
fun NotificationPreferencesSection(
    modifier: Modifier = Modifier,
    isMiniscriptWallet: Boolean,
    notificationPreferences: InheritanceNotificationSettings?,
    userEmail: String,
    emails: List<String>,
    isNotifyToday: Boolean,
    textColor: Color = MaterialTheme.colorScheme.controlFillPrimary,
    emailTextColor: Color = MaterialTheme.colorScheme.controlFillPrimary,
    notifyTextColor: Color = MaterialTheme.colorScheme.controlFillPrimary,
) {
    if (isMiniscriptWallet && notificationPreferences != null) {
        Column(modifier = modifier) {
            if (userEmail.isNotEmpty()) {
                UserNotificationSettingsContent(
                    emailMeWalletConfig = notificationPreferences.emailMeWalletConfig,
                    userEmail = userEmail,
                    textColor = textColor,
                )
            }
            notificationPreferences.perEmailSettings.forEach { emailSettings ->
                ProviderNotificationSettingsContent(
                    emailSettings = emailSettings,
                    textColor = textColor,
                )
            }
        }
    } else {
        SimpleNotificationCard(
            modifier = modifier.padding(top = 12.dp),
            emails = emails,
            isNotifyToday = isNotifyToday,
            emailTextColor = emailTextColor,
            notifyTextColor = notifyTextColor,
        )
    }
}

// ─── Schedule review cards ──────────────────────────────────────────────────

@Composable
fun BeneficiaryScheduleReviewCard(
    modifier: Modifier = Modifier,
    beneficiary: InheritancePlanBeneficiary,
) {
    val stages = beneficiary.stages
    val segments = remember(stages) { stages.toAllocationSegments() }
    val totalAllocated = segments.sumOf { it.allocationPercent }
    val summaryScale = totalAllocated.coerceAtLeast(100)
    val remaining = (summaryScale - totalAllocated).coerceAtLeast(0)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = beneficiary.email,
                style = NunchukTheme.typography.title,
            )

            val firstStage = stages.firstOrNull()
            if (firstStage != null) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = WidgetR.drawable.ic_calendar_blank),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.textPrimary
                    )
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = stringResource(
                            id = R.string.nc_release_schedule_first_withdrawal,
                            formatDateInTimezone(firstStage.firstWithdrawalTimeMillis)
                        ),
                        style = NunchukTheme.typography.body,
                    )
                }
            }

            val bufferText = getBufferApplyOnText(beneficiary.bufferApplyOn)
            if (bufferText != null) {
                BufferPeriodRow(
                    modifier = Modifier.padding(top = 10.dp),
                    text = bufferText,
                )
            }

            if (segments.isNotEmpty()) {
                ReleaseScheduleSummaryProgress(
                    modifier = Modifier.padding(top = 12.dp),
                    segments = segments,
                    summaryScalePercent = summaryScale,
                    remainingSummaryPercent = remaining,
                )
            }
        }
    }
}

@Composable
fun SharedScheduleReviewCard(
    modifier: Modifier = Modifier,
    stages: List<InheritancePlanStage>,
    bufferPeriod: Period?,
    bufferApplyOn: String?,
) {
    val segments = remember(stages) { stages.toAllocationSegments() }
    val totalAllocated = segments.sumOf { it.allocationPercent }
    val summaryScale = totalAllocated.coerceAtLeast(100)
    val remaining = (summaryScale - totalAllocated).coerceAtLeast(0)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            val firstStage = stages.firstOrNull()
            if (firstStage != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = WidgetR.drawable.ic_calendar_blank),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.textPrimary
                    )
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        text = stringResource(
                            id = R.string.nc_release_schedule_first_withdrawal,
                            formatDateInTimezone(firstStage.firstWithdrawalTimeMillis)
                        ),
                        style = NunchukTheme.typography.body,
                    )
                }
            }

            val bufferText = getBufferApplyOnText(bufferApplyOn, bufferPeriod)
            if (bufferText != null) {
                BufferPeriodRow(
                    modifier = Modifier.padding(top = 10.dp),
                    text = bufferText,
                )
            }

            if (segments.isNotEmpty()) {
                ReleaseScheduleSummaryProgress(
                    modifier = Modifier.padding(top = 12.dp),
                    segments = segments,
                    summaryScalePercent = summaryScale,
                    remainingSummaryPercent = remaining,
                )
            }
        }
    }
}

@Composable
private fun BufferPeriodRow(
    modifier: Modifier = Modifier,
    text: String,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = WidgetR.drawable.ic_buffer_period),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.textPrimary
        )
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = text,
            style = NunchukTheme.typography.body,
        )
    }
}

// ─── Buffer period text helpers ─────────────────────────────────────────────

@Composable
fun getBufferApplyOnText(
    bufferApplyOn: String?,
    bufferPeriod: Period? = null,
): String? {
    if (bufferApplyOn == null) return null
    val applyOnText = when (bufferApplyOn) {
        "FIRST_WITHDRAWAL" -> stringResource(id = R.string.nc_release_schedule_buffer_period_first_withdrawal_only)
        "EVERY_WITHDRAWAL" -> stringResource(id = R.string.nc_release_schedule_buffer_period_every_withdrawal)
        else -> return null
    }
    val periodDays = bufferPeriod?.intervalCount ?: return "Buffer period: ($applyOnText)"
    return stringResource(
        id = R.string.nc_release_schedule_buffer_period_summary,
        periodDays,
        applyOnText
    )
}

// ─── Fallback policy summary ────────────────────────────────────────────────

@Composable
fun getFallbackPolicySummary(policy: InheritancePlanFallbackPolicy): String {
    return when (policy.type) {
        "NONE" -> stringResource(id = R.string.nc_fallback_option_none_title)
        "INACTIVITY" -> {
            val count = policy.inactivityIntervalCount ?: 0
            val unit = policy.inactivityInterval.orEmpty().lowercase()
            "Redistribute equally if no withdrawal occurs for $count ${unit}s after the last scheduled payout."
        }

        "DATE_BASED" -> {
            val fallbackTime = policy.fallbackTimeMillis
            val dateText = if (fallbackTime != null && fallbackTime > 0) {
                formatDateInTimezone(fallbackTime)
            } else {
                ""
            }
            if (dateText.isNotEmpty()) {
                "Redistribute equally on $dateText if no withdrawal occurs."
            } else {
                "Date-based fallback"
            }
        }

        else -> policy.type
    }
}

// ─── Notification preferences equality ──────────────────────────────────────

fun notificationPreferencesEqual(
    new: InheritanceNotificationSettings?,
    old: InheritanceNotificationSettings?
): Boolean {
    if (new == null && old == null) return true
    if (new == null || old == null) return false
    if (new.emailMeWalletConfig != old.emailMeWalletConfig) return false
    if (new.perEmailSettings.size != old.perEmailSettings.size) return false
    return new.perEmailSettings.all { newSettings ->
        old.perEmailSettings.any { oldSettings ->
            oldSettings.email == newSettings.email &&
                    oldSettings.notifyOnTimelockExpiry == newSettings.notifyOnTimelockExpiry &&
                    oldSettings.notifyOnWalletChanges == newSettings.notifyOnWalletChanges &&
                    oldSettings.includeWalletConfiguration == newSettings.includeWalletConfiguration
        }
    }
}

// ─── Stage / date conversion utilities ──────────────────────────────────────

fun List<InheritancePlanStage>.toAllocationSegments(): List<ReleaseScheduleAllocationSegment> {
    var runningPercent = 0
    return mapIndexedNotNull { index, stage ->
        val startPercent = runningPercent
        val allocation = stage.totalStageAllocationPercentage
        runningPercent += allocation
        if (allocation <= 0) null
        else ReleaseScheduleAllocationSegment(
            stageNumber = index + 1,
            allocationPercent = allocation,
            firstWithdrawalDate = millisToReleaseScheduleDate(stage.firstWithdrawalTimeMillis),
            startPercent = startPercent,
            endPercent = startPercent + allocation,
        )
    }
}

fun millisToReleaseScheduleDate(millis: Long): ReleaseScheduleDate {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    return ReleaseScheduleDate(
        month = cal.get(Calendar.MONTH) + 1,
        day = cal.get(Calendar.DAY_OF_MONTH),
        year = cal.get(Calendar.YEAR),
    )
}
