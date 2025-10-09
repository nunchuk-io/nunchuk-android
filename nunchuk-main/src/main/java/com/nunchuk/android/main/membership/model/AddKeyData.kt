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
import com.nunchuk.android.model.VerifyType

data class AddKeyData(
    val type: MembershipStep,
    val signer: SignerModel? = null,
    val verifyType: VerifyType = VerifyType.NONE
) {
    val isVerifyOrAddKey: Boolean
        get() = signer != null || verifyType != VerifyType.NONE
}

/**
 * Represents data for a single step within a card
 */
data class StepData(
    val signer: SignerModel? = null,
    val verifyType: VerifyType = VerifyType.NONE
) {
    val isComplete: Boolean
        get() = signer != null || verifyType != VerifyType.NONE
}

/**
 * Represents a card that can contain one or more membership steps
 * For MINISCRIPT: Contains 2 steps [timelockStep, regularStep]
 * For MULTI_SIG: Contains 1 step [regularStep]
 */
data class AddKeyOnChainData(
    val steps: List<MembershipStep>, // Ordered: [timelockStep, regularStep] or [regularStep]
    val stepDataMap: Map<MembershipStep, StepData> = emptyMap()
) {
    /**
     * Gets the primary step for this card (used for display label)
     * For dual-slot: returns the regular (non-timelock) step
     * For single-slot: returns the only step
     */
    val type: MembershipStep
        get() = steps.lastOrNull() ?: MembershipStep.ADD_SEVER_KEY
    
    /**
     * Gets the timelock step if this is a dual-slot card
     */
    val timelockType: MembershipStep?
        get() = if (steps.size >= 2) steps.first() else null
    
    /**
     * Checks if all required steps in this card are complete
     */
    val isVerifyOrAddKey: Boolean
        get() = steps.all { step ->
            stepDataMap[step]?.isComplete == true
        }
    
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
    fun updateStep(step: MembershipStep, signer: SignerModel?, verifyType: VerifyType): AddKeyOnChainData {
        val updatedMap = stepDataMap.toMutableMap()
        updatedMap[step] = StepData(signer, verifyType)
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
        } else if (step.name.contains("_TIMELOCK")) {
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

fun MembershipStep.getLabel(context: Context, isStandard: Boolean): String {
    val defaultKeyName = if (isStandard) {
        DEFAULT_KEY_NAME
    } else {
        HARDWARE_KEY_NAME
    }
    return when (this) {
        MembershipStep.IRON_ADD_HARDWARE_KEY_1 -> "$defaultKeyName #1"
        MembershipStep.IRON_ADD_HARDWARE_KEY_2 -> "$defaultKeyName #2"
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_0 -> "$defaultKeyName #1"
        MembershipStep.ADD_SEVER_KEY -> context.getString(R.string.nc_server_key)
        MembershipStep.TIMELOCK -> context.getString(R.string.nc_timelock)
        MembershipStep.HONEY_ADD_INHERITANCE_KEY, MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY -> "$defaultKeyName #1"
        MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY_1 -> "$defaultKeyName #2"
        MembershipStep.HONEY_ADD_HARDWARE_KEY_1, MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1 -> "$defaultKeyName #2"
        MembershipStep.HONEY_ADD_HARDWARE_KEY_2, MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_2 -> "$defaultKeyName #3"
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_3 -> "$defaultKeyName #4"
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_4 -> "$defaultKeyName #5"
        else -> ""
    }
}

fun MembershipStep.getButtonText(context: Context): String {
    return when (this) {
        MembershipStep.ADD_SEVER_KEY -> context.getString(R.string.nc_configure)
        else -> context.getString(R.string.nc_add)
    }
}