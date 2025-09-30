package com.nunchuk.android.signer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.signer.OnChainAddSignerParam
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.GetIndexFromPathUseCase
import com.nunchuk.android.usecase.signer.GetAllSignersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignerIntroViewModel @Inject constructor(
    private val membershipStepManager: MembershipStepManager,
    private val getAllSignersUseCase: GetAllSignersUseCase,
    private val masterSignerMapper: MasterSignerMapper,
    private val getIndexFromPathUseCase: GetIndexFromPathUseCase,
) : ViewModel() {

    val remainTime = membershipStepManager.remainingTime

    private lateinit var onChainAddSignerParam: OnChainAddSignerParam

    private val _filteredTapSigners = MutableStateFlow<List<SignerModel>>(emptyList())
    val filteredTapSigners = _filteredTapSigners.asStateFlow()

    private val _event = MutableSharedFlow<SignerIntroEvent>()
    val event = _event.asSharedFlow()

    fun init(onChainAddSignerParam: OnChainAddSignerParam?) {
        this.onChainAddSignerParam = onChainAddSignerParam ?: return
        fetchAndFilterTapSigners()
    }

    private fun fetchAndFilterTapSigners() {
        viewModelScope.launch {
            getAllSignersUseCase(true).onSuccess { (masterSigners, singleSigners) ->
                val allSigners = mapSigners(singleSigners, masterSigners)
                val filtered = filterTapSignersByTypeAndIndex(allSigners)
                _filteredTapSigners.update { filtered }
            }
        }
    }

    private suspend fun mapSigners(
        singleSigners: List<SingleSigner>,
        masterSigners: List<MasterSigner>
    ): List<SignerModel> {
        return masterSigners.map { masterSignerMapper(it) } + 
               singleSigners.map(SingleSigner::toModel)
    }

    private fun filterTapSignersByTypeAndIndex(signers: List<SignerModel>): List<SignerModel> {
        return signers.filter { signer ->
            val matchesType = signer.type == SignerType.NFC

            return@filter matchesType
        }
    }

    fun onTapSignerContinueClicked() {
        viewModelScope.launch {
           if (_filteredTapSigners.value.isNotEmpty()) {
                _event.emit(SignerIntroEvent.ShowFilteredTapSigners(_filteredTapSigners.value))
            } else {
                _event.emit(SignerIntroEvent.OpenSetupTapSigner)
            }
        }
    }
}

sealed class SignerIntroEvent {
    data class ShowFilteredTapSigners(val signers: List<SignerModel>) : SignerIntroEvent()
    object OpenSetupTapSigner : SignerIntroEvent()
}

