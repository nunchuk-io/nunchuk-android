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

package com.nunchuk.android.domain.di

import com.nunchuk.android.usecase.*
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
    fun bindDraftTransactionUseCase(useCase: DraftTransactionUseCaseImpl): DraftTransactionUseCase

    @Binds
    fun bindExportTransactionUseCase(useCase: ExportTransactionUseCaseImpl): ExportTransactionUseCase

    @Binds
    fun bindGetTransactionHistoryUseCase(useCase: GetTransactionHistoryUseCaseImpl): GetTransactionHistoryUseCase

    @Binds
    fun bindGetTransactionUseCase(useCase: GetTransactionUseCaseImpl): GetTransactionUseCase

    @Binds
    fun bindImportTransactionUseCase(useCase: ImportTransactionUseCaseImpl): ImportTransactionUseCase

    @Binds
    fun bindExportTransactionHistoryUseCase(useCase: ExportTransactionHistoryUseCaseImpl): ExportTransactionHistoryUseCase

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