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

package com.nunchuk.android.nav.args

import android.content.Intent
import android.os.Bundle
import com.nunchuk.android.core.data.model.QuickWalletParam
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.serializable

data class MembershipArgs(
    val groupStep: MembershipStage,
    val walletId: String? = null,
    val groupId: String? = null,
    val isPersonalWallet: Boolean = false,
    val groupWalletType: GroupWalletType? = null,
    val slug: String? = null,
    val walletTypeName: String? = null,
    val walletType: WalletType? = null,
    val quickWalletParam: QuickWalletParam? = null,
    val inheritanceType: String? = null,
    val replacedWalletId: String? = null,
    val changeTimelockFlow: Int = -1
) {

    fun buildBundle() = Bundle().apply {
        putSerializable(GROUP_STEP, groupStep)
        putString(WALLET_ID, walletId)
        putString(GROUP_ID, groupId)
        putBoolean(IS_PERSONAL_WALLET, isPersonalWallet)
        putSerializable(GROUP_WALLET_TYPE, groupWalletType)
        putString(SLUG, slug)
        putString(WALLET_TYPE_NAME, walletTypeName)
        putSerializable(WALLET_TYPE, walletType)
        putParcelable(QUICK_WALLET_PARAM, quickWalletParam)
        putString(INHERITANCE_TYPE, inheritanceType)
        putString(REPLACED_WALLET_ID, replacedWalletId)
        putInt(CHANGE_TIMELOCK_FLOW, changeTimelockFlow)
    }

    companion object {
        const val GROUP_STEP = "group_step"
        const val WALLET_ID = "wallet_id"
        const val GROUP_ID = "group_id"
        const val IS_PERSONAL_WALLET = "is_personal"
        const val GROUP_WALLET_TYPE = "group_wallet_type"
        const val SLUG = "slug"
        const val WALLET_TYPE_NAME = "wallet_type_name"
        const val WALLET_TYPE = "wallet_type"
        const val QUICK_WALLET_PARAM = "quick_wallet_param"
        const val INHERITANCE_TYPE = "inheritance_type"
        const val REPLACED_WALLET_ID = "replaced_wallet_id"
        const val CHANGE_TIMELOCK_FLOW = "change_timelock_flow"

        fun deserializeFrom(intent: Intent): MembershipArgs = MembershipArgs(
            groupStep = intent.extras?.serializable(GROUP_STEP)
                ?: throw IllegalArgumentException("MembershipStage is required"),
            walletId = intent.extras?.getString(WALLET_ID),
            groupId = intent.extras?.getString(GROUP_ID),
            isPersonalWallet = intent.extras?.getBoolean(IS_PERSONAL_WALLET, false) ?: false,
            groupWalletType = intent.extras?.serializable(GROUP_WALLET_TYPE),
            slug = intent.extras?.getString(SLUG),
            walletTypeName = intent.extras?.getString(WALLET_TYPE_NAME),
            walletType = intent.extras?.serializable(WALLET_TYPE),
            quickWalletParam = intent.extras?.parcelable(QUICK_WALLET_PARAM),
            inheritanceType = intent.extras?.getString(INHERITANCE_TYPE),
            replacedWalletId = intent.extras?.getString(REPLACED_WALLET_ID),
            changeTimelockFlow = intent.extras?.getInt(CHANGE_TIMELOCK_FLOW, -1) ?: -1
        )
    }
}

