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
import com.nunchuk.android.core.signer.PrimaryKeyFlow.isPrimaryKeyFlow
import com.nunchuk.android.core.signer.PrimaryKeyFlow.isReplaceFlow
import com.nunchuk.android.core.signer.PrimaryKeyFlow.isSignUpFlow
import com.nunchuk.android.core.util.gson
import com.nunchuk.android.core.util.nativeErrorCode
import com.nunchuk.android.core.util.orFalse
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
import com.nunchuk.android.usecase.GetMasterSignerUseCase
import com.nunchuk.android.usecase.GetUnusedSignerFromMasterSignerUseCase
import com.nunchuk.android.usecase.membership.SaveMembershipStepUseCase
import com.nunchuk.android.usecase.membership.SyncKeyToGroupUseCase
import com.nunchuk.android.usecase.signer.GetDefaultSignerFromMasterSignerUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SetPassphraseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val createSoftwareSignerUseCase: CreateSoftwareSignerUseCase,
    private val getUnusedSignerUseCase: GetUnusedSignerFromMasterSignerUseCase,
    private val draftWalletUseCase: DraftWalletUseCase,
    private val createWalletUseCase: CreateWalletUseCase,
    private val changePrimaryKeyUseCase: ChangePrimaryKeyUseCase,
    private val syncKeyToGroupUseCase: SyncKeyToGroupUseCase,
    private val membershipStepManager: MembershipStepManager,
    private val getDefaultSignerFromMasterSignerUseCase: GetDefaultSignerFromMasterSignerUseCase,
    private val saveMembershipStepUseCase: SaveMembershipStepUseCase,
    private val getMasterFingerprintUseCase: GetMasterFingerprintUseCase
) : NunchukViewModel<SetPassphraseState, SetPassphraseEvent>() {

    private lateinit var mnemonic: String
    private lateinit var signerName: String

    private val args: SetPassphraseFragmentArgs =
        SetPassphraseFragmentArgs.fromSavedStateHandle(savedStateHandle)

    override val initialState = SetPassphraseState()

    fun init(mnemonic: String, signerName: String) {
        this.mnemonic = mnemonic
        this.signerName = signerName
    }

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
        viewModelScope.launch {
            setEvent(LoadingEvent(true))
            createSoftwareSignerUseCase(
                CreateSoftwareSignerUseCase.Param(
                    name = signerName,
                    mnemonic = mnemonic,
                    passphrase = getState().passphrase,
                    isPrimaryKey = args.primaryKeyFlow.isPrimaryKeyFlow(),
                    replace = isReplaceKey
                )
            ).onSuccess {
                if (!args.groupId.isNullOrEmpty()) {
                    syncKeyToGroup(it, args.groupId.orEmpty())
                } else if (args.isQuickWallet) {
                    createQuickWallet(it)
                } else {
                    setEvent(
                        CreateSoftwareSignerCompletedEvent(
                            it,
                            state.value?.skipPassphrase.orFalse()
                        )
                    )
                }
            }.onFailure {
                val errorCode = it.nativeErrorCode()
                if (errorCode == NativeErrorCode.SIGNER_EXISTS) {
                    getMasterFingerprintUseCase(
                        GetMasterFingerprintUseCase.Param(
                            mnemonic = mnemonic,
                            passphrase = getState().passphrase
                        )
                    ).onSuccess {
                        setEvent(SetPassphraseEvent.ExistingSignerEvent(it.orEmpty()))
                    }
                } else {
                    setEvent(CreateSoftwareSignerErrorEvent(it.message.orUnknownError()))
                }
            }
            setEvent(LoadingEvent(false))
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
               syncKeyToGroupUseCase(
                   SyncKeyToGroupUseCase.Param(
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
                                   signerType = signer.type
                               )
                           ),
                           groupId = groupId
                       )
                   )
                   setEvent(CreateSoftwareSignerCompletedEvent(masterSigner, masterSigner.device.needPassPhraseSent))
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
                .flatMapMerge {
                    createWalletUseCase.execute(
                        name = DEFAULT_WALLET_NAME,
                        totalRequireSigns = 1,
                        signers = it,
                        addressType = addressType,
                        isEscrow = false
                    )
                }
                .flowOn(Dispatchers.Main)
                .onException {
                    setEvent(CreateWalletErrorEvent(it.message.orUnknownError()))
                }
                .collect {
                    setEvent(CreateWalletSuccessEvent(it.id))
                }
        }
    }

    companion object {
        private const val DEFAULT_WALLET_NAME = "My Wallet"
    }
}