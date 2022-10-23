package com.nunchuk.android.signer.software.components.name

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.GetTurnOnNotificationStoreUseCase
import com.nunchuk.android.core.domain.SignInImportPrimaryKeyUseCase
import com.nunchuk.android.core.domain.UpdateTurnOnNotificationStoreUseCase
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.signer.PrimaryKeyFlow.isSignInFlow
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.share.InitNunchukUseCase
import com.nunchuk.android.signer.software.components.name.AddSoftwareSignerNameEvent.SignerNameInputCompletedEvent
import com.nunchuk.android.signer.software.components.name.AddSoftwareSignerNameEvent.SignerNameRequiredEvent
import com.nunchuk.android.utils.onException
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

internal class AddSoftwareSignerNameViewModel @AssistedInject constructor(
    @Assisted private val args: AddSoftwareSignerNameArgs,
    private val signInImportPrimaryKeyUseCase: SignInImportPrimaryKeyUseCase,
    private val signInModeHolder: SignInModeHolder,
    private val initNunchukUseCase: InitNunchukUseCase,
    private val getTurnOnNotificationStoreUseCase: GetTurnOnNotificationStoreUseCase,
    private val updateTurnOnNotificationStoreUseCase: UpdateTurnOnNotificationStoreUseCase
) : NunchukViewModel<AddSoftwareSignerNameState, AddSoftwareSignerNameEvent>() {

    override val initialState = AddSoftwareSignerNameState(args = args)

    init {
        if (args.primaryKeyFlow.isSignInFlow() && args.username.isNullOrBlank().not()) {
            initNunchuk()
        }
        updateSignerName("")
    }

    private fun initNunchuk() = viewModelScope.launch {
        initNunchukUseCase.execute(accountId = args.username.orUnknownError())
            .flowOn(Dispatchers.IO)
            .onException { event(AddSoftwareSignerNameEvent.InitFailure(it.message.orUnknownError())) }
            .collect {}
    }

    fun updateSignerName(signerName: String) {
        updateState { copy(signerName = signerName) }
    }

    fun handleContinue() {
        val signerName = getState().signerName
        if (args.primaryKeyFlow.isSignInFlow()) {
            viewModelScope.launch {
                event(AddSoftwareSignerNameEvent.LoadingEvent(true))
                val result = signInImportPrimaryKeyUseCase(
                    SignInImportPrimaryKeyUseCase.Param(
                        passphrase = args.passphrase.orUnknownError(),
                        address = args.address.orUnknownError(),
                        username = args.username.orUnknownError(),
                        signerName = signerName,
                        mnemonic = args.mnemonic,
                        staySignedIn = true
                    )
                )
                event(AddSoftwareSignerNameEvent.LoadingEvent(false))
                if (result.isSuccess) {
                    signInModeHolder.setCurrentMode(SignInMode.PRIMARY_KEY)
                    event(SignerNameInputCompletedEvent(signerName))
                } else if (result.isFailure) {
                    event(AddSoftwareSignerNameEvent.ImportPrimaryKeyErrorEvent(result.exceptionOrNull()?.message.orUnknownError()))
                }
            }
        } else {
            if (signerName.isBlank()) {
                event(SignerNameRequiredEvent)
            } else {
                event(SignerNameInputCompletedEvent(signerName))
            }
        }
    }

    fun getTurnOnNotification() = viewModelScope.launch {
        val result = getTurnOnNotificationStoreUseCase(Unit)
        var isTurnOn = false
        if (result.isSuccess) {
            isTurnOn = result.getOrDefault(false)
        }
        setEvent(AddSoftwareSignerNameEvent.GetTurnOnNotificationSuccess(isTurnOn))
    }

    fun updateTurnOnNotification() = viewModelScope.launch {
        updateTurnOnNotificationStoreUseCase(UpdateTurnOnNotificationStoreUseCase.Param(false))
    }

    fun getSignerName() = getState().signerName

    @AssistedFactory
    internal interface Factory {
        fun create(args: AddSoftwareSignerNameArgs): AddSoftwareSignerNameViewModel
    }

}