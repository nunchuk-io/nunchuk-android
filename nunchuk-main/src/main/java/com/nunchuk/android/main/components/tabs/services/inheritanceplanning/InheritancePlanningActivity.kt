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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.compose.ui.platform.ComposeView
import androidx.viewbinding.ViewBinding
import com.nunchuk.android.core.base.BaseShareSaveFileActivity
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.InheritanceSourceFlow
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.beneficiaryschedules.InheritanceBeneficiaryScheduleConfig
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.fallbacksettings.FallbackTriggerUnit
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.fallbacksettings.InheritanceFallbackOption
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.fallbacksettings.InheritanceFallbackSettingsValue
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseInstallmentConfig
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseInstallmentFrequency
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleDate
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleStage
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleTime
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleUiState
import com.nunchuk.android.model.Inheritance
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.model.inheritance.InheritanceDistributionMethod
import com.nunchuk.android.model.inheritance.InheritancePlanFallbackPolicy
import com.nunchuk.android.model.inheritance.InheritancePlanStage
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class InheritancePlanningActivity : BaseShareSaveFileActivity<ViewBinding>() {

    @Inject
    internal lateinit var membershipStepManager: MembershipStepManager

    private val viewModel by viewModels<InheritancePlanningViewModel>()
    private var planFlow: Int = InheritancePlanFlow.NONE
    private var startDestination: Int = START_DESTINATION_DEFAULT
    private var bottomSheetOptionListener: ((SheetOption) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        planFlow = intent.getIntExtra(EXTRA_INHERITANCE_PLAN_FLOW, InheritancePlanFlow.NONE)
        startDestination = intent.getIntExtra(EXTRA_START_DESTINATION, START_DESTINATION_DEFAULT)
        super.onCreate(savedInstanceState)

        val groupId = intent.getStringExtra(MembershipFragment.EXTRA_GROUP_ID).orEmpty()
        if (groupId.isEmpty()) {
            membershipStepManager.initStep("", GroupWalletType.TWO_OF_FOUR_MULTISIG)
        }
        membershipStepManager.setCurrentStep(MembershipStep.SETUP_INHERITANCE)

        when (planFlow) {
            InheritancePlanFlow.SETUP -> {
                viewModel.setOrUpdate(
                    InheritancePlanningParam.SetupOrReview(
                        planFlow = planFlow,
                        walletId = intent.getStringExtra(EXTRA_WALLET_ID).orEmpty(),
                        groupId = groupId,
                        sourceFlow = intent.getIntExtra(EXTRA_SOURCE_FLOW, InheritanceSourceFlow.NONE)
                    )
                )
            }

            InheritancePlanFlow.VIEW -> {
                val inheritance = intent.parcelable<Inheritance>(EXTRA_INHERITANCE) ?: return
                viewModel.setOrUpdate(
                    inheritance.toSetupOrReviewParamForView(
                        verifyToken = intent.getStringExtra(EXTRA_VERIFY_TOKEN).orEmpty(),
                        walletId = intent.getStringExtra(EXTRA_WALLET_ID).orEmpty(),
                        groupId = groupId,
                        sourceFlow = intent.getIntExtra(
                            EXTRA_SOURCE_FLOW,
                            InheritanceSourceFlow.NONE
                        ),
                        planFlow = planFlow,
                        dummyTransactionId = intent.getStringExtra(EXTRA_DUMMY_TRANSACTION_ID)
                            .orEmpty(),
                    ),
                )
            }
            InheritancePlanFlow.SIGN_DUMMY_TX -> {
                viewModel.setOrUpdate(
                    InheritancePlanningParam.SetupOrReview(
                        verifyToken = intent.getStringExtra(EXTRA_VERIFY_TOKEN).orEmpty(),
                        planFlow = planFlow,
                        walletId = intent.getStringExtra(EXTRA_WALLET_ID).orEmpty(),
                        groupId = groupId,
                        dummyTransactionId = intent.getStringExtra(EXTRA_DUMMY_TRANSACTION_ID)
                            .orEmpty()
                    )
                )
            }
            InheritancePlanFlow.REQUEST -> {
                viewModel.setOrUpdate(
                    InheritancePlanningParam.SetupOrReview(
                        walletId = intent.getStringExtra(EXTRA_WALLET_ID).orEmpty(),
                        groupId = groupId,
                    )
                )
            }
        }
        observer()
        observeEvent()
    }

    fun showSaveShareOption() {
        super.showSaveShareOption(false)
    }

    fun setBottomSheetOptionListener(listener: ((SheetOption) -> Unit)?) {
        bottomSheetOptionListener = listener
    }

    override fun onOptionClicked(option: SheetOption) {
        super.onOptionClicked(option)
        bottomSheetOptionListener?.invoke(option)
    }

    override fun shareFile() {
        viewModel.handleShareBsms()
    }

    override fun saveFileToLocal() {
        viewModel.saveBSMSToLocal()
    }

    private fun observer() {
        flowObserver(viewModel.state) {
            if (it.groupWalletType != null) {
                membershipStepManager.initStep(it.groupId, it.groupWalletType)
            }
        }
    }

    private fun observeEvent() {
        flowObserver(viewModel.event) { event ->
            handleEvent(event)
        }
    }

    private fun handleEvent(event: InheritancePlanningEvent) {
        when (event) {
            is InheritancePlanningEvent.Success -> shareFile(event)
            is InheritancePlanningEvent.Failure -> NCToastMessage(this).showWarning(event.message)
            is InheritancePlanningEvent.SaveLocalFile -> {
                showSaveFileState(event.isSuccess)
            }
        }
    }

    private fun shareFile(event: InheritancePlanningEvent.Success) {
        controller.shareFile(event.filePath)
    }

    override fun initializeBinding(): ViewBinding = ViewBinding {
        ComposeView(this).apply {
            setContent {
                InheritancePlanningGraph(
                    activity = this@InheritancePlanningActivity,
                    navigator = navigator,
                    activityViewModel = viewModel,
                    planFlow = planFlow,
                    startDestination = startDestination,
                )
            }
        }
    }.also {
        enableEdgeToEdge()
    }

    companion object {

        private const val EXTRA_INHERITANCE_PLAN_FLOW = "extra_inheritance_plan_flow"
        private const val EXTRA_VERIFY_TOKEN = "extra_verify_token"
        private const val EXTRA_INHERITANCE = "extra_inheritance"
        private const val EXTRA_SOURCE_FLOW = "extra_source_flow"
        const val EXTRA_WALLET_ID = "wallet_id"
        private const val EXTRA_DUMMY_TRANSACTION_ID = "dummy_transaction_id"
        private const val EXTRA_START_DESTINATION = "extra_start_destination"

        const val RESULT_REQUEST_PLANNING = "result_request_planning"
        const val START_DESTINATION_DEFAULT = 0
        const val START_DESTINATION_CREATE_SUCCESS = 1

        fun navigate(
            launcher: ActivityResultLauncher<Intent>? = null,
            walletId: String,
            activity: Context,
            verifyToken: String?,
            inheritance: Inheritance?,
            @InheritancePlanFlow.InheritancePlanFlowInfo flowInfo: Int,
            @InheritanceSourceFlow.InheritanceSourceFlowInfo sourceFlow: Int,
            groupId: String?,
            dummyTransactionId: String?,
            startDestination: Int = START_DESTINATION_DEFAULT,
        ) {
            val intent = Intent(activity, InheritancePlanningActivity::class.java)
                .putExtra(EXTRA_INHERITANCE_PLAN_FLOW, flowInfo)
                .putExtra(EXTRA_VERIFY_TOKEN, verifyToken)
                .putExtra(EXTRA_INHERITANCE, inheritance)
                .putExtra(EXTRA_SOURCE_FLOW, sourceFlow)
                .putExtra(EXTRA_WALLET_ID, walletId)
                .putExtra(MembershipFragment.EXTRA_GROUP_ID, groupId)
                .putExtra(EXTRA_DUMMY_TRANSACTION_ID, dummyTransactionId)
                .putExtra(EXTRA_START_DESTINATION, startDestination)
            if (launcher != null) {
                launcher.launch(intent)
            } else {
                activity.startActivity(intent)
            }
        }
    }
}

