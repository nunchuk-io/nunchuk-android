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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.lightGray
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.main.R
import com.nunchuk.android.widget.R as WidgetR

private val SUMMARY_STAGE_COLORS = listOf(
    Color(0xFF95C35D),
    Color(0xFF8052AA),
    Color(0xFFF1CC44),
    Color(0xFF57B7D9),
    Color(0xFFE88767),
)
private val SUMMARY_EDGE_FADE_WIDTH = 56.dp
private val SUMMARY_ARROW_SIZE = 24.dp
private val SUMMARY_ARROW_SPACING = 8.dp
private val SUMMARY_ARROW_RESERVED_WIDTH = SUMMARY_ARROW_SIZE + SUMMARY_ARROW_SPACING
private const val SUMMARY_VISIBLE_STAGE_WINDOW_SIZE = 3

private enum class SummaryEdgeFadeSide {
    START,
    END,
}

@Composable
internal fun InheritanceReleaseScheduleDetailScreen(
    remainTime: Int,
    uiState: ReleaseScheduleUiState = ReleaseScheduleUiState(),
    descriptionText: String? = null,
    bufferPeriodSummaryText: String? = null,
    onUiStateChanged: (ReleaseScheduleUiState) -> Unit = {},
    onContinueClicked: (ReleaseScheduleUiState) -> Unit = {},
    onEditStage: (ReleaseScheduleStage) -> Unit = {},
    onEditBufferPeriodClicked: () -> Unit = {},
    onAddStageRequested: () -> Unit = {},
) {
    InheritanceReleaseScheduleDetailContent(
        remainTime = remainTime,
        uiState = uiState,
        descriptionText = descriptionText,
        bufferPeriodSummaryText = bufferPeriodSummaryText,
        onEditStage = onEditStage,
        onEditBufferPeriodClicked = onEditBufferPeriodClicked,
        onAddStageClicked = {
            onAddStageRequested()
        },
        onToggleExpand = { stageId ->
            onUiStateChanged(uiState.toggleExpand(stageId))
        },
        onContinueClicked = {
            onContinueClicked(uiState)
        }
    )
}

@Composable
private fun InheritanceReleaseScheduleDetailContent(
    remainTime: Int = 0,
    uiState: ReleaseScheduleUiState = ReleaseScheduleUiState(),
    descriptionText: String? = null,
    bufferPeriodSummaryText: String? = null,
    onEditStage: (ReleaseScheduleStage) -> Unit = {},
    onEditBufferPeriodClicked: () -> Unit = {},
    onAddStageClicked: () -> Unit = {},
    onToggleExpand: (Int) -> Unit = {},
    onContinueClicked: () -> Unit = {},
) {
    val isEmptyState = uiState.stages.isEmpty()
    NunchukTheme {
        Scaffold(
            topBar = {
                NcTopAppBar(
                    title = stringResource(
                        id = R.string.nc_estimate_remain_time,
                        remainTime
                    )
                )
            },
            bottomBar = {
                if (isEmptyState) {
                    EmptyStateBottomActionSection(
                        onAddStageClicked = onAddStageClicked,
                    )
                } else {
                    BottomSummarySection(
                        uiState = uiState,
                        onContinueClicked = onContinueClicked
                    )
                }
            }
        ) { innerPadding ->
            if (isEmptyState) {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                        text = stringResource(id = R.string.nc_release_schedule_title),
                        style = NunchukTheme.typography.heading
                    )
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = descriptionText ?: stringResource(id = R.string.nc_release_schedule_desc),
                        style = NunchukTheme.typography.body
                    )
                    EmptyStateContent(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 24.dp)
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                        text = stringResource(id = R.string.nc_release_schedule_title),
                        style = NunchukTheme.typography.heading
                    )
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = descriptionText ?: stringResource(id = R.string.nc_release_schedule_desc),
                        style = NunchukTheme.typography.body
                    )

                    uiState.stages.forEachIndexed { index, stage ->
                        StageCard(
                            modifier = Modifier.padding(top = 24.dp),
                            stage = stage,
                            baseAllocatedPercent = uiState.allocatedBeforeStage(stage.id),
                            showConnector = index != uiState.stages.lastIndex,
                            onEditClick = { onEditStage(stage) },
                            onToggleExpand = { onToggleExpand(stage.id) }
                        )
                    }

                    if (bufferPeriodSummaryText != null) {
                        BufferPeriodSummaryCard(
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                            summaryText = bufferPeriodSummaryText,
                            onEditClick = onEditBufferPeriodClicked
                        )
                    }

                    if (uiState.isOverAllocated) {
                        Row(
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 30.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                modifier = Modifier.size(16.dp),
                                painter = painterResource(id = WidgetR.drawable.ic_error_outline),
                                contentDescription = null,
                                tint = Color.Unspecified
                            )
                            Text(
                                modifier = Modifier.padding(start = 8.dp),
                                text = stringResource(id = R.string.nc_release_schedule_exceeds_limit_error),
                                style = NunchukTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 24.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.lightGray,
                                    shape = RoundedCornerShape(999.dp)
                                )
                                .clickable(onClick = onAddStageClicked)
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.nc_add_stage_label),
                                style = NunchukTheme.typography.title
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun BufferPeriodSummaryCard(
    modifier: Modifier = Modifier,
    summaryText: String,
    onEditClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFFF1F1F1),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(id = WidgetR.drawable.ic_buffer_period),
            contentDescription = null,
            tint = Color.Unspecified
        )
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            text = summaryText,
            style = NunchukTheme.typography.bodySmall
        )
        Text(
            modifier = Modifier.clickable(onClick = onEditClick),
            text = stringResource(id = com.nunchuk.android.core.R.string.nc_edit),
            style = NunchukTheme.typography.titleSmall.copy(textDecoration = TextDecoration.Underline)
        )
    }
}

