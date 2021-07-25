package com.nunchuk.android.contact.di

import com.nunchuk.android.contact.components.add.AddContactsBottomSheet
import com.nunchuk.android.contact.components.contacts.ContactsFragment
import com.nunchuk.android.contact.components.pending.PendingContactsBottomSheet
import com.nunchuk.android.contact.components.pending.receive.ReceivedFragment
import com.nunchuk.android.contact.components.pending.sent.SentFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal interface ContactFragmentModule {

    @ContributesAndroidInjector
    fun contactsFragment(): ContactsFragment

    @ContributesAndroidInjector
    fun addContactsBottomSheet(): AddContactsBottomSheet

    @ContributesAndroidInjector
    fun pendingContactsBottomSheet(): PendingContactsBottomSheet

    @ContributesAndroidInjector
    fun receivedFragment(): ReceivedFragment

    @ContributesAndroidInjector
    fun sentFragment(): SentFragment

}
