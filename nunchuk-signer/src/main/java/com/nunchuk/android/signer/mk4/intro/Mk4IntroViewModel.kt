package com.nunchuk.android.signer.mk4.intro

import android.nfc.NdefRecord
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.CreateMk4SignerUseCase
import com.nunchuk.android.core.domain.GetMk4SingersUseCase
import com.nunchuk.android.core.util.COLDCARD_DEFAULT_KEY_NAME
import com.nunchuk.android.core.util.SIGNER_PATH_PREFIX
import com.nunchuk.android.core.util.gson
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.MembershipStepInfo
import com.nunchuk.android.model.SignerExtra
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.membership.SaveMembershipStepUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class Mk4IntroViewModel @Inject constructor(
    private val getMk4SingersUseCase: GetMk4SingersUseCase,
    private val createMk4SignerUseCase: CreateMk4SignerUseCase,
    private val saveMembershipStepUseCase: SaveMembershipStepUseCase,
    private val membershipStepManager: MembershipStepManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _event = MutableSharedFlow<Mk4IntroViewEvent>()
    private val args: Mk4IntroFragmentArgs =
        Mk4IntroFragmentArgs.fromSavedStateHandle(savedStateHandle)
    val event = _event.asSharedFlow()

    val remainTime = membershipStepManager.remainingTime

    private val _mk4Signers = mutableListOf<SingleSigner>()

    val mk4Signers: List<SingleSigner>
        get() = _mk4Signers

    fun getMk4Signer(records: List<NdefRecord>) {
        viewModelScope.launch {
            _event.emit(Mk4IntroViewEvent.Loading(true))
            val result = getMk4SingersUseCase(records.toTypedArray())
            if (result.isSuccess) {
                if (args.isMembershipFlow) {
                    val sortedSigner = result.getOrThrow().sortedBy { it.derivationPath }
                    val signer = sortedSigner.find { it.derivationPath == SIGNER_PATH } ?: run {
                        sortedSigner.find { it.derivationPath.contains(SIGNER_PATH_PREFIX) }
                    } ?: throw NullPointerException("Can not find signer")
                    if (membershipStepManager.isKeyExisted(signer.masterFingerprint)) {
                        _event.emit(Mk4IntroViewEvent.OnSignerExistInAssistedWallet)
                        _event.emit(Mk4IntroViewEvent.Loading(false))
                        return@launch
                    }
                    val createSignerResult = createMk4SignerUseCase(
                        signer.copy(
                            name = "$COLDCARD_DEFAULT_KEY_NAME${
                                membershipStepManager.getNextKeySuffixByType(SignerType.COLDCARD_NFC)
                            }"
                        )
                    )
                    if (createSignerResult.isSuccess) {
                        val coldcardSigner = createSignerResult.getOrThrow()
                        saveMembershipStepUseCase(
                            MembershipStepInfo(
                                step = membershipStepManager.currentStep
                                    ?: throw IllegalArgumentException("Current step empty"),
                                masterSignerId = coldcardSigner.masterFingerprint,
                                plan = membershipStepManager.plan,
                                verifyType = VerifyType.APP_VERIFIED,
                                extraData = gson.toJson(
                                    SignerExtra(
                                        derivationPath = coldcardSigner.derivationPath,
                                        isAddNew = true,
                                        signerType = coldcardSigner.type
                                    )
                                )
                            )
                        )
                        _event.emit(Mk4IntroViewEvent.OnCreateSignerSuccess)
                    } else {
                        _event.emit(Mk4IntroViewEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
                    }
                } else {
                    _mk4Signers.apply {
                        clear()
                        addAll(result.getOrThrow())
                    }
                    _event.emit(Mk4IntroViewEvent.LoadMk4SignersSuccess(_mk4Signers))
                }
            } else {
                _event.emit(Mk4IntroViewEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
            _event.emit(Mk4IntroViewEvent.Loading(false))
        }
    }

    fun onContinueClicked() {
        viewModelScope.launch {
            _event.emit(Mk4IntroViewEvent.OnContinueClicked)
        }
    }

    companion object {
        private const val SIGNER_PATH = "m/48h/0h/0h/1h"
    }
}