/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

package com.nunchuk.android.signer.mk4.intro

import android.nfc.NdefRecord
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.CreateMk4SignerUseCase
import com.nunchuk.android.core.domain.GetAppSettingUseCase
import com.nunchuk.android.core.domain.GetMk4SingersUseCase
import com.nunchuk.android.core.util.COLDCARD_DEFAULT_KEY_NAME
import com.nunchuk.android.core.util.SIGNER_PATH_PREFIX
import com.nunchuk.android.core.util.gson
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.MembershipStepInfo
import com.nunchuk.android.model.SignerExtra
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.signer.util.isTestNetPath
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.membership.SaveMembershipStepUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class Mk4IntroViewModel @Inject constructor(
    private val getMk4SingersUseCase: GetMk4SingersUseCase,
    private val createMk4SignerUseCase: CreateMk4SignerUseCase,
    private val saveMembershipStepUseCase: SaveMembershipStepUseCase,
    private val membershipStepManager: MembershipStepManager,
    private val getAppSettingUseCase: GetAppSettingUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _event = MutableSharedFlow<Mk4IntroViewEvent>()
    private val args: Mk4IntroFragmentArgs =
        Mk4IntroFragmentArgs.fromSavedStateHandle(savedStateHandle)
    val event = _event.asSharedFlow()

    val remainTime = membershipStepManager.remainingTime
    private var chain: Chain = Chain.MAIN

    private val _mk4Signers = mutableListOf<SingleSigner>()

    init {
        viewModelScope.launch {
            chain = getAppSettingUseCase.execute().first().chain
        }
    }

    val mk4Signers: List<SingleSigner>
        get() = _mk4Signers

    fun getMk4Signer(records: List<NdefRecord>) {
        viewModelScope.launch {
            _event.emit(Mk4IntroViewEvent.Loading(true))
            val result = getMk4SingersUseCase(records.toTypedArray())
            if (result.isSuccess) {
                if (args.isMembershipFlow) {
                    val sortedSigner = result.getOrThrow().sortedBy { it.derivationPath }
                    val signer =
                        sortedSigner.find { it.derivationPath == if (chain == Chain.MAIN) SIGNER_PATH else SIGNER_TESTNET_PATH }
                            ?: run {
                                sortedSigner.find { it.derivationPath.contains(SIGNER_PATH_PREFIX) }
                            } ?: return@launch
                    if (membershipStepManager.isKeyExisted(signer.masterFingerprint)) {
                        _event.emit(Mk4IntroViewEvent.OnSignerExistInAssistedWallet)
                        _event.emit(Mk4IntroViewEvent.Loading(false))
                        return@launch
                    }
                    val createSignerResult = createMk4SignerUseCase(
                        signer.copy(
                            name = "$COLDCARD_DEFAULT_KEY_NAME${
                                membershipStepManager.getNextKeySuffixByType(SignerType.COLDCARD_NFC)
                            }"
                        )
                    )
                    if (createSignerResult.isSuccess) {
                        val coldcardSigner = createSignerResult.getOrThrow()
                        saveMembershipStepUseCase(
                            MembershipStepInfo(
                                step = membershipStepManager.currentStep
                                    ?: throw IllegalArgumentException("Current step empty"),
                                masterSignerId = coldcardSigner.masterFingerprint,
                                plan = membershipStepManager.plan,
                                verifyType = VerifyType.APP_VERIFIED,
                                extraData = gson.toJson(
                                    SignerExtra(
                                        derivationPath = coldcardSigner.derivationPath,
                                        isAddNew = true,
                                        signerType = coldcardSigner.type
                                    )
                                )
                            )
                        )
                        _event.emit(Mk4IntroViewEvent.OnCreateSignerSuccess)
                    } else {
                        _event.emit(Mk4IntroViewEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
                    }
                } else {
                    _mk4Signers.apply {
                        clear()
                        addAll(result.getOrThrow())
                    }
                    if (chain == Chain.MAIN && _mk4Signers.any { isTestNetPath(it.derivationPath) }) {
                        _event.emit(Mk4IntroViewEvent.ErrorMk4TestNet)
                    } else {
                        _event.emit(Mk4IntroViewEvent.LoadMk4SignersSuccess(_mk4Signers))
                    }
                }
            } else {
                _event.emit(Mk4IntroViewEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
            _event.emit(Mk4IntroViewEvent.Loading(false))
        }
    }

    fun onContinueClicked() {
        viewModelScope.launch {
            _event.emit(Mk4IntroViewEvent.OnContinueClicked)
        }
    }

    companion object {
        private const val SIGNER_PATH = "m/48h/0h/0h/2h"
        private const val SIGNER_TESTNET_PATH = "m/48h/1h/0h/2h"
    }
}