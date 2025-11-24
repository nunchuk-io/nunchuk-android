/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2025 Nunchuk                                              *
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
import com.nunchuk.android.main.R
import com.nunchuk.android.model.OnChainReplaceKeyStep
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.WalletTimelock
import com.nunchuk.android.model.isAddInheritanceKey
import com.nunchuk.android.model.isTimelockStep

/**
 * Represents data for a single replace-key step within a card.
 */
data class ReplaceStepData(
    val signer: SignerModel? = null,
    val verifyType: VerifyType = VerifyType.NONE,
) {
    val isComplete: Boolean
        get() = signer != null || verifyType != VerifyType.NONE
}

/**
 * Replace-key flow card. Steps are ordered as [timelockStep, regularStep] when dual-slot.
 */
data class AddReplaceKeyOnChainData(
    val steps: List<OnChainReplaceKeyStep>,
    val stepDataMap: Map<OnChainReplaceKeyStep, ReplaceStepData> = emptyMap(),
    val fingerPrint: String = "",
    val originalSigners: List<SignerModel> = emptyList(),
    val originalTimelock: WalletTimelock? = null,
    val newTimelock: WalletTimelock? = null
) {

    val type: OnChainReplaceKeyStep
        get() = steps.lastOrNull() ?: OnChainReplaceKeyStep.HARDWARE_KEY

    /**
     * Gets the timelock step if this is a dual-slot card.
     */
    val timelockType: OnChainReplaceKeyStep?
        get() = if (steps.size >= 2) steps.first() else null

    /**
     * Checks if any step in this card has been verified.
     */
    val isVerifyOrAddKey: Boolean
        get() = stepDataMap.values.any { it.verifyType != VerifyType.NONE }

    /**
     * Gets the next step that needs to be added (timelock first, then regular).
     */
    fun getNextStepToAdd(): OnChainReplaceKeyStep? {
        return steps.firstOrNull { step ->
            stepDataMap[step]?.isComplete != true
        }
    }

    /**
     * Gets the next step after the given currentStep.
     */
    fun getNextStepToAdd(currentStep: OnChainReplaceKeyStep): OnChainReplaceKeyStep? {
        val currentIndex = steps.indexOf(currentStep)
        return if (currentIndex != -1 && currentIndex + 1 < steps.size) {
            steps[currentIndex + 1]
        } else {
            null
        }
    }

    /**
     * Gets all added signers (for UI display), ordered by step order.
     */
    fun getAllSigners(): List<SignerModel> {
        return steps.mapNotNull { step ->
            stepDataMap[step]?.signer
        }
    }

    /**
     * Gets signer for a specific step.
     */
    fun getSignerForStep(step: OnChainReplaceKeyStep): SignerModel? {
        return stepDataMap[step]?.signer
    }

    /**
     * Gets verify type for a specific step.
     */
    fun getVerifyTypeForStep(step: OnChainReplaceKeyStep): VerifyType {
        return stepDataMap[step]?.verifyType ?: VerifyType.NONE
    }

    /**
     * Updates the data for a specific step.
     */
    fun updateStep(
        step: OnChainReplaceKeyStep,
        signer: SignerModel?,
        verifyType: VerifyType,
    ): AddReplaceKeyOnChainData {
        val updatedMap = stepDataMap.toMutableMap()
        updatedMap[step] = ReplaceStepData(signer, verifyType)
        return copy(stepDataMap = updatedMap)
    }

    fun setOriginalTimelock(
        timelock: WalletTimelock
    ): AddReplaceKeyOnChainData {
        return copy(originalTimelock = timelock)
    }

    /**
     * Updates the new timelock configuration.
     */
    fun updateTimelock(
        newTimelock: WalletTimelock
    ): AddReplaceKeyOnChainData {
        return copy(newTimelock = newTimelock)
    }

    /**
     * Checks if this is a dual-slot card.
     */
    val hasDualSlots: Boolean
        get() = steps.size >= 2

    /**
     * Legacy support: Gets all signers as a list (for old code compatibility).
     */
    val replaceSigners: List<SignerModel>?
        get() = getAllSigners().takeIf { it.isNotEmpty() }

    /**
     * Legacy support: Gets overall verify type (returns APP_VERIFIED if any step complete).
     */
    val verifyType: VerifyType
        get() = if (isVerifyOrAddKey) VerifyType.APP_VERIFIED else VerifyType.NONE

    /**
     * Checks whether to show the acctX badge for this card.
     * Hide for timelock-only steps.
     */
    fun shouldShowAcctXBadge(): Boolean {
        return type.isTimelockStep.not()
    }

    /**
     * Checks if this card represents an inheritance key.
     */
    fun isInheritanceKey(): Boolean {
        return type.isAddInheritanceKey
    }

    fun isHasTimelock(): Boolean {
        return newTimelock != null || originalTimelock != null
    }

    fun isServerOrTimelockKey(): Boolean {
        return type == OnChainReplaceKeyStep.SERVER_KEY || type == OnChainReplaceKeyStep.TIMELOCK
    }
}

