package com.nunchuk.android.contact.di

import androidx.lifecycle.ViewModel
import com.nunchuk.android.arch.vm.ViewModelKey
import com.nunchuk.android.contact.components.add.AddContactsViewModel
import com.nunchuk.android.contact.components.pending.receive.ReceivedViewModel
import com.nunchuk.android.contact.components.pending.sent.SentViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
interface ContactViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(AddContactsViewModel::class)
    fun bindAddContactsViewModel(viewModel: AddContactsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ReceivedViewModel::class)
    fun bindReceivedViewModel(viewModel: ReceivedViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SentViewModel::class)
    fun bindSentViewModel(viewModel: SentViewModel): ViewModel

}