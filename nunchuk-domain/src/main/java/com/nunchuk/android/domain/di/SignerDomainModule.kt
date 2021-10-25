package com.nunchuk.android.domain.di

import com.nunchuk.android.usecase.*
import dagger.Binds
import dagger.Module

@Module
internal interface SignerDomainModule {

    @Binds
    fun bindCreateSignerUseCase(useCase: CreateSignerUseCaseImpl): CreateSignerUseCase

    @Binds
    fun bindGetRemoteSignerUseCase(useCase: GetRemoteSignerUseCaseImpl): GetRemoteSignerUseCase

    @Binds
    fun bindGetAppSettingsUseCase(useCase: GetAppSettingsUseCaseImpl): GetAppSettingsUseCase

    @Binds
    fun bindGetOrCreateRootDirUseCase(useCase: GetOrCreateRootDirUseCaseImpl): GetOrCreateRootDirUseCase

    @Binds
    fun bindGetRemoteSignersUseCase(useCase: GetRemoteSignersUseCaseImpl): GetRemoteSignersUseCase

    @Binds
    fun bindDeleteRemoteSignerUseCase(useCase: DeleteRemoteSignerUseCaseImpl): DeleteRemoteSignerUseCase

    @Binds
    fun bindUpdateRemoteSignerUseCase(useCase: UpdateRemoteSignerUseCaseImpl): UpdateRemoteSignerUseCase

    @Binds
    fun bindCreateCoboSignerUseCase(useCase: CreateCoboSignerUseCaseImpl): CreateCoboSignerUseCase

    @Binds
    fun bindCreateKeystoneSignerUseCase(useCase: CreateKeystoneSignerUseCaseImpl): CreateKeystoneSignerUseCase

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

    @Binds
    fun bindGetCompoundSignersUseCase(useCase: GetCompoundSignersUseCaseImpl): GetCompoundSignersUseCase

}