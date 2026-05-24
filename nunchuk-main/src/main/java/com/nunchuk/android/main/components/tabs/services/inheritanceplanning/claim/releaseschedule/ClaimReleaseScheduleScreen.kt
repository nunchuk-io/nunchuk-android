package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.releaseschedule

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.lightGray
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.ClaimInheritanceActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.ClaimInheritanceViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseInstallmentConfig
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseInstallmentFrequency
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleDate
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleStage
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleUiState
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.StageCard
import com.nunchuk.android.model.InheritanceAdditional
import com.nunchuk.android.model.inheritance.InheritancePlanExpandedInstallment
import com.nunchuk.android.model.inheritance.InheritancePlanStage
import java.util.Calendar
import com.nunchuk.android.widget.R as WidgetR

@Composable
internal fun ClaimReleaseScheduleScreen(
    onWithdrawClicked: () -> Unit = {},
    onDoneClicked: () -> Unit = {},
) {
    val activity = LocalActivity.current as ClaimInheritanceActivity
    val activityViewModel: ClaimInheritanceViewModel = hiltViewModel(viewModelStoreOwner = activity)
    val claimData by activityViewModel.claimData.collectAsStateWithLifecycle()
    claimData.inheritanceAdditional?.let { inheritanceAdditional ->
        ClaimReleaseScheduleContent(
            inheritanceAdditional = inheritanceAdditional,
            onWithdrawClicked = onWithdrawClicked,
            onDoneClicked = onDoneClicked,
        )
    }

}

@Composable
private fun ClaimReleaseScheduleContent(
    inheritanceAdditional: InheritanceAdditional,
    onWithdrawClicked: () -> Unit = {},
    onDoneClicked: () -> Unit = {},
) {
    val stages = inheritanceAdditional.stages
    val currentStageIndex = inheritanceAdditional.currentStageIndex
    val currentInstallmentIndex = inheritanceAdditional.currentInstallmentIndex
    val availableToWithdraw = inheritanceAdditional.availableToWithdraw

    val releaseScheduleStages = remember(stages) {
        stages.mapIndexed { index, stage ->
            stage.toReleaseScheduleStage(id = index + 1, stageNumber = index + 1)
        }
    }
    val releaseScheduleUiState = remember(releaseScheduleStages) {
        ReleaseScheduleUiState(stages = releaseScheduleStages)
    }
    val expandedStages = remember { mutableStateMapOf<Int, Boolean>() }

    val availablePercent = computeAvailablePercent(
        stages = stages,
        currentStageIndex = currentStageIndex,
        currentInstallmentIndex = currentInstallmentIndex,
    )

    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcTopAppBar(title = "")
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    NcPrimaryDarkButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = availableToWithdraw > 0,
                        onClick = onWithdrawClicked,
                    ) {
                        Text(text = stringResource(id = com.nunchuk.android.core.R.string.nc_withdraw_bitcoin))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    NcOutlineButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        onClick = onDoneClicked,
                    ) {
                        Text(text = stringResource(id = WidgetR.string.nc_text_done))
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    text = stringResource(id = R.string.nc_release_schedule_title),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(id = R.string.nc_claim_release_schedule_desc),
                    style = NunchukTheme.typography.body
                )

                stages.forEachIndexed { index, stage ->
                    val isExpanded = expandedStages[index] ?: false
                    val releaseStage =
                        releaseScheduleStages.getOrNull(index) ?: return@forEachIndexed
                    val isDisabled = index > currentStageIndex

                    StageCard(
                        topSpacing = 24.dp,
                        stage = releaseStage,
                        isExpanded = isExpanded,
                        isDisabled = isDisabled,
                        showEditIcon = false,
                        currentInstallmentIndex = if (index == currentStageIndex) currentInstallmentIndex else if (index < currentStageIndex) Int.MAX_VALUE else -1,
                        baseAllocatedPercent = releaseScheduleUiState.allocatedBeforeStage(
                            releaseStage.id
                        ),
                        showIncomingConnector = index > 0,
                        showConnector = index != stages.lastIndex,
                        onToggleExpand = { expandedStages[index] = !isExpanded },
                    )
                }

                AvailableBalanceCard(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    availablePercent = availablePercent,
                    availableBtc = availableToWithdraw,
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AvailableBalanceCard(
    modifier: Modifier = Modifier,
    availablePercent: Int,
    availableBtc: Double,
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
        NcIcon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(id = WidgetR.drawable.ic_buffer_period),
            contentDescription = null,
        )
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            text = stringResource(
                id = R.string.nc_claim_available_balance,
                availablePercent,
                availableBtc.getBTCAmount()
            ),
            style = NunchukTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
        )
    }
}

