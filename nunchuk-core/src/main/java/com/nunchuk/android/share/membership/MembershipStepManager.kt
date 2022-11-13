package com.nunchuk.android.share.membership

import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.usecase.membership.GetMembershipStepUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MembershipStepManager @Inject constructor(
   private val getMembershipStepUseCase: GetMembershipStepUseCase,
   private val applicationScope: CoroutineScope,
) {
    private var job: Job? = null

    // TODO Hai Save instance state
    var plan = MembershipPlan.IRON_HAND
        private set
    private val steps = hashMapOf<MembershipStep, MembershipStepFlow>()
    private val _stepDone = MutableStateFlow<Set<MembershipStep>>(emptySet())
    val stepDone = _stepDone.asStateFlow()

    var currentStep: MembershipStep? = null
        private set

    private val _remainingTime = MutableStateFlow(0)
    val remainingTime = _remainingTime.asStateFlow()

    init {
        initStep()
    }

    private fun initStep() {
        steps[MembershipStep.ADD_TAP_SIGNER_1] = MembershipStepFlow(totalStep = 8)
        steps[MembershipStep.ADD_TAP_SIGNER_2] = MembershipStepFlow(totalStep = 8)
        steps[MembershipStep.HONEY_ADD_TAP_SIGNER] = MembershipStepFlow(totalStep = 8)
        steps[MembershipStep.ADD_SEVER_KEY] = MembershipStepFlow(totalStep = 2)
        steps[MembershipStep.SETUP_KEY_RECOVERY] = MembershipStepFlow(totalStep = 1)
        steps[MembershipStep.CREATE_WALLET] = MembershipStepFlow(totalStep = 2)

        // TODO Hai
        steps[MembershipStep.HONEY_ADD_HARDWARE_KEY_1] = MembershipStepFlow(totalStep = 2)
        steps[MembershipStep.HONEY_ADD_HARDWARE_KEY_2] = MembershipStepFlow(totalStep = 2)
        steps[MembershipStep.SETUP_INHERITANCE] = MembershipStepFlow(totalStep = 2)

        _remainingTime.value = steps.values.sumOf { it.totalStep * 2 }
        _stepDone.value = emptySet()
    }

    private fun observerStep(plan: MembershipPlan) {
        job?.cancel()
        job = applicationScope.launch {
            getMembershipStepUseCase(plan)
                .map { it.getOrElse { emptyList() } }
                .collect { steps ->
                    if (steps.isEmpty()) {
                        initStep()
                    } else {
                        steps.forEach { step ->
                            if (step.isVerifyOrAddKey) markStepDone(step.step)
                            else addRequireStep(step.step)
                        }
                    }
                }
        }
    }

    fun setCurrentPlan(plan: MembershipPlan) {
        this.plan = plan
        initStep()
        observerStep(plan)
    }

    fun setCurrentStep(step: MembershipStep) {
        currentStep = step
    }

    private fun markStepDone(step: MembershipStep) {
        if (_stepDone.value.contains(step)) return
        val stepInfo = steps[step] ?: throw IllegalArgumentException("Not support $step")
        stepInfo.currentStep = stepInfo.totalStep
        _stepDone.value = _stepDone.value.toMutableSet().apply {
            add(step)
        }
        updateRemainTime()
    }

    private fun addRequireStep(step: MembershipStep) {
        val stepInfo = steps[step] ?: throw IllegalArgumentException("Not support $step")
        stepInfo.currentStep = 0
        _stepDone.value = _stepDone.value.toMutableSet().apply {
            remove(step)
        }
        updateRemainTime()
    }

    fun updateStep(isForward: Boolean) {
        val step = currentStep ?: return
        val stepInfo = steps[step] ?: throw IllegalArgumentException("Not support $step")
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
        return isConfigKeyDone() && _stepDone.value.contains(MembershipStep.ADD_SEVER_KEY)
    }

    fun isCreatedAssistedWalletDone() = isConfigRecoverKeyDone() && _stepDone.value.contains(MembershipStep.CREATE_WALLET)

    fun getRemainTimeBySteps(querySteps: List<MembershipStep>) =
        calculateRemainTime(steps.filter { it.key in querySteps }.values)

    private fun updateRemainTime() {
        _remainingTime.update {
            calculateRemainTime(steps.values)
        }
    }

    private fun calculateRemainTime(stepFlows: Collection<MembershipStepFlow>) =
        stepFlows.sumOf { (it.totalStep - it.currentStep).coerceAtLeast(0) * 2 }
}