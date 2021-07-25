package com.nunchuk.android.messages.di

import com.nunchuk.android.messages.components.create.CreateRoomBottomSheet
import com.nunchuk.android.messages.components.list.RoomsFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal interface MessagesFragmentModule {

    @ContributesAndroidInjector
    fun createRoomBottomSheet(): CreateRoomBottomSheet

    @ContributesAndroidInjector
    fun messagesFragment(): RoomsFragment

}