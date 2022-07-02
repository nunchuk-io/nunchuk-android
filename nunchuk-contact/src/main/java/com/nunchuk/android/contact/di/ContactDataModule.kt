package com.nunchuk.android.contact.di

import com.nunchuk.android.contact.repository.ContactsRepository
import com.nunchuk.android.contact.repository.ContactsRepositoryImpl
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