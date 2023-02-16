/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.guestmode.isGuestMode
import com.nunchuk.android.core.matrix.UploadFileUseCase
import com.nunchuk.android.core.profile.GetUserProfileUseCase
import com.nunchuk.android.core.profile.SendSignOutUseCase
import com.nunchuk.android.core.profile.UpdateUseProfileUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.SyncFileEventHelper
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@HiltViewModel
internal class AccountViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateUseProfileUseCase: UpdateUseProfileUseCase,
    private val uploadFileUseCase: UploadFileUseCase,
    private val sendSignOutUseCase: SendSignOutUseCase,
    private val appScope: CoroutineScope,
    private val signInModeHolder: SignInModeHolder,
    private val clearInfoSessionUseCase: ClearInfoSessionUseCase,
    private val membershipStepManager: MembershipStepManager,
    private val application: Application,
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
    }

    fun getCurrentAccountInfo() = accountManager.getAccount()

    fun getCurrentUser() {
        if (signInModeHolder.getCurrentMode().isGuestMode()) {
            event(AccountEvent.GetUserProfileGuestEvent)
        } else {
            viewModelScope.launch {
                getUserProfileUseCase.execute()
                    .flowOn(Dispatchers.IO)
                    .onException { }
                    .flowOn(Dispatchers.Main)
                    .collect {
                        updateStateUserAccount()
                        event(
                            AccountEvent.GetUserProfileSuccessEvent(
                                name = accountManager.getAccount().name,
                                avatarUrl = accountManager.getAccount().avatarUrl
                            )
                        )
                    }
            }
        }
    }

    fun updateUserProfile(name: String? = null, avatarUrl: String? = null) {
        viewModelScope.launch {
            updateUseProfileUseCase.execute(name, avatarUrl)
                .flowOn(Dispatchers.IO)
                .onException { }
                .flowOn(Dispatchers.Main)
                .collect {
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
            clearInfoSessionUseCase.invoke(Unit)
            sendSignOutUseCase(Unit)
            event(AccountEvent.SignOutEvent)
        }
    }

    val plan: MembershipPlan
        get() = membershipStepManager.plan

    override fun onCleared() {
        super.onCleared()
        SyncFileEventHelper.syncFileExecutor = object : SyncFileCallBack {
            override fun onSync(finished: Boolean, progress: Int) {}
        }
    }
}