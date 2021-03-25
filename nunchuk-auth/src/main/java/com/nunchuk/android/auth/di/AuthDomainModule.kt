package com.nunchuk.android.auth.di

import com.nunchuk.android.auth.domain.*
import dagger.Binds
import dagger.Module

@Module
internal abstract class AuthDomainModule {

    @Binds
    abstract fun bindRegisterUseCase(useCase: RegisterUseCaseImpl): RegisterUseCase

    @Binds
    abstract fun bindSignInUseCase(useCase: SignInUseCaseImpl): SignInUseCase

    @Binds
    abstract fun bindChangePasswordUseCase(useCase: ChangePasswordUseCaseImpl): ChangePasswordUseCase

    @Binds
    abstract fun bindRecoverPasswordUseCase(useCase: RecoverPasswordUseCaseImpl): RecoverPasswordUseCase

}