private const val DEFAULT_FALLBACK_DATE = "05/29/2050"
private val REVIEW_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")

private fun Inheritance.toSetupOrReviewParamForView(
    verifyToken: String,
    walletId: String,
    groupId: String,
    @InheritanceSourceFlow.InheritanceSourceFlowInfo sourceFlow: Int,
    @InheritancePlanFlow.InheritancePlanFlowInfo planFlow: Int,
    dummyTransactionId: String,
): InheritancePlanningParam.SetupOrReview {
    val setupFlowType = toSetupFlowTypeForView()
    val releaseMethodType = releaseMethod.toReleaseMethodType()
    val sharedStages = when (setupFlowType) {
        InheritanceSetupFlowType.OLD_FLOW -> emptyList()
        InheritanceSetupFlowType.SINGLE_BENEFICIARY -> {
            stages.ifEmpty { beneficiaries.firstOrNull()?.stages.orEmpty() }
        }
        InheritanceSetupFlowType.MULTI_BENEFICIARY -> {
            if (releaseMethodType == InheritanceReleaseMethodType.SHARED_SCHEDULE) {
                stages
            } else {
                emptyList()
            }
        }
    }
    val sharedBufferPeriod = when (setupFlowType) {
        InheritanceSetupFlowType.OLD_FLOW -> bufferPeriod
        InheritanceSetupFlowType.SINGLE_BENEFICIARY -> {
            bufferPeriod ?: beneficiaries.firstOrNull()?.bufferPeriod
        }
        InheritanceSetupFlowType.MULTI_BENEFICIARY -> {
            if (releaseMethodType == InheritanceReleaseMethodType.SHARED_SCHEDULE) {
                bufferPeriod
            } else {
                null
            }
        }
    }
    val sharedBufferApplyType = when (setupFlowType) {
        InheritanceSetupFlowType.OLD_FLOW -> bufferApplyOn.toBufferPeriodApplyType()
        InheritanceSetupFlowType.SINGLE_BENEFICIARY -> {
            bufferApplyOn.toBufferPeriodApplyType()
                ?: beneficiaries.firstOrNull()?.bufferApplyOn.toBufferPeriodApplyType()
        }
        InheritanceSetupFlowType.MULTI_BENEFICIARY -> {
            if (releaseMethodType == InheritanceReleaseMethodType.SHARED_SCHEDULE) {
                bufferApplyOn.toBufferPeriodApplyType()
            } else {
                null
            }
        }
    }
    val sharedScheduleConfig = if (
        setupFlowType != InheritanceSetupFlowType.OLD_FLOW &&
        (sharedStages.isNotEmpty() || sharedBufferPeriod != null || sharedBufferApplyType != null)
    ) {
        InheritanceBeneficiaryScheduleConfig(
            releaseScheduleUiState = sharedStages.toReleaseScheduleUiState(timezone),
            bufferPeriod = sharedBufferPeriod,
            bufferPeriodApplyType = sharedBufferApplyType,
        )
    } else {
        null
    }
    val individualScheduleConfigs = if (
        setupFlowType == InheritanceSetupFlowType.MULTI_BENEFICIARY &&
        releaseMethodType == InheritanceReleaseMethodType.INDIVIDUAL_SCHEDULES
    ) {
        beneficiaries.mapNotNull { beneficiary ->
            val applyType = beneficiary.bufferApplyOn.toBufferPeriodApplyType()
            val hasConfig =
                beneficiary.stages.isNotEmpty() || beneficiary.bufferPeriod != null || applyType != null
            if (!hasConfig) {
                null
            } else {
                beneficiary.email.trim().lowercase() to InheritanceBeneficiaryScheduleConfig(
                    releaseScheduleUiState = beneficiary.stages.toReleaseScheduleUiState(timezone),
                    bufferPeriod = beneficiary.bufferPeriod,
                    bufferPeriodApplyType = applyType,
                )
            }
        }.toMap()
    } else {
        emptyMap()
    }

    return InheritancePlanningParam.SetupOrReview(
        activationDate = activationTimeMilis,
        emails = notificationEmails,
        isNotify = notificationEmails.isNotEmpty(),
        magicalPhrase = magic,
        note = note,
        verifyToken = verifyToken,
        planFlow = planFlow,
        bufferPeriod = bufferPeriod,
        walletId = walletId,
        sourceFlow = sourceFlow,
        groupId = groupId,
        dummyTransactionId = dummyTransactionId,
        notificationSettings = notificationPreferences,
        inheritanceKeys = inheritanceKeys.map { it.xfp },
        selectedZoneId = timezone,
        setupFlowType = setupFlowType,
        releaseMethodType = releaseMethodType,
        beneficiaryAllocations = beneficiaries.map { beneficiary ->
            InheritanceBeneficiaryAllocation(
                email = beneficiary.email,
                allocationPercent = beneficiary.assetPercentage,
                magic = beneficiary.magic,
                note = beneficiary.note,
            )
        },
        sharedScheduleConfig = sharedScheduleConfig,
        individualScheduleConfigs = individualScheduleConfigs,
        isSharedScheduleConfigured = sharedScheduleConfig != null,
        bufferPeriodApplyType = sharedBufferApplyType,
        fallbackSettings = fallbackPolicy?.toFallbackSettingsValue(timezone),
    )
}

