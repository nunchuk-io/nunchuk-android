package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.view

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcSpannedText
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.SpanIndicator
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritanceBeneficiaryAllocation
import com.nunchuk.android.utils.CoinTagColorUtil

private val chartColors by lazy {
    CoinTagColorUtil.hexColors.map { Color(android.graphics.Color.parseColor(it)) }
}

@Composable
fun AllocationDonutChart(
    modifier: Modifier = Modifier,
    beneficiaries: List<InheritanceBeneficiaryAllocation>,
    labelPrimaryColor: Color = MaterialTheme.colorScheme.textPrimary,
    labelSecondaryColor: Color = MaterialTheme.colorScheme.textSecondary,
    labelPrimaryColorForBeneficiary: ((InheritanceBeneficiaryAllocation) -> Color)? = null,
    labelSecondaryColorForBeneficiary: ((InheritanceBeneficiaryAllocation) -> Color)? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val totalAllocation = beneficiaries.sumOf { it.allocationPercent }.coerceAtLeast(1)
        Canvas(modifier = Modifier.size(60.dp)) {
            val strokeWidth = 10.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            var startAngle = -90f
            beneficiaries.forEachIndexed { index, beneficiary ->
                val sweepAngle = (beneficiary.allocationPercent.toFloat() / totalAllocation) * 360f
                drawArc(
                    color = chartColors[index % chartColors.size],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth),
                    topLeft = topLeft,
                    size = arcSize,
                )
                startAngle += sweepAngle
            }
        }
        val scrollState = rememberScrollState()
        val scrollbarColor = MaterialTheme.colorScheme.textPrimary
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(max = 60.dp)
                .padding(start = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                beneficiaries.forEachIndexed { index, beneficiary ->
                    val effectivePrimaryColor = labelPrimaryColorForBeneficiary?.invoke(beneficiary)
                        ?: labelPrimaryColor
                    val effectiveSecondaryColor = labelSecondaryColorForBeneficiary?.invoke(beneficiary)
                        ?: labelSecondaryColor
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    color = chartColors[index % chartColors.size],
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                        val label = beneficiary.email.ifEmpty {
                            stringResource(R.string.nc_beneficiary_n, index + 1)
                        }
                        NcSpannedText(
                            modifier = Modifier.padding(start = 8.dp),
                            text = stringResource(
                                R.string.nc_beneficiary_allocation,
                                label,
                                beneficiary.allocationPercent
                            ),
                            baseStyle = NunchukTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.MiddleEllipsis,
                            styles = mapOf(
                                SpanIndicator('A') to SpanStyle(color = effectiveSecondaryColor),
                                SpanIndicator('B') to SpanStyle(
                                    color = effectivePrimaryColor,
                                    fontWeight = FontWeight.Bold,
                                ),
                            ),
                        )
                    }
                }
            }
            if (scrollState.maxValue > 0) {
                Canvas(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(3.dp)
                ) {
                    val trackHeight = size.height
                    val thumbRatio =
                        trackHeight / (trackHeight + scrollState.maxValue.toFloat())
                    val thumbHeight = (trackHeight * thumbRatio).coerceAtLeast(16.dp.toPx())
                    val scrollFraction = if (scrollState.maxValue > 0) {
                        scrollState.value.toFloat() / scrollState.maxValue
                    } else 0f
                    val thumbOffset = (trackHeight - thumbHeight) * scrollFraction
                    drawRoundRect(
                        color = scrollbarColor.copy(alpha = 0.3f),
                        topLeft = Offset(0f, 0f),
                        size = Size(size.width, trackHeight),
                        cornerRadius = CornerRadius(size.width / 2),
                    )
                    drawRoundRect(
                        color = scrollbarColor,
                        topLeft = Offset(0f, thumbOffset),
                        size = Size(size.width, thumbHeight),
                        cornerRadius = CornerRadius(size.width / 2),
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun AllocationDonutChartTwoBeneficiariesPreview() {
    NunchukTheme {
        AllocationDonutChart(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            beneficiaries = listOf(
                InheritanceBeneficiaryAllocation(email = "wife@gmail.com", allocationPercent = 50),
                InheritanceBeneficiaryAllocation(email = "son@gmail.com", allocationPercent = 50),
            ),
        )
    }
}

@PreviewLightDark
@Composable
private fun AllocationDonutChartEmptyEmailsPreview() {
    NunchukTheme {
        AllocationDonutChart(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            beneficiaries = listOf(
                InheritanceBeneficiaryAllocation(email = "", allocationPercent = 50),
                InheritanceBeneficiaryAllocation(email = "", allocationPercent = 50),
            ),
        )
    }
}

@PreviewLightDark
@Composable
private fun AllocationDonutChartLongEmailPreview() {
    NunchukTheme {
        AllocationDonutChart(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            beneficiaries = listOf(
                InheritanceBeneficiaryAllocation(email = "thisisaverylongemailaddress@gmail.com", allocationPercent = 50),
                InheritanceBeneficiaryAllocation(email = "", allocationPercent = 50),
                InheritanceBeneficiaryAllocation(email = "", allocationPercent = 0),
            ),
        )
    }
}

@PreviewLightDark
@Composable
private fun AllocationDonutChartManyBeneficiariesPreview() {
    NunchukTheme {
        AllocationDonutChart(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            beneficiaries = listOf(
                InheritanceBeneficiaryAllocation(email = "wife@gmail.com", allocationPercent = 30),
                InheritanceBeneficiaryAllocation(email = "son@gmail.com", allocationPercent = 25),
                InheritanceBeneficiaryAllocation(email = "daughter@gmail.com", allocationPercent = 25),
                InheritanceBeneficiaryAllocation(email = "brother@gmail.com", allocationPercent = 10),
                InheritanceBeneficiaryAllocation(email = "sister@gmail.com", allocationPercent = 10),
            ),
        )
    }
}

@PreviewLightDark
@Composable
private fun AllocationDonutChartUnevenPreview() {
    NunchukTheme {
        AllocationDonutChart(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            beneficiaries = listOf(
                InheritanceBeneficiaryAllocation(email = "main@gmail.com", allocationPercent = 90),
                InheritanceBeneficiaryAllocation(email = "other@gmail.com", allocationPercent = 10),
            ),
        )
    }
}
