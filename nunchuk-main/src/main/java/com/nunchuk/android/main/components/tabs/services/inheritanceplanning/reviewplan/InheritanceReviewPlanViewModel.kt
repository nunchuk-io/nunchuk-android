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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.membership.CalculateRequiredSignaturesInheritanceUseCase
import com.nunchuk.android.core.domain.membership.CancelInheritanceUseCase
import com.nunchuk.android.core.domain.membership.CancelInheritanceUserDataUseCase
import com.nunchuk.android.core.domain.membership.CreateOrUpdateInheritanceUseCase
import com.nunchuk.android.core.domain.membership.GetInheritanceUserDataUseCase
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningParam
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritanceBufferPeriodApplyType
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritanceReleaseMethodType
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritanceSetupFlowType
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.fallbacksettings.FallbackTriggerUnit
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.fallbacksettings.InheritanceFallbackOption
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseInstallmentFrequency
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleDate
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleTime
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleUiState
import com.nunchuk.android.model.CalculateRequiredSignatures
import com.nunchuk.android.model.CalculateRequiredSignaturesAction
import com.nunchuk.android.model.VerificationType
import com.nunchuk.android.model.inheritance.InheritancePlanBeneficiary
import com.nunchuk.android.model.inheritance.InheritancePlanExpandedInstallment
import com.nunchuk.android.model.inheritance.InheritancePlanStage
import com.nunchuk.android.model.inheritance.InheritanceDistributionMethod
import com.nunchuk.android.model.inheritance.InheritancePlanFallbackPolicy
import com.nunchuk.android.model.inheritance.InheritanceNotificationSettings
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.usecase.byzantine.GetGroupRemoteUseCase
import com.nunchuk.android.usecase.membership.MarkSetupInheritanceUseCase
import com.nunchuk.android.utils.ByzantineGroupUtils
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class InheritanceReviewPlanViewModel @Inject constructor(
    private val calculateRequiredSignaturesInheritanceUseCase: CalculateRequiredSignaturesInheritanceUseCase,
    private val getInheritanceUserDataUseCase: GetInheritanceUserDataUseCase,
    private val cancelInheritanceUserDataUseCase: CancelInheritanceUserDataUseCase,
    private val createOrUpdateInheritanceUseCase: CreateOrUpdateInheritanceUseCase,
    private val cancelInheritanceUseCase: CancelInheritanceUseCase,
    private val getWalletUseCase: GetWalletUseCase,
    private val membershipStepManager: MembershipStepManager,
    private val getGroupRemoteUseCase: GetGroupRemoteUseCase,
    private val byzantineGroupUtils: ByzantineGroupUtils,
    private val markSetupInheritanceUseCase: MarkSetupInheritanceUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private lateinit var param: InheritancePlanningParam.SetupOrReview
    private var initialParam: InheritancePlanningParam.SetupOrReview? = null

    internal val reviewFlow: ReviewFlow?
        get() = savedStateHandle.get<ReviewFlow>(EXTRA_REVIEW_FLOW)

    private val _event = MutableSharedFlow<InheritanceReviewPlanEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(InheritanceReviewPlanState())
    val state = _state.asStateFlow()

    private val _isContinueButtonEnabled = MutableStateFlow(true)
    val isContinueButtonEnabled = _isContinueButtonEnabled.asStateFlow()

    val remainTime = membershipStepManager.remainingTime

    fun init(
        param: InheritancePlanningParam.SetupOrReview,
        initialParam: InheritancePlanningParam.SetupOrReview? = null,
    ) {
        this.param = param
        if (this.initialParam == null) {
            this.initialParam = (initialParam ?: param).copy()
            getWalletName()
            if (param.groupId.isNotEmpty()) {
                getGroup()
            }
        }
        updateDerivedReviewState()
    }

    fun update(
        param: InheritancePlanningParam.SetupOrReview,
        initialParam: InheritancePlanningParam.SetupOrReview? = null,
    ) {
        this.param = param
        if (this.initialParam == null) {
            this.initialParam = (initialParam ?: param).copy()
        }
        updateDerivedReviewState()
    }

    private fun updateDerivedReviewState() {
        val initial = initialParam
        val isDataChanged = hasDataChanged()
        val changeHighlights = calculateReviewPlanChangeHighlights(
            initial = initial,
            current = param,
        )
        _state.update {
            it.copy(
                isDataChanged = isDataChanged,
                changeHighlights = changeHighlights,
            )
        }
        val shouldEnable = param.planFlow == InheritancePlanFlow.SETUP ||
            (param.planFlow == InheritancePlanFlow.VIEW && isDataChanged)
        _isContinueButtonEnabled.value = shouldEnable
    }

    private fun hasDataChanged(): Boolean {
        val initial = initialParam ?: return true
        return initial.activationDate != param.activationDate ||
                initial.selectedZoneId != param.selectedZoneId ||
                initial.emails != param.emails ||
                initial.isNotify != param.isNotify ||
                initial.note != param.note ||
                initial.bufferPeriod != param.bufferPeriod ||
                initial.bufferPeriodApplyType != param.bufferPeriodApplyType ||
                initial.releaseMethodType != param.releaseMethodType ||
                initial.beneficiaryAllocations != param.beneficiaryAllocations ||
                initial.individualScheduleConfigs.mapKeys { it.key.toEmailKey() } !=
                param.individualScheduleConfigs.mapKeys { it.key.toEmailKey() } ||
                initial.sharedScheduleConfig != param.sharedScheduleConfig ||
                initial.isSharedScheduleConfigured != param.isSharedScheduleConfigured ||
                initial.fallbackSettings != param.fallbackSettings ||
                !notificationSettingsEqual(initial.notificationSettings, param.notificationSettings)
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

    private fun getGroup() {
        viewModelScope.launch {
            getGroupRemoteUseCase(GetGroupRemoteUseCase.Params(param.groupId)).onSuccess { group ->
                val currentUserRole = byzantineGroupUtils.getCurrentUserRole(group)
                _state.update {
                    it.copy(currentUserRole = currentUserRole)
                }
            }
        }
    }

    private fun getWalletName() = viewModelScope.launch {
        getWalletUseCase.execute(param.walletId)
            .flowOn(Dispatchers.IO)
            .onException { _event.emit(InheritanceReviewPlanEvent.ProcessFailure(it.message.orUnknownError())) }
            .flowOn(Dispatchers.Main)
            .collect { wallet ->
                _state.update { state ->
                    state.copy(
                        walletId = param.walletId,
                        walletName = wallet.wallet.name
                    )
                }
            }
    }

    fun calculateRequiredSignatures(
        flow: ReviewFlow,
        releaseScheduleUiState: ReleaseScheduleUiState? = null,
    ) = viewModelScope.launch {
        savedStateHandle[EXTRA_REVIEW_FLOW] = flow
        val walletId = param.walletId
        val scheduleRequestData = buildScheduleRequestData(param, releaseScheduleUiState)
        _event.emit(InheritanceReviewPlanEvent.Loading(true))
        val resultCalculate = calculateRequiredSignaturesInheritanceUseCase(
            CalculateRequiredSignaturesInheritanceUseCase.Param(
                walletId = walletId,
                note = param.note,
                notificationEmails = param.emails.toList(),
                notifyToday = param.isNotify,
                activationTimeMilis = param.activationDate,
                bufferPeriodId = sharedBufferPeriod(param)?.id,
                action = if (flow == ReviewFlow.CREATE_OR_UPDATE) CalculateRequiredSignaturesAction.CREATE_OR_UPDATE else CalculateRequiredSignaturesAction.CANCEL,
                groupId = param.groupId,
                notificationPreferences = param.notificationSettings,
                timezone = param.selectedZoneId,
                distributionMethod = toDistributionMethod(param),
                beneficiaryMode = toBeneficiaryMode(param),
                bufferApplyOn = toBufferApplyOn(param),
                releaseMethod = toReleaseMethod(param),
                fallbackPolicy = toFallbackPolicy(param),
                stages = scheduleRequestData.stages,
                beneficiaries = scheduleRequestData.beneficiaries,
            )
        )
        val userData = getUserData(releaseScheduleUiState)
        _event.emit(InheritanceReviewPlanEvent.Loading(false))
        if (resultCalculate.isSuccess) {
            handleCalculateRequiredSignatures(resultCalculate.getOrThrow(), userData)
        } else {
            _event.emit(InheritanceReviewPlanEvent.ProcessFailure(resultCalculate.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    private suspend fun handleCalculateRequiredSignatures(
        signatures: CalculateRequiredSignatures,
        userData: String
    ) {
        if (reviewFlow == null) return
        if (signatures.type == VerificationType.SIGN_DUMMY_TX) {
            if (reviewFlow == ReviewFlow.CREATE_OR_UPDATE) {
                createOrUpdateInheritanceUseCase(
                    CreateOrUpdateInheritanceUseCase.Param(
                        signatures = hashMapOf(),
                        verifyToken = param.verifyToken,
                        userData = userData,
                        securityQuestionToken = "",
                        isUpdate = param.planFlow == InheritancePlanFlow.VIEW,
                        draft = true
                    )
                ).onSuccess { transactionId ->
                    _state.update {
                        it.copy(
                            dummyTransactionId = transactionId,
                            requiredSignature = signatures
                        )
                    }
                    _event.emit(
                        InheritanceReviewPlanEvent.CalculateRequiredSignaturesSuccess(
                            type = signatures.type,
                            walletId = param.walletId,
                            userData = userData,
                            requiredSignatures = signatures.requiredSignatures,
                            dummyTransactionId = transactionId
                        )
                    )
                }.onFailure {
                    _event.emit(InheritanceReviewPlanEvent.ProcessFailure(it.message.orUnknownError()))
                }
            } else {
                cancelInheritanceUseCase(
                    CancelInheritanceUseCase.Param(
                        signatures = hashMapOf(),
                        verifyToken = param.verifyToken,
                        userData = userData,
                        securityQuestionToken = "",
                        walletId = param.walletId,
                        draft = true
                    )
                ).onSuccess { transactionId ->
                    _state.update {
                        it.copy(
                            dummyTransactionId = transactionId,
                            requiredSignature = signatures
                        )
                    }
                    _event.emit(
                        InheritanceReviewPlanEvent.CalculateRequiredSignaturesSuccess(
                            type = signatures.type,
                            walletId = param.walletId,
                            userData = userData,
                            requiredSignatures = signatures.requiredSignatures,
                            dummyTransactionId = transactionId
                        )
                    )
                }.onFailure {
                    _event.emit(InheritanceReviewPlanEvent.ProcessFailure(it.message.orUnknownError()))
                }
            }
        } else if (signatures.type == VerificationType.SECURITY_QUESTION) {
            _event.emit(
                InheritanceReviewPlanEvent.CalculateRequiredSignaturesSuccess(
                    type = signatures.type,
                    walletId = param.walletId,
                    userData = userData,
                    requiredSignatures = signatures.requiredSignatures,
                    dummyTransactionId = ""
                )
            )
        }
    }

    private suspend fun getUserData(
        releaseScheduleUiState: ReleaseScheduleUiState? = null,
    ): String {
        val scheduleRequestData = buildScheduleRequestData(param, releaseScheduleUiState)
        val resultUserData = if (reviewFlow == ReviewFlow.CANCEL) {
            cancelInheritanceUserDataUseCase(
                CancelInheritanceUserDataUseCase.Param(
                    walletId = param.walletId,
                    groupId = param.groupId
                )
            )
        } else {
            getInheritanceUserDataUseCase(
                GetInheritanceUserDataUseCase.Param(
                    walletId = param.walletId,
                    note = param.note,
                    notificationEmails = param.emails.toList(),
                    notifyToday = param.isNotify,
                    activationTimeMilis = param.activationDate,
                    bufferPeriodId = sharedBufferPeriod(param)?.id,
                    groupId = param.groupId,
                    notificationPreferences = param.notificationSettings,
                    timezone = param.selectedZoneId,
                    distributionMethod = toDistributionMethod(param),
                    beneficiaryMode = toBeneficiaryMode(param),
                    bufferApplyOn = toBufferApplyOn(param),
                    releaseMethod = toReleaseMethod(param),
                    fallbackPolicy = toFallbackPolicy(param),
                    stages = scheduleRequestData.stages,
                    beneficiaries = scheduleRequestData.beneficiaries,
                )
            )
        }
        val userData = resultUserData.getOrThrow()
        _state.update {
            it.copy(
                userData = userData,
                walletId = param.walletId
            )
        }
        return userData
    }

    fun handleFlow(
        signatures: HashMap<String, String>,
        securityQuestionToken: String
    ) {
        if (reviewFlow == null) return
        if (reviewFlow == ReviewFlow.CREATE_OR_UPDATE) {
            createOrUpdateInheritance(signatures, securityQuestionToken)
        } else {
            cancelInheritance(signatures, securityQuestionToken)
        }
    }

    private fun createOrUpdateInheritance(
        signatures: HashMap<String, String>,
        securityQuestionToken: String
    ) = viewModelScope.launch {
        val state = _state.value
        _event.emit(InheritanceReviewPlanEvent.Loading(true))
        val isUpdate = param.planFlow == InheritancePlanFlow.VIEW
        val result = createOrUpdateInheritanceUseCase(
            CreateOrUpdateInheritanceUseCase.Param(
                signatures = signatures,
                verifyToken = param.verifyToken,
                userData = state.userData.orEmpty(),
                securityQuestionToken = securityQuestionToken,
                isUpdate = isUpdate,
                draft = false
            )
        )
        _event.emit(InheritanceReviewPlanEvent.Loading(false))
        if (result.isSuccess) {
            _state.update { it.copy(isDataChanged = true) }
            markSetupInheritance()
            _event.emit(InheritanceReviewPlanEvent.CreateOrUpdateInheritanceSuccess)
        } else {
            _event.emit(InheritanceReviewPlanEvent.ProcessFailure(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun isDataChanged() = _state.value.isDataChanged

    private fun cancelInheritance(
        signatures: HashMap<String, String>,
        securityQuestionToken: String
    ) = viewModelScope.launch {
        val state = _state.value
        _event.emit(InheritanceReviewPlanEvent.Loading(true))
        val result = cancelInheritanceUseCase(
            CancelInheritanceUseCase.Param(
                signatures = signatures,
                verifyToken = param.verifyToken,
                userData = state.userData.orEmpty(),
                securityQuestionToken = securityQuestionToken,
                walletId = param.walletId,
                draft = false
            )
        )
        _event.emit(InheritanceReviewPlanEvent.Loading(false))
        if (result.isSuccess) {
            _state.update { it.copy(isDataChanged = true) }
            markSetupInheritance()
            _event.emit(InheritanceReviewPlanEvent.CancelInheritanceSuccess)
        } else {
            _event.emit(InheritanceReviewPlanEvent.ProcessFailure(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun markSetupInheritance() = viewModelScope.launch {
        markSetupInheritanceUseCase(
            MarkSetupInheritanceUseCase.Param(
                walletId = param.walletId,
                isSetupInheritance = reviewFlow == ReviewFlow.CREATE_OR_UPDATE
            )
        )
        _event.emit(InheritanceReviewPlanEvent.MarkSetupInheritance)
    }

    private data class ScheduleRequestData(
        val stages: List<InheritancePlanStage>?,
        val beneficiaries: List<InheritancePlanBeneficiary>?,
    )

    private fun buildScheduleRequestData(
        param: InheritancePlanningParam.SetupOrReview,
        releaseScheduleUiState: ReleaseScheduleUiState?,
    ): ScheduleRequestData {
        return when (param.setupFlowType) {
            InheritanceSetupFlowType.OLD_FLOW -> {
                ScheduleRequestData(stages = null, beneficiaries = null)
            }

            InheritanceSetupFlowType.SINGLE_BENEFICIARY -> {
                val effectiveSharedUiState =
                    releaseScheduleUiState ?: param.sharedScheduleConfig?.releaseScheduleUiState
                ScheduleRequestData(
                    stages = effectiveSharedUiState.toInheritancePlanStages(param.selectedZoneId),
                    beneficiaries = null,
                )
            }

            InheritanceSetupFlowType.MULTI_BENEFICIARY -> {
                val beneficiaries = param.beneficiaryAllocations.map { allocation ->
                    val emailKey = allocation.email.trim().lowercase()
                    val scheduleConfig = param.individualScheduleConfigs[emailKey]
                    InheritancePlanBeneficiary(
                        email = allocation.email,
                        assetPercentage = allocation.allocationPercent,
                        magic = allocation.magic,
                        note = allocation.note,
                        bufferPeriodId = scheduleConfig?.bufferPeriod?.id,
                        bufferApplyOn = scheduleConfig?.bufferPeriodApplyType?.toApiBufferApplyOn(),
                        bufferPeriod = scheduleConfig?.bufferPeriod,
                        stages = if (param.releaseMethodType == InheritanceReleaseMethodType.INDIVIDUAL_SCHEDULES) {
                            scheduleConfig?.releaseScheduleUiState
                                .toInheritancePlanStages(param.selectedZoneId)
                        } else {
                            emptyList()
                        },
                    )
                }

                val effectiveSharedUiState =
                    releaseScheduleUiState ?: param.sharedScheduleConfig?.releaseScheduleUiState
                val sharedStages = if (param.releaseMethodType == InheritanceReleaseMethodType.SHARED_SCHEDULE) {
                    effectiveSharedUiState.toInheritancePlanStages(param.selectedZoneId)
                } else {
                    null
                }

                ScheduleRequestData(stages = sharedStages, beneficiaries = beneficiaries)
            }
        }
    }

    private fun toDistributionMethod(param: InheritancePlanningParam.SetupOrReview): String {
        return when (param.setupFlowType) {
            InheritanceSetupFlowType.OLD_FLOW -> InheritanceDistributionMethod.LUMP_SUM
            InheritanceSetupFlowType.SINGLE_BENEFICIARY,
            InheritanceSetupFlowType.MULTI_BENEFICIARY -> InheritanceDistributionMethod.CUSTOMIZE
        }
    }

    private fun toBeneficiaryMode(param: InheritancePlanningParam.SetupOrReview): String {
        return if (param.setupFlowType == InheritanceSetupFlowType.MULTI_BENEFICIARY) {
            "MULTIPLE"
        } else {
            "SINGLE"
        }
    }

    private fun toBufferApplyOn(param: InheritancePlanningParam.SetupOrReview): String? {
        return when (sharedBufferApplyType(param)) {
            InheritanceBufferPeriodApplyType.FIRST_WITHDRAWAL_ONLY -> "FIRST_WITHDRAWAL"
            InheritanceBufferPeriodApplyType.EVERY_WITHDRAWAL -> "EVERY_WITHDRAWAL"
            null -> null
        }
    }

    private fun toReleaseMethod(param: InheritancePlanningParam.SetupOrReview): String? {
        if (param.setupFlowType != InheritanceSetupFlowType.MULTI_BENEFICIARY) return null
        return when (param.releaseMethodType) {
            InheritanceReleaseMethodType.SHARED_SCHEDULE -> "SHARED"
            InheritanceReleaseMethodType.INDIVIDUAL_SCHEDULES -> "INDIVIDUAL"
        }
    }

    private fun sharedBufferPeriod(
        param: InheritancePlanningParam.SetupOrReview,
    ) = param.sharedScheduleConfig?.bufferPeriod ?: param.bufferPeriod

    private fun sharedBufferApplyType(
        param: InheritancePlanningParam.SetupOrReview,
    ) = param.sharedScheduleConfig?.bufferPeriodApplyType ?: param.bufferPeriodApplyType

    private fun toFallbackPolicy(param: InheritancePlanningParam.SetupOrReview): InheritancePlanFallbackPolicy? {
        val settings = param.fallbackSettings ?: return null
        return when (settings.selectedOption) {
            InheritanceFallbackOption.NO_FALLBACK -> {
                InheritancePlanFallbackPolicy(type = "NONE")
            }

            InheritanceFallbackOption.INACTIVITY_FALLBACK -> {
                val intervalMapping = mapFallbackTrigger(
                    unit = settings.triggerUnit,
                    value = settings.triggerValue.toIntOrNull()?.coerceAtLeast(1),
                )
                InheritancePlanFallbackPolicy(
                    type = "INACTIVITY",
                    inactivityInterval = intervalMapping.interval,
                    inactivityIntervalCount = intervalMapping.count,
                )
            }

            InheritanceFallbackOption.DATE_BASED_FALLBACK -> {
                InheritancePlanFallbackPolicy(
                    type = "DATE_BASED",
                    fallbackTimeMillis = parseDateToMillis(
                        date = settings.fallbackDate,
                        zoneId = param.selectedZoneId
                    ),
                )
            }
        }
    }

    private fun parseDateToMillis(date: String, zoneId: String): Long? {
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
        val actualZoneId = runCatching {
            if (zoneId.isBlank()) ZoneId.systemDefault() else ZoneId.of(zoneId)
        }.getOrDefault(ZoneId.systemDefault())

        return runCatching {
            LocalDate.parse(date, formatter)
                .atStartOfDay(actualZoneId)
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
    }

    private data class IntervalMapping(
        val interval: String,
        val count: Int?,
    )

    private fun mapFallbackTrigger(
        unit: FallbackTriggerUnit,
        value: Int?,
    ): IntervalMapping {
        val safeValue = value?.coerceAtLeast(1)
        return when (unit) {
            FallbackTriggerUnit.DAY -> IntervalMapping(interval = "DAY", count = safeValue)
            FallbackTriggerUnit.WEEK -> IntervalMapping(
                interval = "DAY",
                count = safeValue?.times(7),
            )
            FallbackTriggerUnit.MONTH -> IntervalMapping(interval = "MONTH", count = safeValue)
            FallbackTriggerUnit.YEAR -> IntervalMapping(interval = "YEAR", count = safeValue)
        }
    }

    private fun ReleaseScheduleUiState?.toInheritancePlanStages(fallbackZoneId: String): List<InheritancePlanStage> {
        val uiState = this ?: return emptyList()
        return uiState.stages
            .sortedBy { it.stageNumber }
            .map { stage ->
                val zoneId = stage.timeZoneId.ifBlank { fallbackZoneId }
                val firstWithdrawalTimeMillis = toEpochMillis(
                    date = stage.firstWithdrawalDate,
                    time = stage.firstWithdrawalTime,
                    zoneId = zoneId,
                )
                val baseAllocatedPercent = uiState.allocatedBeforeStage(stage.id)
                val installments = stage.buildInstallmentLines(baseAllocatedPercent = baseAllocatedPercent)
                    .map { line ->
                        InheritancePlanExpandedInstallment(
                            index = (line.order - 1).coerceAtLeast(0),
                            withdrawalTimeMillis = toEpochMillis(
                                date = line.availableBy,
                                time = stage.firstWithdrawalTime,
                                zoneId = zoneId,
                            ),
                            allocationPercentage = line.availablePercent,
                        )
                    }
                val repeatInterval =
                    stage.installmentConfig.frequency.toApiRepeatInterval(stage.installmentConfig.repeatEvery)

                InheritancePlanStage(
                    amountPerReleasePercentage = stage.installmentConfig.installmentPercent,
                    repeatInterval = repeatInterval.interval,
                    repeatIntervalCount = repeatInterval.count ?: 1,
                    totalStageAllocationPercentage = stage.allocationPercent,
                    firstWithdrawalTimeMillis = firstWithdrawalTimeMillis,
                    expandedInstallments = installments,
                )
            }
    }

    private fun ReleaseInstallmentFrequency.toApiRepeatInterval(repeatEvery: Int): IntervalMapping {
        val safeRepeatEvery = repeatEvery.coerceAtLeast(1)
        return when (this) {
            ReleaseInstallmentFrequency.DAILY -> IntervalMapping("DAY", safeRepeatEvery)
            ReleaseInstallmentFrequency.WEEKLY -> IntervalMapping("DAY", safeRepeatEvery * 7)
            ReleaseInstallmentFrequency.MONTHLY -> IntervalMapping("MONTH", safeRepeatEvery)
            ReleaseInstallmentFrequency.ANNUALLY -> IntervalMapping("YEAR", safeRepeatEvery)
        }
    }

    private fun InheritanceBufferPeriodApplyType.toApiBufferApplyOn(): String {
        return when (this) {
            InheritanceBufferPeriodApplyType.FIRST_WITHDRAWAL_ONLY -> "FIRST_WITHDRAWAL"
            InheritanceBufferPeriodApplyType.EVERY_WITHDRAWAL -> "EVERY_WITHDRAWAL"
        }
    }

    private fun toEpochMillis(
        date: ReleaseScheduleDate,
        time: ReleaseScheduleTime,
        zoneId: String,
    ): Long {
        val safeZoneId = runCatching {
            if (zoneId.isBlank()) ZoneId.systemDefault() else ZoneId.of(zoneId)
        }.getOrDefault(ZoneId.systemDefault())
        return LocalDateTime.of(
            date.year,
            date.month,
            date.day,
            time.hour,
            time.minute,
        ).atZone(safeZoneId).toInstant().toEpochMilli()
    }

    enum class ReviewFlow {
        CREATE_OR_UPDATE, CANCEL
    }

    companion object {
        private const val EXTRA_REVIEW_FLOW = "review_flow"
    }
}
