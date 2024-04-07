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

import com.nunchuk.android.usecase.CheckMnemonicUseCase
import com.nunchuk.android.usecase.CheckMnemonicUseCaseImpl
import com.nunchuk.android.usecase.CreateKeystoneSignerUseCase
import com.nunchuk.android.usecase.CreateKeystoneSignerUseCaseImpl
import com.nunchuk.android.usecase.CreatePassportSignersUseCase
import com.nunchuk.android.usecase.CreatePassportSignersUseCaseImpl
import com.nunchuk.android.usecase.GetBip39WordListUseCase
import com.nunchuk.android.usecase.GetBip39WordListUseCaseImpl
import com.nunchuk.android.usecase.GetCompoundSignersUseCase
import com.nunchuk.android.usecase.GetCompoundSignersUseCaseImpl
import com.nunchuk.android.usecase.GetMasterSignersUseCase
import com.nunchuk.android.usecase.GetMasterSignersUseCaseImpl
import com.nunchuk.android.usecase.GetRemoteSignersUseCase
import com.nunchuk.android.usecase.GetRemoteSignersUseCaseImpl
import com.nunchuk.android.usecase.GetUnusedSignerFromMasterSignerUseCase
import com.nunchuk.android.usecase.GetUnusedSignerFromMasterSignerUseCaseImpl
import com.nunchuk.android.usecase.UpdateRemoteSignerUseCase
import com.nunchuk.android.usecase.UpdateRemoteSignerUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface SignerDomainModule {

    @Binds
    fun bindGetRemoteSignersUseCase(useCase: GetRemoteSignersUseCaseImpl): GetRemoteSignersUseCase

    @Binds
    fun bindUpdateRemoteSignerUseCase(useCase: UpdateRemoteSignerUseCaseImpl): UpdateRemoteSignerUseCase

    @Binds
    fun bindCreateKeystoneSignerUseCase(useCase: CreateKeystoneSignerUseCaseImpl): CreateKeystoneSignerUseCase

    @Binds
    fun bindGetBip39WordListUseCase(useCase: GetBip39WordListUseCaseImpl): GetBip39WordListUseCase

    @Binds
    fun bindCheckMnemonicUseCase(useCase: CheckMnemonicUseCaseImpl): CheckMnemonicUseCase

    @Binds
    fun bindGetMasterSignersUseCase(useCase: GetMasterSignersUseCaseImpl): GetMasterSignersUseCase

    @Binds
    fun bindGetUnusedSignerFromMasterSignerUseCase(useCase: GetUnusedSignerFromMasterSignerUseCaseImpl): GetUnusedSignerFromMasterSignerUseCase

    @Binds
    fun bindGetCompoundSignersUseCase(useCase: GetCompoundSignersUseCaseImpl): GetCompoundSignersUseCase

    @Binds
    fun bindCreatePassportSignersUseCase(useCase: CreatePassportSignersUseCaseImpl): CreatePassportSignersUseCase

}