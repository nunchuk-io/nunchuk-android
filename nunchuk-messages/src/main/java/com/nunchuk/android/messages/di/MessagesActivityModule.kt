package com.nunchuk.android.messages.di

import com.nunchuk.android.messages.components.detail.RoomDetailActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal interface MessagesActivityModule {

    @ContributesAndroidInjector
    fun roomDetailActivity(): RoomDetailActivity

}
