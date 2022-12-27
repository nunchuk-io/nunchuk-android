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

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.callbacks.SyncFileCallBack
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.ClearInfoSessionUseCase
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.guestmode.isGuestMode
import com.nunchuk.android.core.matrix.UploadFileUseCase
import com.nunchuk.android.core.profile.GetUserProfileUseCase
import com.nunchuk.android.core.profile.UpdateUseProfileUseCase
import com.nunchuk.android.core.profile.UserProfileRepository
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.SyncFileEventHelper
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

@HiltViewModel
internal class AccountViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateUseProfileUseCase: UpdateUseProfileUseCase,
    private val uploadFileUseCase: UploadFileUseCase,
    private val repository: UserProfileRepository,
    private val appScope: CoroutineScope,
    private val signInModeHolder: SignInModeHolder,
    private val clearInfoSessionUseCase: ClearInfoSessionUseCase,
    private val membershipStepManager: MembershipStepManager,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
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

    fun uploadPhotoToMaTrix(fileData: ByteArray) {
        viewModelScope.launch {
            uploadFileUseCase.execute(System.currentTimeMillis().toString(), "image/jpeg", fileData)
                .flowOn(Dispatchers.IO)
                .onException { }
                .flowOn(Dispatchers.Main)
                .collect {
                    event(
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
        appScope.launch(dispatcher) {
            event(AccountEvent.LoadingEvent(true))
            repository.signOut()
                .flowOn(Dispatchers.IO)
                .onException {
                    event(AccountEvent.LoadingEvent(false))
                }
                .first()
            clearInfoSessionUseCase.invoke(Unit)
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