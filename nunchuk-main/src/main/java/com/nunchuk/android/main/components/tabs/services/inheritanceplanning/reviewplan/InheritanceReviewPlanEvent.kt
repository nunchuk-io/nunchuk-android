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

import com.nunchuk.android.model.CalculateRequiredSignatures
import com.nunchuk.android.model.Period
import com.nunchuk.android.model.byzantine.AssistedWalletRole

sealed class InheritanceReviewPlanEvent {
    data class Loading(val loading: Boolean) : InheritanceReviewPlanEvent()
    data class CalculateRequiredSignaturesSuccess(
        val type: String,
        val walletId: String,
        val userData: String,
        val requiredSignatures: Int,
        val dummyTransactionId: String
    ) : InheritanceReviewPlanEvent()

    data class ProcessFailure(val message: String) : InheritanceReviewPlanEvent()
    object CreateOrUpdateInheritanceSuccess : InheritanceReviewPlanEvent()
    object CancelInheritanceSuccess : InheritanceReviewPlanEvent()
    object MarkSetupInheritance : InheritanceReviewPlanEvent()
}

data class InheritanceReviewPlanState(
    val activationDate: Long = 0,
    val note: String = "",
    val isNotifyToday: Boolean = false,
    val emails: List<String> = emptyList(),
    val userData: String? = null,
    val walletId: String? = null,
    val walletName: String? = null,
    val isCreateOrUpdateFlow: Boolean = true,
    val isDataChanged: Boolean = false,
    val bufferPeriod: Period? = null,
    val requiredSignature: CalculateRequiredSignatures = CalculateRequiredSignatures(),
    val dummyTransactionId: String = "",
    val currentUserRole: String = AssistedWalletRole.NONE.name,
)