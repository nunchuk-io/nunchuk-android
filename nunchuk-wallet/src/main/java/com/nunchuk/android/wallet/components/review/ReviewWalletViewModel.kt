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

package com.nunchuk.android.wallet.components.review

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.GetTapSignerStatusByIdUseCase
import com.nunchuk.android.core.domain.HasSignerUseCase
import com.nunchuk.android.core.mapper.SingleSignerMapper
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.compose.wallet.pathKey
import com.nunchuk.android.core.signer.toSingleSigner
import com.nunchuk.android.core.util.isPlatformKey
import com.nunchuk.android.core.util.isTaproot
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.listener.GroupDeleteListener
import com.nunchuk.android.listener.GroupSandboxListener
import com.nunchuk.android.nav.args.ReviewWalletArgs
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.type.WalletType.ESCROW
import com.nunchuk.android.type.WalletType.SINGLE_SIG
import com.nunchuk.android.usecase.CreateWalletUseCase
import com.nunchuk.android.usecase.GetNonStandardPathSignersUseCase
import com.nunchuk.android.usecase.free.groupwallet.RemoveSignerFromGroupUseCase
import com.nunchuk.android.usecase.pathKey
import com.nunchuk.android.usecase.free.groupwallet.FinalizeGroupSandboxUseCase
import com.nunchuk.android.usecase.free.groupwallet.GetGroupSandboxUseCase
import com.nunchuk.android.usecase.signer.GetSignerUseCase
import com.nunchuk.android.wallet.components.review.ReviewWalletEvent.CreateWalletErrorEvent
import com.nunchuk.android.wallet.components.review.ReviewWalletEvent.CreateWalletSuccessEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber

