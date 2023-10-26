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

package com.nunchuk.android.share.membership

import com.google.gson.Gson
import com.nunchuk.android.core.domain.GetAssistedWalletsFlowUseCase
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.core.util.TAPSIGNER_INHERITANCE_NAME
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.MembershipStepInfo
import com.nunchuk.android.model.SignerExtra
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.model.membership.AssistedWalletBrief
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.membership.GetMembershipStepUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class MembershipStepManager @Inject constructor(
    private val getMembershipStepUseCase: GetMembershipStepUseCase,
    private val getAssistedWalletsFlowUseCase: GetAssistedWalletsFlowUseCase,
    private val applicationScope: CoroutineScope,
    private val gson: Gson,
    private val ncDataStore: NcDataStore
) {
    private var job: Job? = null
    private val _plan = MutableStateFlow(MembershipPlan.NONE)
    val plan: MembershipPlan
        get() = _plan.value
    private val steps = mutableMapOf<MembershipStep, MembershipStepFlow>()
    private val _stepDone = MutableStateFlow<Set<MembershipStep>>(emptySet())
    val stepDone = _stepDone.asStateFlow()

    var currentStep: MembershipStep? = null
        private set
    val assistedWallets = mutableListOf<AssistedWalletBrief>()

    private val _remainingTime = MutableStateFlow(0)
    val remainingTime = _remainingTime.asStateFlow()

    private val stepInfo = MutableStateFlow<List<MembershipStepInfo>>(emptyList())

    init {
        applicationScope.launch {
            getAssistedWalletsFlowUseCase(Unit)
                .map { it.getOrElse { emptyList() } }
                .collect { wallets ->
                    assistedWallets.apply {
                        clear()
                        addAll(wallets)
                    }
                }
        }
        applicationScope.launch {
            ncDataStore.membershipPlan.collect {
                _plan.value = it
            }
        }
        applicationScope.launch {
            ncDataStore.currentStep.filterNotNull().collect {
                Timber.d("currentStep $it")
                currentStep = it
            }
        }
    }

    // Special case when set up wallet done and login in another device to setup inheritance we should mark all created wallet step to done
    fun initStep(groupId: String, groupWalletType: GroupWalletType? = null) =
        synchronized(this) {
            steps.clear()
            val currentPlan = plan
            when (currentPlan) {
                MembershipPlan.IRON_HAND -> {
                    steps[MembershipStep.IRON_ADD_HARDWARE_KEY_1] =
                        MembershipStepFlow(totalStep = 8)
                    steps[MembershipStep.IRON_ADD_HARDWARE_KEY_2] =
                        MembershipStepFlow(totalStep = 8)
                    steps[MembershipStep.HONEY_ADD_TAP_SIGNER] = MembershipStepFlow(totalStep = 8)
                    steps[MembershipStep.ADD_SEVER_KEY] = MembershipStepFlow(totalStep = 2)
                    steps[MembershipStep.SETUP_KEY_RECOVERY] = MembershipStepFlow(totalStep = 1)
                    steps[MembershipStep.CREATE_WALLET] = MembershipStepFlow(totalStep = 2)
                }

                MembershipPlan.HONEY_BADGER -> {
                    steps[MembershipStep.HONEY_ADD_HARDWARE_KEY_1] =
                        MembershipStepFlow(totalStep = 8)
                    steps[MembershipStep.HONEY_ADD_HARDWARE_KEY_2] =
                        MembershipStepFlow(totalStep = 8)
                    steps[MembershipStep.ADD_SEVER_KEY] = MembershipStepFlow(totalStep = 2)
                    steps[MembershipStep.SETUP_KEY_RECOVERY] = MembershipStepFlow(totalStep = 2)
                    steps[MembershipStep.CREATE_WALLET] = MembershipStepFlow(totalStep = 2)
                    steps[MembershipStep.SETUP_INHERITANCE] = MembershipStepFlow(totalStep = 12)
                }

                MembershipPlan.BYZANTINE, MembershipPlan.BYZANTINE_PRO -> {
                    when (groupWalletType) {
                        GroupWalletType.TWO_OF_THREE -> {
                            steps[MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_0] =
                                MembershipStepFlow(totalStep = 8)
                            steps[MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1] =
                                MembershipStepFlow(totalStep = 8)
                            steps[MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_2] =
                                MembershipStepFlow(totalStep = 8)
                        }

                        GroupWalletType.THREE_OF_FIVE -> {
                            steps[MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_0] =
                                MembershipStepFlow(totalStep = 8)
                            steps[MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1] =
                                MembershipStepFlow(totalStep = 8)
                            steps[MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_2] =
                                MembershipStepFlow(totalStep = 8)
                            steps[MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_3] =
                                MembershipStepFlow(totalStep = 8)
                            steps[MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_4] =
                                MembershipStepFlow(totalStep = 8)
                        }

                        else -> {
                            steps[MembershipStep.BYZANTINE_ADD_TAP_SIGNER] =
                                MembershipStepFlow(totalStep = 8)
                            steps[MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1] =
                                MembershipStepFlow(totalStep = 8)
                            steps[MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_2] =
                                MembershipStepFlow(totalStep = 8)
                            steps[MembershipStep.ADD_SEVER_KEY] = MembershipStepFlow(totalStep = 2)
                        }
                    }
                    steps[MembershipStep.SETUP_KEY_RECOVERY] = MembershipStepFlow(totalStep = 2)
                    steps[MembershipStep.CREATE_WALLET] = MembershipStepFlow(totalStep = 2)
                }

                MembershipPlan.BYZANTINE_PREMIER -> {
                    steps[MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_0] =
                        MembershipStepFlow(totalStep = 8)
                    steps[MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1] =
                        MembershipStepFlow(totalStep = 8)
                    steps[MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_2] =
                        MembershipStepFlow(totalStep = 8)
                    steps[MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_3] =
                        MembershipStepFlow(totalStep = 8)
                }

                MembershipPlan.NONE -> Unit
            }
            _remainingTime.value = calculateRemainTime(steps.toMap().values)
            _stepDone.value = emptySet()
            observerStep(currentPlan, groupId)
        }

    private fun observerStep(currentPlan: MembershipPlan, groupId: String) {
        job?.cancel()
        job = applicationScope.launch {
            getMembershipStepUseCase(GetMembershipStepUseCase.Param(currentPlan, groupId))
                .map { it.getOrElse { emptyList() } }
                .collect { steps ->
                    stepInfo.value = steps
                    if (steps.isEmpty()) {
                        restart()
                    } else {
                        steps.forEach { step ->
                            if (step.isVerifyOrAddKey) markStepDone(step.step)
                            else addRequireStep(step.step)
                        }
                    }

                    _stepDone.value = steps.filter { it.isVerifyOrAddKey }.map { it.step }.toSet()
                }
        }
    }

    fun setCurrentStep(step: MembershipStep) {
        Timber.d("setCurrentStep")
        currentStep = step
        applicationScope.launch {
            ncDataStore.setCurrentStep(step)
        }
    }

    private fun markStepDone(step: MembershipStep) {
        val stepInfo = steps[step] ?: return
        stepInfo.currentStep = stepInfo.totalStep
        updateRemainTime()
    }

    private fun addRequireStep(step: MembershipStep) {
        val stepInfo = steps[step] ?: return
        stepInfo.currentStep = 0
        updateRemainTime()
    }

    fun restart() {
        synchronized(this) {
            steps.forEach { it.value.currentStep = 0 }
            updateRemainTime()
        }
    }

    fun updateStep(isForward: Boolean) {
        val step = currentStep ?: return
        val stepInfo = steps[step] ?: return
        if (_stepDone.value.contains(step)) return
        if (isForward) {
            stepInfo.currentStep = stepInfo.currentStep.inc().coerceAtMost(stepInfo.totalStep)
        } else {
            stepInfo.currentStep = stepInfo.currentStep.dec().coerceAtLeast(0)
        }
        updateRemainTime()
    }

    fun isNotConfig() = _stepDone.value.none { it != MembershipStep.SETUP_INHERITANCE }

    fun isConfigKeyDone(): Boolean {
        val isConfigKeyDone = when (plan) {
            MembershipPlan.IRON_HAND -> {
                _stepDone.value.containsAll(
                    listOf(
                        MembershipStep.IRON_ADD_HARDWARE_KEY_1,
                        MembershipStep.IRON_ADD_HARDWARE_KEY_2,
                        MembershipStep.ADD_SEVER_KEY
                    )
                )
            }

            else -> {
                _stepDone.value.containsAll(
                    listOf(
                        MembershipStep.HONEY_ADD_TAP_SIGNER,
                        MembershipStep.HONEY_ADD_HARDWARE_KEY_1,
                        MembershipStep.HONEY_ADD_HARDWARE_KEY_2,
                        MembershipStep.ADD_SEVER_KEY
                    )
                )
            }
        }
        return isConfigKeyDone
    }

    val isConfigRecoverKeyDone =
        ncDataStore.isSetupSecurityQuestion.stateIn(applicationScope, SharingStarted.Eagerly, false)

    fun isCreatedAssistedWalletDone(): Boolean {
        return isConfigRecoverKeyDone.value && _stepDone.value.contains(MembershipStep.CREATE_WALLET)
    }

    fun isSetupInheritanceDone(): Boolean {
        return isCreatedAssistedWalletDone() && assistedWallets.isNotEmpty() && assistedWallets.all { it.isSetupInheritance }
    }

    fun getRemainTimeBySteps(querySteps: List<MembershipStep>) =
        calculateRemainTime(steps.toMap().filter { it.key in querySteps }.values)

    fun getRemainTimeByOtherSteps(querySteps: List<MembershipStep>) =
        calculateRemainTime(steps.toMap().filter { it.key !in querySteps }.values)

    fun getTapSignerName() =
        if (currentStep == MembershipStep.HONEY_ADD_TAP_SIGNER) TAPSIGNER_INHERITANCE_NAME else "TAPSIGNER${
            getNextKeySuffixByType(SignerType.NFC)
        }"

    fun getNextKeySuffixByType(type: SignerType): String {
        val index = stepInfo.value.asSequence().mapNotNull {
            runCatching {
                gson.fromJson(
                    it.extraData,
                    SignerExtra::class.java
                )
            }.getOrNull()
        }.count { it.signerType == type }.inc()
        return if (index == 1) "" else " #${index}"
    }

    fun isKeyExisted(masterSignerId: String) =
        stepInfo.value.any { it.masterSignerId == masterSignerId }

    private fun updateRemainTime() = synchronized(this) {
        _remainingTime.update {
            calculateRemainTime(steps.toMap().values)
        }
    }

    private fun calculateRemainTime(stepFlows: Collection<MembershipStepFlow>) =
        stepFlows.sumOf { (it.totalStep - it.currentStep).coerceAtLeast(0) * DURATION_EACH_STEP }
            .roundToInt()

    companion object {
        private const val DURATION_EACH_STEP = 0.5
    }
}