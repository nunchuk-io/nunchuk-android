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

import com.nunchuk.android.usecase.BroadcastTransactionUseCase
import com.nunchuk.android.usecase.BroadcastTransactionUseCaseImpl
import com.nunchuk.android.usecase.ExportKeystoneTransactionUseCase
import com.nunchuk.android.usecase.ExportKeystoneTransactionUseCaseImpl
import com.nunchuk.android.usecase.ExportTransactionUseCase
import com.nunchuk.android.usecase.ExportTransactionUseCaseImpl
import com.nunchuk.android.usecase.GetAddressBalanceUseCase
import com.nunchuk.android.usecase.GetAddressBalanceUseCaseImpl
import com.nunchuk.android.usecase.GetAddressesUseCase
import com.nunchuk.android.usecase.GetAddressesUseCaseImpl
import com.nunchuk.android.usecase.GetTransactionHistoryUseCase
import com.nunchuk.android.usecase.GetTransactionHistoryUseCaseImpl
import com.nunchuk.android.usecase.GetTransactionUseCase
import com.nunchuk.android.usecase.ImportKeystoneTransactionUseCase
import com.nunchuk.android.usecase.ImportKeystoneTransactionUseCaseImpl
import com.nunchuk.android.usecase.NewAddressUseCase
import com.nunchuk.android.usecase.NewAddressUseCaseImpl
import com.nunchuk.android.usecase.SendSignerPassphrase
import com.nunchuk.android.usecase.SendSignerPassphraseImpl
import com.nunchuk.android.usecase.ValueFromAmountUseCase
import com.nunchuk.android.usecase.ValueFromAmountUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface TransactionDomainModule {

    @Binds
    fun binBroadcastTransactionUseCase(useCase: BroadcastTransactionUseCaseImpl): BroadcastTransactionUseCase

    @Binds
    fun bindExportTransactionUseCase(useCase: ExportTransactionUseCaseImpl): ExportTransactionUseCase

    @Binds
    fun bindGetTransactionHistoryUseCase(useCase: GetTransactionHistoryUseCaseImpl): GetTransactionHistoryUseCase

    @Binds
    fun bindGetAddressBalanceUseCase(useCase: GetAddressBalanceUseCaseImpl): GetAddressBalanceUseCase

    @Binds
    fun bindGetAddressesUseCase(useCase: GetAddressesUseCaseImpl): GetAddressesUseCase

    @Binds
    fun bindNewAddressUseCase(useCase: NewAddressUseCaseImpl): NewAddressUseCase

    @Binds
    fun bindValueFromAmountUseCase(useCase: ValueFromAmountUseCaseImpl): ValueFromAmountUseCase

    @Binds
    fun bindSendSignerPassphrase(useCase: SendSignerPassphraseImpl): SendSignerPassphrase

    @Binds
    fun bindImportKeystoneTransactionUseCase(useCase: ImportKeystoneTransactionUseCaseImpl): ImportKeystoneTransactionUseCase

    @Binds
    fun bindExportKeystoneTransactionUseCase(useCase: ExportKeystoneTransactionUseCaseImpl): ExportKeystoneTransactionUseCase
}