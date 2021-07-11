package com.nunchuk.android.messages.di

import com.nunchuk.android.messages.contact.AddContactsBottomSheet
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal interface MessagesFragmentModule {

    @ContributesAndroidInjector
    fun addContactsBottomSheetDialog(): AddContactsBottomSheet

}
