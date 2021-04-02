package com.nunchuk.android.auth.di

import com.nunchuk.android.auth.validator.EmailValidator
import com.nunchuk.android.auth.validator.EmailValidatorImpl
import com.nunchuk.android.auth.validator.NameValidator
import com.nunchuk.android.auth.validator.NameValidatorImpl
import dagger.Binds
import dagger.Module

@Module
internal interface AuthCommonModule {

    @Binds
    fun bindEmailValidator(validator: EmailValidatorImpl): EmailValidator

    @Binds
    fun bindNameValidator(validator: NameValidatorImpl): NameValidator

}