private fun Inheritance.toSetupFlowTypeForView(): InheritanceSetupFlowType {
    if (distributionMethod != InheritanceDistributionMethod.CUSTOMIZE) {
        return InheritanceSetupFlowType.OLD_FLOW
    }
    return if (beneficiaryMode.equals("MULTIPLE", ignoreCase = true)) {
        InheritanceSetupFlowType.MULTI_BENEFICIARY
    } else {
        InheritanceSetupFlowType.SINGLE_BENEFICIARY
    }
}

private fun String?.toReleaseMethodType(): InheritanceReleaseMethodType {
    return if (this.equals("INDIVIDUAL", ignoreCase = true)) {
        InheritanceReleaseMethodType.INDIVIDUAL_SCHEDULES
    } else {
        InheritanceReleaseMethodType.SHARED_SCHEDULE
    }
}

private fun String?.toBufferPeriodApplyType(): InheritanceBufferPeriodApplyType? {
    return when (this?.uppercase()) {
        "FIRST_WITHDRAWAL" -> InheritanceBufferPeriodApplyType.FIRST_WITHDRAWAL_ONLY
        "EVERY_WITHDRAWAL" -> InheritanceBufferPeriodApplyType.EVERY_WITHDRAWAL
        else -> null
    }
}

private fun List<InheritancePlanStage>.toReleaseScheduleUiState(
    timezoneId: String,
): ReleaseScheduleUiState {
    if (isEmpty()) return ReleaseScheduleUiState(stages = emptyList())
    val zoneId = timezoneId.toZoneIdOrDefault()
    return ReleaseScheduleUiState(
        stages = mapIndexed { index, stage ->
            val dateTime = Instant.ofEpochMilli(stage.firstWithdrawalTimeMillis).atZone(zoneId)
            val installmentConfig = stage.toReleaseInstallmentConfig()
            ReleaseScheduleStage(
                id = index + 1,
                stageNumber = index + 1,
                allocationPercent = stage.totalStageAllocationPercentage,
                firstWithdrawalDate = ReleaseScheduleDate(
                    month = dateTime.monthValue,
                    day = dateTime.dayOfMonth,
                    year = dateTime.year,
                ),
                firstWithdrawalTime = ReleaseScheduleTime(
                    hour = dateTime.hour,
                    minute = dateTime.minute,
                ),
                timeZoneId = zoneId.id,
                installmentConfig = installmentConfig,
            )
        }
    )
}