@Composable
private fun EmptyStateContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            modifier = Modifier.size(60.dp),
            painter = painterResource(id = WidgetR.drawable.ic_plus_square),
            contentDescription = null,
            tint = Color.Unspecified
        )
        NcHighlightText(
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth(),
            text = stringResource(id = R.string.nc_release_schedule_empty_hint),
            style = NunchukTheme.typography.body.copy(
                color = MaterialTheme.colorScheme.textSecondary,
                textAlign = TextAlign.Center
            )
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun EmptyStateBottomActionSection(
    onAddStageClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.lightGray)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .navigationBarsPadding()
    ) {
        NcOutlineButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onAddStageClicked,
        ) {
            Icon(
                painter = painterResource(id = WidgetR.drawable.ic_add_2),
                contentDescription = null,
                tint = Color.Unspecified
            )
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = stringResource(id = R.string.nc_add_stage)
            )
        }
        NcPrimaryDarkButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            enabled = false,
            onClick = {},
        ) {
            Text(text = stringResource(id = R.string.nc_text_continue))
        }
    }
}

@Composable
private fun StageCard(
    modifier: Modifier = Modifier,
    stage: ReleaseScheduleStage,
    baseAllocatedPercent: Int,
    showConnector: Boolean = false,
    onEditClick: () -> Unit = {},
    onToggleExpand: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color = Color(0xFFD8D8D8), shape = CircleShape)
            )
            if (showConnector) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .width(1.dp)
                        .weight(1f)
                        .background(Color(0xFFD5D5D5))
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        id = R.string.nc_release_schedule_stage_title,
                        stage.stageNumber,
                        stage.allocationPercent
                    ),
                    style = NunchukTheme.typography.title.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onEditClick),
                    painter = painterResource(id = WidgetR.drawable.ic_edit_small_2),
                    contentDescription = stringResource(id = R.string.nc_release_schedule_edit_stage),
                    tint = Color.Unspecified
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(id = WidgetR.drawable.ic_calendar_blank),
                    contentDescription = null,
                    tint = Color.Unspecified
                )
                Text(
                    modifier = Modifier.padding(start = 10.dp),
                    text = stringResource(
                        id = R.string.nc_release_schedule_first_withdrawal,
                        stage.firstWithdrawalDate.display()
                    ),
                    style = NunchukTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(id = WidgetR.drawable.ic_release_installment),
                    contentDescription = null,
                    tint = Color.Unspecified
                )
                Text(
                    modifier = Modifier.padding(start = 10.dp),
                    text = stringResource(
                        id = R.string.nc_release_schedule_release_pattern,
                        stage.installmentConfig.installmentPercent,
                        frequencyText(stage.installmentConfig),
                        stage.installmentCount
                    ),
                    style = NunchukTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .clickable(onClick = onToggleExpand)
                        .padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            id = if (stage.isExpanded) {
                                R.string.nc_release_schedule_collapse
                            } else {
                                R.string.nc_release_schedule_expand
                            }
                        ),
                        style = NunchukTheme.typography.titleSmall.copy(textDecoration = TextDecoration.Underline)
                    )
                    Icon(
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(18.dp)
                            .rotate(if (stage.isExpanded) 180f else 0f),
                        painter = painterResource(id = WidgetR.drawable.ic_arrow_down),
                        contentDescription = null,
                        tint = Color.Unspecified
                    )
                }
            }

            if (stage.isExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .padding(top = 8.dp, start = 24.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Color(0xFFD5D5D5))
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        stage.buildInstallmentLines(baseAllocatedPercent = baseAllocatedPercent)
                            .forEach { line ->
                                Text(
                                    modifier = Modifier.padding(bottom = 10.dp),
                                    text = stringResource(
                                        id = R.string.nc_release_schedule_installment_line,
                                        line.orderLabel,
                                        line.availablePercent,
                                        line.availableBy.display()
                                    ),
                                    style = NunchukTheme.typography.bodySmall
                                )
                            }
                    }
                }
            }
        }
    }
}

