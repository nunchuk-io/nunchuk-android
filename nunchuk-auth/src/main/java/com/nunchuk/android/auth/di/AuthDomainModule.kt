package com.nunchuk.android.auth.di

import com.nunchuk.android.auth.domain.*
import dagger.Binds
import dagger.Module

@Module
internal interface AuthDomainModule {

    @Binds
    fun bindRegisterUseCase(useCase: RegisterUseCaseImpl): RegisterUseCase

    @Binds
    fun bindSignInUseCase(useCase: SignInUseCaseImpl): SignInUseCase

    @Binds
    fun bindChangePasswordUseCase(useCase: ChangePasswordUseCaseImpl): ChangePasswordUseCase

    @Binds
    fun bindRecoverPasswordUseCase(useCase: RecoverPasswordUseCaseImpl): RecoverPasswordUseCase

    @Binds
    fun bindForgotPasswordUseCase(useCase: ForgotPasswordUseCaseImpl): ForgotPasswordUseCase

    @Binds
    fun bindGetCurrentUserUseCase(useCase: GetCurrentUserUseCaseImpl): GetCurrentUserUseCase

    @Binds
    fun bindVerifyNewDeviceUseCase(useCase: VerifyNewDeviceUseCaseImpl): VerifyNewDeviceUseCase

}