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

package com.nunchuk.android.app.bitbox

import android.app.Activity
import android.os.Bundle

/**
 * Target of the `USB_DEVICE_ATTACHED` filter for BitBox devices, and deliberately does nothing
 * but finish. The BitBox twin of
 * [com.nunchuk.android.app.ledger.LedgerUsbAttachActivity] — see that class for why the filter
 * is declared at all.
 *
 * In short: declaring it puts Nunchuk in the system chooser shown when a BitBox is plugged in,
 * so the user can tick "use by default for this USB device", and being the handler grants the
 * app permission for that device so the flows don't have to prompt.
 *
 * It must not open a screen: a BitBox is usually plugged in *while* the user is somewhere in the
 * app, and launching anything would throw them out of it. The device itself is picked up by the
 * runtime `ACTION_USB_DEVICE_ATTACHED` receiver in `BitBoxController`.
 */
class BitBoxUsbAttachActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}
