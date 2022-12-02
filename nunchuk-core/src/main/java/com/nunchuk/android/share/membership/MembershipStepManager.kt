package com.nunchuk.android.share.membership

import com.google.gson.Gson
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.MembershipStepInfo
import com.nunchuk.android.model.SignerExtra
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.membership.GetMembershipStepUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MembershipStepManager @Inject constructor(
    private val getMembershipStepUseCase: GetMembershipStepUseCase,
    private val applicationScope: CoroutineScope,
    private val gson: Gson,
    ncDataStore: NcDataStore
) {
    private var job: Job? = null
    private val _plan = MutableStateFlow(MembershipPlan.NONE)
    val plan: MembershipPlan
        get() = _plan.value
    private val steps = hashMapOf<MembershipStep, MembershipStepFlow>()
    private val _stepDone = MutableStateFlow<Set<MembershipStep>>(emptySet())
    val stepDone = _stepDone.asStateFlow()

    var currentStep: MembershipStep? = null
        private set

    private val _remainingTime = MutableStateFlow(0)
    val remainingTime = _remainingTime.asStateFlow()

    private val stepInfo = MutableStateFlow<List<MembershipStepInfo>>(emptyList())

    init {
        applicationScope.launch {
            ncDataStore.membershipPlan.zip(ncDataStore.assistedWalletPlan)
            { currentPlan, assistedWalletPlan ->
                currentPlan to assistedWalletPlan
            }.collect {
                _plan.value = it.first
                initStep(it.first, it.second)
                observerStep(it.first, it.second)
            }
        }
    }
    // Special case when set up wallet done and login in another device to setup inheritance we should mark all created wallet step to done
    private fun initStep(currentPlan: MembershipPlan, assistedWalletPlan: MembershipPlan) {
        steps.clear()
        if (currentPlan == MembershipPlan.IRON_HAND && currentPlan != assistedWalletPlan) {
            steps[MembershipStep.ADD_TAP_SIGNER_1] = MembershipStepFlow(totalStep = 8)
            steps[MembershipStep.ADD_TAP_SIGNER_2] = MembershipStepFlow(totalStep = 8)
            steps[MembershipStep.HONEY_ADD_TAP_SIGNER] = MembershipStepFlow(totalStep = 8)
            steps[MembershipStep.ADD_SEVER_KEY] = MembershipStepFlow(totalStep = 2)
            steps[MembershipStep.SETUP_KEY_RECOVERY] = MembershipStepFlow(totalStep = 1)
            steps[MembershipStep.CREATE_WALLET] = MembershipStepFlow(totalStep = 2)
        } else if (currentPlan == MembershipPlan.HONEY_BADGER && currentPlan != assistedWalletPlan) {
            steps[MembershipStep.HONEY_ADD_HARDWARE_KEY_1] = MembershipStepFlow(totalStep = 8)
            steps[MembershipStep.HONEY_ADD_HARDWARE_KEY_2] = MembershipStepFlow(totalStep = 8)
            steps[MembershipStep.ADD_SEVER_KEY] = MembershipStepFlow(totalStep = 2)
            steps[MembershipStep.SETUP_KEY_RECOVERY] = MembershipStepFlow(totalStep = 2)
            steps[MembershipStep.CREATE_WALLET] = MembershipStepFlow(totalStep = 2)
        }
        if (currentPlan == MembershipPlan.HONEY_BADGER) {
            steps[MembershipStep.SETUP_INHERITANCE] = MembershipStepFlow(totalStep = 12)
        }
        _remainingTime.value = steps.values.sumOf { it.totalStep * 2 }
        _stepDone.value = emptySet()
    }

    private fun observerStep(currentPlan: MembershipPlan, assistedWalletPlan: MembershipPlan) {
        job?.cancel()
        job = applicationScope.launch {
            getMembershipStepUseCase(currentPlan)
                .map { it.getOrElse { emptyList() } }
                .collect { steps ->
                    stepInfo.value = steps
                    if (steps.isEmpty()) {
                        initStep(currentPlan, assistedWalletPlan)
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
        currentStep = step
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

    fun isNotConfig() = _stepDone.value.isEmpty()

    fun isConfigKeyDone(): Boolean {
        return if (plan == MembershipPlan.IRON_HAND) {
            _stepDone.value.containsAll(
                listOf(
                    MembershipStep.ADD_TAP_SIGNER_1,
                    MembershipStep.ADD_TAP_SIGNER_2,
                    MembershipStep.ADD_SEVER_KEY
                )
            )
        } else {
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

    fun isConfigRecoverKeyDone(): Boolean {
        return isConfigKeyDone() && _stepDone.value.contains(MembershipStep.SETUP_KEY_RECOVERY)
    }

    fun isCreatedAssistedWalletDone() =
        isConfigRecoverKeyDone() && _stepDone.value.contains(MembershipStep.CREATE_WALLET)

    fun getRemainTimeBySteps(querySteps: List<MembershipStep>) =
        calculateRemainTime(steps.filter { it.key in querySteps }.values)

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

    private fun updateRemainTime() {
        _remainingTime.update {
            calculateRemainTime(steps.filter { isStepInThisPlan(it.key, plan) }.values)
        }
    }

    private fun calculateRemainTime(stepFlows: Collection<MembershipStepFlow>) =
        stepFlows.sumOf { (it.totalStep - it.currentStep).coerceAtLeast(0) * 2 }

    private fun isStepInThisPlan(step: MembershipStep, plan: MembershipPlan): Boolean {
        return when (plan) {
            MembershipPlan.IRON_HAND ->
                step == MembershipStep.CREATE_WALLET
                        || step == MembershipStep.ADD_SEVER_KEY
                        || step == MembershipStep.ADD_TAP_SIGNER_1
                        || step == MembershipStep.ADD_TAP_SIGNER_2
            MembershipPlan.HONEY_BADGER ->
                step == MembershipStep.CREATE_WALLET
                        || step == MembershipStep.ADD_SEVER_KEY
                        || step == MembershipStep.HONEY_ADD_TAP_SIGNER
                        || step == MembershipStep.HONEY_ADD_HARDWARE_KEY_1
                        || step == MembershipStep.HONEY_ADD_HARDWARE_KEY_2
                        || step == MembershipStep.SETUP_INHERITANCE
            MembershipPlan.NONE -> false
        }
    }
}