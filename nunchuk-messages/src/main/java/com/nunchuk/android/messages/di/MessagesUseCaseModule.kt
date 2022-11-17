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

package com.nunchuk.android.messages.di

import com.nunchuk.android.messages.usecase.message.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface MessagesUseCaseModule {

    @Binds
    fun bindCreateRoomUseCase(useCase: CreateRoomUseCaseImpl): CreateRoomUseCase

    @Binds
    fun bindCreateRoomWithTagUseCase(useCase: CreateRoomWithTagUseCaseImpl): CreateRoomWithTagUseCase

    @Binds
    fun bindCreateDirectRoomUseCase(useCase: CreateDirectRoomUseCaseImpl): CreateDirectRoomUseCase

    @Binds
    fun bindAddTagRoomUseCase(useCase: AddTagRoomUseCaseImpl): AddTagRoomUseCase

    @Binds
    fun bindJoinRoomUseCase(useCase: JoinRoomUseCaseImpl): JoinRoomUseCase

    @Binds
    fun bindCheckShowBannerNewChatUseCase(useCase: CheckShowBannerNewChatUseCaseImpl): CheckShowBannerNewChatUseCase

}