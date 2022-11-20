package com.nunchuk.android.signer.mk4.recover

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.COLDCARD_DEFAULT_KEY_NAME
import com.nunchuk.android.core.util.getFileFromUri
import com.nunchuk.android.core.util.gson
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MembershipStepInfo
import com.nunchuk.android.model.SignerExtra
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.CreateSignerUseCase
import com.nunchuk.android.usecase.ParseJsonSignerUseCase
import com.nunchuk.android.usecase.membership.SaveMembershipStepUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ColdcardRecoverViewModel @Inject constructor(
    private val membershipStepManager: MembershipStepManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val application: Application,
    private val parseJsonSignerUseCase: ParseJsonSignerUseCase,
    private val saveMembershipStepUseCase: SaveMembershipStepUseCase,
    private val createSignerUseCase: CreateSignerUseCase,
) : ViewModel() {
    private val _event = MutableSharedFlow<ColdcardRecoverEvent>()
    val event = _event.asSharedFlow()

    val remainTime = membershipStepManager.remainingTime

    fun onContinueClicked() {
        viewModelScope.launch {
            _event.emit(ColdcardRecoverEvent.OnContinue)
        }
    }

    fun onOpenGuideClicked() {
        viewModelScope.launch {
            _event.emit(ColdcardRecoverEvent.OnOpenGuide)
        }
    }

    fun parseColdcardSigner(uri: Uri) {
        viewModelScope.launch {
            _event.emit(ColdcardRecoverEvent.LoadingEvent(true))
            withContext(ioDispatcher) {
                getFileFromUri(application.contentResolver, uri, application.cacheDir)?.readText()
            }?.let { content ->
                val parseResult = parseJsonSignerUseCase(
                    ParseJsonSignerUseCase.Params(content, SignerType.COLDCARD_NFC)
                )
                if (parseResult.isFailure) {
                    _event.emit(ColdcardRecoverEvent.ShowError(parseResult.exceptionOrNull()?.message.orUnknownError()))
                    return@launch
                }
                val signer = parseResult.getOrThrow().first()
                val createSignerResult = createSignerUseCase(
                    CreateSignerUseCase.Params(
                        name = "$COLDCARD_DEFAULT_KEY_NAME${
                            membershipStepManager.getNextKeySuffixByType(SignerType.COLDCARD_NFC)
                        }",
                        xpub = signer.xpub,
                        derivationPath = signer.derivationPath,
                        masterFingerprint = signer.masterFingerprint,
                        type = SignerType.COLDCARD_NFC
                    )
                )
                if (createSignerResult.isFailure) {
                    _event.emit(ColdcardRecoverEvent.ShowError(createSignerResult.exceptionOrNull()?.message.orUnknownError()))
                    return@launch
                }
                val coldcardSigner = createSignerResult.getOrThrow()
                saveMembershipStepUseCase(
                    MembershipStepInfo(
                        step = membershipStepManager.currentStep
                            ?: throw IllegalArgumentException("Current step empty"),
                        masterSignerId = coldcardSigner.masterFingerprint,
                        plan = membershipStepManager.plan,
                        isVerify = true,
                        extraData = gson.toJson(
                            SignerExtra(
                                derivationPath = coldcardSigner.derivationPath,
                                isAddNew = true,
                                signerType = coldcardSigner.type
                            )
                        )
                    )
                )
                _event.emit(ColdcardRecoverEvent.CreateSignerSuccess)
            }
            _event.emit(ColdcardRecoverEvent.LoadingEvent(false))
        }
    }
}

sealed class ColdcardRecoverEvent {
    class LoadingEvent(val isLoading: Boolean) : ColdcardRecoverEvent()
    class ShowError(val message: String) : ColdcardRecoverEvent()
    object OnOpenGuide : ColdcardRecoverEvent()
    object OnContinue : ColdcardRecoverEvent()
    object CreateSignerSuccess : ColdcardRecoverEvent()
}