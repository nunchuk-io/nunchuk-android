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

package com.nunchuk.android.main.membership.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.nunchuk.android.GetDefaultSignerFromMasterSignerUseCase
import com.nunchuk.android.core.domain.membership.CreateServerWalletUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.ServerKeyExtra
import com.nunchuk.android.model.SignerExtra
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.CreateSignerUseCase
import com.nunchuk.android.usecase.CreateWalletUseCase
import com.nunchuk.android.usecase.GetRemoteSignerUseCase
import com.nunchuk.android.usecase.membership.GetMembershipStepUseCase
import com.nunchuk.android.usecase.user.SetRegisterAirgapUseCase
import com.nunchuk.android.usecase.user.SetRegisterColdcardUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateWalletViewModel @Inject constructor(
    private val getDefaultSignerFromMasterSignerUseCase: GetDefaultSignerFromMasterSignerUseCase,
    private val createWalletUseCase: CreateWalletUseCase,
    private val getMembershipStepUseCase: GetMembershipStepUseCase,
    private val createSignerUseCase: CreateSignerUseCase,
    private val gson: Gson,
    private val createServerWalletUseCase: CreateServerWalletUseCase,
    private val membershipStepManager: MembershipStepManager,
    private val getRemoteSignerUseCase: GetRemoteSignerUseCase,
    private val setRegisterColdcardUseCase: SetRegisterColdcardUseCase,
    private val setRegisterAirgapUseCase: SetRegisterAirgapUseCase
) : ViewModel() {
    private val signers = hashMapOf<String, SignerExtra>()
    private var serverKeyExtra: ServerKeyExtra? = null
    private var serverKeyId: String? = null

    private val _event = MutableSharedFlow<CreateWalletEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(CreateWalletState.EMPTY)
    val state = _state.asStateFlow()

    val plan = membershipStepManager.plan

    init {
        viewModelScope.launch {
            getMembershipStepUseCase(membershipStepManager.plan)
                .filter { it.isSuccess }
                .map { it.getOrThrow() }
                .collect { steps ->
                    steps.forEach { stepInfo ->
                        when (stepInfo.step) {
                            MembershipStep.ADD_TAP_SIGNER_1,
                            MembershipStep.ADD_TAP_SIGNER_2,
                            MembershipStep.HONEY_ADD_TAP_SIGNER,
                            MembershipStep.HONEY_ADD_HARDWARE_KEY_1,
                            MembershipStep.HONEY_ADD_HARDWARE_KEY_2 -> {
                                runCatching {
                                    gson.fromJson(
                                        stepInfo.extraData,
                                        SignerExtra::class.java
                                    )
                                }.getOrNull()?.let { signerExtra ->
                                    signers[stepInfo.masterSignerId] = signerExtra
                                }
                            }
                            MembershipStep.ADD_SEVER_KEY -> {
                                serverKeyExtra = runCatching {
                                    gson.fromJson(
                                        stepInfo.extraData,
                                        ServerKeyExtra::class.java
                                    )
                                }.getOrNull()
                                serverKeyId = stepInfo.keyIdInServer
                            }
                            else -> {}
                        }
                    }
                }
        }
    }

    fun updateWalletName(walletName: String) {
        _state.update {
            it.copy(walletName = walletName)
        }
    }

    fun onContinueClicked() {
        createQuickWallet()
    }

    private fun createQuickWallet() {
        val serverKey = serverKeyExtra ?: return
        val serverKeyId = serverKeyId ?: return
        viewModelScope.launch {
            val addressType = AddressType.NATIVE_SEGWIT
            _event.emit(CreateWalletEvent.Loading(true))
            val masterSigners =
                signers.filter { it.value.signerType == SignerType.NFC }.map { it.key }
            val getSingleSingerResult = getDefaultSignerFromMasterSignerUseCase(
                GetDefaultSignerFromMasterSignerUseCase.Params(
                    masterSigners,
                    WalletType.MULTI_SIG,
                    addressType
                )
            )
            if (getSingleSingerResult.isFailure) {
                _event.emit(
                    CreateWalletEvent.ShowError(
                        getSingleSingerResult.exceptionOrNull()?.message.orUnknownError()
                    )
                )
                _event.emit(CreateWalletEvent.Loading(false))
                return@launch
            }
            val remoteSignerResults =
                signers.filter { it.value.signerType == SignerType.COLDCARD_NFC || it.value.signerType == SignerType.AIRGAP }
                    .map {
                        async {
                            getRemoteSignerUseCase(
                                GetRemoteSignerUseCase.Data(
                                    it.key,
                                    it.value.derivationPath
                                )
                            )
                        }
                    }.awaitAll()
            if (remoteSignerResults.any { it.isFailure }) {
                _event.emit(CreateWalletEvent.ShowError("Can not get remote signer"))
                _event.emit(CreateWalletEvent.Loading(false))
                return@launch
            }
            val createServerSignerResult = createSignerUseCase(
                CreateSignerUseCase.Params(
                    name = serverKey.name,
                    xpub = serverKey.xpub,
                    derivationPath = serverKey.derivationPath,
                    masterFingerprint = serverKey.xfp,
                    type = SignerType.SERVER
                )
            )
            if (createServerSignerResult.isFailure) {
                _event.emit(
                    CreateWalletEvent.ShowError(
                        createServerSignerResult.exceptionOrNull()?.message.orUnknownError()
                    )
                )
                _event.emit(CreateWalletEvent.Loading(false))
                return@launch
            }
            val wallet = Wallet(
                name = _state.value.walletName,
                totalRequireSigns = 2,
                signers = getSingleSingerResult.getOrThrow() + remoteSignerResults.mapNotNull { it.getOrNull() } + createServerSignerResult.getOrThrow(),
                addressType = addressType,
                escrow = false,
            )
            val result = createServerWalletUseCase(
                CreateServerWalletUseCase.Params(wallet, serverKeyId, membershipStepManager.plan)
            )
            if (result.isSuccess) {
                createWalletUseCase.execute(
                    name = _state.value.walletName,
                    totalRequireSigns = 2,
                    signers = getSingleSingerResult.getOrThrow() + remoteSignerResults.mapNotNull { it.getOrNull() } + createServerSignerResult.getOrThrow(),
                    addressType = addressType,
                    isEscrow = false
                ).flowOn(Dispatchers.IO)
                    .flowOn(Dispatchers.Main)
                    .onCompletion { _event.emit(CreateWalletEvent.Loading(false)) }
                    .onException {
                        _event.emit(CreateWalletEvent.ShowError(it.message.orUnknownError()))
                    }
                    .collect {
                        if (signers.any { it.value.signerType == SignerType.COLDCARD_NFC }) {
                            setRegisterColdcardUseCase(false)
                        }
                        if (signers.any { it.value.signerType == SignerType.AIRGAP }) {
                            setRegisterAirgapUseCase(false)
                        }
                        _event.emit(
                            CreateWalletEvent.OnCreateWalletSuccess(
                                walletId = it.id,
                                hasColdcard = signers.any { signer -> signer.value.signerType == SignerType.COLDCARD_NFC },
                                hasAirgap = signers.any { signer -> signer.value.signerType == SignerType.AIRGAP }
                            )
                        )
                    }
            } else {
                _event.emit(
                    CreateWalletEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError())
                )
                _event.emit(CreateWalletEvent.Loading(false))
            }
        }
    }
}