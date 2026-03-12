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

package com.nunchuk.android.model

import android.os.Parcelable
import com.nunchuk.android.model.inheritance.InheritancePlanStage
import kotlinx.parcelize.Parcelize

@Parcelize
data class InheritanceAdditional(
    val inheritance: Inheritance?,
    val balance: Double,
    val availableToWithdraw: Double,
    val bufferPeriodCountdown: BufferPeriodCountdown?,
    val currentStageIndex: Int,
    val currentInstallmentIndex: Int,
    val stages: List<InheritancePlanStage> = emptyList(),
) : Parcelable

@Parcelize
data class BufferPeriodCountdown(
    val activationTimeMilis: Long,
    val bufferInterval: String,
    val bufferIntervalCount: Int,
    val remainingCount: Int,
    val remainingDisplayName: String,
    val claimableTimeMilis: Long = 0L
) : Parcelable