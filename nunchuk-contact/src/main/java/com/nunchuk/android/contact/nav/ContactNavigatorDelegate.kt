/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

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