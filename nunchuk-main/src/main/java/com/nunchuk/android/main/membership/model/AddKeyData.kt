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

package com.nunchuk.android.main.membership.model

import android.content.Context
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.DEFAULT_KEY_NAME
import com.nunchuk.android.core.util.HARDWARE_KEY_NAME
import com.nunchuk.android.main.R
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.TimelockExtra
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.inheritance.ClaimOption
import com.nunchuk.android.model.inheritance.InheritanceKeyVerification
import com.nunchuk.android.model.inheritance.isResolved
import com.nunchuk.android.model.inheritance.isVerified
import com.nunchuk.android.model.isAddInheritanceKey
import com.nunchuk.android.model.isTimelockStep

data class AddKeyData(
    val type: MembershipStep,
    val signer: SignerModel? = null,
    val verifyType: VerifyType = VerifyType.NONE,
    /**
     * Off-chain inheritance key only: how the owner chose to pass it to their Beneficiary. Read
     * from the draft wallet rather than the local step, because the server tracks it per key.
     * Empty means the choice has not been made yet — the design's "sharing method not set" state.
     */
    val claimOptions: List<ClaimOption> = emptyList(),
    /** One record per entry in [claimOptions]; empty on a legacy plan. */
    val verifications: List<InheritanceKeyVerification> = emptyList(),
    /**
     * Whether the encrypted backup file has been uploaded for this key. The verification record
     * alone cannot tell "no backup yet" from "backup uploaded, not verified yet", and the row
     * distinguishes them: the first offers "Backup", the second "Verify backup".
     */
    val hasEncryptedBackupFile: Boolean = false,
    /**
     * Whether this slot is the inheritance key.
     *
     * The membership step is the reliable signal here: a freshly created draft comes back with an
     * empty `signers` list, so keying this off the draft alone left the row, its status line and
     * the distribution flow silently dead. The draft still supplies [claimOptions] and
     * [verifications] when it has the key.
     */
    val isInheritanceKey: Boolean = false,
) {
    val isVerifyOrAddKey: Boolean
        get() = signer != null || verifyType != VerifyType.NONE

    /** Whether the row carries the inheritance status line at all. */
    val showsClaimStatus: Boolean
        get() = isInheritanceKey && signer != null

    /** An inheritance key that is in place but whose sharing method still has to be picked. */
    val needsClaimOptions: Boolean
        get() = showsClaimStatus && claimOptions.isEmpty()

    /**
     * Whether [option] has been dealt with — verified or deliberately skipped. The server keeps one
     * record per method and can still hold records for a method the owner has since dropped, so the
     * chosen options — not [verifications] — decide what counts.
     */
    fun isClaimOptionResolved(option: ClaimOption): Boolean =
        verifications.any { it.method == option && it.isResolved }

    /** Whether [option] was actually checked. A skipped verification is resolved but not verified. */
    fun isClaimOptionVerified(option: ClaimOption): Boolean =
        verifications.any { it.method == option && it.isVerified }

    /** How many of the chosen sharing methods are verified (or deliberately skipped). */
    val resolvedClaimOptionCount: Int
        get() = claimOptions.count { isClaimOptionResolved(it) }

    /** True once every chosen sharing method has been dealt with. */
    val isClaimSettled: Boolean
        get() = claimOptions.isNotEmpty() && resolvedClaimOptionCount == claimOptions.size

    /** True only once every chosen sharing method has actually been verified. */
    val isClaimVerified: Boolean
        get() = claimOptions.isNotEmpty() && claimOptions.all { isClaimOptionVerified(it) }

    /**
     * An off-chain inheritance key whose chosen sharing method still owes a backup or its
     * verification — the state that keeps the row amber with an action on it.
     *
     * This outranks [verifyType]: the local step holds a single flag and goes green as soon as one
     * artifact is done, which would hide the second half of a "do both" key behind an "Added" tick.
     * A skipped verification does not satisfy it either; the owner can still come back and verify,
     * which is why skipping unblocks the wizard ([isInheritanceIncomplete]) without greening the row.
     */
    val needsClaimVerification: Boolean
        get() = showsClaimStatus && claimOptions.isNotEmpty() && !isClaimVerified

    /**
     * An off-chain inheritance key that still owes something the wizard insists on — a sharing
     * method, or an untouched verification of one it chose. The wallet cannot be configured until
     * it is settled; a deliberately skipped verification counts as settled.
     */
    val isInheritanceIncomplete: Boolean
        get() = needsClaimOptions || (showsClaimStatus && claimOptions.isNotEmpty() && !isClaimSettled)

    /** Whether the row is finished, i.e. renders green with a tick rather than an action. */
    val isRowComplete: Boolean
        get() = if (isInheritanceKey && claimOptions.isNotEmpty()) {
            isClaimVerified
        } else {
            verifyType != VerifyType.NONE
        }
}

