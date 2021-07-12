package com.nunchuk.android.messages.di

import com.nunchuk.android.messages.contact.AddContactsBottomSheet
import com.nunchuk.android.messages.room.create.CreateRoomBottomSheet
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal interface MessagesFragmentModule {

    @ContributesAndroidInjector
    fun addContactsBottomSheet(): AddContactsBottomSheet

    @ContributesAndroidInjector
    fun createRoomBottomSheet(): CreateRoomBottomSheet

}