@Composable
private fun frequencyText(config: ReleaseInstallmentConfig): String {
    val repeatEvery = config.repeatEvery.coerceAtLeast(1)
    return when (config.frequency) {
        ReleaseInstallmentFrequency.DAILY -> {
            if (repeatEvery == 1) {
                stringResource(id = R.string.nc_release_schedule_frequency_daily)
            } else {
                stringResource(
                    id = R.string.nc_release_schedule_frequency_every_days_short,
                    repeatEvery
                )
            }
        }

        ReleaseInstallmentFrequency.WEEKLY -> {
            if (repeatEvery == 1) {
                stringResource(id = R.string.nc_release_schedule_frequency_weekly)
            } else {
                stringResource(
                    id = R.string.nc_release_schedule_frequency_every_weeks_short,
                    repeatEvery
                )
            }
        }

        ReleaseInstallmentFrequency.MONTHLY -> {
            if (repeatEvery == 1) {
                stringResource(id = R.string.nc_release_schedule_frequency_monthly)
            } else {
                stringResource(
                    id = R.string.nc_release_schedule_frequency_every_months_short,
                    repeatEvery
                )
            }
        }

        ReleaseInstallmentFrequency.ANNUALLY -> {
            if (repeatEvery == 1) {
                stringResource(id = R.string.nc_release_schedule_frequency_annually)
            } else {
                stringResource(
                    id = R.string.nc_release_schedule_frequency_every_years_short,
                    repeatEvery
                )
            }
        }
    }
}

@Composable
private fun BottomSummarySection(
    uiState: ReleaseScheduleUiState,
    onContinueClicked: () -> Unit,
) {
    val summarySurfaceColor = MaterialTheme.colorScheme.lightGray
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(summarySurfaceColor)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .navigationBarsPadding()
    ) {
        Text(
            text = stringResource(id = R.string.nc_release_schedule_total_allocated, uiState.totalAllocatedPercent),
            style = NunchukTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
        ReleaseScheduleSummaryProgress(
            modifier = Modifier.padding(top = 10.dp),
            segments = uiState.allocationSegments,
            summaryScalePercent = uiState.summaryScalePercent,
            remainingSummaryPercent = uiState.remainingSummaryPercent,
            surfaceColor = summarySurfaceColor,
        )

        NcPrimaryDarkButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            enabled = uiState.totalAllocatedPercent == 100,
            onClick = onContinueClicked,
        ) {
            Text(text = stringResource(id = R.string.nc_text_continue))
        }
    }
}