/**
 * Represents data for a single step within a card
 */
data class StepData(
    val signer: SignerModel? = null,
    val verifyType: VerifyType = VerifyType.NONE,
    val timelock: TimelockExtra? = null
) {
    val isComplete: Boolean
        get() = signer != null || verifyType != VerifyType.NONE

    fun isTimelockComplete(): Boolean {
        return timelock?.value?.let { it > 0 } == true
    }
}


data class AddKeyOnChainData(
    val steps: List<MembershipStep>, // Ordered: [timelockStep, regularStep]
    val stepDataMap: Map<MembershipStep, StepData> = emptyMap()
) {

    val type: MembershipStep
        get() = steps.lastOrNull() ?: MembershipStep.ADD_SEVER_KEY

    /**
     * Gets the timelock step if this is a dual-slot card
     */
    val timelockType: MembershipStep?
        get() = if (steps.size >= 2) steps.first() else null

    /**
     * Checks if any step in this card has been verified
     */
    val isVerifyOrAddKey: Boolean
        get() = stepDataMap.values.any { it.verifyType != VerifyType.NONE }

    /**
     * Gets the next step that needs to be added (timelock first, then regular)
     * @return The next MembershipStep to add, or null if all are complete
     */
    fun getNextStepToAdd(): MembershipStep? {
        return steps.firstOrNull { step ->
            stepDataMap[step]?.isComplete != true
        }
    }

    /**
     * Gets the next step after the given currentStep
     * @param currentStep The step that was just completed
     * @return The next MembershipStep to add, or null if currentStep is the last step
     */
    fun getNextStepToAdd(currentStep: MembershipStep): MembershipStep? {
        val currentIndex = steps.indexOf(currentStep)
        return if (currentIndex != -1 && currentIndex + 1 < steps.size) {
            steps[currentIndex + 1]
        } else {
            null
        }
    }

    /**
     * Gets all added signers (for UI display), ordered by step order
     * @return List of signers
     */
    fun getAllSigners(): List<SignerModel> {
        return steps.mapNotNull { step ->
            stepDataMap[step]?.signer
        }
    }

    /**
     * Gets signer for a specific step
     */
    fun getSignerForStep(step: MembershipStep): SignerModel? {
        return stepDataMap[step]?.signer
    }

    /**
     * Gets verify type for a specific step
     */
    fun getVerifyTypeForStep(step: MembershipStep): VerifyType {
        return stepDataMap[step]?.verifyType ?: VerifyType.NONE
    }

    /**
     * Updates the data for a specific step
     */
    fun updateStep(
        step: MembershipStep,
        signer: SignerModel?,
        verifyType: VerifyType,
        timelock: TimelockExtra? = null
    ): AddKeyOnChainData {
        val updatedMap = stepDataMap.toMutableMap()
        updatedMap[step] = StepData(signer, verifyType, timelock)
        return copy(stepDataMap = updatedMap)
    }

    /**
     * Checks if this is a dual-slot card
     */
    val hasDualSlots: Boolean
        get() = steps.size >= 2

    /**
     * Legacy support: Gets all signers as a list (for old code compatibility)
     */
    val signers: List<SignerModel>?
        get() = getAllSigners().takeIf { it.isNotEmpty() }

    /**
     * Legacy support: Gets overall verify type (returns APP_VERIFIED if all steps complete)
     */
    val verifyType: VerifyType
        get() = if (isVerifyOrAddKey) VerifyType.APP_VERIFIED else VerifyType.NONE

    /**
     * Checks whether to show the acctX badge for this card
     * Badge should not be shown for ADD_SEVER_KEY or TIMELOCK steps
     */
    fun shouldShowAcctXBadge(): Boolean {
        return type != MembershipStep.ADD_SEVER_KEY && type != MembershipStep.TIMELOCK
    }

    /**
     * Checks if this card represents an inheritance key
     */
    fun isInheritanceKey(): Boolean {
        return type.isAddInheritanceKey
    }
}

/**
 * Maps a regular step to its timelock counterpart
 */
private fun MembershipStep.getTimelockStep(): MembershipStep? {
    return when (this) {
        MembershipStep.HONEY_ADD_INHERITANCE_KEY -> MembershipStep.HONEY_ADD_INHERITANCE_KEY_TIMELOCK
        MembershipStep.HONEY_ADD_HARDWARE_KEY_1 -> MembershipStep.HONEY_ADD_HARDWARE_KEY_1_TIMELOCK
        MembershipStep.HONEY_ADD_HARDWARE_KEY_2 -> MembershipStep.HONEY_ADD_HARDWARE_KEY_2_TIMELOCK
        MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY -> MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY_TIMELOCK
        MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY_1 -> MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY_1_TIMELOCK
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_0 -> MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_0_TIMELOCK
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1 -> MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1_TIMELOCK
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_2 -> MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_2_TIMELOCK
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_3 -> MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_3_TIMELOCK
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_4 -> MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_4_TIMELOCK
        else -> null
    }
}

