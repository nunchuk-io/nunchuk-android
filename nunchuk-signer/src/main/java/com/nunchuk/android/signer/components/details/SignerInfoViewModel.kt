package com.nunchuk.android.signer.components.details

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.HealthCheckMasterSignerUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.signer.components.details.SignerInfoEvent.*
import com.nunchuk.android.type.HealthStatus
import com.nunchuk.android.usecase.*
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SignerInfoViewModel @Inject constructor(
    private val getMasterSignerUseCase: GetMasterSignerUseCase,
    private val getRemoteSignerUseCase: GetRemoteSignerUseCase,
    private val deleteMasterSignerUseCase: DeleteMasterSignerUseCase,
    private val deleteRemoteSignerUseCase: DeleteRemoteSignerUseCase,
    private val updateMasterSignerUseCase: UpdateMasterSignerUseCase,
    private val updateRemoteSignerUseCase: UpdateRemoteSignerUseCase,
    private val healthCheckMasterSignerUseCase: HealthCheckMasterSignerUseCase,
    private val sendSignerPassphrase: SendSignerPassphrase
) : NunchukViewModel<SignerInfoState, SignerInfoEvent>() {

    override val initialState = SignerInfoState()

    lateinit var id: String
    private var software: Boolean = false

    fun init(id: String, software: Boolean) {
        this.id = id
        this.software = software
        viewModelScope.launch {
            if (software) {
                when (val result = getMasterSignerUseCase.execute(id)) {
                    is Success -> updateState { copy(masterSigner = result.data) }
                    is Error -> Log.e(TAG, "get software signer error", result.exception)
                }
            } else {
                when (val result = getRemoteSignerUseCase.execute(id)) {
                    is Success -> updateState { copy(remoteSigner = result.data) }
                    is Error -> Log.e(TAG, "get remote signer error", result.exception)
                }
            }
        }

    }

    fun handleEditCompletedEvent(updateSignerName: String) {
        viewModelScope.launch {
            val state = getState()
            if (software) {
                state.masterSigner?.let {
                    when (val result = updateMasterSignerUseCase.execute(masterSigner = it.copy(name = updateSignerName))) {
                        is Success -> event(UpdateNameSuccessEvent(updateSignerName))
                        is Error -> event(UpdateNameErrorEvent(result.exception.message.orUnknownError()))
                    }
                }
            } else {
                state.remoteSigner?.let {
                    when (val result = updateRemoteSignerUseCase.execute(signer = it.copy(name = updateSignerName))) {
                        is Success -> event(UpdateNameSuccessEvent(updateSignerName))
                        is Error -> event(UpdateNameErrorEvent(result.exception.message.orUnknownError()))
                    }
                }
            }
        }
    }

    fun handleRemoveSigner() {
        viewModelScope.launch {
            val state = getState()
            if (software) {
                state.masterSigner?.let {
                    when (val result = deleteMasterSignerUseCase.execute(
                        masterSignerId = it.id
                    )) {
                        is Success -> event(RemoveSignerCompletedEvent)
                        is Error -> event(RemoveSignerErrorEvent(result.exception.message.orUnknownError()))
                    }
                }
            } else {
                state.remoteSigner?.let {
                    when (val result = deleteRemoteSignerUseCase.execute(
                        masterFingerprint = it.masterFingerprint,
                        derivationPath = it.derivationPath
                    )) {
                        is Success -> event(RemoveSignerCompletedEvent)
                        is Error -> event(RemoveSignerErrorEvent(result.exception.message.orUnknownError()))
                    }
                }
            }
        }
    }

    fun handleHealthCheck(masterSigner: MasterSigner, passPhrase: String? = null) {
        if (passPhrase != null) {
            viewModelScope.launch {
                sendSignerPassphrase.execute(masterSigner.id, passPhrase)
                    .flowOn(Dispatchers.IO)
                    .onException { event(HealthCheckErrorEvent(it.message.orEmpty())) }
                    .flowOn(Dispatchers.Main)
                    .collect { healthCheck(fingerprint = masterSigner.device.masterFingerprint, path = masterSigner.device.path) }
            }
        } else {
            healthCheck(fingerprint = masterSigner.device.masterFingerprint, path = masterSigner.device.path)
        }

    }

    private fun healthCheck(fingerprint: String, path: String) {
        viewModelScope.launch {
            healthCheckMasterSignerUseCase.execute(fingerprint = fingerprint, message = "", signature = "", path = path)
                .flowOn(Dispatchers.IO)
                .onException { event(HealthCheckErrorEvent(it.message)) }
                .flowOn(Dispatchers.Main)
                .collect {
                    if (it == HealthStatus.SUCCESS) {
                        event(HealthCheckSuccessEvent)
                    } else {
                        event(HealthCheckErrorEvent())
                    }
                }
        }
    }

    companion object {
        private const val TAG = "SignerInfoViewModel"
    }

}