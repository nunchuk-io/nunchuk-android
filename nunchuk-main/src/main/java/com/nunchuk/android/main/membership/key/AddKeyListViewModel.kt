package com.nunchuk.android.main.membership.key

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.nunchuk.android.core.domain.utils.NfcFileManager
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.SIGNER_PATH_PREFIX
import com.nunchuk.android.main.membership.model.AddKeyData
import com.nunchuk.android.model.*
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.GetCompoundSignersUseCase
import com.nunchuk.android.usecase.GetMasterSignerUseCase
import com.nunchuk.android.usecase.GetRemoteSignerUseCase
import com.nunchuk.android.usecase.GetRemoteSignersUseCase
import com.nunchuk.android.usecase.membership.GetMembershipStepUseCase
import com.nunchuk.android.usecase.membership.SaveMembershipStepUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddKeyListViewModel @Inject constructor(
    private val getCompoundSignersUseCase: GetCompoundSignersUseCase,
    private val savedStateHandle: SavedStateHandle,
    getMembershipStepUseCase: GetMembershipStepUseCase,
    private val getMasterSignerUseCase: GetMasterSignerUseCase,
    private val membershipStepManager: MembershipStepManager,
    private val nfcFileManager: NfcFileManager,
    private val masterSignerMapper: MasterSignerMapper,
    private val getRemoteSignersUseCase: GetRemoteSignersUseCase,
    private val getRemoteSignerUseCase: GetRemoteSignerUseCase,
    private val saveMembershipStepUseCase: SaveMembershipStepUseCase,
    private val gson: Gson,
) : ViewModel() {
    private val _state = MutableStateFlow(AddKeyListState())
    private val _event = MutableSharedFlow<AddKeyListEvent>()
    val event = _event.asSharedFlow()

    private val currentStep =
        savedStateHandle.getStateFlow<MembershipStep?>(KEY_CURRENT_STEP, null)

    private val membershipStepState = getMembershipStepUseCase(membershipStepManager.plan)
        .map { it.getOrElse { emptyList() } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _keys = MutableStateFlow(listOf<AddKeyData>())
    val key = _keys.asStateFlow()

    init {
        viewModelScope.launch {
            currentStep.filterNotNull().collect {
                membershipStepManager.setCurrentStep(it)
            }
        }
        if (membershipStepManager.plan == MembershipPlan.IRON_HAND) {
            _keys.value = listOf(
                AddKeyData(type = MembershipStep.ADD_TAP_SIGNER_1),
                AddKeyData(type = MembershipStep.ADD_TAP_SIGNER_2),
                AddKeyData(type = MembershipStep.ADD_SEVER_KEY),
            )
        } else {
            _keys.value = listOf(
                AddKeyData(type = MembershipStep.HONEY_ADD_TAP_SIGNER),
                AddKeyData(type = MembershipStep.HONEY_ADD_HARDWARE_KEY_1),
                AddKeyData(type = MembershipStep.HONEY_ADD_HARDWARE_KEY_2),
                AddKeyData(type = MembershipStep.ADD_SEVER_KEY),
            )
        }
        viewModelScope.launch {
            getCompoundSignersUseCase.execute().collect { pair ->
                _state.update {
                    it.copy(
                        signers = pair.first.map { signer ->
                            masterSignerMapper(signer)
                        } + pair.second.map { signer -> signer.toModel() }
                    )
                }
            }
        }
        viewModelScope.launch {
            getRemoteSignersUseCase.execute().collect {

            }
        }
        viewModelScope.launch {
            membershipStepState.collect {
                val news = _keys.value.map { addKeyData ->
                    val info = getStepInfo(addKeyData.type)
                    if (addKeyData.signer == null && info.masterSignerId.isNotEmpty()) {
                        val extra = runCatching {
                            gson.fromJson(
                                info.extraData,
                                SignerExtra::class.java
                            )
                        }.getOrNull()
                        if (extra?.signerType == SignerType.COLDCARD_NFC || extra?.signerType == SignerType.AIRGAP) {
                            val result = getRemoteSignerUseCase(GetRemoteSignerUseCase.Data(info.masterSignerId, extra.derivationPath))
                            if (result.isSuccess) {
                                return@map addKeyData.copy(
                                    signer = result.getOrThrow().toModel(),
                                    verifyType = info.verifyType
                                )
                            }
                        } else {
                            val result = getMasterSignerUseCase(info.masterSignerId)
                            if (result.isSuccess) {
                                return@map addKeyData.copy(
                                    signer = masterSignerMapper(
                                        result.getOrThrow(),
                                    ),
                                    verifyType = info.verifyType
                                )
                            }
                        }
                    }
                    addKeyData.copy(verifyType = info.verifyType)
                }
                _keys.value = news
            }
        }
    }

    // COLDCARD or Airgap
    fun onSelectedExistingHardwareSigner(signer: SignerModel) {
        viewModelScope.launch {
            if (isSignerExist(signer.fingerPrint)) {
                _event.emit(AddKeyListEvent.OnAddSameKey)
            } else {
                saveMembershipStepUseCase(
                    MembershipStepInfo(
                        step = membershipStepManager.currentStep
                            ?: throw IllegalArgumentException("Current step empty"),
                        masterSignerId = signer.fingerPrint,
                        plan = membershipStepManager.plan,
                        verifyType = VerifyType.APP_VERIFIED,
                        extraData = gson.toJson(
                            SignerExtra(
                                derivationPath = signer.derivationPath,
                                isAddNew = false,
                                signerType = signer.type
                            )
                        )
                    )
                )
            }
        }
    }

    fun onAddKeyClicked(data: AddKeyData) {
        viewModelScope.launch {
            savedStateHandle[KEY_CURRENT_STEP] = data.type
            _event.emit(AddKeyListEvent.OnAddKey(data))
        }
    }

    fun isSignerExist(masterSignerId: String) = membershipStepManager.isKeyExisted(masterSignerId)

    fun onVerifyClicked(data: AddKeyData) {
        data.signer?.let { signer ->
            savedStateHandle[KEY_CURRENT_STEP] = data.type
            viewModelScope.launch {
                val stepInfo = getStepInfo(data.type)
                _event.emit(
                    AddKeyListEvent.OnVerifySigner(
                        signer = signer,
                        filePath = nfcFileManager.buildFilePath(stepInfo.keyIdInServer)
                    )
                )
            }
        }
    }

    fun onContinueClicked() {
        viewModelScope.launch {
            _event.emit(AddKeyListEvent.OnAddAllKey)
        }
    }

    private fun getStepInfo(step: MembershipStep) =
        membershipStepState.value.find { it.step == step } ?: run {
            MembershipStepInfo(step = step, plan = membershipStepManager.plan)
        }

    fun getTapSigners() = _state.value.signers.filter { it.type == SignerType.NFC }

    fun getColdcard() = _state.value.signers.filter {
        it.type == SignerType.COLDCARD_NFC
                && it.derivationPath.contains(SIGNER_PATH_PREFIX)
    }

    fun getAirgap() = _state.value.signers.filter { it.type == SignerType.AIRGAP }

    companion object {
        private const val KEY_CURRENT_STEP = "current_step"
    }
}

sealed class AddKeyListEvent {
    data class OnAddKey(val data: AddKeyData) : AddKeyListEvent()
    data class OnVerifySigner(val signer: SignerModel, val filePath: String) : AddKeyListEvent()
    object OnAddSameKey : AddKeyListEvent()
    object OnAddAllKey : AddKeyListEvent()
}

data class AddKeyListState(val signers: List<SignerModel> = emptyList())