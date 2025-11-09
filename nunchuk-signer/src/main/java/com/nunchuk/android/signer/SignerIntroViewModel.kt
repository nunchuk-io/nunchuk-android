package com.nunchuk.android.signer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.signer.OnChainAddSignerParam
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.SupportedSignerConfig
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.GetUserWalletConfigsSetupFromCacheUseCase
import com.nunchuk.android.usecase.GetUserWalletConfigsSetupUseCase
import com.nunchuk.android.usecase.signer.GetAllSignersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignerIntroState(
    val filteredTapSigners: List<SignerModel> = emptyList(),
    val supportedSigners: List<SupportedSigner> = emptyList(),
    val supportedSignerConfigs: List<SupportedSignerConfig> = emptyList(),
    val isAddInheritanceSigner: Boolean = false,
    val dynamicSupportedSigners: List<SupportedSigner> = emptyList()
)

@HiltViewModel
class SignerIntroViewModel @Inject constructor(
    private val membershipStepManager: MembershipStepManager,
    private val getAllSignersUseCase: GetAllSignersUseCase,
    private val masterSignerMapper: MasterSignerMapper,
    private val getUserWalletConfigsSetupFromCacheUseCase: GetUserWalletConfigsSetupFromCacheUseCase,
    private val getUserWalletConfigsSetupUseCase: GetUserWalletConfigsSetupUseCase,
) : ViewModel() {

    val remainTime = membershipStepManager.remainingTime

    private var onChainAddSignerParam: OnChainAddSignerParam? = null

    private val _state = MutableStateFlow(SignerIntroState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<SignerIntroEvent>()
    val event = _event.asSharedFlow()

    fun init(onChainAddSignerParam: OnChainAddSignerParam?) {
        this.onChainAddSignerParam = onChainAddSignerParam
        if (onChainAddSignerParam != null) {
            _state.update { it.copy(isAddInheritanceSigner = onChainAddSignerParam.isAddInheritanceSigner() || onChainAddSignerParam.isVerifyBackupSeedPhrase()) }
            fetchUserWalletConfigs()
            fetchAndFilterTapSigners()
        }
    }

    private fun fetchUserWalletConfigs() {
        viewModelScope.launch {
            getUserWalletConfigsSetupFromCacheUseCase(Unit).collect { result ->
                result.getOrNull()?.let { walletConfigs ->
                    val supportedSigners = convertToSupportedSigners(walletConfigs.supportedSigners)
                    _state.update {
                        it.copy(
                            supportedSignerConfigs = walletConfigs.supportedSigners,
                            supportedSigners = supportedSigners
                        )
                    }
                    updateDynamicSupportedSigners()
                }
            }
        }
        viewModelScope.launch {
            getUserWalletConfigsSetupUseCase(Unit)
        }
    }

    private fun convertToSupportedSigners(configs: List<SupportedSignerConfig>): List<SupportedSigner> {
        return configs.map { config ->
            SupportedSigner(
                type = SignerType.valueOf(config.signerType),
                tag = config.signerTag?.let { SignerTag.valueOf(it) },
                walletType = WalletType.valueOf(config.walletType),
                addressType = AddressType.NATIVE_SEGWIT // Default address type
            )
        }
    }

    private fun updateDynamicSupportedSigners() {
        val currentState = _state.value
        val dynamicSigners = currentState.supportedSignerConfigs.filter {
            it.isInheritanceKey == currentState.isAddInheritanceSigner
        }
        _state.update { it.copy(dynamicSupportedSigners = convertToSupportedSigners(dynamicSigners)) }
    }

    private fun fetchAndFilterTapSigners() {
        viewModelScope.launch {
            getAllSignersUseCase(true).onSuccess { (masterSigners, singleSigners) ->
                val allSigners = mapSigners(singleSigners, masterSigners)
                val filtered = filterTapSignersByTypeAndIndex(allSigners)
                _state.update { it.copy(filteredTapSigners = filtered) }
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
           if (_state.value.filteredTapSigners.isNotEmpty()) {
                _event.emit(SignerIntroEvent.ShowFilteredTapSigners(_state.value.filteredTapSigners))
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

