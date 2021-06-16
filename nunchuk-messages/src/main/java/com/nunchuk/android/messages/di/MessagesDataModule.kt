package com.nunchuk.android.messages.di

import com.nunchuk.android.messages.repository.ContactsRepository
import com.nunchuk.android.messages.repository.ContactsRepositoryImpl
import dagger.Binds
import dagger.Module

@Module
internal interface MessagesDataModule {

    @Binds
    fun bindContactRepository(repository: ContactsRepositoryImpl): ContactsRepository

}