/**
 * Converts a flat list of steps to AddKeyOnChainData with dual-slot pairing for MINISCRIPT
 * @param walletType The wallet type (MULTI_SIG or MINISCRIPT)
 * @return List of AddKeyOnChainData, with paired timelock steps for MINISCRIPT
 */
fun List<MembershipStep>.toAddKeyOnChainDataList(): List<AddKeyOnChainData> {
    // For MINISCRIPT, pair regular steps with their timelock counterparts
    val result = mutableListOf<AddKeyOnChainData>()
    val processedSteps = mutableSetOf<MembershipStep>()

    forEach { step ->
        if (processedSteps.contains(step)) return@forEach

        val timelockStep = step.getTimelockStep()
        if (timelockStep != null) {
            // This is a regular step that has a timelock pair
            result.add(
                AddKeyOnChainData(
                    steps = listOf(timelockStep, step) // [timelock, regular]
                )
            )
            processedSteps.add(step)
            processedSteps.add(timelockStep)
        } else if (step.isTimelockStep) {
            // This is a timelock step - check if its regular counterpart exists
            val regularStep = this.find { it.getTimelockStep() == step }
            if (regularStep != null && !processedSteps.contains(regularStep)) {
                result.add(
                    AddKeyOnChainData(
                        steps = listOf(step, regularStep) // [timelock, regular]
                    )
                )
                processedSteps.add(step)
                processedSteps.add(regularStep)
            } else if (!processedSteps.contains(step)) {
                // Timelock step without regular pair (shouldn't happen, but handle gracefully)
                result.add(AddKeyOnChainData(steps = listOf(step)))
                processedSteps.add(step)
            }
        } else {
            // Single step (like ADD_SEVER_KEY, TIMELOCK)
            result.add(AddKeyOnChainData(steps = listOf(step)))
            processedSteps.add(step)
        }
    }

    return result
}

val MembershipStep.resId: Int
    get() {
        return when (this) {
            MembershipStep.ADD_SEVER_KEY -> R.drawable.ic_server_key_dark
            MembershipStep.TIMELOCK -> R.drawable.ic_timer
            MembershipStep.HONEY_ADD_INHERITANCE_KEY,
            MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY,
            MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY_1,
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_0,
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1,
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_2,
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_3,
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_4,
            MembershipStep.IRON_ADD_HARDWARE_KEY_1,
            MembershipStep.IRON_ADD_HARDWARE_KEY_2,
            MembershipStep.HONEY_ADD_HARDWARE_KEY_1,
            MembershipStep.HONEY_ADD_HARDWARE_KEY_2 -> R.drawable.ic_hardware_key

            else -> 0
        }
    }

fun MembershipStep.getLabel(context: Context, isStandard: Boolean, isOnChain: Boolean = false): String {
    val defaultKeyName = if (isStandard) {
        DEFAULT_KEY_NAME
    } else {
        HARDWARE_KEY_NAME
    }
    return when (this) {
        MembershipStep.IRON_ADD_HARDWARE_KEY_1 -> "$defaultKeyName #1"
        MembershipStep.IRON_ADD_HARDWARE_KEY_2 -> "$defaultKeyName #2"
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_0 -> if (isOnChain) "$defaultKeyName #3" else "$defaultKeyName #1"
        MembershipStep.ADD_SEVER_KEY -> context.getString(R.string.nc_server_key)
        MembershipStep.TIMELOCK -> context.getString(R.string.nc_timelock)
        MembershipStep.HONEY_ADD_INHERITANCE_KEY, MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY -> if (isOnChain) defaultKeyName else "$defaultKeyName #1"
        MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY_1 -> "$defaultKeyName #2"
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1 -> if (isOnChain) "$defaultKeyName #4" else "$defaultKeyName #2"
        MembershipStep.HONEY_ADD_HARDWARE_KEY_1  -> "$defaultKeyName #2"
        MembershipStep.HONEY_ADD_HARDWARE_KEY_2, MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_2 -> "$defaultKeyName #3"
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_3 -> "$defaultKeyName #4"
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_4 -> "$defaultKeyName #5"
        else -> ""
    }
}

fun MembershipStep.getButtonText(context: Context): String {
    return when (this) {
        MembershipStep.ADD_SEVER_KEY, MembershipStep.TIMELOCK -> context.getString(R.string.nc_configure)
        else -> context.getString(R.string.nc_add)
    }
}