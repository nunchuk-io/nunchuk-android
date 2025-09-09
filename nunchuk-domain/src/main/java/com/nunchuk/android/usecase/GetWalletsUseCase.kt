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

package com.nunchuk.android.usecase

import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber
import javax.inject.Inject

interface GetWalletsUseCase {
    fun execute(): Flow<List<WalletExtended>>
}

internal class GetWalletsUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    private val assistedWalletManager: AssistedWalletManager,
) : GetWalletsUseCase {

    override fun execute() = flow {
        val wallets = nativeSdk.getWallets()
        val rWalletIds = nativeSdk.getAllRoomWalletIds()
        emit(
            wallets.map {
                val name = assistedWalletManager.getWalletAlias(it.id).ifEmpty { it.name }
                WalletExtended(it.copy(name = name), it.isShared(rWalletIds))
            }
        )
    }.flowOn(Dispatchers.IO)

}

interface GetWalletUseCase {
    fun execute(walletId: String): Flow<WalletExtended>
}

internal class GetWalletUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    private val assistedWalletManager: AssistedWalletManager,
) : GetWalletUseCase {

    override fun execute(walletId: String) = flow {
        val wallet = nativeSdk.getWallet(walletId)
        val brief = assistedWalletManager.getBriefWallet(walletId)
        val rWallets = nativeSdk.getAllRoomWallet()
        val rWalletIds = rWallets.map(RoomWallet::walletId)
        val roomWallet = rWallets.firstOrNull { wallet.id == it.walletId }
        emit(
            WalletExtended(
                wallet = wallet.copy(name = brief?.alias.orEmpty().ifEmpty { wallet.name }),
                isShared = wallet.isShared(rWalletIds),
                roomWallet = roomWallet,
            )
        )
    }.catch { Timber.e(it) }.flowOn(Dispatchers.IO)

}

internal fun NunchukNativeSdk.getAllRoomWalletIds() = try {
    getAllRoomWallets().map(RoomWallet::walletId)
} catch (t: Throwable) {
    emptyList()
}

internal fun NunchukNativeSdk.getAllRoomWallet() = try {
    getAllRoomWallets()
} catch (t: Throwable) {
    emptyList()
}

private fun Wallet.isShared(rWalletIds: List<String>) = id in rWalletIds