package com.nunchuk.android.signer.signer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.usecase.signer.GetAllSignersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignersViewModel @Inject constructor(
    private val getAllSignersUseCase: GetAllSignersUseCase,
    private val masterSignerMapper: MasterSignerMapper,
    private val accountManager: AccountManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(SignerUiState())
    val uiState = _uiState.asStateFlow()

    private val loginType = accountManager.loginType()
    private var job: Job? = null

    init {
        getAllSigners()
    }

    fun getAllSigners() {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            getAllSignersUseCase(true).onSuccess { (masterSigners, singleSigners) ->
                _uiState.update {
                    it.copy(
                        signers = mapSigners(
                            singleSigners,
                            masterSigners
                        ).sortedByDescending { signer ->
                            isPrimaryKey(
                                signer.id
                            )
                        },
                    )
                }
            }
        }
    }

    private fun isPrimaryKey(id: String) =
        loginType == SignInMode.PRIMARY_KEY.value && accountManager.getPrimaryKeyInfo()?.xfp == id

    private suspend fun mapSigners(
        singleSigners: List<SingleSigner>, masterSigners: List<MasterSigner>,
    ): List<SignerModel> {
        return masterSigners.map {
            masterSignerMapper(it)
        } + singleSigners.map(SingleSigner::toModel)
    }

    fun hasSigner() = uiState.value.signers.isNotEmpty()
}

