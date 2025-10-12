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

package com.nunchuk.android.signer.tapsigner.intro

import android.nfc.tech.IsoDep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.BaseNfcUseCase
import com.nunchuk.android.core.domain.GetTapSignerStatusUseCase
import com.nunchuk.android.core.helper.CheckAssistedSignerExistenceHelper
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.TapSignerStatus
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.GetMasterSignerUseCase
import com.nunchuk.android.usecase.byzantine.GetReplaceSignerNameUseCase
import com.nunchuk.android.usecase.signer.GetSignerFromMasterSignerByIndexUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AddTapSignerIntroViewModel @Inject constructor(
    private val getTapSignerStatusUseCase: GetTapSignerStatusUseCase,
    private val membershipStepManager: MembershipStepManager,
    private val getMasterSignerUseCase: GetMasterSignerUseCase,
    private val checkAssistedSignerExistenceHelper: CheckAssistedSignerExistenceHelper,
    private val getReplaceSignerNameUseCase: GetReplaceSignerNameUseCase,
    private val getSignerFromMasterSignerByIndexUseCase: GetSignerFromMasterSignerByIndexUseCase,
) : ViewModel() {
    private val _event = MutableSharedFlow<AddTapSignerIntroEvent>()
    val event = _event.asSharedFlow()

    private val _replaceSignerName = MutableStateFlow("")

    init {
        checkAssistedSignerExistenceHelper.init(viewModelScope)
    }

    fun getReplaceSignerName(walletId: String) {
        viewModelScope.launch {
            getReplaceSignerNameUseCase(
                GetReplaceSignerNameUseCase.Params(
                    walletId = walletId,
                    signerType = SignerType.NFC
                )
            ).onSuccess {
                _replaceSignerName.value = it
            }
        }
    }

    fun onContinueClicked() {
        viewModelScope.launch {
            _event.emit(AddTapSignerIntroEvent.ContinueEventAddTapSigner)
        }
    }

    fun getTapSignerStatus(isoDep: IsoDep?) {
        isoDep ?: return
        viewModelScope.launch {
            _event.emit(AddTapSignerIntroEvent.Loading(true))
            val result = getTapSignerStatusUseCase(BaseNfcUseCase.Data(isoDep))
            _event.emit(AddTapSignerIntroEvent.Loading(false))
            if (result.isSuccess) {
                Timber.d("TapSigner auth delay ${result.getOrThrow().authDelayInSecond}")
                _event.emit(AddTapSignerIntroEvent.GetTapSignerStatusSuccess(result.getOrThrow()))
            } else {
                _event.emit(AddTapSignerIntroEvent.GetTapSignerStatusError(result.exceptionOrNull()))
            }
        }
    }

    fun getMasterSigner(masterSignerId: String) = viewModelScope.launch {
        if (masterSignerId.isEmpty()) return@launch
        getMasterSignerUseCase(masterSignerId).onSuccess {
            _event.emit(AddTapSignerIntroEvent.GetMasterSignerSuccess(it))
        }.onFailure {
            _event.emit(AddTapSignerIntroEvent.GetTapSignerStatusError(it))
        }
    }

    fun isKeyAddedToAssistedWallet(masterSignerId: String) =
        membershipStepManager.isKeyExisted(masterSignerId)

    fun getSignerName() = _replaceSignerName.value.ifEmpty {
        membershipStepManager.getInheritanceKeyName(isTapsigner = true)
    }

    fun isInAssistedWallet(masterSignerId: String) =
        checkAssistedSignerExistenceHelper.isInAssistedWallet(masterSignerId)

    fun getSignerModel(masterSignerId: String, signerIndex: Int) = viewModelScope.launch {
        if (masterSignerId.isEmpty()) return@launch
        getSignerFromMasterSignerByIndexUseCase(
            GetSignerFromMasterSignerByIndexUseCase.Param(
                masterSignerId = masterSignerId,
                index = signerIndex,
                walletType = WalletType.MULTI_SIG,
                addressType = AddressType.NATIVE_SEGWIT
            )
        ).onSuccess { singleSigner ->
            singleSigner?.let {
                _event.emit(AddTapSignerIntroEvent.ReturnSignerModel(it))
            } ?: run {
                _event.emit(AddTapSignerIntroEvent.GetTapSignerStatusError(Exception("Signer not found")))
            }
        }.onFailure {
            _event.emit(AddTapSignerIntroEvent.GetTapSignerStatusError(it))
        }
    }
}

sealed class AddTapSignerIntroEvent {
    data class Loading(val isLoading: Boolean) : AddTapSignerIntroEvent()
    data class GetTapSignerStatusSuccess(
        val status: TapSignerStatus
    ) : AddTapSignerIntroEvent()

    data class GetTapSignerStatusError(val e: Throwable?) : AddTapSignerIntroEvent()
    data object ContinueEventAddTapSigner : AddTapSignerIntroEvent()
    data class GetMasterSignerSuccess(val masterSigner: MasterSigner) : AddTapSignerIntroEvent()
    data class ReturnSignerModel(val singleSigner: SingleSigner) : AddTapSignerIntroEvent()
}