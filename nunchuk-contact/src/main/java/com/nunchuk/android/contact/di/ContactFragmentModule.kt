package com.nunchuk.android.contact.di

import com.nunchuk.android.contact.add.AddContactsBottomSheet
import com.nunchuk.android.contact.pending.PendingContactsBottomSheet
import com.nunchuk.android.contact.pending.receive.ReceivedFragment
import com.nunchuk.android.contact.pending.sent.SentFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal interface ContactFragmentModule {

    @ContributesAndroidInjector
    fun addContactsBottomSheet(): AddContactsBottomSheet

    @ContributesAndroidInjector
    fun pendingContactsBottomSheet(): PendingContactsBottomSheet

    @ContributesAndroidInjector
    fun receivedFragment(): ReceivedFragment

    @ContributesAndroidInjector
    fun sentFragment(): SentFragment

}
