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

package com.nunchuk.android.signer.software.components.passphrase

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.constants.NativeErrorCode
import com.nunchuk.android.core.domain.ChangePrimaryKeyUseCase
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.signer.KeyFlow.isPrimaryKeyFlow
import com.nunchuk.android.core.signer.KeyFlow.isReplaceFlow
import com.nunchuk.android.core.signer.KeyFlow.isReplaceKeyInFreeWalletFlow
import com.nunchuk.android.core.signer.KeyFlow.isSignUpFlow
import com.nunchuk.android.core.util.gson
import com.nunchuk.android.core.util.nativeErrorCode
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.MembershipStepInfo
import com.nunchuk.android.model.SignerExtra
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.signer.software.components.passphrase.SetPassphraseEvent.ConfirmPassPhraseNotMatchedEvent
import com.nunchuk.android.signer.software.components.passphrase.SetPassphraseEvent.ConfirmPassPhraseRequiredEvent
import com.nunchuk.android.signer.software.components.passphrase.SetPassphraseEvent.CreateSoftwareSignerCompletedEvent
import com.nunchuk.android.signer.software.components.passphrase.SetPassphraseEvent.CreateSoftwareSignerErrorEvent
import com.nunchuk.android.signer.software.components.passphrase.SetPassphraseEvent.CreateWalletErrorEvent
import com.nunchuk.android.signer.software.components.passphrase.SetPassphraseEvent.CreateWalletSuccessEvent
import com.nunchuk.android.signer.software.components.passphrase.SetPassphraseEvent.LoadingEvent
import com.nunchuk.android.signer.software.components.passphrase.SetPassphraseEvent.PassPhraseRequiredEvent
import com.nunchuk.android.signer.software.components.passphrase.SetPassphraseEvent.PassPhraseValidEvent
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.CreateSoftwareSignerUseCase
import com.nunchuk.android.usecase.CreateWalletUseCase
import com.nunchuk.android.usecase.DraftWalletUseCase
import com.nunchuk.android.usecase.GetMasterFingerprintUseCase
import com.nunchuk.android.usecase.GetUnusedSignerFromMasterSignerUseCase
import com.nunchuk.android.usecase.byzantine.GetLocalGroupUseCase
import com.nunchuk.android.usecase.free.groupwallet.AddSignerToGroupUseCase
import com.nunchuk.android.usecase.membership.SaveMembershipStepUseCase
import com.nunchuk.android.usecase.membership.SyncKeyUseCase
import com.nunchuk.android.usecase.replace.ReplaceKeyUseCase
import com.nunchuk.android.usecase.signer.CreateSoftwareSignerByXprvUseCase
import com.nunchuk.android.usecase.signer.GetDefaultSignerFromMasterSignerUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SetPassphraseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val createSoftwareSignerUseCase: CreateSoftwareSignerUseCase,
    private val createSoftwareSignerByXprvUseCase: CreateSoftwareSignerByXprvUseCase,
    private val getUnusedSignerUseCase: GetUnusedSignerFromMasterSignerUseCase,
    private val draftWalletUseCase: DraftWalletUseCase,
    private val createWalletUseCase: CreateWalletUseCase,
    private val changePrimaryKeyUseCase: ChangePrimaryKeyUseCase,
    private val syncKeyUseCase: SyncKeyUseCase,
    private val membershipStepManager: MembershipStepManager,
    private val getDefaultSignerFromMasterSignerUseCase: GetDefaultSignerFromMasterSignerUseCase,
    private val saveMembershipStepUseCase: SaveMembershipStepUseCase,
    private val getMasterFingerprintUseCase: GetMasterFingerprintUseCase,
    private val replaceKeyUseCase: ReplaceKeyUseCase,
    private val pushEventManager: PushEventManager,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val getLocalGroupUseCase: GetLocalGroupUseCase,
    private val addSignerToGroupUseCase: AddSignerToGroupUseCase,
) : NunchukViewModel<SetPassphraseState, SetPassphraseEvent>() {
    private val args: SetPassphraseFragmentArgs by lazy {
        SetPassphraseFragmentArgs.fromSavedStateHandle(savedStateHandle)
    }

    override val initialState = SetPassphraseState()

    fun updatePassphrase(passphrase: String) {
        updateState { copy(passphrase = passphrase) }
    }

    fun updateConfirmPassphrase(confirmPassphrase: String) {
        updateState { copy(confirmPassphrase = confirmPassphrase) }
    }

    fun skipPassphraseEvent() {
        updatePassphrase("")
        if (args.primaryKeyFlow.isSignUpFlow()) {
            event(CreateSoftwareSignerCompletedEvent(skipPassphrase = true))
            return
        } else if (args.primaryKeyFlow.isReplaceFlow()) {
            replacePrimaryKey()
            return
        }
        updateState { copy(skipPassphrase = true) }
        createSoftwareSigner(isReplaceKey = false)
    }

    fun confirmPassphraseEvent() {
        val state = getState()
        val passphrase = state.passphrase
        val confirmPassphrase = state.confirmPassphrase
        when {
            passphrase.isEmpty() -> event(PassPhraseRequiredEvent)
            confirmPassphrase.isEmpty() -> event(ConfirmPassPhraseRequiredEvent)
            passphrase != confirmPassphrase -> event(ConfirmPassPhraseNotMatchedEvent)
            else -> {
                event(PassPhraseValidEvent)
                if (args.primaryKeyFlow.isSignUpFlow()) {
                    event(CreateSoftwareSignerCompletedEvent(skipPassphrase = false))
                    return
                } else if (args.primaryKeyFlow.isReplaceFlow()) {
                    replacePrimaryKey()
                    return
                }
                updateState { copy(skipPassphrase = false) }
                createSoftwareSigner(isReplaceKey = false)
            }
        }
    }

    private fun replacePrimaryKey() = viewModelScope.launch {
        setEvent(LoadingEvent(true))
        val result = changePrimaryKeyUseCase(
            ChangePrimaryKeyUseCase.Param(
                mnemonic = args.mnemonic,
                newKeyPassphrase = getState().passphrase,
                signerName = args.signerName,
                oldKeyPassphrase = args.passphrase
            )
        )
        if (result.isFailure) {
            setEvent(CreateSoftwareSignerErrorEvent(result.exceptionOrNull()?.message.orUnknownError()))
            return@launch
        }
        if (result.isSuccess) {
            setEvent(CreateSoftwareSignerCompletedEvent(result.getOrThrow(), false))
        }
    }

    fun createSoftwareSigner(isReplaceKey: Boolean) {
        createSoftwareSigner(
            isReplaceKey = isReplaceKey,
            signerName = args.signerName,
            mnemonic = args.mnemonic,
            passphrase = getState().passphrase,
            primaryKeyFlow = args.primaryKeyFlow,
            groupId = args.groupId.orEmpty(),
            replacedXfp = args.replacedXfp,
            walletId = args.walletId,
            isQuickWallet = args.isQuickWallet,
            skipPassphrase = getState().skipPassphrase,
            index = args.index
        )
    }

    fun createSoftwareSigner(
        isReplaceKey: Boolean,
        signerName: String,
        mnemonic: String,
        passphrase: String,
        primaryKeyFlow: Int,
        groupId: String,
        replacedXfp: String,
        walletId: String,
        isQuickWallet: Boolean,
        skipPassphrase: Boolean,
        index: Int,
        xprv: String = ""
    ) {
        viewModelScope.launch {
            setEvent(LoadingEvent(true))
            if (xprv.isNotEmpty()) {
                createSoftwareSignerByXprvUseCase(
                    CreateSoftwareSignerByXprvUseCase.Param(
                        name = signerName,
                        xprv = xprv,
                        isPrimaryKey = primaryKeyFlow.isPrimaryKeyFlow(),
                        replace = isReplaceKey
                    )
                )
            } else {
                createSoftwareSignerUseCase(
                    CreateSoftwareSignerUseCase.Param(
                        name = signerName,
                        mnemonic = mnemonic,
                        passphrase = passphrase,
                        isPrimaryKey = primaryKeyFlow.isPrimaryKeyFlow(),
                        replace = isReplaceKey
                    )
                )
            }.onSuccess { signer ->
                if (replacedXfp.isNotEmpty() && walletId.isNotEmpty()) {
                    replacedKey(signer, groupId, replacedXfp, walletId)
                } else if (groupId.isNotEmpty()) {
                    if (getLocalGroupUseCase(groupId).getOrNull() != null) {
                        syncKeyToGroup(signer, groupId)
                    } else {
                        syncKeyToGroupSandbox(signer, groupId, index)
                    }
                } else if (isQuickWallet) {
                    createQuickWallet(signer)
                } else if (primaryKeyFlow.isReplaceKeyInFreeWalletFlow()) {
                    // for replace key in free wallet flow
                    getSingleSignerForFreeWallet(signer, walletId)
                    setEvent(
                        CreateSoftwareSignerCompletedEvent(
                            masterSigner = signer,
                            skipPassphrase = skipPassphrase
                        )
                    )
                } else {
                    setEvent(
                        CreateSoftwareSignerCompletedEvent(
                            masterSigner = signer,
                            skipPassphrase = skipPassphrase
                        )
                    )
                }
            }.onFailure {
                val errorCode = it.nativeErrorCode()
                if (errorCode == NativeErrorCode.SIGNER_EXISTS) {
                    getMasterFingerprintUseCase(
                        GetMasterFingerprintUseCase.Param(
                            mnemonic = mnemonic,
                            passphrase = passphrase
                        )
                    ).onSuccess { signer ->
                        setEvent(SetPassphraseEvent.ExistingSignerEvent(signer.orEmpty()))
                    }
                } else {
                    setEvent(CreateSoftwareSignerErrorEvent(it.message.orUnknownError()))
                }
            }
            setEvent(LoadingEvent(false))
        }
    }

    private suspend fun getSingleSignerForFreeWallet(signer: MasterSigner, walletId: String) {
        if (walletId.isNotEmpty()) {
            getWalletDetail2UseCase(walletId).onSuccess { wallet ->
                val walletType =
                    if (wallet.signers.size > 1) WalletType.MULTI_SIG else WalletType.SINGLE_SIG
                getDefaultSignerFromMasterSignerUseCase(
                    GetDefaultSignerFromMasterSignerUseCase.Params(
                        masterSignerId = signer.id,
                        walletType = walletType,
                        addressType = AddressType.NATIVE_SEGWIT
                    )
                ).onSuccess { singleSigner ->
                    pushEventManager.push(PushEvent.LocalUserSignerAdded(singleSigner))
                }
            }
        }
    }

    private fun replacedKey(
        masterSigner: MasterSigner,
        groupId: String,
        replacedXfp: String,
        walletId: String
    ) {
        viewModelScope.launch {
            getDefaultSignerFromMasterSignerUseCase(
                GetDefaultSignerFromMasterSignerUseCase.Params(
                    masterSignerId = masterSigner.id,
                    walletType = WalletType.MULTI_SIG,
                    addressType = AddressType.NATIVE_SEGWIT
                )
            ).onSuccess { signer ->
                replaceKeyUseCase(
                    ReplaceKeyUseCase.Param(
                        groupId = groupId,
                        xfp = replacedXfp,
                        signer = signer,
                        walletId = walletId,
                    )
                ).onSuccess {
                    setEvent(
                        CreateSoftwareSignerCompletedEvent(
                            masterSigner,
                            masterSigner.device.needPassPhraseSent
                        )
                    )
                }.onFailure {
                    setEvent(CreateSoftwareSignerErrorEvent(it.message.orUnknownError()))
                }
            }.onFailure {
                setEvent(CreateSoftwareSignerErrorEvent(it.message.orUnknownError()))
            }
        }
    }

    private fun syncKeyToGroupSandbox(masterSigner: MasterSigner, groupId: String, index: Int) {
        viewModelScope.launch {
            getDefaultSignerFromMasterSignerUseCase(
                GetDefaultSignerFromMasterSignerUseCase.Params(
                    masterSignerId = masterSigner.id,
                    walletType = WalletType.MULTI_SIG,
                    addressType = AddressType.NATIVE_SEGWIT
                )
            ).onSuccess { signer ->
                addSignerToGroupUseCase(
                    AddSignerToGroupUseCase.Params(
                        groupId = groupId,
                        signer = signer,
                        index = index
                    )
                ).onSuccess {
                    setEvent(
                        CreateSoftwareSignerCompletedEvent(
                            masterSigner,
                            masterSigner.device.needPassPhraseSent
                        )
                    )
                }.onFailure {
                    setEvent(CreateSoftwareSignerErrorEvent(it.message.orUnknownError()))
                }
            }.onFailure {
                setEvent(CreateSoftwareSignerErrorEvent(it.message.orUnknownError()))
            }
        }
    }

    private fun syncKeyToGroup(masterSigner: MasterSigner, groupId: String) {
        viewModelScope.launch {
            getDefaultSignerFromMasterSignerUseCase(
                GetDefaultSignerFromMasterSignerUseCase.Params(
                    masterSignerId = masterSigner.id,
                    walletType = WalletType.MULTI_SIG,
                    addressType = AddressType.NATIVE_SEGWIT
                )
            ).onSuccess { signer ->
                syncKeyUseCase(
                    SyncKeyUseCase.Param(
                        step = membershipStepManager.currentStep
                            ?: throw IllegalArgumentException("Current step empty"),
                        groupId = groupId,
                        signer = signer
                    )
                ).onSuccess {
                    saveMembershipStepUseCase(
                        MembershipStepInfo(
                            step = membershipStepManager.currentStep
                                ?: throw IllegalArgumentException("Current step empty"),
                            masterSignerId = signer.masterFingerprint,
                            plan = membershipStepManager.localMembershipPlan,
                            verifyType = VerifyType.APP_VERIFIED,
                            extraData = gson.toJson(
                                SignerExtra(
                                    derivationPath = signer.derivationPath,
                                    isAddNew = true,
                                    signerType = signer.type,
                                    userKeyFileName = "",
                                )
                            ),
                            groupId = groupId
                        )
                    )
                    setEvent(
                        CreateSoftwareSignerCompletedEvent(
                            masterSigner,
                            masterSigner.device.needPassPhraseSent
                        )
                    )
                }.onFailure {
                    setEvent(CreateSoftwareSignerErrorEvent(it.message.orUnknownError()))
                }
            }.onFailure {
                setEvent(CreateSoftwareSignerErrorEvent(it.message.orUnknownError()))
            }
        }
    }

    private fun createQuickWallet(masterSigner: MasterSigner) {
        val addressType = AddressType.NATIVE_SEGWIT
        viewModelScope.launch {
            getUnusedSignerUseCase.execute(listOf(masterSigner), WalletType.SINGLE_SIG, addressType)
                .flowOn(Dispatchers.IO)
                .onStart { event(LoadingEvent(true)) }
                .map {
                    draftWalletUseCase.execute(
                        name = DEFAULT_WALLET_NAME,
                        totalRequireSigns = 1,
                        signers = it,
                        addressType = addressType,
                        isEscrow = false
                    )
                    it
                }
                .flowOn(Dispatchers.IO)
                .map {
                    createWalletUseCase(
                        CreateWalletUseCase.Params(
                            name = DEFAULT_WALLET_NAME,
                            totalRequireSigns = 1,
                            signers = it,
                            addressType = addressType,
                            isEscrow = false,
                        )
                    ).getOrThrow()
                }
                .flowOn(Dispatchers.Main)
                .onException {
                    setEvent(CreateWalletErrorEvent(it.message.orUnknownError()))
                }
                .collect {
                    setEvent(CreateWalletSuccessEvent(it))
                }
        }
    }

    companion object {
        private const val DEFAULT_WALLET_NAME = "My Wallet"
    }
}