/**
 * Maps a regular step to its timelock counterpart.
 */
private fun OnChainReplaceKeyStep.getTimelockCounterpart(): OnChainReplaceKeyStep? {
    return when (this) {
        OnChainReplaceKeyStep.INHERITANCE_KEY -> OnChainReplaceKeyStep.INHERITANCE_KEY_TIMELOCK
        OnChainReplaceKeyStep.INHERITANCE_KEY_1 -> OnChainReplaceKeyStep.INHERITANCE_KEY_1_TIMELOCK
        OnChainReplaceKeyStep.HARDWARE_KEY -> OnChainReplaceKeyStep.HARDWARE_KEY_TIMELOCK
        OnChainReplaceKeyStep.HARDWARE_KEY_1 -> OnChainReplaceKeyStep.HARDWARE_KEY_1_TIMELOCK
        else -> null
    }
}

/**
 * Converts a flat list of steps to AddReplaceKeyOnChainData with dual-slot pairing.
 */
fun List<OnChainReplaceKeyStep>.toAddReplaceKeyOnChainDataList(): List<AddReplaceKeyOnChainData> {
    val result = mutableListOf<AddReplaceKeyOnChainData>()
    val processedSteps = mutableSetOf<OnChainReplaceKeyStep>()

    forEach { step ->
        if (processedSteps.contains(step)) return@forEach

        val timelockStep = step.getTimelockCounterpart()
        if (timelockStep != null) {
            // This is a regular step that has a timelock pair
            result.add(
                AddReplaceKeyOnChainData(
                    steps = listOf(timelockStep, step) // [timelock, regular]
                )
            )
            processedSteps.add(step)
            processedSteps.add(timelockStep)
        } else if (step.isTimelockStep) {
            // This is a timelock step - check if its regular counterpart exists
            val regularStep = this.find { it.getTimelockCounterpart() == step }
            if (regularStep != null && !processedSteps.contains(regularStep)) {
                result.add(
                    AddReplaceKeyOnChainData(
                        steps = listOf(step, regularStep) // [timelock, regular]
                    )
                )
                processedSteps.add(step)
                processedSteps.add(regularStep)
            } else if (!processedSteps.contains(step)) {
                // Timelock step without regular pair (shouldn't happen, but handle gracefully)
                result.add(AddReplaceKeyOnChainData(steps = listOf(step)))
                processedSteps.add(step)
            }
        } else {
            // Single step
            result.add(AddReplaceKeyOnChainData(steps = listOf(step)))
            processedSteps.add(step)
        }
    }

    return result
}

val OnChainReplaceKeyStep.resId: Int
    get() {
        return when (this) {
            OnChainReplaceKeyStep.SERVER_KEY -> R.drawable.ic_server_key_dark
            OnChainReplaceKeyStep.TIMELOCK -> R.drawable.ic_timer
            OnChainReplaceKeyStep.INHERITANCE_KEY,
            OnChainReplaceKeyStep.INHERITANCE_KEY_TIMELOCK,
            OnChainReplaceKeyStep.INHERITANCE_KEY_1,
            OnChainReplaceKeyStep.INHERITANCE_KEY_1_TIMELOCK,
            OnChainReplaceKeyStep.HARDWARE_KEY,
            OnChainReplaceKeyStep.HARDWARE_KEY_TIMELOCK,
            OnChainReplaceKeyStep.HARDWARE_KEY_1,
            OnChainReplaceKeyStep.HARDWARE_KEY_1_TIMELOCK -> R.drawable.ic_hardware_key
            else -> 0
        }
    }

fun OnChainReplaceKeyStep.getButtonText(context: Context): String {
    return when (this) {
        OnChainReplaceKeyStep.SERVER_KEY, OnChainReplaceKeyStep.TIMELOCK -> context.getString(R.string.nc_configure)
        else -> context.getString(R.string.nc_add)
    }
}


