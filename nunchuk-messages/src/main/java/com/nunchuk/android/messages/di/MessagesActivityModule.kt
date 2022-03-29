package com.nunchuk.android.messages.di

import com.nunchuk.android.messages.components.detail.RoomDetailActivity
import com.nunchuk.android.messages.components.direct.ChatInfoActivity
import com.nunchuk.android.messages.components.group.ChatGroupInfoActivity
import com.nunchuk.android.messages.components.group.members.GroupMembersActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal interface MessagesActivityModule {

    @ContributesAndroidInjector
    fun roomDetailActivity(): RoomDetailActivity

    @ContributesAndroidInjector
    fun chatInfoActivity(): ChatInfoActivity

    @ContributesAndroidInjector
    fun chatGroupInfoActivity(): ChatGroupInfoActivity

    @ContributesAndroidInjector
    fun groupMembersActivity(): GroupMembersActivity

}
