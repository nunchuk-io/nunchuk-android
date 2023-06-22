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

package com.nunchuk.android.domain.di

import com.nunchuk.android.usecase.GetTransactionsUseCase
import com.nunchuk.android.usecase.GetTransactionsUseCaseImpl
import com.nunchuk.android.usecase.room.transaction.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface RoomTransactionDomainModule {

    @Binds
    fun bindInitRoomTransactionUseCase(useCase: InitRoomTransactionUseCaseImpl): InitRoomTransactionUseCase

    @Binds
    fun bindSignRoomTransactionUseCase(useCase: SignRoomTransactionUseCaseImpl): SignRoomTransactionUseCase

    @Binds
    fun bindRejectRoomTransactionUseCase(useCase: RejectRoomTransactionUseCaseImpl): RejectRoomTransactionUseCase

    @Binds
    fun bindCancelRoomTransactionUseCase(useCase: CancelRoomTransactionUseCaseImpl): CancelRoomTransactionUseCase

    @Binds
    fun bindBroadcastRoomTransactionUseCase(useCase: BroadcastRoomTransactionUseCaseImpl): BroadcastRoomTransactionUseCase

    @Binds
    fun bindGetRoomTransactionUseCase(useCase: GetRoomTransactionUseCaseImpl): GetRoomTransactionUseCase

    @Binds
    fun bindGetTransactionsUseCase(useCase: GetTransactionsUseCaseImpl): GetTransactionsUseCase

}
