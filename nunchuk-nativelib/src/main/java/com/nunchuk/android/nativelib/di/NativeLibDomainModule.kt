package com.nunchuk.android.nativelib.di

import com.nunchuk.android.usecase.*
import dagger.Binds
import dagger.Module

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
}