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

package com.nunchuk.android.persistence

const val DATABASE_NAME = "NunchukDatabase.db"
const val DATABASE_VERSION = 18

const val TABLE_CONTACT = "contact"
const val TABLE_SYNC_FILE = "sync_file"
const val TABLE_SYNC_EVENT = "sync_event"
const val TABLE_ADD_DESKTOP_KEY = "add_desktop_key"
const val TABLE_HANDLED_EVENT = "handled_event"
const val TABLE_MEMBERSHIP_STEP = "membership_flow"
const val TABLE_ASSISTED_WALLET = "assisted_wallet"
const val TABLE_GROUP = "byzantine_group"
const val TABLE_ALERT = "byzantine_alert"
const val TABLE_DUMMY_TRANSACTION = "dummy_transaction"
const val TABLE_KEY_HEALTH_STATUS = "key_health_status"