@Composable
internal fun ReleaseScheduleSummaryProgress(
    modifier: Modifier = Modifier,
    segments: List<ReleaseScheduleAllocationSegment>,
    summaryScalePercent: Int,
    remainingSummaryPercent: Int,
    surfaceColor: Color = Color.Unspecified,
) {
    val effectiveSurfaceColor = if (surfaceColor == Color.Unspecified) {
        MaterialTheme.colorScheme.lightGray
    } else {
        surfaceColor
    }
    val pageStartIndexes = remember(segments.size) {
        buildSummaryPageStartIndexes(
            totalSegments = segments.size,
            windowSize = SUMMARY_VISIBLE_STAGE_WINDOW_SIZE
        )
    }
    var selectedPageIndex by rememberSaveable(segments.size) { mutableIntStateOf(0) }
    val safeSelectedPageIndex = selectedPageIndex.coerceIn(0, (pageStartIndexes.size - 1).coerceAtLeast(0))

    val visibleSegments = if (segments.isEmpty()) {
        emptyList()
    } else {
        val startIndex = pageStartIndexes.getOrElse(safeSelectedPageIndex) { 0 }
        segments.drop(startIndex).take(SUMMARY_VISIBLE_STAGE_WINDOW_SIZE)
    }

    val canGoPreviousPage = safeSelectedPageIndex > 0
    val canGoNextPage = safeSelectedPageIndex < pageStartIndexes.lastIndex
    val isLastPage = safeSelectedPageIndex == pageStartIndexes.lastIndex
    val isWindowedSummary = pageStartIndexes.size > 1

    val windowStartPercent = visibleSegments.firstOrNull()?.startPercent ?: 0
    val lastVisibleEndPercent = visibleSegments.lastOrNull()?.endPercent ?: 0
    val windowEndPercent = if (isWindowedSummary && !isLastPage) {
        lastVisibleEndPercent
    } else {
        summaryScalePercent
    }
    val windowRemainingPercent = (windowEndPercent - lastVisibleEndPercent).coerceAtLeast(0)
    val labelRemainingPercent = if (isWindowedSummary) windowRemainingPercent else remainingSummaryPercent
    val showUnallocatedLabel = remainingSummaryPercent > 0 && (!isWindowedSummary || isLastPage)
    val unallocatedLabelPercent = if (showUnallocatedLabel) remainingSummaryPercent else 0
    val startArrowInset = if (canGoPreviousPage) SUMMARY_ARROW_RESERVED_WIDTH else 0.dp
    val endArrowInset = if (canGoNextPage) SUMMARY_ARROW_RESERVED_WIDTH else 0.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            BottomSummaryStageLabels(
                modifier = Modifier.padding(start = startArrowInset, end = endArrowInset),
                segments = visibleSegments,
                remainingPercent = labelRemainingPercent,
                unallocatedPercent = unallocatedLabelPercent,
            )

            SummaryProgressBarRow(
                modifier = Modifier.padding(top = 8.dp),
                showPreviousButton = canGoPreviousPage,
                showNextButton = canGoNextPage,
                onPreviousClick = { selectedPageIndex = safeSelectedPageIndex - 1 },
                onNextClick = { selectedPageIndex = safeSelectedPageIndex + 1 },
            ) { contentModifier ->
                SegmentedAllocationProgressBar(
                    modifier = contentModifier.height(10.dp),
                    segments = visibleSegments,
                    scaleStartPercent = windowStartPercent,
                    scaleEndPercent = windowEndPercent,
                    remainingPercent = windowRemainingPercent
                )
            }

            BottomSummaryStageDates(
                modifier = Modifier.padding(top = 8.dp, start = startArrowInset, end = endArrowInset),
                segments = visibleSegments,
                remainingPercent = labelRemainingPercent
            )
        }

        if (canGoPreviousPage) {
            SummaryEdgeFadeOverlay(
                modifier = Modifier.matchParentSize(),
                horizontalInset = startArrowInset,
                side = SummaryEdgeFadeSide.START,
                surfaceColor = effectiveSurfaceColor,
            )
        }

        if (canGoNextPage) {
            SummaryEdgeFadeOverlay(
                modifier = Modifier.matchParentSize(),
                horizontalInset = endArrowInset,
                side = SummaryEdgeFadeSide.END,
                surfaceColor = effectiveSurfaceColor,
            )
        }
    }
}

