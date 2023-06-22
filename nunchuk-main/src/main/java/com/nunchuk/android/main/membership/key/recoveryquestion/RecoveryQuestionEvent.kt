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

package com.nunchuk.android.main.membership.key.recoveryquestion

import com.nunchuk.android.main.membership.model.SecurityQuestionModel

sealed class RecoveryQuestionEvent {
    data class Loading(val isLoading: Boolean) : RecoveryQuestionEvent()
    object ContinueStepEvent : RecoveryQuestionEvent()
    object ConfigRecoveryQuestionSuccess : RecoveryQuestionEvent()

    data class GetSecurityQuestionSuccess(
        val questions: List<SecurityQuestionModel>,
    ) : RecoveryQuestionEvent()

    data class CalculateRequiredSignaturesSuccess(
        val walletId: String,
        val userData: String,
        val requiredSignatures: Int,
        val type: String,
    ) : RecoveryQuestionEvent()

    data class ShowError(val message: String) : RecoveryQuestionEvent()
    object RecoveryQuestionUpdateSuccess : RecoveryQuestionEvent()
    object DiscardChangeClick : RecoveryQuestionEvent()
}

data class RecoveryQuestionState(
    val recoveries: List<RecoveryData> = emptyList(),
    val securityQuestions: List<SecurityQuestionModel> = emptyList(),
    val interactQuestionIndex: Int = InitValue,
    val userData: String? = null,
    val clearFocusRequest: Boolean = false
) {
    companion object {
        val Empty = RecoveryQuestionState()
        const val InitValue = -1
    }
}

data class RecoveryData(
    val index: Int,
    val question: SecurityQuestionModel = SecurityQuestionModel(),
    val answer: String = "",
    val change: Boolean = false,
    val isShowMask: Boolean = false
)