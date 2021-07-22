package com.nunchuk.android.nav

import androidx.fragment.app.FragmentManager

interface ContactNavigator {
    fun openAddContactsScreen(fragmentManager: FragmentManager, onDismiss: () -> Unit = {})

    fun openPendingContactsScreen(fragmentManager: FragmentManager, onDismiss: () -> Unit = {})
}