@Composable
private fun BottomSummaryStageLabels(
    modifier: Modifier = Modifier,
    segments: List<ReleaseScheduleAllocationSegment>,
    remainingPercent: Int,
    unallocatedPercent: Int = 0,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        segments.forEach { segment ->
            Text(
                modifier = Modifier
                    .weight(segment.allocationPercent.toFloat())
                    .padding(end = 8.dp),
                text = stringResource(
                    id = R.string.nc_release_schedule_stage_label_with_value,
                    segment.stageNumber,
                    segment.allocationPercent
                ),
                style = NunchukTheme.typography.captionSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (remainingPercent > 0) {
            if (unallocatedPercent > 0) {
                Text(
                    modifier = Modifier
                        .weight(remainingPercent.toFloat()),
                    text = stringResource(
                        id = R.string.nc_release_schedule_unallocated_with_value,
                        unallocatedPercent
                    ),
                    style = NunchukTheme.typography.captionSmall,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Spacer(modifier = Modifier.weight(remainingPercent.toFloat()))
            }
        }
    }
}

@Composable
private fun SummaryEdgeFadeOverlay(
    modifier: Modifier = Modifier,
    horizontalInset: Dp = 0.dp,
    side: SummaryEdgeFadeSide,
    surfaceColor: Color = Color.Unspecified,
) {
    val effectiveSurfaceColor = if (surfaceColor == Color.Unspecified) {
        MaterialTheme.colorScheme.lightGray
    } else {
        surfaceColor
    }
    val brush = when (side) {
        SummaryEdgeFadeSide.START -> Brush.horizontalGradient(
            colorStops = arrayOf(
                0f to effectiveSurfaceColor,
                0.55f to effectiveSurfaceColor.copy(alpha = 0.92f),
                1f to Color.Transparent
            )
        )

        SummaryEdgeFadeSide.END -> Brush.horizontalGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                0.45f to effectiveSurfaceColor.copy(alpha = 0.92f),
                1f to effectiveSurfaceColor
            )
        )
    }

    Box(modifier = modifier) {
        val edgeModifier = Modifier
            .align(
                if (side == SummaryEdgeFadeSide.START) {
                    Alignment.CenterStart
                } else {
                    Alignment.CenterEnd
                }
            )
            .padding(
                start = if (side == SummaryEdgeFadeSide.START) horizontalInset else 0.dp,
                end = if (side == SummaryEdgeFadeSide.END) horizontalInset else 0.dp
            )
            .fillMaxHeight()
            .width(SUMMARY_EDGE_FADE_WIDTH)
            .blur(10.dp)
            .background(brush = brush)

        Box(modifier = edgeModifier)
    }
}

@Composable
private fun SummaryProgressBarRow(
    modifier: Modifier = Modifier,
    showPreviousButton: Boolean,
    showNextButton: Boolean,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showPreviousButton) {
            Icon(
                modifier = Modifier
                    .size(SUMMARY_ARROW_SIZE)
                    .rotate(180f)
                    .clickable(onClick = onPreviousClick),
                painter = painterResource(id = WidgetR.drawable.ic_circle_arrow),
                contentDescription = null,
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.width(SUMMARY_ARROW_SPACING))
        }

        content(Modifier.weight(1f))

        if (showNextButton) {
            Spacer(modifier = Modifier.width(SUMMARY_ARROW_SPACING))
            Icon(
                modifier = Modifier
                    .size(SUMMARY_ARROW_SIZE)
                    .clickable(onClick = onNextClick),
                painter = painterResource(id = WidgetR.drawable.ic_circle_arrow),
                contentDescription = null,
                tint = Color.Unspecified
            )
        }
    }
}

