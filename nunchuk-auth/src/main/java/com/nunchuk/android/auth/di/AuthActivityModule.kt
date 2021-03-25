package com.nunchuk.android.auth.di

import com.nunchuk.android.auth.components.changepass.ChangePasswordActivity
import com.nunchuk.android.auth.components.changepass.ChangePasswordModule
import com.nunchuk.android.auth.components.signin.SignInActivity
import com.nunchuk.android.auth.components.signin.SignInModule
import com.nunchuk.android.auth.components.signup.SignUpActivity
import com.nunchuk.android.auth.components.signup.SignUpModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class AuthActivityModule {

    @ContributesAndroidInjector(modules = [SignUpModule::class])
    abstract fun signUpActivity(): SignUpActivity

    @ContributesAndroidInjector(modules = [ChangePasswordModule::class])
    abstract fun changePasswordActivity(): ChangePasswordActivity

    @ContributesAndroidInjector(modules = [SignInModule::class])
    abstract fun signInActivity(): SignInActivity

}
