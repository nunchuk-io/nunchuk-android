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
import com.nunchuk.android.core.signer.OnChainAddSignerParam
import com.nunchuk.android.share.ColdcardAction
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.utils.parcelable

data class SetupMk4Args(
    val fromMembershipFlow: Boolean = false,
    val action: ColdcardAction = ColdcardAction.CREATE,
    val groupId: String = "",
    val newIndex: Int = -1,
    val isScanQRCode: Boolean = false,
    val replacedXfp: String? = null,
    val walletId: String? = null,
    val signerType: SignerType? = null,
    val backUpFilePath: String? = null,
    val keyId: String? = null,
    val keyName: String? = null,
    val xfp: String? = null,
    val backUpFileName: String? = null,
    val isFromAddKey: Boolean = false,
    val quickWalletParam: QuickWalletParam? = null,
    val onChainAddSignerParam: OnChainAddSignerParam? = null,
) {

    fun buildBundle() = Bundle().apply {
        putBoolean(FROM_MEMBERSHIP_FLOW, fromMembershipFlow)
        putSerializable(ACTION, action)
        putString(GROUP_ID, groupId)
        putInt(NEW_INDEX, newIndex)
        putBoolean(IS_SCAN_QR_CODE, isScanQRCode)
        putString(REPLACED_XFP, replacedXfp)
        putString(WALLET_ID, walletId)
        putSerializable(SIGNER_TYPE, signerType)
        putString(BACKUP_FILE_PATH, backUpFilePath)
        putString(KEY_ID, keyId)
        putString(KEY_NAME, keyName)
        putString(XFP, xfp)
        putString(BACKUP_FILE_NAME, backUpFileName)
        putBoolean(IS_FROM_ADD_KEY, isFromAddKey)
        putParcelable(QUICK_WALLET_PARAM, quickWalletParam)
        putParcelable(ONCHAIN_ADD_SIGNER_PARAM, onChainAddSignerParam)
    }

    companion object {
        private const val FROM_MEMBERSHIP_FLOW = "from_membership_flow"
        private const val ACTION = "action"
        private const val GROUP_ID = "group_id"
        private const val NEW_INDEX = "new_index"
        private const val IS_SCAN_QR_CODE = "is_scan_qr_code"
        private const val REPLACED_XFP = "replaced_xfp"
        private const val WALLET_ID = "wallet_id"
        private const val SIGNER_TYPE = "signer_type"
        private const val BACKUP_FILE_PATH = "backup_file_path"
        private const val KEY_ID = "key_id"
        private const val KEY_NAME = "key_name"
        private const val XFP = "xfp"
        private const val BACKUP_FILE_NAME = "backup_file_name"
        private const val IS_FROM_ADD_KEY = "is_from_add_key"
        private const val QUICK_WALLET_PARAM = "quick_wallet_param"
        private const val ONCHAIN_ADD_SIGNER_PARAM = "onchain_add_signer_param"

        fun deserializeFrom(intent: Intent): SetupMk4Args = SetupMk4Args(
            fromMembershipFlow = intent.extras?.getBoolean(FROM_MEMBERSHIP_FLOW, false) == true,
            action = intent.extras?.getSerializable(ACTION) as? ColdcardAction ?: ColdcardAction.CREATE,
            groupId = intent.extras?.getString(GROUP_ID, "").orEmpty(),
            newIndex = intent.extras?.getInt(NEW_INDEX, -1) ?: -1,
            isScanQRCode = intent.extras?.getBoolean(IS_SCAN_QR_CODE, false) == true,
            replacedXfp = intent.extras?.getString(REPLACED_XFP),
            walletId = intent.extras?.getString(WALLET_ID),
            signerType = intent.extras?.getSerializable(SIGNER_TYPE) as? SignerType,
            backUpFilePath = intent.extras?.getString(BACKUP_FILE_PATH),
            keyId = intent.extras?.getString(KEY_ID),
            keyName = intent.extras?.getString(KEY_NAME),
            xfp = intent.extras?.getString(XFP),
            backUpFileName = intent.extras?.getString(BACKUP_FILE_NAME),
            isFromAddKey = intent.extras?.getBoolean(IS_FROM_ADD_KEY, false) == true,
            quickWalletParam = intent.parcelable<QuickWalletParam>(QUICK_WALLET_PARAM),
            onChainAddSignerParam = intent.parcelable<OnChainAddSignerParam>(ONCHAIN_ADD_SIGNER_PARAM)
        )
    }
}