private fun InheritancePlanStage.toReleaseInstallmentConfig(): ReleaseInstallmentConfig {
    val normalizedRepeatInterval = repeatInterval.uppercase()
    val normalizedRepeatCount = repeatIntervalCount.coerceAtLeast(1)
    val frequency = when (normalizedRepeatInterval) {
        "DAY", "DAILY" -> {
            if (normalizedRepeatCount % 7 == 0) {
                ReleaseInstallmentFrequency.WEEKLY
            } else {
                ReleaseInstallmentFrequency.DAILY
            }
        }
        "WEEK", "WEEKLY" -> ReleaseInstallmentFrequency.WEEKLY
        "MONTH", "MONTHLY" -> ReleaseInstallmentFrequency.MONTHLY
        "YEAR", "YEARLY", "ANNUALLY" -> ReleaseInstallmentFrequency.ANNUALLY
        else -> ReleaseInstallmentFrequency.ANNUALLY
    }
    val repeatEvery = when {
        frequency == ReleaseInstallmentFrequency.WEEKLY &&
            (normalizedRepeatInterval == "DAY" || normalizedRepeatInterval == "DAILY") -> {
            (normalizedRepeatCount / 7).coerceAtLeast(1)
        }
        else -> normalizedRepeatCount
    }
    return ReleaseInstallmentConfig(
        installmentPercent = amountPerReleasePercentage,
        repeatEvery = repeatEvery,
        frequency = frequency,
    )
}

