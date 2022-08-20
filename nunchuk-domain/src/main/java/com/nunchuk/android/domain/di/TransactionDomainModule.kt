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
    fun binCreateTransactionUseCase(useCase: CreateTransactionUseCaseImpl): CreateTransactionUseCase

    @Binds
    fun bindDeleteTransactionUseCase(useCase: DeleteTransactionUseCaseImpl): DeleteTransactionUseCase

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
    fun bindSignTransactionUseCase(useCase: SignTransactionUseCaseImpl): SignTransactionUseCase

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
    fun bindCheckAddressValidUseCase(useCase: CheckAddressValidUseCaseImpl): CheckAddressValidUseCase

    @Binds
    fun bindSendSignerPassphrase(useCase: SendSignerPassphraseImpl): SendSignerPassphrase

    @Binds
    fun bindImportKeystoneTransactionUseCase(useCase: ImportKeystoneTransactionUseCaseImpl): ImportKeystoneTransactionUseCase

    @Binds
    fun bindExportKeystoneTransactionUseCase(useCase: ExportKeystoneTransactionUseCaseImpl): ExportKeystoneTransactionUseCase

    @Binds
    fun bindImportPassportTransactionUseCase(useCase: ImportPassportTransactionUseCaseImpl): ImportPassportTransactionUseCase

    @Binds
    fun bindExportPassportTransactionUseCase(useCase: ExportPassportTransactionUseCaseImpl): ExportPassportTransactionUseCase
}