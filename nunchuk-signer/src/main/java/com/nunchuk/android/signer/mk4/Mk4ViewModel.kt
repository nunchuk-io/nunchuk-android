package com.nunchuk.android.signer.mk4

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.SaveMembershipExistingColdCardUseCase
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.isRecommendedMultiSigPath
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.signer.GetAllSignersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class Mk4ViewModel @Inject constructor(
    private val saveMembershipExistingColdCardUseCase: SaveMembershipExistingColdCardUseCase,
    private val membershipStepManager: MembershipStepManager,
    private val getAllSignersUseCase: GetAllSignersUseCase,
    private val masterSignerMapper: MasterSignerMapper,
) : ViewModel() {

    private val _event = MutableSharedFlow<Mk4Event>()
    val event = _event.asSharedFlow()

    lateinit var coldCardBackUpParam: ColdCardBackUpParam
        private set

    var existingSigner: SignerModel? = null

    fun setOrUpdate(param: ColdCardBackUpParam) {
        coldCardBackUpParam = param

        if (coldCardBackUpParam.xfp.isNotEmpty()) {
            getColdcardSigners()
        }
    }

    private fun getColdcardSigners() = viewModelScope.launch {
        getAllSignersUseCase(false).onSuccess { pair ->
            val signers = pair.first.map { signer ->
                masterSignerMapper(signer)
            } + pair.second.map { signer -> signer.toModel() }
            val coldCard = getColdcard(signers)
            existingSigner = coldCard.firstOrNull { it.fingerPrint == coldCardBackUpParam.xfp }
        }
    }

    private fun getColdcard(signers: List<SignerModel>) = signers.filter {
        ((it.type == SignerType.COLDCARD_NFC && it.derivationPath.isRecommendedMultiSigPath)
                || (it.type == SignerType.AIRGAP && (it.tags.isEmpty() || it.tags.contains(SignerTag.COLDCARD))))
    }

    fun getColdCardExistingSigner(): SignerModel? {
        return existingSigner ?: run {
            Timber.e("Existing signer is null")
            null
        }
    }


    fun saveMembershipExistingColdCard() = viewModelScope.launch {
        _event.emit(Mk4Event.Loading(true))
        existingSigner ?: return@launch
        saveMembershipExistingColdCardUseCase(
            SaveMembershipExistingColdCardUseCase.Params(
                step = membershipStepManager.currentStep
                    ?: throw IllegalArgumentException("Current step empty"),
                plan = membershipStepManager.localMembershipPlan,
                groupId = coldCardBackUpParam.groupId,
                signer = existingSigner!!
            )
        ).onSuccess {
            _event.emit(Mk4Event.Success)
        }.onFailure {
            Timber.e(it)
        }
        _event.emit(Mk4Event.Loading(false))
    }
}

sealed class Mk4Event {
    data class Loading(val isLoading: Boolean) : Mk4Event()
    data object Success : Mk4Event()
}

data class ColdCardBackUpParam(
    val isHasPassphrase: Boolean = false,
    val xfp: String,
    val keyType: SignerType,
    val filePath: String,
    val keyName: String,
    val backUpFileName: String,
    val keyId: String,
    val isRequestAddOrReplaceKey: Boolean,
    val groupId: String = "",
)