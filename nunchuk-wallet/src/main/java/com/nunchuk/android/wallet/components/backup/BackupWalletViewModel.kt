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

package com.nunchuk.android.wallet.components.backup

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.GetWalletBannerStateUseCase
import com.nunchuk.android.core.domain.RemoveWalletBannerStateUseCase
import com.nunchuk.android.core.domain.UpdateWalletBannerStateUseCase
import com.nunchuk.android.core.domain.wallet.GetWalletBsmsUseCase
import com.nunchuk.android.core.util.isColdCard
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.BannerState
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.CreateShareFileUseCase
import com.nunchuk.android.usecase.SaveLocalFileUseCase
import com.nunchuk.android.wallet.components.backup.BackupWalletEvent.Failure
import com.nunchuk.android.wallet.components.backup.BackupWalletEvent.Success
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class BackupWalletState(
    val wallet: Wallet? = null,
    val hasHardwareOrAirgapSigner: Boolean = false,
    val bannerState: BannerState? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
internal class BackupWalletViewModel @Inject constructor(
    private val createShareFileUseCase: CreateShareFileUseCase,
    private val getWalletBsmsUseCase: GetWalletBsmsUseCase,
    private val getWalletBannerStateUseCase: GetWalletBannerStateUseCase,
    private val updateWalletBannerStateUseCase: UpdateWalletBannerStateUseCase,
    private val removeWalletBannerStateUseCase: RemoveWalletBannerStateUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val saveLocalFileUseCase: SaveLocalFileUseCase
) : NunchukViewModel<BackupWalletState, BackupWalletEvent>() {

    override val initialState = BackupWalletState()

    fun init(wallet: Wallet) {
        val hasHardwareOrAirgapSigner = wallet.signers.any {
            it.type == SignerType.HARDWARE || (it.type == SignerType.AIRGAP && !it.isColdCard)
        }
        
        updateState { 
            copy(
                wallet = wallet,
                hasHardwareOrAirgapSigner = hasHardwareOrAirgapSigner
            )
        }
        
        // Get the current banner state for this wallet
        viewModelScope.launch {
            getWalletBannerStateUseCase(wallet.id).onSuccess { bannerState ->
                updateState { 
                    copy(bannerState = bannerState)
                }
            }
        }
    }

    fun handleBackupDescriptorEvent() {
        val currentWallet = getState().wallet ?: return
        
        updateState { copy(isLoading = true) }
        
        viewModelScope.launch {
            when (val event = createShareFileUseCase.execute("${currentWallet.id}.bsms")) {
                is Result.Success -> exportWallet(event.data, currentWallet)
                is Result.Error -> {
                    updateState { copy(isLoading = false) }
                    event(Failure(event.exception.message.orUnknownError()))
                }
            }
        }
    }

    private fun exportWallet(filePath: String, wallet: Wallet) {
        viewModelScope.launch {
            getWalletBsmsUseCase(wallet).onSuccess {
                withContext(ioDispatcher) {
                    File(filePath).writeText(it)
                }
                updateState { copy(isLoading = false) }
                event(Success(filePath))
            }.onFailure {
                updateState { copy(isLoading = false) }
                event(Failure(it.message.orUnknownError()))
            }
        }
    }

    fun saveBSMSToLocal() {
        val currentWallet = getState().wallet ?: return
        
        updateState { copy(isLoading = true) }
        
        viewModelScope.launch {
            getWalletBsmsUseCase(currentWallet).onSuccess {
                val result = saveLocalFileUseCase(SaveLocalFileUseCase.Params("${currentWallet.id}.bsms", it))
                updateState { copy(isLoading = false) }
                event(BackupWalletEvent.SaveLocalFile(result.isSuccess))
            }.onFailure {
                updateState { copy(isLoading = false) }
                event(BackupWalletEvent.SaveLocalFile(false))
            }
        }
    }

    fun updateBannerState(newState: BannerState) {
        val walletId = getState().wallet?.id ?: return
        viewModelScope.launch {
            updateWalletBannerStateUseCase(
                UpdateWalletBannerStateUseCase.Param(walletId, newState)
            ).onSuccess {
                updateState { copy(bannerState = newState) }
            }.onFailure {
                // Handle error silently - banner state updates are not critical
            }
        }
    }

    fun removeBannerState() {
        val walletId = getState().wallet?.id ?: return
        viewModelScope.launch {
            removeWalletBannerStateUseCase(walletId).onSuccess {
                updateState { copy(bannerState = null) }
            }.onFailure {
                // Handle error silently - banner state removal is not critical
            }
        }
    }

    fun getBannerState(): BannerState? = getState().bannerState
    
    fun hasHardwareOrAirgapSigner(): Boolean = getState().hasHardwareOrAirgapSigner
    
    fun getWalletId(): String? = getState().wallet?.id
}