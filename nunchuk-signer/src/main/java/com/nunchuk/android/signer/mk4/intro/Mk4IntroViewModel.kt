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

package com.nunchuk.android.signer.mk4.intro

import android.nfc.NdefRecord
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.CreateMk4SignerUseCase
import com.nunchuk.android.core.domain.CreateWallet2UseCase
import com.nunchuk.android.core.domain.GetMk4SingersUseCase
import com.nunchuk.android.core.domain.ImportWalletFromMk4UseCase
import com.nunchuk.android.core.domain.coldcard.ExtractWalletsFromColdCard
import com.nunchuk.android.core.domain.settings.GetChainSettingFlowUseCase
import com.nunchuk.android.core.domain.wallet.ParseMk4WalletUseCase
import com.nunchuk.android.core.util.COLDCARD_DEFAULT_KEY_NAME
import com.nunchuk.android.core.util.DEFAULT_COLDCARD_WALLET_NAME
import com.nunchuk.android.core.util.gson
import com.nunchuk.android.core.util.isRecommendedPath
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.MembershipStepInfo
import com.nunchuk.android.model.SignerExtra
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.signer.util.isTestNetPath
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.membership.SaveMembershipStepUseCase
import com.nunchuk.android.usecase.membership.SyncKeyToGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class Mk4IntroViewModel @Inject constructor(
    private val getMk4SingersUseCase: GetMk4SingersUseCase,
    private val createMk4SignerUseCase: CreateMk4SignerUseCase,
    private val saveMembershipStepUseCase: SaveMembershipStepUseCase,
    private val membershipStepManager: MembershipStepManager,
    private val getChainSettingFlowUseCase: GetChainSettingFlowUseCase,
    private val importWalletFromMk4UseCase: ImportWalletFromMk4UseCase,
    private val parseMk4WalletUseCase: ParseMk4WalletUseCase,
    private val extractWalletsFromColdCard: ExtractWalletsFromColdCard,
    private val createWallet2UseCase: CreateWallet2UseCase,
    private val syncKeyToGroupUseCase: SyncKeyToGroupUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _event = MutableSharedFlow<Mk4IntroViewEvent>()
    private val args: Mk4IntroFragmentArgs =
        Mk4IntroFragmentArgs.fromSavedStateHandle(savedStateHandle)
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(Mk4IntroState())
    val state = _state.asStateFlow()

    val remainTime = membershipStepManager.remainingTime
    private var chain: Chain = Chain.MAIN

    private val _mk4Signers = mutableListOf<SingleSigner>()

    init {
        viewModelScope.launch {
            chain = getChainSettingFlowUseCase(Unit).map { it.getOrElse { Chain.MAIN } }.first()
        }
    }

    val mk4Signers: List<SingleSigner>
        get() = _mk4Signers

    fun getMk4Signer(
        records: List<NdefRecord>,
        groupId: String,
        newIndex: Int,
        xfp: String?,
    ) {
        viewModelScope.launch {
            _event.emit(Mk4IntroViewEvent.Loading(true))
            val result = getMk4SingersUseCase(records.toTypedArray())
            if (result.isSuccess) {
                if (args.isMembershipFlow) {
                    val sortedSigner = result.getOrThrow().sortedBy { it.derivationPath }
                    val signer = sortedSigner.find { it.derivationPath.isRecommendedPath }
                    if (signer == null) {
                        _event.emit(Mk4IntroViewEvent.ShowError("XPUBs file is invalid"))
                        _event.emit(Mk4IntroViewEvent.Loading(false))
                        return@launch
                    }
                    if (chain == Chain.MAIN && isTestNetPath(signer.derivationPath)) {
                        _event.emit(Mk4IntroViewEvent.ErrorMk4TestNet)
                        _event.emit(Mk4IntroViewEvent.Loading(false))
                        return@launch
                    }
                    if (!xfp.isNullOrEmpty() && signer.masterFingerprint != xfp) {
                        _event.emit(Mk4IntroViewEvent.XfpNotMatchException)
                        _event.emit(Mk4IntroViewEvent.Loading(false))
                        return@launch
                    }
                    if (newIndex >= 0 && !signer.derivationPath.endsWith("${newIndex}h/2h")) {
                        _event.emit(Mk4IntroViewEvent.NewIndexNotMatchException)
                        _event.emit(Mk4IntroViewEvent.Loading(false))
                        return@launch
                    }
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
                        // force type coldcard nfc in case we import hardware key first
                        val coldcardSigner =
                            createSignerResult.getOrThrow().copy(type = SignerType.COLDCARD_NFC)
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
                                ),
                                groupId = groupId
                            )
                        )
                        if (groupId.isNotEmpty()) {
                            syncKeyToGroupUseCase(
                                SyncKeyToGroupUseCase.Param(
                                    step = membershipStepManager.currentStep
                                        ?: throw IllegalArgumentException("Current step empty"),
                                    groupId = groupId,
                                    signer = coldcardSigner
                                )
                            )
                        }
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


    fun importWalletFromMk4(records: List<NdefRecord>) {
        viewModelScope.launch {
            _event.emit(Mk4IntroViewEvent.NfcLoading(true))
            val result = importWalletFromMk4UseCase(records)
            _event.emit(Mk4IntroViewEvent.NfcLoading(false))
            if (result.isSuccess && result.getOrThrow() != null) {
                _event.emit(Mk4IntroViewEvent.ImportWalletFromMk4Success(result.getOrThrow()!!.id))
            } else {
                _event.emit(Mk4IntroViewEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun parseWalletFromMk4(records: List<NdefRecord>) {
        viewModelScope.launch {
            _event.emit(Mk4IntroViewEvent.NfcLoading(true))
            parseMk4WalletUseCase(records).onSuccess {
                _event.emit(Mk4IntroViewEvent.ParseWalletFromMk4Success(it))
            }.onFailure {
                _event.emit(Mk4IntroViewEvent.ShowError(it.message.orUnknownError()))
            }
            _event.emit(Mk4IntroViewEvent.NfcLoading(false))
        }
    }

    fun getWalletsFromColdCard(records: List<NdefRecord>) {
        viewModelScope.launch {
            _event.emit(Mk4IntroViewEvent.NfcLoading(true))
            val result = extractWalletsFromColdCard(records)
            _event.emit(Mk4IntroViewEvent.NfcLoading(false))
            if (result.isSuccess && result.getOrThrow().isNotEmpty()) {
                _state.update { it.copy(wallets = result.getOrThrow()) }
                _event.emit(Mk4IntroViewEvent.ExtractWalletsFromColdCard(result.getOrThrow()))
            } else {
                _event.emit(Mk4IntroViewEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun createWallet(walletId: String) {
        viewModelScope.launch {
            _state.value.wallets.find { it.id == walletId }?.let { wallet ->
                _event.emit(Mk4IntroViewEvent.Loading(true))
                val result = createWallet2UseCase(wallet.copy(name = DEFAULT_COLDCARD_WALLET_NAME))
                _event.emit(Mk4IntroViewEvent.Loading(false))
                if (result.isSuccess) {
                    _event.emit(Mk4IntroViewEvent.ImportWalletFromMk4Success(result.getOrThrow().id))
                } else {
                    _event.emit(Mk4IntroViewEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
                }
            }
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

data class Mk4IntroState(val wallets: List<Wallet> = emptyList())