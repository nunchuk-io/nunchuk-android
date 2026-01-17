package com.nunchuk.android.signer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.settings.GetChainSettingFlowUseCase
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.signer.KeyFlow
import com.nunchuk.android.core.signer.OnChainAddSignerParam
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.isRecommendedMultiSigPath
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.SupportedSignerConfig
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.GetUserWalletConfigsSetupFromCacheUseCase
import com.nunchuk.android.usecase.GetUserWalletConfigsSetupUseCase
import com.nunchuk.android.usecase.membership.RestartWizardUseCase
import com.nunchuk.android.usecase.signer.GetAllSignersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignerIntroState(
    val allSigners: List<SignerModel> = emptyList(),
    val supportedSigners: List<SupportedSigner> = emptyList(),
    val supportedSignerConfigs: List<SupportedSignerConfig> = emptyList(),
    val isAddInheritanceSigner: Boolean = false,
    val dynamicSupportedSigners: List<SupportedSigner> = emptyList(),
    val showDynamicSelection: Boolean = false,
    val isGenericAirgapEnable: Boolean = false
)

@HiltViewModel
class SignerIntroViewModel @Inject constructor(
    private val membershipStepManager: MembershipStepManager,
    private val getAllSignersUseCase: GetAllSignersUseCase,
    private val masterSignerMapper: MasterSignerMapper,
    private val getUserWalletConfigsSetupFromCacheUseCase: GetUserWalletConfigsSetupFromCacheUseCase,
    private val getUserWalletConfigsSetupUseCase: GetUserWalletConfigsSetupUseCase,
    private val restartWizardUseCase: RestartWizardUseCase,
    private val getChainSettingFlowUseCase: GetChainSettingFlowUseCase,
) : ViewModel() {

    val remainTime = membershipStepManager.remainingTime

    private var onChainAddSignerParam: OnChainAddSignerParam? = null
    private var keyFlow: Int = KeyFlow.NONE
    private var isTestNet: Boolean = false

    private val _state = MutableStateFlow(SignerIntroState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<SignerIntroEvent>()
    val event = _event.asSharedFlow()

    init {
        viewModelScope.launch {
            getChainSettingFlowUseCase(Unit)
                .map { it.getOrDefault(Chain.MAIN) }
                .collect {
                    isTestNet = it == Chain.TESTNET
                }
        }
    }

    fun init(
        onChainAddSignerParam: OnChainAddSignerParam?,
        supportedSigners: List<SupportedSigner> = emptyList(),
        keyFlow: Int = KeyFlow.NONE
    ) {
        this.onChainAddSignerParam = onChainAddSignerParam
        this.keyFlow = keyFlow

        if (supportedSigners.isNotEmpty()) {
            _state.update {
                it.copy(
                    supportedSigners = supportedSigners,
                    isGenericAirgapEnable = calculateIsGenericAirgapEnable(supportedSigners)
                )
            }
        }

        if (onChainAddSignerParam != null) {
            _state.update { it.copy(isAddInheritanceSigner = onChainAddSignerParam.isAddInheritanceSigner() || onChainAddSignerParam.isVerifyBackupSeedPhrase()) }
            if (onChainAddSignerParam.isAddInheritanceOffChainSigner()) {
                _state.update {
                    it.copy(
                        supportedSigners = offChainInheritanceKeyTypes,
                        isGenericAirgapEnable = calculateIsGenericAirgapEnable(offChainInheritanceKeyTypes)
                    )
                }
            } else {
                fetchUserWalletConfigs()
            }
            fetchAndFilterTapSigners()
        } else {
            if (supportedSigners.isEmpty()) {
                _state.update {
                    it.copy(
                        isGenericAirgapEnable = calculateIsGenericAirgapEnable(emptyList())
                    )
                }
            }
        }
    }

    private fun calculateIsGenericAirgapEnable(
        supportedSigners: List<SupportedSigner>,
    ): Boolean {
        val isDisableAll = keyFlow != KeyFlow.NONE
        return (supportedSigners.isEmpty()
                || supportedSigners.any { it.type == SignerType.AIRGAP && it.tag == null }) && isDisableAll.not()
    }

    private fun fetchUserWalletConfigs() {
        viewModelScope.launch {
            getUserWalletConfigsSetupFromCacheUseCase(Unit).collect { result ->
                result.getOrNull()?.let { walletConfigs ->
                    val supportedSigners = convertToSupportedSigners(walletConfigs.supportedSigners)
                    _state.update {
                        it.copy(
                            supportedSignerConfigs = walletConfigs.supportedSigners,
                            supportedSigners = supportedSigners,
                            isGenericAirgapEnable = calculateIsGenericAirgapEnable(supportedSigners),
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
                addressType = AddressType.NATIVE_SEGWIT
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
                _state.update { it.copy(allSigners = mapSigners(singleSigners, masterSigners)) }
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

    private fun filterSignerByType(type: SignerType, tag: SignerTag? = null): List<SignerModel> {
        return state.value.allSigners.filter { signer ->
            signer.type == type || (tag != null && signer.tags.contains(tag))
        }
    }

    fun showExistingSignerOrCreateNew(type: SignerType, tag: SignerTag? = null) {
        viewModelScope.launch {
            val signers = filterSignerByType(
                type,
                tag
            ).filter { signer -> signer.derivationPath.isRecommendedMultiSigPath }
                .let { filterExistingSigners(it) }
            if (signers.isNotEmpty()) {
                _event.emit(SignerIntroEvent.ShowFilteredSigners(type, tag, signers))
            } else {
                _event.emit(SignerIntroEvent.OpenSetupSigner(type, tag))
            }
        }
    }

    private fun filterExistingSigners(signers: List<SignerModel>): List<SignerModel> {
        val existingSigners = onChainAddSignerParam?.existingSigners ?: return signers
        if (existingSigners.isEmpty()) return signers
        return signers.filter { signer ->
            val signerDerivationPath = signer.derivationPath.ifEmpty {
                getPath(index = if (signer.index <= 0) 0 else signer.index)
            }
            existingSigners.none { existing ->
                val existingDerivationPath = existing.derivationPath.ifEmpty {
                    getPath(index = if (existing.index <= 0) 0 else existing.index)
                }
                existing.fingerPrint == signer.fingerPrint && existingDerivationPath == signerDerivationPath
            }
        }
    }

    private fun getPath(index: Int): String {
        return if (isTestNet) "m/48h/1h/${index}h/2h" else "m/48h/0h/${index}h/2h"
    }

    fun resetWizard(plan: MembershipPlan, groupId: String) {
        viewModelScope.launch {
            restartWizardUseCase(RestartWizardUseCase.Param(plan, groupId))
                .onSuccess {
                    membershipStepManager.restart()
                    _event.emit(SignerIntroEvent.RestartWizardSuccess)
                }.onFailure {
                    _event.emit(SignerIntroEvent.Error(it.message.orUnknownError()))
                }
        }
    }

    fun createNewSigner(type: SignerType, tag: SignerTag? = null) {
        viewModelScope.launch {
            _event.emit(SignerIntroEvent.OpenSetupSigner(type, tag))
        }
    }
}

private val offChainInheritanceKeyTypes = listOf(
    SupportedSigner(
        type = SignerType.COLDCARD_NFC,
        tag = null,
        walletType = WalletType.MULTI_SIG,
        addressType = AddressType.NATIVE_SEGWIT
    ),
    SupportedSigner(
        type = SignerType.NFC,
        tag = null,
        walletType = WalletType.MULTI_SIG,
        addressType = AddressType.NATIVE_SEGWIT
    ),
    SupportedSigner(
        type = SignerType.SOFTWARE,
        tag = null,
        walletType = WalletType.MULTI_SIG,
        addressType = AddressType.NATIVE_SEGWIT
    )
)

val defaultSupportedSigners = listOf(
    SupportedSigner(
        type = SignerType.NFC,
        tag = null,
        walletType = WalletType.MULTI_SIG,
        addressType = AddressType.NATIVE_SEGWIT
    ),
    SupportedSigner(
        type = SignerType.COLDCARD_NFC,
        tag = null,
        walletType = WalletType.MULTI_SIG,
        addressType = AddressType.NATIVE_SEGWIT
    ),
    SupportedSigner(
        type = SignerType.AIRGAP,
        tag = SignerTag.JADE,
        walletType = WalletType.MULTI_SIG,
        addressType = AddressType.NATIVE_SEGWIT
    ),
    SupportedSigner(
        type = SignerType.PORTAL_NFC,
        tag = null,
        walletType = WalletType.MULTI_SIG,
        addressType = AddressType.NATIVE_SEGWIT
    ),
    SupportedSigner(
        type = SignerType.AIRGAP,
        tag = SignerTag.SEEDSIGNER,
        walletType = WalletType.MULTI_SIG,
        addressType = AddressType.NATIVE_SEGWIT
    ),
    SupportedSigner(
        type = SignerType.AIRGAP,
        tag = SignerTag.KEYSTONE,
        walletType = WalletType.MULTI_SIG,
        addressType = AddressType.NATIVE_SEGWIT
    ),
    SupportedSigner(
        type = SignerType.AIRGAP,
        tag = SignerTag.PASSPORT,
        walletType = WalletType.MULTI_SIG,
        addressType = AddressType.NATIVE_SEGWIT
    ),
    SupportedSigner(
        type = SignerType.HARDWARE,
        tag = SignerTag.LEDGER,
        walletType = WalletType.MULTI_SIG,
        addressType = AddressType.NATIVE_SEGWIT
    ),
    SupportedSigner(
        type = SignerType.HARDWARE,
        tag = SignerTag.BITBOX,
        walletType = WalletType.MULTI_SIG,
        addressType = AddressType.NATIVE_SEGWIT
    ),
    SupportedSigner(
        type = SignerType.HARDWARE,
        tag = SignerTag.TREZOR,
        walletType = WalletType.MULTI_SIG,
        addressType = AddressType.NATIVE_SEGWIT
    ),
    SupportedSigner(
        type = SignerType.SOFTWARE,
        tag = null,
        walletType = WalletType.MULTI_SIG,
        addressType = AddressType.NATIVE_SEGWIT
    )
)

sealed class SignerIntroEvent {
    data class ShowFilteredSigners(val type: SignerType, val tag: SignerTag?, val signers: List<SignerModel>) :
        SignerIntroEvent()

    data class OpenSetupSigner(val type: SignerType, val tag: SignerTag?) : SignerIntroEvent()
    data object RestartWizardSuccess : SignerIntroEvent()
    data class Error(val message: String) : SignerIntroEvent()
}

