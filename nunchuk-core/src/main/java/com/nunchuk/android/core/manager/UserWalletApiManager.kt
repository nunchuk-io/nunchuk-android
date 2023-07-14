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

package com.nunchuk.android.core.manager

import com.nunchuk.android.core.data.api.GroupWalletApi
import com.nunchuk.android.core.data.api.UserWalletsApi
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.network.util.TEST_NET_USER_WALLET_API
import com.nunchuk.android.type.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class UserWalletApiManager @Inject constructor(
    private val _userWalletsApi: UserWalletsApi,
    @Named(TEST_NET_USER_WALLET_API)
    private val _testNetUserWalletsApi: UserWalletsApi,
    private val _groupWalletApi: GroupWalletApi,
    @Named(TEST_NET_USER_WALLET_API)
    private val _testNetGroupWalletApi: GroupWalletApi,
    applicationScope: CoroutineScope,
    ncDataStore: NcDataStore,
) {
    private val chain =
        ncDataStore.chain.stateIn(applicationScope, SharingStarted.Eagerly, Chain.MAIN)

    val walletApi: UserWalletsApi
        get() = if (chain.value == Chain.MAIN) _userWalletsApi else _testNetUserWalletsApi

    val groupWalletApi: GroupWalletApi
        get() = if (chain.value == Chain.MAIN) _groupWalletApi else _testNetGroupWalletApi
}