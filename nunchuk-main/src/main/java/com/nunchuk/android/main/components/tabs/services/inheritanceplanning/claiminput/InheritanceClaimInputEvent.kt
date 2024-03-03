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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claiminput

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.InheritanceAdditional

sealed class InheritanceClaimInputEvent {
    data class Loading(val isLoading: Boolean) : InheritanceClaimInputEvent()
    data class Error(val message: String) : InheritanceClaimInputEvent()
    data object SubscriptionExpired : InheritanceClaimInputEvent()
    data class InActivated(val message: String) : InheritanceClaimInputEvent()
    data class GetInheritanceStatusSuccess(
        val inheritanceAdditional: InheritanceAdditional,
        val signers: List<SignerModel>,
        val magic: String,
        val derivationPaths: List<String>
    ) :
        InheritanceClaimInputEvent()
}

data class InheritanceClaimInputState(
    private var _magicalPhrase: String = "",
    var _backupPasswords: List<String> = arrayListOf("",""),
    val suggestions: List<String> = emptyList()
) {
    var magicalPhrase: String
        get() = _magicalPhrase.trim()
        set(value) {
            _magicalPhrase = value
        }

    var backupPasswords: List<String>
        get() = _backupPasswords.filter { it.isNotBlank() }.map { it.trim() }
        set(value) {
            _backupPasswords = value
        }
}