private fun InheritancePlanStage.toReleaseScheduleStage(
    id: Int,
    stageNumber: Int,
): ReleaseScheduleStage {
    val cal = Calendar.getInstance().apply { timeInMillis = firstWithdrawalTimeMillis }
    return ReleaseScheduleStage(
        id = id,
        stageNumber = stageNumber,
        allocationPercent = totalStageAllocationPercentage,
        firstWithdrawalDate = ReleaseScheduleDate(
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH),
            year = cal.get(Calendar.YEAR),
        ),
        installmentConfig = ReleaseInstallmentConfig(
            installmentPercent = amountPerReleasePercentage,
            repeatEvery = repeatIntervalCount.coerceAtLeast(1),
            frequency = when (repeatInterval.uppercase()) {
                "DAILY" -> ReleaseInstallmentFrequency.DAILY
                "WEEKLY" -> ReleaseInstallmentFrequency.WEEKLY
                "MONTHLY" -> ReleaseInstallmentFrequency.MONTHLY
                else -> ReleaseInstallmentFrequency.ANNUALLY
            },
        ),
    )
}

private fun computeAvailablePercent(
    stages: List<InheritancePlanStage>,
    currentStageIndex: Int,
    currentInstallmentIndex: Int,
): Int {
    return stages.getOrNull(currentStageIndex)
        ?.expandedInstallments?.getOrNull(currentInstallmentIndex)
        ?.allocationPercentage ?: 0
}

// ─── Preview ────────────────────────────────────────────────────────────────

@PreviewLightDark
@Composable
private fun ClaimReleaseScheduleContentPreview() {
    ClaimReleaseScheduleContent(
        inheritanceAdditional = InheritanceAdditional(
            inheritance = null,
            balance = 1.0,
            availableToWithdraw = 0.05,
            bufferPeriodCountdown = null,
            currentStageIndex = 0,
            currentInstallmentIndex = 0,
            stages = listOf(
                InheritancePlanStage(
                    amountPerReleasePercentage = 5,
                    repeatInterval = "ANNUALLY",
                    repeatIntervalCount = 1,
                    totalStageAllocationPercentage = 20,
                    firstWithdrawalTimeMillis = 1843171200000, // 05/29/2028
                    expandedInstallments = listOf(
                        InheritancePlanExpandedInstallment(
                            index = 0,
                            withdrawalTimeMillis = 1843171200000,
                            allocationPercentage = 5
                        ),
                        InheritancePlanExpandedInstallment(
                            index = 1,
                            withdrawalTimeMillis = 1874707200000,
                            allocationPercentage = 10
                        ),
                        InheritancePlanExpandedInstallment(
                            index = 2,
                            withdrawalTimeMillis = 1906243200000,
                            allocationPercentage = 15
                        ),
                        InheritancePlanExpandedInstallment(
                            index = 3,
                            withdrawalTimeMillis = 1937865600000,
                            allocationPercentage = 20
                        ),
                    ),
                ),
                InheritancePlanStage(
                    amountPerReleasePercentage = 10,
                    repeatInterval = "ANNUALLY",
                    repeatIntervalCount = 2,
                    totalStageAllocationPercentage = 60,
                    firstWithdrawalTimeMillis = 1969401600000, // 05/29/2032
                    expandedInstallments = listOf(
                        InheritancePlanExpandedInstallment(
                            index = 0,
                            withdrawalTimeMillis = 1969401600000,
                            allocationPercentage = 30
                        ),
                        InheritancePlanExpandedInstallment(
                            index = 1,
                            withdrawalTimeMillis = 2032473600000,
                            allocationPercentage = 40
                        ),
                        InheritancePlanExpandedInstallment(
                            index = 2,
                            withdrawalTimeMillis = 2095632000000,
                            allocationPercentage = 50
                        ),
                        InheritancePlanExpandedInstallment(
                            index = 3,
                            withdrawalTimeMillis = 2158704000000,
                            allocationPercentage = 60
                        ),
                        InheritancePlanExpandedInstallment(
                            index = 4,
                            withdrawalTimeMillis = 2221862400000,
                            allocationPercentage = 70
                        ),
                        InheritancePlanExpandedInstallment(
                            index = 5,
                            withdrawalTimeMillis = 2284934400000,
                            allocationPercentage = 80
                        ),
                    ),
                ),
                InheritancePlanStage(
                    amountPerReleasePercentage = 10,
                    repeatInterval = "ANNUALLY",
                    repeatIntervalCount = 1,
                    totalStageAllocationPercentage = 20,
                    firstWithdrawalTimeMillis = 2379715200000, // 05/29/2045
                    expandedInstallments = listOf(
                        InheritancePlanExpandedInstallment(
                            index = 0,
                            withdrawalTimeMillis = 2379715200000,
                            allocationPercentage = 90
                        ),
                        InheritancePlanExpandedInstallment(
                            index = 1,
                            withdrawalTimeMillis = 2411251200000,
                            allocationPercentage = 100
                        ),
                    ),
                ),
            ),
        ),
    )
}
