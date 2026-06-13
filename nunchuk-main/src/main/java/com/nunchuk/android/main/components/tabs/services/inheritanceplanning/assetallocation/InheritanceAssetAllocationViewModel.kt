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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.assetallocation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritanceBeneficiaryAllocation
import com.nunchuk.android.model.inheritance.InheritancePlanBeneficiary
import com.nunchuk.android.usecase.membership.InheritanceAssociateMagicUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InheritanceAssetAllocationViewModel @Inject constructor(
    private val inheritanceAssociateMagicUseCase: InheritanceAssociateMagicUseCase,
) : ViewModel() {
    private val _event = MutableSharedFlow<InheritanceAssetAllocationEvent>()
    val event = _event.asSharedFlow()

    /**
     * Re-associates magic phrases for the given beneficiaries so the edited/deleted email
     * reflects an up-to-date magic phrase before returning to the review screen.
     */
    fun associateMagic(
        walletId: String,
        groupId: String,
        allocations: List<InheritanceBeneficiaryAllocation>,
    ) = viewModelScope.launch {
        _event.emit(InheritanceAssetAllocationEvent.Loading(true))
        val result = inheritanceAssociateMagicUseCase(
            InheritanceAssociateMagicUseCase.Param(
                walletId = walletId,
                groupId = groupId.takeIf { it.isNotEmpty() },
                beneficiaries = allocations.map {
                    InheritancePlanBeneficiary(
                        email = it.email,
                        assetPercentage = it.allocationPercent,
                        magic = it.magic,
                        note = it.note,
                    )
                },
            )
        )
        _event.emit(InheritanceAssetAllocationEvent.Loading(false))
        if (result.isSuccess) {
            val beneficiaries = result.getOrThrow()
            _event.emit(
                InheritanceAssetAllocationEvent.AssociateMagicSuccess(
                    allocations = beneficiaries.map { beneficiary ->
                        InheritanceBeneficiaryAllocation(
                            email = beneficiary.email,
                            allocationPercent = beneficiary.assetPercentage,
                            magic = beneficiary.magic,
                            note = beneficiary.note,
                        )
                    }
                )
            )
        } else {
            _event.emit(
                InheritanceAssetAllocationEvent.Error(
                    result.exceptionOrNull()?.message.orUnknownError()
                )
            )
        }
    }
}

sealed class InheritanceAssetAllocationEvent {
    data class Loading(val loading: Boolean) : InheritanceAssetAllocationEvent()
    data class Error(val message: String) : InheritanceAssetAllocationEvent()
    data class AssociateMagicSuccess(
        val allocations: List<InheritanceBeneficiaryAllocation>,
    ) : InheritanceAssetAllocationEvent()
}
