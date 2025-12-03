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

import com.nunchuk.android.usecase.DraftWalletUseCase
import com.nunchuk.android.usecase.DraftWalletUseCaseImpl
import com.nunchuk.android.usecase.ExportKeystoneWalletUseCase
import com.nunchuk.android.usecase.ExportKeystoneWalletUseCaseImpl
import com.nunchuk.android.usecase.ExportWalletUseCase
import com.nunchuk.android.usecase.ExportWalletUseCaseImpl
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.usecase.GetWalletUseCaseImpl
import com.nunchuk.android.usecase.GetWalletsUseCase
import com.nunchuk.android.usecase.GetWalletsUseCaseImpl
import com.nunchuk.android.usecase.ImportKeystoneWalletUseCase
import com.nunchuk.android.usecase.ImportKeystoneWalletUseCaseImpl
import com.nunchuk.android.usecase.ImportWalletUseCase
import com.nunchuk.android.usecase.ImportWalletUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface WalletDomainModule {

    @Binds
    fun bindGetWalletsUseCase(useCase: GetWalletsUseCaseImpl): GetWalletsUseCase

    @Binds
    fun bindDraftWalletUseCase(useCase: DraftWalletUseCaseImpl): DraftWalletUseCase

    @Binds
    fun bindExportWalletUseCase(useCase: ExportWalletUseCaseImpl): ExportWalletUseCase

    @Binds
    fun bindExportKeystoneWalletUseCase(useCase: ExportKeystoneWalletUseCaseImpl): ExportKeystoneWalletUseCase

    @Binds
    fun bindGetWalletUseCase(useCase: GetWalletUseCaseImpl): GetWalletUseCase

    @Binds
    fun bindImportKeystoneWalletUseCase(useCase: ImportKeystoneWalletUseCaseImpl): ImportKeystoneWalletUseCase

    @Binds
    fun bindImportWalletUseCase(useCase: ImportWalletUseCaseImpl): ImportWalletUseCase
}