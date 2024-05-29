/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.signer.mk4.recover

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.settings.GetChainSettingFlowUseCase
import com.nunchuk.android.core.util.COLDCARD_DEFAULT_KEY_NAME
import com.nunchuk.android.core.util.getFileFromUri
import com.nunchuk.android.core.util.gson
import com.nunchuk.android.core.util.isRecommendedPath
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MembershipStepInfo
import com.nunchuk.android.model.SignerExtra
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.signer.util.isTestNetPath
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.CreateSignerUseCase
import com.nunchuk.android.usecase.ParseJsonSignerUseCase
import com.nunchuk.android.usecase.membership.SaveMembershipStepUseCase
import com.nunchuk.android.usecase.membership.SyncKeyToGroupUseCase
import com.nunchuk.android.usecase.replace.ReplaceKeyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
    private val syncKeyToGroupUseCase: SyncKeyToGroupUseCase,
    private val getChainSettingFlowUseCase: GetChainSettingFlowUseCase,
    private val replaceKeyUseCase: ReplaceKeyUseCase
) : ViewModel() {
    private val _event = MutableSharedFlow<ColdcardRecoverEvent>()
    val event = _event.asSharedFlow()

    val remainTime = membershipStepManager.remainingTime

    private var chain: Chain = Chain.MAIN

    init {
        viewModelScope.launch {
            chain = getChainSettingFlowUseCase(Unit).map { it.getOrElse { Chain.MAIN } }.first()
        }
    }

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

    fun parseColdcardSigner(
        uri: Uri,
        groupId: String,
        newIndex: Int,
        replacedXfp: String?,
        walletId: String?
    ) {
        viewModelScope.launch {
            _event.emit(ColdcardRecoverEvent.LoadingEvent(true))
            withContext(ioDispatcher) {
                getFileFromUri(application.contentResolver, uri, application.cacheDir)?.readText()
            }?.let { content ->
                val parseResult = parseJsonSignerUseCase(
                    ParseJsonSignerUseCase.Params(content, SignerType.AIRGAP)
                )
                if (parseResult.isFailure) {
                    _event.emit(ColdcardRecoverEvent.ParseFileError)
                    _event.emit(ColdcardRecoverEvent.LoadingEvent(false))
                    return@launch
                }
                handleSigner(
                    singleSigners = parseResult.getOrThrow(),
                    groupId = groupId,
                    newIndex = newIndex,
                    replacedXfp = replacedXfp,
                    walletId = walletId
                )
            }
            _event.emit(ColdcardRecoverEvent.LoadingEvent(false))
        }
    }

    fun handleSigner(
        singleSigners: List<SingleSigner>,
        groupId: String,
        newIndex: Int,
        replacedXfp: String?,
        walletId: String?
    ) =
        viewModelScope.launch {
            val signer =
                singleSigners.find { it.derivationPath.isRecommendedPath }
            if (signer == null) {
                _event.emit(ColdcardRecoverEvent.ShowError("Can not find valid signer path"))
                _event.emit(ColdcardRecoverEvent.LoadingEvent(false))
                return@launch
            }
            if (chain == Chain.MAIN && isTestNetPath(signer.derivationPath)) {
                _event.emit(ColdcardRecoverEvent.ErrorMk4TestNet)
                _event.emit(ColdcardRecoverEvent.LoadingEvent(false))
                return@launch
            }
            if (newIndex >= 0 && !signer.derivationPath.endsWith("${newIndex}h/2h")) {
                _event.emit(ColdcardRecoverEvent.NewIndexNotMatchException)
                _event.emit(ColdcardRecoverEvent.LoadingEvent(false))
                return@launch
            }
            if (membershipStepManager.isKeyExisted(signer.masterFingerprint)) {
                _event.emit(ColdcardRecoverEvent.AddSameKey)
                _event.emit(ColdcardRecoverEvent.LoadingEvent(false))
                return@launch
            }
            val createSignerResult = createSignerUseCase(
                CreateSignerUseCase.Params(
                    name = "$COLDCARD_DEFAULT_KEY_NAME${
                        membershipStepManager.getNextKeySuffixByType(SignerType.COLDCARD_NFC)
                    }",
                    xpub = signer.xpub,
                    derivationPath = signer.derivationPath,
                    masterFingerprint = signer.masterFingerprint,
                    type = SignerType.AIRGAP,
                    tags = listOf(SignerTag.COLDCARD)
                )
            )
            if (createSignerResult.isFailure) {
                _event.emit(ColdcardRecoverEvent.ShowError(createSignerResult.exceptionOrNull()?.message.orUnknownError()))
                _event.emit(ColdcardRecoverEvent.LoadingEvent(false))
                return@launch
            }
            val coldcardSigner = createSignerResult.getOrThrow()
            if (replacedXfp.isNullOrEmpty()) {
                saveMembershipStepUseCase(
                    MembershipStepInfo(
                        step = membershipStepManager.currentStep
                            ?: throw IllegalArgumentException("Current step empty"),
                        masterSignerId = coldcardSigner.masterFingerprint,
                        plan = membershipStepManager.localMembershipPlan,
                        verifyType = VerifyType.APP_VERIFIED,
                        extraData = gson.toJson(
                            SignerExtra(
                                derivationPath = coldcardSigner.derivationPath,
                                isAddNew = true,
                                signerType = coldcardSigner.type
                            )
                        ),
                        groupId = groupId
                    )
                )
                if (groupId.isNotEmpty()) {
                    syncKeyToGroupUseCase(
                        SyncKeyToGroupUseCase.Param(
                            step = membershipStepManager.currentStep
                                ?: throw IllegalArgumentException("Current step empty"),
                            groupId = groupId,
                            signer = coldcardSigner
                        )
                    ).onFailure {
                        _event.emit(ColdcardRecoverEvent.ShowError(it.message.orUnknownError()))
                    }
                }
            } else {
                replaceKeyUseCase(
                    ReplaceKeyUseCase.Param(
                        groupId = groupId,
                        xfp = replacedXfp,
                        walletId = walletId.orEmpty(),
                        signer = coldcardSigner
                    )
                ).onFailure {
                    _event.emit(ColdcardRecoverEvent.ShowError(it.message.orUnknownError()))
                }
            }
            _event.emit(ColdcardRecoverEvent.CreateSignerSuccess)
        }
}

sealed class ColdcardRecoverEvent {
    class LoadingEvent(val isLoading: Boolean) : ColdcardRecoverEvent()
    class ShowError(val message: String) : ColdcardRecoverEvent()
    data object OnOpenGuide : ColdcardRecoverEvent()
    data object OnContinue : ColdcardRecoverEvent()
    data object CreateSignerSuccess : ColdcardRecoverEvent()
    data object AddSameKey : ColdcardRecoverEvent()
    data object ParseFileError : ColdcardRecoverEvent()
    data object NewIndexNotMatchException : ColdcardRecoverEvent()
    object ErrorMk4TestNet : ColdcardRecoverEvent()
}