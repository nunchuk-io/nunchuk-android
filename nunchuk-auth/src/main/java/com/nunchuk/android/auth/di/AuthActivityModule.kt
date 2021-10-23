package com.nunchuk.android.auth.di

import com.nunchuk.android.auth.components.changepass.ChangePasswordActivity
import com.nunchuk.android.auth.components.changepass.ChangePasswordModule
import com.nunchuk.android.auth.components.forgot.ForgotPasswordActivity
import com.nunchuk.android.auth.components.forgot.ForgotPasswordModule
import com.nunchuk.android.auth.components.recover.RecoverPasswordActivity
import com.nunchuk.android.auth.components.recover.RecoverPasswordModule
import com.nunchuk.android.auth.components.signin.SignInActivity
import com.nunchuk.android.auth.components.signin.SignInModule
import com.nunchuk.android.auth.components.signup.SignUpActivity
import com.nunchuk.android.auth.components.signup.SignUpModule
import com.nunchuk.android.auth.components.verify.VerifyNewDeviceModule
import com.nunchuk.android.auth.components.verify.VerifyNewDeviceActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal interface AuthActivityModule {

    @ContributesAndroidInjector(modules = [SignUpModule::class])
    fun signUpActivity(): SignUpActivity

    @ContributesAndroidInjector(modules = [SignInModule::class])
    fun signInActivity(): SignInActivity

    @ContributesAndroidInjector(modules = [ChangePasswordModule::class])
    fun changePasswordActivity(): ChangePasswordActivity

    @ContributesAndroidInjector(modules = [ForgotPasswordModule::class])
    fun forgotPasswordActivity(): ForgotPasswordActivity

    @ContributesAndroidInjector(modules = [RecoverPasswordModule::class])
    fun recoverPasswordActivity(): RecoverPasswordActivity

    @ContributesAndroidInjector(modules = [VerifyNewDeviceModule::class])
    fun verifyNewDeviceActivity(): VerifyNewDeviceActivity
}