internal fun InheritancePlanFallbackPolicy.toFallbackSettingsValue(
    timezoneId: String,
): InheritanceFallbackSettingsValue {
    return when (type.uppercase()) {
        "NONE" -> InheritanceFallbackSettingsValue(
            selectedOption = InheritanceFallbackOption.NO_FALLBACK,
            triggerValue = "1",
            triggerUnit = FallbackTriggerUnit.YEAR,
            fallbackDate = DEFAULT_FALLBACK_DATE,
        )

        "DATE_BASED" -> InheritanceFallbackSettingsValue(
            selectedOption = InheritanceFallbackOption.DATE_BASED_FALLBACK,
            triggerValue = "1",
            triggerUnit = FallbackTriggerUnit.YEAR,
            fallbackDate = fallbackTimeMillis.toFallbackDateDisplay(timezoneId),
        )

        else -> {
            val (triggerUnit, triggerValue) = mapFallbackTrigger(
                inactivityInterval = inactivityInterval,
                inactivityIntervalCount = inactivityIntervalCount,
            )
            InheritanceFallbackSettingsValue(
                selectedOption = InheritanceFallbackOption.INACTIVITY_FALLBACK,
                triggerValue = triggerValue,
                triggerUnit = triggerUnit,
                fallbackDate = DEFAULT_FALLBACK_DATE,
            )
        }
    }
}

private fun mapFallbackTrigger(
    inactivityInterval: String?,
    inactivityIntervalCount: Int?,
): Pair<FallbackTriggerUnit, String> {
    val safeCount = inactivityIntervalCount?.coerceAtLeast(1) ?: 1
    return when (inactivityInterval?.uppercase()) {
        "YEAR" -> FallbackTriggerUnit.YEAR to safeCount.toString()
        "MONTH" -> FallbackTriggerUnit.MONTH to safeCount.toString()
        "DAY" -> {
            if (safeCount % 7 == 0) {
                FallbackTriggerUnit.WEEK to (safeCount / 7).coerceAtLeast(1).toString()
            } else {
                FallbackTriggerUnit.DAY to safeCount.toString()
            }
        }
        else -> FallbackTriggerUnit.YEAR to safeCount.toString()
    }
}

private fun Long?.toFallbackDateDisplay(timezoneId: String): String {
    if (this == null || this <= 0L) return DEFAULT_FALLBACK_DATE
    return Instant.ofEpochMilli(this)
        .atZone(timezoneId.toZoneIdOrDefault())
        .format(REVIEW_DATE_FORMATTER)
}

private fun String.toZoneIdOrDefault(): ZoneId {
    return runCatching {
        if (isBlank()) ZoneId.systemDefault() else ZoneId.of(this)
    }.getOrDefault(ZoneId.systemDefault())
}
