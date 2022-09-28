package com.nunchuk.android.contact.di

import com.nunchuk.android.contact.repository.ContactsRepositoryImpl
import com.nunchuk.android.repository.ContactsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ContactDataModule {

    @Binds
    @Singleton
    fun bindContactRepository(repository: ContactsRepositoryImpl): ContactsRepository

}