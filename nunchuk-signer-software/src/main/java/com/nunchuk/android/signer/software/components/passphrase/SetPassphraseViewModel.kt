package com.nunchuk.android.signer.software.components.passphrase

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.ChangePrimaryKeyUseCase
import com.nunchuk.android.core.signer.PrimaryKeyFlow.isPrimaryKeyFlow
import com.nunchuk.android.core.signer.PrimaryKeyFlow.isReplaceFlow
import com.nunchuk.android.core.signer.PrimaryKeyFlow.isSignUpFlow
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.signer.software.components.passphrase.SetPassphraseEvent.*
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.CreateSoftwareSignerUseCase
import com.nunchuk.android.usecase.CreateWalletUseCase
import com.nunchuk.android.usecase.DraftWalletUseCase
import com.nunchuk.android.usecase.GetUnusedSignerFromMasterSignerUseCase
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
    private val changePrimaryKeyUseCase: ChangePrimaryKeyUseCase
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
        createSoftwareSigner(true)
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
                createSoftwareSigner(false)
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

    private fun createSoftwareSigner(skipPassphrase: Boolean) {
        viewModelScope.launch {
            createSoftwareSignerUseCase.execute(
                name = signerName,
                mnemonic = mnemonic,
                passphrase = getState().passphrase,
                isPrimaryKey = args.primaryKeyFlow.isPrimaryKeyFlow()
            )
                .flowOn(Dispatchers.IO)
                .onStart { event(LoadingEvent(true)) }
                .onException {
                    event(CreateSoftwareSignerErrorEvent(it.message.orUnknownError()))
                }
                .flowOn(Dispatchers.Main)
                .collect {
                    if (args.isQuickWallet) {
                        createQuickWallet(it)
                    } else {
                        event(
                            CreateSoftwareSignerCompletedEvent(it, skipPassphrase)
                        )
                    }
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