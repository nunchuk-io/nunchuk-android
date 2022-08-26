package com.nunchuk.android.signer.software.components.primarykey.signin

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.GetAppSettingUseCase
import com.nunchuk.android.core.domain.SignInPrimaryKeyUseCase
import com.nunchuk.android.core.domain.UpdateAppSettingUseCase
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.share.InitNunchukUseCase
import com.nunchuk.android.utils.onException
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

internal class PKeySignInViewModel @AssistedInject constructor(
    @Assisted private val args: PKeySignInArgs,
    private val initNunchukUseCase: InitNunchukUseCase,
    private val getAppSettingUseCase: GetAppSettingUseCase,
    private val updateAppSettingUseCase: UpdateAppSettingUseCase,
    private val signInPrimaryKeyUseCase: SignInPrimaryKeyUseCase,
    private val signInModeHolder: SignInModeHolder
) : NunchukViewModel<PKeySignInState, PKeySignInEvent>() {

    override val initialState: PKeySignInState =
        PKeySignInState(primaryKey = args.primaryKey)

    init {
        setupNunchuk()
    }

    private fun setupNunchuk() {
        viewModelScope.launch {
            getAppSettingUseCase.execute()
                .flatMapConcat {
                    val appSettingsNew = it.copy(chain = args.primaryKey.chain!!)
                    updateAppSettingUseCase.execute(appSettingsNew)
                }.flatMapConcat {
                    initNunchukUseCase.execute(accountId = args.primaryKey.account)
                        .onException {
                            event(PKeySignInEvent.InitFailure(it.message.orUnknownError()))
                        }
                }
                .flowOn(Dispatchers.IO)
                .onException { }
                .collect {}
        }
    }

    fun setStaySignedIn(staySignedIn: Boolean) = updateState { copy(staySignedIn = staySignedIn) }

    fun handleSignIn(passphrase: String) = viewModelScope.launch {
        setEvent(PKeySignInEvent.LoadingEvent(true))
        val result = signInPrimaryKeyUseCase(
            SignInPrimaryKeyUseCase.Param(
                passphrase = passphrase,
                address = args.primaryKey.address,
                signerName = args.primaryKey.name,
                username = args.primaryKey.account,
                masterFingerprint = args.primaryKey.masterFingerprint,
                staySignedIn = getState().staySignedIn
            )
        )
        setEvent(PKeySignInEvent.LoadingEvent(false))
        if (result.isFailure) {
            setEvent(PKeySignInEvent.ProcessErrorEvent(result.exceptionOrNull()?.message.orUnknownError()))
            return@launch
        }
        if (result.isSuccess) {
            signInModeHolder.setCurrentMode(SignInMode.PRIMARY_KEY)
            setEvent(PKeySignInEvent.SignInSuccessEvent)
        }
    }

    @AssistedFactory
    internal interface Factory {
        fun create(args: PKeySignInArgs): PKeySignInViewModel
    }
}