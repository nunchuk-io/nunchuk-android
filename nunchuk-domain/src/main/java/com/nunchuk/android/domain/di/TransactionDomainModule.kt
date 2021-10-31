package com.nunchuk.android.domain.di

import com.nunchuk.android.usecase.*
import dagger.Binds
import dagger.Module

@Module
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
    fun bindExportCoboTransactionUseCase(useCase: ExportCoboTransactionUseCaseImpl): ExportCoboTransactionUseCase

    @Binds
    fun bindExportTransactionUseCase(useCase: ExportTransactionUseCaseImpl): ExportTransactionUseCase

    @Binds
    fun bindGetTransactionHistoryUseCase(useCase: GetTransactionHistoryUseCaseImpl): GetTransactionHistoryUseCase

    @Binds
    fun bindGetTransactionUseCase(useCase: GetTransactionUseCaseImpl): GetTransactionUseCase

    @Binds
    fun bindImportTransactionUseCase(useCase: ImportTransactionUseCaseImpl): ImportTransactionUseCase

    @Binds
    fun bindImportCoboTransactionUseCase(useCase: ImportCoboTransactionUseCaseImpl): ImportCoboTransactionUseCase

    @Binds
    fun bindReplaceTransactionUseCase(useCase: ReplaceTransactionUseCaseImpl): ReplaceTransactionUseCase

    @Binds
    fun bindSignTransactionUseCase(useCase: SignTransactionUseCaseImpl): SignTransactionUseCase

    @Binds
    fun bindUpdateTransactionMemoUseCase(useCase: UpdateTransactionMemoUseCaseImpl): UpdateTransactionMemoUseCase

    @Binds
    fun bindExportTransactionHistoryUseCase(useCase: ExportTransactionHistoryUseCaseImpl): ExportTransactionHistoryUseCase

    @Binds
    fun bindGetAddressBalanceUseCase(useCase: GetAddressBalanceUseCaseImpl): GetAddressBalanceUseCase

    @Binds
    fun bindGetAddressesUseCase(useCase: GetAddressesUseCaseImpl): GetAddressesUseCase

    @Binds
    fun bindGetUnspentOutputsUseCase(useCase: GetUnspentOutputsUseCaseImpl): GetUnspentOutputsUseCase

    @Binds
    fun bindNewAddressUseCase(useCase: NewAddressUseCaseImpl): NewAddressUseCase

    @Binds
    fun bindValueFromAmountUseCase(useCase: ValueFromAmountUseCaseImpl): ValueFromAmountUseCase

    @Binds
    fun bindEstimateFeeUseCase(useCase: EstimateFeeUseCaseImpl): EstimateFeeUseCase

    @Binds
    fun bindCheckAddressValidUseCase(useCase: CheckAddressValidUseCaseImpl): CheckAddressValidUseCase

    @Binds
    fun bindSendSignerPassphrase(useCase: SendSignerPassphraseImpl): SendSignerPassphrase

}