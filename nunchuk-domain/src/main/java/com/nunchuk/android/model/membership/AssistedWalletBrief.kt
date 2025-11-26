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

package com.nunchuk.android.model.membership

import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.wallet.WalletStatus
import com.nunchuk.android.type.WalletType

data class AssistedWalletBrief(
    val localId: String,
    val groupId: String,
    val plan: MembershipPlan,
    val isSetupInheritance: Boolean,
    val registerAirgapCount: Int,
    val primaryMembershipId: String? = null,
    val ext: AssistedWalletBriefExt,
    val alias: String = "",
    val status: String = "",
    val replaceByWalletId: String = "",
    val hideFiatCurrency: Boolean = false,
    val walletType: String = "",
)

val AssistedWalletBrief.isActiveWallet: Boolean
    get() = status == WalletStatus.ACTIVE.name

val AssistedWalletBrief.isMiniscriptWallet: Boolean
    get() = walletType == WalletType.MINISCRIPT.name

data class AssistedWalletBriefExt(
    val inheritanceOwnerId: String? = null,
    val isPlanningRequest: Boolean = false,
)