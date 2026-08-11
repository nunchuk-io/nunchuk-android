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

package com.nunchuk.android.app.ledger

import android.app.Activity
import android.os.Bundle

/**
 * Target of the `USB_DEVICE_ATTACHED` filter for Ledger devices, and deliberately does nothing
 * but finish.
 *
 * Declaring the filter is what puts Nunchuk in the system chooser that appears when a Ledger is
 * plugged in, so the user can tick "use by default for this USB device" and stop being asked —
 * there is no API to route that chooser to whichever app is in the foreground. Being the handler
 * also grants the app permission for that device, so the sign flow doesn't have to prompt,
 * including after the Ledger re-enumerates while it opens the Bitcoin app.
 *
 * It must not open a screen: a Ledger is usually plugged in *while* the user is somewhere in the
 * app (say the sign sheet), and launching anything would throw them out of it. The device itself
 * is picked up by the runtime `ACTION_USB_DEVICE_ATTACHED` receiver in `LedgerBleController`.
 */
class LedgerUsbAttachActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}
