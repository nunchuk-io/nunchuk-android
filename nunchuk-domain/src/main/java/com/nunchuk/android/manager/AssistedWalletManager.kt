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

package com.nunchuk.android.manager

import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.membership.AssistedWalletBrief

interface AssistedWalletManager {
    fun isActiveAssistedWallet(walletId: String): Boolean
    fun getGroupId(walletId: String): String?
    fun isInactiveAssistedWallet(walletId: String): Boolean
    fun getWalletAlias(walletId: String): String
    fun getWalletPlan(walletId: String): MembershipPlan
    fun getBriefWallet(walletId: String): AssistedWalletBrief?
    fun getGroup(groupId: String): ByzantineGroup?
    fun isGroupAssistedWallet(groupId: String?): Boolean
    fun isSyncableWallet(walletId: String): Boolean
}