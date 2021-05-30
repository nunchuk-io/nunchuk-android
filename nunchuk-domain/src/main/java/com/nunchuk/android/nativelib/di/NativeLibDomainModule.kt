package com.nunchuk.android.nativelib.di

import com.nunchuk.android.nativelib.LibNunchukFacade
import com.nunchuk.android.usecase.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
internal interface NativeLibDomainModule {

    @Binds
    fun bindCreateSignerUseCase(useCase: CreateSignerUseCaseImpl): CreateSignerUseCase

    @Binds
    fun bindGetRemoteSignerUseCase(useCase: GetRemoteSignerUseCaseImpl): GetRemoteSignerUseCase

    @Binds
    fun bindGetAppSettingsUseCase(useCase: GetAppSettingsUseCaseImpl): GetAppSettingsUseCase

    @Binds
    fun bindInitNunchukUseCase(useCase: InitNunchukUseCaseImpl): InitNunchukUseCase

    @Binds
    fun bindGetOrCreateRootDirUseCase(useCase: GetOrCreateRootDirUseCaseImpl): GetOrCreateRootDirUseCase

    @Binds
    fun bindGetRemoteSignersUseCase(useCase: GetRemoteSignersUseCaseImpl): GetRemoteSignersUseCase

    @Binds
    fun bindDeleteRemoteSignerUseCase(useCase: DeleteRemoteSignerUseCaseImpl): DeleteRemoteSignerUseCase

    @Binds
    fun bindUpdateRemoteSignerUseCase(useCase: UpdateRemoteSignerUseCaseImpl): UpdateRemoteSignerUseCase

    @Binds
    fun bindGetWalletsUseCase(useCase: GetWalletsUseCaseImpl): GetWalletsUseCase

    @Binds
    fun bindCreateWalletUseCase(useCase: CreateWalletUseCaseImpl): CreateWalletUseCase

    @Binds
    fun bindDraftWalletUseCase(useCase: DraftWalletUseCaseImpl): DraftWalletUseCase

    @Binds
    fun bindExportWalletUseCase(useCase: ExportWalletUseCaseImpl): ExportWalletUseCase

    @Binds
    fun bindExportCoboWalletUseCase(useCase: ExportCoboWalletUseCaseImpl): ExportCoboWalletUseCase

    @Binds
    fun bindGetWalletUseCase(useCase: GetWalletUseCaseImpl): GetWalletUseCase

    @Binds
    fun bindCreateWalletFilePathUseCase(useCase: CreateWalletFilePathUseCaseImpl): CreateWalletFilePathUseCase

    @Binds
    fun bindUpdateWalletUseCase(useCase: UpdateWalletUseCaseImpl): UpdateWalletUseCase

    // SOFTWARE SIGNER
    @Binds
    fun bindGenerateMnemonicUseCase(useCase: GenerateMnemonicUseCaseImpl): GenerateMnemonicUseCase

    @Binds
    fun bindGetBip39WordListUseCase(useCase: GetBip39WordListUseCaseImpl): GetBip39WordListUseCase

    @Binds
    fun bindCheckMnemonicUseCase(useCase: CheckMnemonicUseCaseImpl): CheckMnemonicUseCase

    @Binds
    fun bindCreateSoftwareSignerUseCase(useCase: CreateSoftwareSignerUseCaseImpl): CreateSoftwareSignerUseCase

    // MASTER SIGNER
    @Binds
    fun bindGetMasterSignerUseCase(useCase: GetMasterSignerUseCaseImpl): GetMasterSignerUseCase

    @Binds
    fun bindGetMasterSignersUseCase(useCase: GetMasterSignersUseCaseImpl): GetMasterSignersUseCase

    @Binds
    fun bindDeleteMasterSignerUseCase(useCase: DeleteMasterSignerUseCaseImpl): DeleteMasterSignerUseCase

    @Binds
    fun bindUpdateMasterSignerUseCase(useCase: UpdateMasterSignerUseCaseImpl): UpdateMasterSignerUseCase

    @Binds
    fun bindGetUnusedSignerFromMasterSignerUseCase(useCase: GetUnusedSignerFromMasterSignerUseCaseImpl): GetUnusedSignerFromMasterSignerUseCase

    // TRANSACTION
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
    fun bindGetBlockchainExplorerUrlUseCase(useCase: GetBlockchainExplorerUrlUseCaseImpl): GetBlockchainExplorerUrlUseCase

    @Binds
    fun bindGetChainTipUseCase(useCase: GetChainTipUseCaseImpl): GetChainTipUseCase

    @Binds
    fun bindGetDeviceUseCase(useCase: GetDevicesUseCaseImpl): GetDevicesUseCase

    companion object {

        @Singleton
        @Provides
        fun provideLibNunchukFacade() = LibNunchukProvider.instance.libNunchuk

    }
}

internal class LibNunchukProvider {
    val libNunchuk = LibNunchukFacade()

    companion object {
        val instance = InstanceHolder.instance
    }

    private object InstanceHolder {
        var instance = LibNunchukProvider()
    }
}


