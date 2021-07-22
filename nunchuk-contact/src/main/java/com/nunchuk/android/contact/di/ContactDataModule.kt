package com.nunchuk.android.contact.di

import com.nunchuk.android.contact.repository.ContactsRepository
import com.nunchuk.android.contact.repository.ContactsRepositoryImpl
import dagger.Binds
import dagger.Module

@Module
internal interface ContactDataModule {

    @Binds
    fun bindContactRepository(repository: ContactsRepositoryImpl): ContactsRepository

}