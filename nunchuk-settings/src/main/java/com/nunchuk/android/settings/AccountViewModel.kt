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

package com.nunchuk.android.settings

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.callbacks.SyncFileCallBack
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.ClearInfoSessionUseCase
import com.nunchuk.android.core.domain.membership.GetLocalMembershipPlansFlowUseCase
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.guestmode.isGuestMode
import com.nunchuk.android.core.matrix.UploadFileUseCase
import com.nunchuk.android.core.profile.GetUserProfileUseCase
import com.nunchuk.android.core.profile.SendSignOutUseCase
import com.nunchuk.android.core.profile.UpdateUseProfileUseCase
import com.nunchuk.android.core.util.USD_CURRENCY
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SyncFileEventHelper
import com.nunchuk.android.usecase.GetLocalCurrencyUseCase
import com.nunchuk.android.usecase.GetWalletsUseCase
import com.nunchuk.android.usecase.campaign.GetLocalCurrentCampaignUseCase
import com.nunchuk.android.usecase.campaign.GetLocalReferrerCodeUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@HiltViewModel
internal class AccountViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateUseProfileUseCase: UpdateUseProfileUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val uploadFileUseCase: UploadFileUseCase,
    private val sendSignOutUseCase: SendSignOutUseCase,
    private val appScope: CoroutineScope,
    private val signInModeHolder: SignInModeHolder,
    private val clearInfoSessionUseCase: ClearInfoSessionUseCase,
    private val getLocalCurrencyUseCase: GetLocalCurrencyUseCase,
    private val application: Application,
    private val getLocalMembershipPlansFlowUseCase: GetLocalMembershipPlansFlowUseCase,
    private val getLocalCurrentCampaignUseCase: GetLocalCurrentCampaignUseCase,
    private val getLocalReferrerCodeUseCase: GetLocalReferrerCodeUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : NunchukViewModel<AccountState, AccountEvent>() {

    override val initialState = AccountState()

    init {
        updateState {
            copy(
                account = accountManager.getAccount()
            )
        }

        SyncFileEventHelper.syncFileExecutor = object : SyncFileCallBack {
            override fun onSync(finished: Boolean, progress: Int) {
                postState {
                    copy(
                        syncProgress = progress
                    )
                }
            }
        }
        viewModelScope.launch {
            getLocalCurrencyUseCase(Unit).collect { result ->
                updateState { copy(localCurrency = result.getOrDefault(USD_CURRENCY)) }
            }
        }
        viewModelScope.launch {
            getLocalMembershipPlansFlowUseCase(Unit).collect { result ->
                updateState { copy(plans = result.getOrDefault(emptyList())) }
            }
        }
        viewModelScope.launch {
            getLocalCurrentCampaignUseCase(Unit).collect { result ->
                updateState { copy(campaign = result.getOrDefault(null)) }
            }
        }
        viewModelScope.launch {
            getLocalReferrerCodeUseCase(Unit).distinctUntilChanged().collect {
                updateState { copy(localReferrerCode = it.getOrNull()) }
            }
        }
        viewModelScope.launch {
            getWalletsUseCase.execute()
                .catch { Timber.e(it) }
                .collect { wallets ->
                    updateState {
                        copy(isHasWallet = wallets.isNotEmpty())
                    }
                }
        }
    }

    fun getCurrentAccountInfo() = accountManager.getAccount()

    fun getCurrentUser() {
        if (signInModeHolder.getCurrentMode().isGuestMode().not()) {
            viewModelScope.launch {
                getUserProfileUseCase(Unit)
                    .onSuccess {
                        updateStateUserAccount()
                        event(
                            AccountEvent.GetUserProfileSuccessEvent(
                                name = accountManager.getAccount().name,
                                avatarUrl = accountManager.getAccount().avatarUrl
                            )
                        )
                    }.onFailure {
                        Timber.e(it)
                    }
            }
        }
    }

    fun updateUserProfile(name: String? = null, avatarUrl: String? = null) {
        viewModelScope.launch {
            updateUseProfileUseCase(
                UpdateUseProfileUseCase.Params(
                    name = name,
                    avatarUrl = avatarUrl
                )
            ).onSuccess {
                updateStateUserAccount()
            }
        }
    }

    fun uploadPhotoToMaTrix(uri: Uri) {
        viewModelScope.launch {
            setEvent(AccountEvent.LoadingEvent(true))
            val byteArray = withContext(ioDispatcher) {
                val bitmap = Glide.with(application)
                    .asBitmap()
                    .override(application.resources.getDimensionPixelSize(R.dimen.nc_avatar_size))
                    .load(uri)
                    .submit()
                    .get()
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                byteArrayOutputStream.toByteArray()
            }
            uploadFileUseCase.execute(
                System.currentTimeMillis().toString(),
                "image/jpeg",
                byteArray
            ).onException { setEvent(AccountEvent.ShowError(it.message.orUnknownError())) }
                .onCompletion { setEvent(AccountEvent.LoadingEvent(false)) }
                .collect {
                    updateState { copy(account = getState().account.copy(avatarUrl = it.contentUri)) }
                    setEvent(
                        AccountEvent.UploadPhotoSuccessEvent(matrixUri = it.contentUri)
                    )
                }
        }
    }

    private fun updateStateUserAccount() {
        updateState {
            copy(
                account = accountManager.getAccount()
            )
        }
    }

    fun handleSignOutEvent() {
        appScope.launch {
            event(AccountEvent.LoadingEvent(true))
            signInModeHolder.clear()
            clearInfoSessionUseCase.invoke(Unit)
            sendSignOutUseCase(Unit)
            accountManager.removeAccountBackup()
            event(AccountEvent.SignOutEvent)
        }
    }

    fun getLocalReferrerCode() = getState().localReferrerCode
    fun getCampaign() = getState().campaign

    override fun onCleared() {
        super.onCleared()
        SyncFileEventHelper.syncFileExecutor = object : SyncFileCallBack {
            override fun onSync(finished: Boolean, progress: Int) {}
        }
    }
}