package com.nunchuk.android.contact.nav

import androidx.fragment.app.FragmentManager
import com.nunchuk.android.contact.components.add.AddContactsBottomSheet
import com.nunchuk.android.contact.components.pending.PendingContactsBottomSheet
import com.nunchuk.android.nav.ContactNavigator

interface ContactNavigatorDelegate : ContactNavigator {

    override fun openAddContactsScreen(fragmentManager: FragmentManager, onDismiss: () -> Unit) {
        val bottomSheet = AddContactsBottomSheet.show(fragmentManager)
        bottomSheet.listener = onDismiss
    }

    override fun openPendingContactsScreen(fragmentManager: FragmentManager, onDismiss: () -> Unit) {
        val bottomSheet = PendingContactsBottomSheet.show(fragmentManager)
        bottomSheet.listener = onDismiss
    }

}