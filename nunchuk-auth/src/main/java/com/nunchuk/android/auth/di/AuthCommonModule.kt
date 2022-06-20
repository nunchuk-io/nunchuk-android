package com.nunchuk.android.auth.di

import com.nunchuk.android.auth.validator.NameValidator
import com.nunchuk.android.auth.validator.NameValidatorImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface AuthCommonModule {

    @Binds
    fun bindNameValidator(validator: NameValidatorImpl): NameValidator

}