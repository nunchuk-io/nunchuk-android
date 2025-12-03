package com.nunchuk.android.main.membership.onchaintimelock.checkfirmware

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.isRecommendedMultiSigPath
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nav.args.CheckFirmwareArgs
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.GetIndexFromPathUseCase
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

@HiltViewModel
class CheckFirmwareViewModel @Inject constructor(
    private val membershipStepManager: MembershipStepManager,
    private val getAllSignersUseCase: GetAllSignersUseCase,
    private val masterSignerMapper: MasterSignerMapper,
    private val getIndexFromPathUseCase: GetIndexFromPathUseCase,
    private val getUserWalletConfigsSetupFromCacheUseCase: GetUserWalletConfigsSetupFromCacheUseCase,
    private val getUserWalletConfigsSetupUseCase: GetUserWalletConfigsSetupUseCase,
) : ViewModel() {

    val remainTime = membershipStepManager.remainingTime

    private lateinit var args: CheckFirmwareArgs

    private val _filteredSigners = MutableStateFlow<List<SignerModel>>(emptyList())
    val filteredSigners = _filteredSigners.asStateFlow()

    private val _firmwareVersion = MutableStateFlow<String>("")
    val firmwareVersion = _firmwareVersion.asStateFlow()

    private val _event = MutableSharedFlow<CheckFirmwareEvent>()
    val event = _event.asSharedFlow()

    fun init(args: CheckFirmwareArgs) {
        this.args = args
        fetchFirmwareVersion()
        fetchAndFilterSigners()
    }

    private fun fetchFirmwareVersion() {
        viewModelScope.launch {
            getUserWalletConfigsSetupFromCacheUseCase(Unit).collect { result ->
                result.getOrNull()?.let { configs ->
                    val signerTagString = args.signerTag.name
                    val firmware = configs.miniscriptSupportedFirmwares.find { firmware ->
                        firmware.signerTag.equals(signerTagString, ignoreCase = true)
                    }
                    firmware?.let { fw ->
                        _firmwareVersion.update { fw.version }
                    }
                }
            }
        }
    }

    private fun fetchAndFilterSigners() {
        viewModelScope.launch {
            getAllSignersUseCase(true).onSuccess { (masterSigners, singleSigners) ->
                val allSigners = mapSigners(singleSigners, masterSigners)
                val filtered = filterSignersByTypeAndIndex(allSigners)
                _filteredSigners.update { filtered }
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

    private suspend fun filterSignersByTypeAndIndex(signers: List<SignerModel>): List<SignerModel> {
        return signers.filter { signer ->
            // Filter by signer type and tag based on the selected SignerTag
            val matchesType = when (args.signerTag) {
                SignerTag.COLDCARD -> signer.type == SignerType.COLDCARD_NFC || signer.tags.contains(
                    SignerTag.COLDCARD
                )

                SignerTag.JADE -> signer.type == SignerType.AIRGAP && signer.tags.contains(SignerTag.JADE)
                else -> false
            }

            if (!matchesType) return@filter false

            // Filter by derivation path index = 0
            try {
                val index = getIndexFromPathUseCase(signer.derivationPath).getOrThrow()
                index == 0
            } catch (e: Exception) {
                false
            }
        }
    }

    fun onContinueClicked() {
        viewModelScope.launch {
            val signers = _filteredSigners.value.filter { signer -> signer.derivationPath.isRecommendedMultiSigPath }
            if (args.onChainAddSignerParam?.isVerifyBackupSeedPhrase() == true) {
                _event.emit(CheckFirmwareEvent.OpenNextScreen)
            } else if (signers.isNotEmpty()) {
                _event.emit(CheckFirmwareEvent.ShowFilteredSigners(signers))
            } else {
                _event.emit(CheckFirmwareEvent.OpenNextScreen)
            }
        }
    }
}

sealed class CheckFirmwareEvent {
    data class ShowFilteredSigners(val signers: List<SignerModel>) : CheckFirmwareEvent()
    object OpenNextScreen : CheckFirmwareEvent()
}

