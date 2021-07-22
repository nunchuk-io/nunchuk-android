package com.nunchuk.android.messages.di

import com.nunchuk.android.messages.components.room.create.CreateRoomBottomSheet
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal interface MessagesFragmentModule {

    @ContributesAndroidInjector
    fun createRoomBottomSheet(): CreateRoomBottomSheet

}