@Composable
private fun BottomSummaryStageDates(
    modifier: Modifier = Modifier,
    segments: List<ReleaseScheduleAllocationSegment>,
    remainingPercent: Int,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        segments.forEach { segment ->
            Text(
                modifier = Modifier
                    .weight(segment.allocationPercent.toFloat())
                    .padding(end = 8.dp),
                text = segment.firstWithdrawalDate.display(),
                style = NunchukTheme.typography.captionSmall.copy(color = MaterialTheme.colorScheme.textSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (remainingPercent > 0) {
            Spacer(modifier = Modifier.weight(remainingPercent.toFloat()))
        }
    }
}

@Composable
private fun SegmentedAllocationProgressBar(
    modifier: Modifier = Modifier,
    segments: List<ReleaseScheduleAllocationSegment>,
    scaleStartPercent: Int,
    scaleEndPercent: Int,
    remainingPercent: Int,
) {
    val safeStart = scaleStartPercent.coerceAtLeast(0)
    val safeEnd = scaleEndPercent.coerceAtLeast(safeStart + 1)
    val safeScale = (safeEnd - safeStart).coerceAtLeast(1)
    Canvas(modifier = modifier) {
        val trackPath = Path().apply {
            addRoundRect(
                RoundRect(
                    left = 0f,
                    top = 0f,
                    right = size.width,
                    bottom = size.height,
                    topLeftCornerRadius = CornerRadius(size.height / 2, size.height / 2),
                    topRightCornerRadius = CornerRadius(size.height / 2, size.height / 2),
                    bottomLeftCornerRadius = CornerRadius(size.height / 2, size.height / 2),
                    bottomRightCornerRadius = CornerRadius(size.height / 2, size.height / 2),
                )
            )
        }

        clipPath(trackPath) {
            drawRect(color = Color(0xFFE3E3E3), size = size)

            segments.forEach { segment ->
                val startX = size.width * (((segment.startPercent - safeStart).coerceIn(0, safeScale)) / safeScale.toFloat())
                val endX = size.width * (((segment.endPercent - safeStart).coerceIn(0, safeScale)) / safeScale.toFloat())
                if (endX > startX) {
                    drawRect(
                        color = SUMMARY_STAGE_COLORS[(segment.stageNumber - 1).mod(SUMMARY_STAGE_COLORS.size)],
                        topLeft = Offset(startX, 0f),
                        size = Size(endX - startX, size.height)
                    )
                }
            }

            if (remainingPercent > 0) {
                val allocatedEndPercent = segments.lastOrNull()?.endPercent ?: safeStart
                val hatchStartX = size.width * (((allocatedEndPercent - safeStart).coerceIn(0, safeScale)) / safeScale.toFloat())
                clipRect(left = hatchStartX, top = 0f, right = size.width, bottom = size.height) {
                    val hatchSpacing = 7.dp.toPx()
                    val hatchStroke = 1.dp.toPx()
                    var x = hatchStartX - size.height
                    while (x < size.width + size.height) {
                        drawLine(
                            color = Color(0xFFD3D3D3),
                            start = Offset(x, size.height),
                            end = Offset(x + size.height, 0f),
                            strokeWidth = hatchStroke
                        )
                        x += hatchSpacing
                    }
                }
            }
        }
    }
}

private fun buildSummaryPageStartIndexes(
    totalSegments: Int,
    windowSize: Int,
): List<Int> {
    if (totalSegments <= 0 || windowSize <= 0) return listOf(0)
    if (totalSegments <= windowSize) return listOf(0)

    val safeWindow = windowSize.coerceAtLeast(1)
    val pageStep = (safeWindow - 1).coerceAtLeast(1)
    val starts = mutableListOf<Int>()
    var start = 0
    while (start + safeWindow < totalSegments) {
        starts += start
        start += pageStep
    }

    val lastStart = (totalSegments - safeWindow).coerceAtLeast(0)
    if (starts.lastOrNull() != lastStart) {
        starts += lastStart
    }
    return starts
}

@PreviewLightDark
@Composable
private fun InheritanceReleaseScheduleDetailScreenPreview() {
    InheritanceReleaseScheduleDetailContent(
        remainTime = 18,
        uiState = ReleaseScheduleUiState(
            stages = ReleaseScheduleUiState.largeDataPreviewStages()
        ),
        onContinueClicked = {},
    )
}

@PreviewLightDark
@Composable
private fun InheritanceReleaseScheduleDetailErrorPreview() {
    InheritanceReleaseScheduleDetailContent(
        remainTime = 18,
        uiState = ReleaseScheduleUiState(
            stages = ReleaseScheduleUiState.errorPreviewStages()
        ),
        onContinueClicked = {},
    )
}

@PreviewLightDark
@Composable
private fun InheritanceReleaseScheduleDetailEmptyPreview() {
    InheritanceReleaseScheduleDetailContent(
        remainTime = 18,
        uiState = ReleaseScheduleUiState(stages = emptyList()),
        onContinueClicked = {},
    )
}
