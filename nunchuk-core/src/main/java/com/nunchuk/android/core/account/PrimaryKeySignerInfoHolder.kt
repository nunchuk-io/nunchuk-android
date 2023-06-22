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

package com.nunchuk.android.core.account

import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.PrimaryKey
import com.nunchuk.android.usecase.GetMasterSignerUseCase
import com.nunchuk.android.usecase.GetPrimaryKeyListUseCase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrimaryKeySignerInfoHolder @Inject constructor(
    private val accountManager: AccountManager,
    private val getMasterSignerUseCase: GetMasterSignerUseCase,
    private val getPrimaryKeyListUseCase: GetPrimaryKeyListUseCase
) {

    private var masterSigner: MasterSigner? = null
    private var primaryKey: PrimaryKey? = null
    private val signerMutex = Mutex()
    private val primaryKeyMutex = Mutex()

    fun setSignerInfo(masterSigner: MasterSigner) {
        this.masterSigner = masterSigner
    }

    suspend fun getSignerInfo(forceNewData: Boolean = false): MasterSigner? {
        signerMutex.withLock {
            if (masterSigner != null && forceNewData.not()) return masterSigner
            val signerId = accountManager.getPrimaryKeyInfo()?.xfp ?: return null
            val result = getMasterSignerUseCase(signerId)
            if (result.isSuccess) {
                masterSigner = result.getOrThrow()
            }
        }
        return masterSigner
    }

    fun setPrimaryKeyInfo(primaryKey: PrimaryKey) {
        this.primaryKey = primaryKey
    }

    suspend fun getPrimaryKeyInfo(): PrimaryKey? {
        primaryKeyMutex.withLock {
            if (primaryKey != null) return primaryKey
            val result = getPrimaryKeyListUseCase.invoke(Unit)
            if (result.isSuccess) {
                result.getOrNull()?.firstOrNull {
                    it.masterFingerprint == accountManager.getPrimaryKeyInfo()?.xfp
                }?.also {
                    this.primaryKey = it
                }
            }
        }
        return primaryKey
    }

    suspend fun isNeedPassphraseSent(forceNewData: Boolean = false): Boolean {
        val masterSigner = getSignerInfo(forceNewData)
        return masterSigner?.let {
            it.software && it.device.needPassPhraseSent
        } == true
    }

    fun clear() {
        masterSigner = null
        primaryKey = null
    }
}