internal class ReviewWalletViewModel @AssistedInject constructor(
    @Assisted private val args: ReviewWalletArgs,
    private val createWalletUseCase: CreateWalletUseCase,
    private val accountManager: AccountManager,
    private val getTapSignerStatusByIdUseCase: GetTapSignerStatusByIdUseCase,
    private val getSignerUseCase: GetSignerUseCase,
    private val hasSignerUseCase: HasSignerUseCase,
    private val finalizeGroupSandboxUseCase: FinalizeGroupSandboxUseCase,
    private val getGroupSandboxUseCase: GetGroupSandboxUseCase,
    private val singleSignerMapper: SingleSignerMapper,
    private val getNonStandardPathSignersUseCase: GetNonStandardPathSignersUseCase,
    private val removeSignerFromGroupUseCase: RemoveSignerFromGroupUseCase,
) : NunchukViewModel<Unit, ReviewWalletEvent>() {
    private val _uiState = MutableStateFlow(ReviewWalletUiState(isGroupWallet = args.groupId.isNotEmpty()))
    val uiState = _uiState.asStateFlow()

    private var groupUpdateJob: Job? = null

    // Create wallet waits for both, otherwise a fast tap would skip the compatibility warning.
    private var mapSignersJob: Job? = null
    private var pathCheckJob: Job? = null

    override val initialState = Unit

    init {
        mapSigners()
        checkNonStandardPaths()
        if (args.groupId.isNotEmpty()) {
            listenGroupSandbox()
        }
    }

    private fun listenGroupSandbox() {
        groupUpdateJob = viewModelScope.launch {
            GroupSandboxListener.getGroupFlow().collect { groupSandbox ->
                Timber.d("GroupSandboxListener $groupSandbox")
                if (groupSandbox.id == args.groupId) {
                    if (groupSandbox.finalized) {
                        _uiState.update { it.copy(finalizedWalletId = groupSandbox.walletId) }
                    }
                }
            }
        }
        viewModelScope.launch {
            GroupDeleteListener.groupDeleteFlow.collect { it ->
                Timber.d("GroupDeleteListener $it")
                if (it == args.groupId) {
                    _uiState.update { it.copy(groupWalletUnavailable = true) }
                }
            }
        }
    }

    private fun checkNonStandardPaths() {
        pathCheckJob = viewModelScope.launch {
            val signers = (args.signers + args.namedSigners.mapNotNull { it.signer?.toSingleSigner() })
                .filterNot { it.type.isPlatformKey }
                .distinctBy { it.pathKey() }
            getNonStandardPathSignersUseCase(
                GetNonStandardPathSignersUseCase.Param(
                    signers = signers,
                    addressType = args.addressType,
                    walletType = args.walletType,
                )
            ).onSuccess { keys ->
                _uiState.update { it.copy(nonStandardPathKeys = keys) }
            }
        }
    }

    fun handleContinueEvent() = viewModelScope.launch {
        mapSignersJob?.join()
        pathCheckJob?.join()
        if (_uiState.value.nonStandardSigners.isNotEmpty()) {
            _uiState.update { it.copy(showKeyCompatibilityWarning = true) }
        } else {
            createWallet()
        }
    }

    fun dismissKeyCompatibilityWarning() {
        _uiState.update { it.copy(showKeyCompatibilityWarning = false) }
    }

    fun createAnyway() {
        dismissKeyCompatibilityWarning()
        createWallet()
    }

    /**
     * Removes the non-standard keys from the draft. A personal wallet hands them back to the
     * key-selection screen (it owns the selection); a group wallet removes them from the sandbox,
     * limited to the keys local to this device.
     */
    fun removeNonStandardKeys() {
        dismissKeyCompatibilityWarning()
        val nonStandardSigners = _uiState.value.removableNonStandardSigners
        if (nonStandardSigners.isEmpty()) return
        if (args.groupId.isEmpty()) {
            val keys = nonStandardSigners.map { it.pathKey() }.toSet()
            setEvent(
                ReviewWalletEvent.RemoveNonStandardKeysEvent(
                    args.signers.filter { keys.contains(it.pathKey()) }
                )
            )
        } else {
            removeNonStandardKeysFromGroup(nonStandardSigners)
        }
    }

    private fun removeNonStandardKeysFromGroup(nonStandardSigners: List<SignerModel>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val keys = nonStandardSigners.map { it.pathKey() }.toSet()
            var removedCount = 0
            val result = runCatching {
                if (args.walletType == WalletType.MINISCRIPT) {
                    args.namedSigners
                        .filter { named -> named.signer?.let { keys.contains(it.pathKey()) } == true }
                        .forEach { named ->
                            removeSignerFromGroupUseCase(
                                RemoveSignerFromGroupUseCase.Params(
                                    groupId = args.groupId,
                                    index = -1,
                                    keyName = named.keyName,
                                )
                            ).getOrThrow()
                            removedCount++
                        }
                } else {
                    val group = getGroupSandboxUseCase(args.groupId).getOrThrow()
                    group.signers.forEachIndexed { index, signer ->
                        if (signer.masterFingerprint.isNotEmpty() && keys.contains(signer.pathKey())) {
                            removeSignerFromGroupUseCase(
                                RemoveSignerFromGroupUseCase.Params(groupId = args.groupId, index = index)
                            ).getOrThrow()
                            removedCount++
                        }
                    }
                }
            }
            _uiState.update { it.copy(isLoading = false) }
            result.onSuccess {
                setEvent(ReviewWalletEvent.RemoveNonStandardKeysEvent(emptyList()))
            }.onFailure { error ->
                if (removedCount > 0) {
                    // Part of the draft already changed: Review's signer list is stale, so go back to
                    // key selection (it reloads the sandbox) and report the error there.
                    setEvent(
                        ReviewWalletEvent.RemoveNonStandardKeysEvent(
                            signers = emptyList(),
                            errorMessage = error.message.orUnknownError(),
                        )
                    )
                } else {
                    setEvent(CreateWalletErrorEvent(error.message.orUnknownError()))
                }
            }
        }
    }

    private fun createWallet() {
        if (args.groupId.isNotEmpty()) {
            createFreeGroupWallet()
        } else {
            createNormalWallet()
        }
    }

    private fun createFreeGroupWallet() {
        groupUpdateJob?.cancel()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val signerTaprootIndexes = mutableSetOf<Int>()
            if (args.addressType.isTaproot() && args.isValueKeySetEnable) {
                getGroupSandboxUseCase(args.groupId).onSuccess { group ->
                    val signerIndexes = mutableMapOf<String, Int>()
                    group.signers.mapIndexed { index, singleSigner ->
                        signerIndexes[singleSigner.masterFingerprint] = index
                    }

                    args.signers.take(args.totalRequireSigns).forEach {
                        signerIndexes[it.masterFingerprint]?.let { index ->
                            signerTaprootIndexes.add(index)
                        }
                    }
                }
            }
            finalizeGroupSandboxUseCase(
                FinalizeGroupSandboxUseCase.Params(
                    groupId = args.groupId,
                    signerIndexes = signerTaprootIndexes
                )
            ).onSuccess { groupSandbox ->
                setEvent(ReviewWalletEvent.CreateFreeGroupWalletSuccessEvent(groupSandbox.walletId))
            }.onFailure {
                setEvent(CreateWalletErrorEvent(it.message.orUnknownError()))
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun createNormalWallet() {
        val totalSigns = args.signers.size
        val normalizeWalletType =
            if (args.walletType == ESCROW) ESCROW else if (totalSigns > 1) WalletType.MULTI_SIG else SINGLE_SIG
        viewModelScope.launch {
            createWalletUseCase(
                CreateWalletUseCase.Params(
                    name = args.walletName,
                    totalRequireSigns = args.totalRequireSigns,
                    signers = args.signers,
                    addressType = args.addressType,
                    isEscrow = normalizeWalletType == ESCROW,
                    decoyPin = args.decoyPin,
                    disableValueKeyset = !args.isValueKeySetEnable
                )
            ).onSuccess {
                setEvent(CreateWalletSuccessEvent(it))
            }.onFailure {
                setEvent(CreateWalletErrorEvent(it.message.orUnknownError()))
            }
        }
    }

    private fun mapSigners() {
        mapSignersJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            if (args.groupId.isNotEmpty()) {
                val signers = args.signers.map { signer ->
                    if (hasSignerUseCase(signer).getOrNull() == true) {
                        singleSignerMapper(getSignerUseCase(signer).getOrThrow()).copy(isVisible = true)
                    } else {
                        signer.toModel().copy(isVisible = false)
                    }
                }
                _uiState.update {
                    it.copy(signers = signers)
                }
            } else {
                val signers = args.signers.map {
                    it.toModel(isPrimaryKey = it.hasMasterSigner && accountManager.getPrimaryKeyInfo()?.xfp == it.masterFingerprint)
                }.map {
                    if (it.type == SignerType.NFC) {
                        val status = runBlocking { getTapSignerStatusByIdUseCase(it.id) }
                        return@map it.copy(cardId = status.getOrNull()?.ident.orEmpty())
                    }
                    return@map it
                }
                _uiState.update {
                    it.copy(signers = signers)
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    @AssistedFactory
    internal interface Factory {
        fun create(args: ReviewWalletArgs): ReviewWalletViewModel
    }
}

data class ReviewWalletUiState(
    val signers: List<SignerModel> = emptyList(),
    val groupWalletUnavailable: Boolean = false,
    val isLoading: Boolean = false,
    val finalizedWalletId: String = "",
    val nonStandardPathKeys: Set<String> = emptySet(),
    val showKeyCompatibilityWarning: Boolean = false,
    val isGroupWallet: Boolean = false,
) {
    val nonStandardSigners: List<SignerModel>
        get() = signers.filter { nonStandardPathKeys.contains(it.pathKey()) }.distinctBy { it.pathKey() }

    /**
     * Keys "Remove non-standard keys" may drop. A personal wallet only has local keys. In a group
     * wallet only keys local to this device can be removed: `isVisible` is set from
     * `HasSignerUseCase` in `mapSigners()`, other members' keys are `isVisible = false`.
     */
    val removableNonStandardSigners: List<SignerModel>
        get() = if (isGroupWallet) nonStandardSigners.filter { it.isVisible } else nonStandardSigners
}
