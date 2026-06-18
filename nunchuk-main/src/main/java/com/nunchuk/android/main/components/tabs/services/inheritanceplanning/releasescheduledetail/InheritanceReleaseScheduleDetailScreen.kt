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

import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.estimateRemainTimeTitle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.lightGray
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.main.R
import kotlinx.coroutines.launch
import com.nunchuk.android.widget.R as WidgetR

private val SUMMARY_STAGE_COLORS = listOf(
    Color(0xFF95C35D),
    Color(0xFF8052AA),
    Color(0xFFF1CC44),
    Color(0xFF57B7D9),
    Color(0xFFE88767),
)
private val SUMMARY_TEXT_END_PADDING = 8.dp
private val SUMMARY_MIN_STAGE_WIDTH = 92.dp
private val SUMMARY_MIN_REMAINING_WIDTH = 120.dp
private val SUMMARY_EDGE_FADE_WIDTH = 56.dp
private val SUMMARY_ARROW_SIZE = 24.dp
private val STAGE_CARD_TOP_SPACING = 24.dp
private val STAGE_TIMELINE_DOT_TOP_PADDING = 5.dp
private val STAGE_TIMELINE_DOT_SIZE = 10.dp
private val STAGE_TIMELINE_CONNECTOR_TOP_PADDING = 4.dp
private val STAGE_TIMELINE_CONNECTOR_COLOR = Color(0xFFD5D5D5)

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
                    title = estimateRemainTimeTitle(remainTime)
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
                            topSpacing = STAGE_CARD_TOP_SPACING,
                            stage = stage,
                            isExpanded = stage.isExpanded,
                            baseAllocatedPercent = uiState.allocatedBeforeStage(stage.id),
                            showIncomingConnector = index > 0,
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
                color = MaterialTheme.colorScheme.lightGray,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(id = WidgetR.drawable.ic_buffer_period),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.textPrimary
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
internal fun StageCard(
    modifier: Modifier = Modifier,
    stage: ReleaseScheduleStage,
    isExpanded: Boolean,
    baseAllocatedPercent: Int,
    topSpacing: Dp = 0.dp,
    showIncomingConnector: Boolean = false,
    showConnector: Boolean = false,
    showEditIcon: Boolean = true,
    isDisabled: Boolean = false,
    currentInstallmentIndex: Int = Int.MAX_VALUE,
    onEditClick: () -> Unit = {},
    onToggleExpand: () -> Unit = {},
) {
    val textColor = if (isDisabled) MaterialTheme.colorScheme.textSecondary else Color.Unspecified
    val installmentLines = stage.buildInstallmentLines(baseAllocatedPercent)
    val incomingConnectorHeight = topSpacing + STAGE_TIMELINE_DOT_TOP_PADDING

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
            if (incomingConnectorHeight > 0.dp) {
                if (showIncomingConnector) {
                    TimelineConnector(modifier = Modifier.height(incomingConnectorHeight))
                } else {
                    Spacer(modifier = Modifier.height(incomingConnectorHeight))
                }
            }
            Box(
                modifier = Modifier
                    .size(STAGE_TIMELINE_DOT_SIZE)
                    .background(
                        color = if (isDisabled) Color(0xFFE3E3E3) else Color(0xFFD8D8D8),
                        shape = CircleShape
                    )
            )
            if (showConnector) {
                TimelineConnector(
                    modifier = Modifier
                        .padding(top = STAGE_TIMELINE_CONNECTOR_TOP_PADDING)
                        .weight(1f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = topSpacing, end = 16.dp)
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
                    style = NunchukTheme.typography.title.copy(
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                )
                if (showEditIcon) {
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
                    tint = if (isDisabled) MaterialTheme.colorScheme.textSecondary else Color.Unspecified
                )
                Text(
                    modifier = Modifier.padding(start = 10.dp),
                    text = stringResource(
                        id = R.string.nc_release_schedule_first_withdrawal,
                        stage.firstWithdrawalDate.display()
                    ),
                    style = NunchukTheme.typography.bodySmall.copy(color = textColor)
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
                    tint = if (isDisabled) MaterialTheme.colorScheme.textSecondary else Color.Unspecified
                )
                Text(
                    modifier = Modifier.padding(start = 10.dp),
                    text = stringResource(
                        id = R.string.nc_release_schedule_release_pattern,
                        stage.installmentConfig.installmentPercent,
                        frequencyText(stage.installmentConfig),
                        stage.installmentCount
                    ),
                    style = NunchukTheme.typography.bodySmall.copy(color = textColor)
                )
                Spacer(modifier = Modifier.weight(1f))
                if (installmentLines.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .clickable(onClick = onToggleExpand)
                            .padding(start = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(
                                id = if (isExpanded) R.string.nc_release_schedule_collapse
                                else R.string.nc_release_schedule_expand
                            ),
                            style = NunchukTheme.typography.titleSmall.copy(
                                textDecoration = TextDecoration.Underline,
                                color = textColor,
                            )
                        )
                        Icon(
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(18.dp)
                                .rotate(if (isExpanded) 180f else 0f),
                            painter = painterResource(id = WidgetR.drawable.ic_arrow_down),
                            contentDescription = null,
                            tint = if (isDisabled) MaterialTheme.colorScheme.textSecondary else Color.Unspecified
                        )
                    }
                }
            }

            if (isExpanded && installmentLines.isNotEmpty()) {
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
                        installmentLines.forEach { line ->
                            val isInstallmentActive = line.order - 1 <= currentInstallmentIndex
                            val lineColor = if (!isInstallmentActive || isDisabled) {
                                MaterialTheme.colorScheme.textSecondary
                            } else {
                                Color.Unspecified
                            }
                            Text(
                                modifier = Modifier.padding(bottom = 10.dp),
                                text = stringResource(
                                    id = R.string.nc_release_schedule_installment_line,
                                    line.orderLabel,
                                    line.availablePercent,
                                    line.availableBy.display()
                                ),
                                style = NunchukTheme.typography.bodySmall.copy(color = lineColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineConnector(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(1.dp)
            .background(STAGE_TIMELINE_CONNECTOR_COLOR)
    )
}

@Composable
internal fun frequencyText(config: ReleaseInstallmentConfig): String {
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
    labelColorForSegment: ((ReleaseScheduleAllocationSegment) -> Color)? = null,
    dateColorForSegment: ((ReleaseScheduleAllocationSegment) -> Color)? = null,
) {
    val effectiveSurfaceColor = if (surfaceColor == Color.Unspecified) {
        MaterialTheme.colorScheme.lightGray
    } else {
        surfaceColor
    }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val labelTextStyle = NunchukTheme.typography.captionSmall
    val dateTextStyle = NunchukTheme.typography.captionSmall

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
    ) {
        val safeScalePercent = summaryScalePercent
            .coerceAtLeast(segments.sumOf { it.allocationPercent } + remainingSummaryPercent)
            .coerceAtLeast(1)
        val stageItems = segments.map { segment ->
            val label = stringResource(
                id = R.string.nc_release_schedule_stage_label_with_value,
                segment.stageNumber,
                segment.allocationPercent
            )
            val date = segment.firstWithdrawalDate.display()
            val minWidth = maxOf(
                SUMMARY_MIN_STAGE_WIDTH,
                measureSummaryTextWidth(textMeasurer, density, label, labelTextStyle) + SUMMARY_TEXT_END_PADDING,
                measureSummaryTextWidth(textMeasurer, density, date, dateTextStyle) + SUMMARY_TEXT_END_PADDING,
            )
            val proportionalWidth =
                maxWidth * (segment.allocationPercent.coerceAtLeast(0) / safeScalePercent.toFloat())
            SummaryStageItem(
                segment = segment,
                label = label,
                date = date,
                width = maxOf(proportionalWidth, minWidth),
            )
        }
        val remainingItem = if (remainingSummaryPercent > 0) {
            val label = stringResource(
                id = R.string.nc_release_schedule_unallocated_with_value,
                remainingSummaryPercent
            )
            val minWidth = maxOf(
                SUMMARY_MIN_REMAINING_WIDTH,
                measureSummaryTextWidth(textMeasurer, density, label, labelTextStyle) + SUMMARY_TEXT_END_PADDING,
            )
            val proportionalWidth =
                maxWidth * (remainingSummaryPercent / safeScalePercent.toFloat())
            SummaryRemainingItem(
                label = label,
                width = maxOf(proportionalWidth, minWidth),
            )
        } else {
            null
        }
        val contentWidth = maxOf(
            maxWidth,
            stageItems.fold(0.dp) { total, item -> total + item.width } + (remainingItem?.width ?: 0.dp)
        )
        val isScrollable = contentWidth > maxWidth
        val canScrollPrevious = isScrollable && scrollState.value > 0
        val canScrollNext = isScrollable && scrollState.value < scrollState.maxValue
        val scrollBy = with(density) { maxWidth.roundToPx() }

        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .width(contentWidth)
            ) {
                BottomSummaryStageLabels(
                    modifier = Modifier.width(contentWidth),
                    stageItems = stageItems,
                    remainingItem = remainingItem,
                    labelColorForSegment = labelColorForSegment,
                )

                SegmentedAllocationProgressBar(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .width(contentWidth)
                        .height(10.dp),
                    stageItems = stageItems,
                    remainingItem = remainingItem,
                )

                BottomSummaryStageDates(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .width(contentWidth),
                    stageItems = stageItems,
                    remainingItem = remainingItem,
                    dateColorForSegment = dateColorForSegment,
                )
            }

            if (canScrollPrevious) {
                SummaryEdgeFadeOverlay(
                    modifier = Modifier.matchParentSize(),
                    side = SummaryEdgeFadeSide.START,
                    surfaceColor = effectiveSurfaceColor,
                )
                SummaryScrollArrow(
                    modifier = Modifier.align(Alignment.CenterStart),
                    isPrevious = true,
                    onClick = {
                        coroutineScope.launch {
                            scrollState.animateScrollTo((scrollState.value - scrollBy).coerceAtLeast(0))
                        }
                    },
                )
            }

            if (canScrollNext) {
                SummaryEdgeFadeOverlay(
                    modifier = Modifier.matchParentSize(),
                    side = SummaryEdgeFadeSide.END,
                    surfaceColor = effectiveSurfaceColor,
                )
                SummaryScrollArrow(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    isPrevious = false,
                    onClick = {
                        coroutineScope.launch {
                            scrollState.animateScrollTo((scrollState.value + scrollBy).coerceAtMost(scrollState.maxValue))
                        }
                    },
                )
            }
        }
    }
}

private data class SummaryStageItem(
    val segment: ReleaseScheduleAllocationSegment,
    val label: String,
    val date: String,
    val width: Dp,
)

private data class SummaryRemainingItem(
    val label: String,
    val width: Dp,
)

private fun measureSummaryTextWidth(
    textMeasurer: TextMeasurer,
    density: Density,
    text: String,
    style: TextStyle,
): Dp = with(density) {
    textMeasurer.measure(
        text = text,
        style = style,
    ).size.width.toDp()
}

@Composable
private fun BottomSummaryStageLabels(
    modifier: Modifier = Modifier,
    stageItems: List<SummaryStageItem>,
    remainingItem: SummaryRemainingItem? = null,
    labelColorForSegment: ((ReleaseScheduleAllocationSegment) -> Color)? = null,
) {
    Row(modifier = modifier) {
        stageItems.forEach { item ->
            Text(
                modifier = Modifier
                    .width(item.width)
                    .padding(end = SUMMARY_TEXT_END_PADDING),
                text = item.label,
                style = NunchukTheme.typography.captionSmall,
                color = labelColorForSegment?.invoke(item.segment) ?: MaterialTheme.colorScheme.textPrimary,
                maxLines = 1,
                softWrap = false,
            )
        }
        if (remainingItem != null) {
            Text(
                modifier = Modifier
                    .width(remainingItem.width)
                    .padding(end = SUMMARY_TEXT_END_PADDING),
                text = remainingItem.label,
                style = NunchukTheme.typography.captionSmall,
                color = MaterialTheme.colorScheme.textPrimary,
                maxLines = 1,
                softWrap = false,
            )
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
private fun SummaryScrollArrow(
    modifier: Modifier = Modifier,
    isPrevious: Boolean,
    onClick: () -> Unit,
) {
    Icon(
        modifier = modifier
            .size(SUMMARY_ARROW_SIZE)
            .rotate(if (isPrevious) 180f else 0f)
            .clickable(onClick = onClick),
        painter = painterResource(id = WidgetR.drawable.ic_circle_arrow),
        contentDescription = null,
        tint = Color.Unspecified
    )
}

@Composable
private fun BottomSummaryStageDates(
    modifier: Modifier = Modifier,
    stageItems: List<SummaryStageItem>,
    remainingItem: SummaryRemainingItem? = null,
    dateColorForSegment: ((ReleaseScheduleAllocationSegment) -> Color)? = null,
) {
    Row(modifier = modifier) {
        stageItems.forEach { item ->
            Text(
                modifier = Modifier
                    .width(item.width)
                    .padding(end = SUMMARY_TEXT_END_PADDING),
                text = item.date,
                style = NunchukTheme.typography.captionSmall.copy(
                    color = dateColorForSegment?.invoke(item.segment) ?: MaterialTheme.colorScheme.textSecondary
                ),
                maxLines = 1,
                softWrap = false,
            )
        }
        if (remainingItem != null) {
            Spacer(modifier = Modifier.width(remainingItem.width))
        }
    }
}

@Composable
private fun SegmentedAllocationProgressBar(
    modifier: Modifier = Modifier,
    stageItems: List<SummaryStageItem>,
    remainingItem: SummaryRemainingItem? = null,
) {
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

            var startX = 0f
            stageItems.forEach { item ->
                val itemWidth = item.width.toPx()
                if (itemWidth > 0f) {
                    drawRect(
                        color = SUMMARY_STAGE_COLORS[(item.segment.stageNumber - 1).mod(SUMMARY_STAGE_COLORS.size)],
                        topLeft = Offset(startX, 0f),
                        size = Size(itemWidth, size.height)
                    )
                }
                startX += itemWidth
            }

            if (remainingItem != null) {
                val hatchEndX = (startX + remainingItem.width.toPx()).coerceAtMost(size.width)
                clipRect(left = startX, top = 0f, right = hatchEndX, bottom = size.height) {
                    val hatchSpacing = 7.dp.toPx()
                    val hatchStroke = 1.dp.toPx()
                    var x = startX - size.height
                    while (x < hatchEndX + size.height) {
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
