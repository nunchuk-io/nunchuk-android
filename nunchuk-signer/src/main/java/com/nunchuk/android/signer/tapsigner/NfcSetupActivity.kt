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

package com.nunchuk.android.signer.tapsigner

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcViewModel.Companion.EXTRA_MASTER_SIGNER_ID
import com.nunchuk.android.core.signer.OnChainAddSignerParam
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.tapsigner.backup.onchain.TapSignerBackingUpIntroOnChainFragmentArgs
import com.nunchuk.android.signer.tapsigner.backup.verify.TapSignerVerifyBackUpOptionFragmentArgs
import com.nunchuk.android.signer.tapsigner.id.TapSignerIdFragmentArgs
import com.nunchuk.android.signer.tapsigner.intro.AddTapSignerIntroFragmentArgs
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NfcSetupActivity : BaseNfcActivity<ActivityNavigationBinding>() {
    override fun initializeBinding(): ActivityNavigationBinding =
        ActivityNavigationBinding.inflate(layoutInflater).also {
            enableEdgeToEdge()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        keyId = intent.getStringExtra(EXTRA_KEY_ID).orEmpty()
        initStartDestination()
    }

    private fun initStartDestination() {
        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment)
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.nfc_setup_navigation)
        val startDestinationId = when (setUpAction) {
            SETUP_TAP_SIGNER -> {
                if (fromMembershipFlow) R.id.addTapSignerIntroFragment else R.id.tapSignerIntroFragment
            }
            SETUP_SATSCARD -> R.id.setupChainCodeFragment
            RECOVER_NFC -> R.id.recoverNfcKeyGuideFragment
            VERIFY_TAP_SIGNER -> R.id.tapSignerVerifyBackUpOptionFragment
            CREATE_BACK_UP_KEY -> if (isOnChainBackUp) R.id.tapSignerBackingUpIntroOnChainFragment else R.id.tapSignerIdFragment
            CHANGE_CVC -> R.id.changeNfcCvcFragment
            else -> R.id.addNfcNameFragment
        }
        graph.setStartDestination(startDestinationId)
        val extras = when (setUpAction) {
            SETUP_TAP_SIGNER -> AddTapSignerIntroFragmentArgs(fromMembershipFlow).toBundle()
            VERIFY_TAP_SIGNER -> TapSignerVerifyBackUpOptionFragmentArgs(
                masterSignerId = intent.getStringExtra(EXTRA_MASTER_SIGNER_ID).orEmpty(),
                filePath = intent.getStringExtra(EXTRA_BACKUP_FILE_PATH).orEmpty()
            ).toBundle()

            CREATE_BACK_UP_KEY -> if (isOnChainBackUp) {
                TapSignerBackingUpIntroOnChainFragmentArgs(
                    masterSignerId = intent.getStringExtra(EXTRA_MASTER_SIGNER_ID).orEmpty()
                ).toBundle()
            } else {
                TapSignerIdFragmentArgs(
                    masterSignerId = intent.getStringExtra(EXTRA_MASTER_SIGNER_ID).orEmpty(),
                    isMembershipFlow = fromMembershipFlow
                ).toBundle()
            }

            else -> intent.extras
        }
        navHostFragment.navController.setGraph(graph, extras)
    }

    val setUpAction: Int
            by lazy(LazyThreadSafetyMode.NONE) {
                intent.getIntExtra(
                    EXTRA_ACTION,
                    SETUP_TAP_SIGNER
                )
            }

    val fromMembershipFlow: Boolean
            by lazy(LazyThreadSafetyMode.NONE) {
                intent.getBooleanExtra(
                    EXTRA_FROM_MEMBERSHIP_FLOW,
                    false
                )
            }

    val hasWallet: Boolean
            by lazy(LazyThreadSafetyMode.NONE) { intent.getBooleanExtra(EXTRA_HAS_WALLET, false) }

    val isAddNewSigner: Boolean
            by lazy(LazyThreadSafetyMode.NONE) {
                intent.getStringExtra(
                    EXTRA_MASTER_SIGNER_ID
                ).orEmpty().isEmpty()
            }

    val groupId: String
            by lazy(LazyThreadSafetyMode.NONE) { intent.getStringExtra(EXTRA_GROUP_ID).orEmpty() }

    val signerIndex: Int
            by lazy(LazyThreadSafetyMode.NONE) { intent.getIntExtra(EXTRA_SIGNER_INDEX, 0) }

    val replacedXfp: String
            by lazy(LazyThreadSafetyMode.NONE) {
                intent.getStringExtra(EXTRA_REPLACED_XFP).orEmpty()
            }

    val walletId: String
            by lazy(LazyThreadSafetyMode.NONE) { intent.getStringExtra(EXTRA_WALLET_ID).orEmpty() }

    val onChainAddSignerParam: OnChainAddSignerParam?
            by lazy(LazyThreadSafetyMode.NONE) { 
                intent.getParcelableExtra(EXTRA_ONCHAIN_ADD_SIGNER_PARAM)
            }

    val isOnChainBackUp: Boolean
            by lazy(LazyThreadSafetyMode.NONE) {
                intent.getBooleanExtra(EXTRA_IS_ONCHAIN_BACKUP, false)
            }

    var keyId: String = ""

    companion object {
        private const val EXTRA_ACTION = "EXTRA_ACTION"
        private const val EXTRA_HAS_WALLET = "EXTRA_HAS_WALLET"
        private const val EXTRA_FROM_MEMBERSHIP_FLOW = "isMembershipFlow"
        private const val EXTRA_BACKUP_FILE_PATH = "EXTRA_BACKUP_FILE_PATH"
        private const val EXTRA_KEY_ID = "key_id"
        const val EXTRA_SATSCARD_SLOT = "EXTRA_SATSCARD_SLOT"
        const val EXTRA_GROUP_ID = "group_id"
        const val EXTRA_SIGNER_INDEX = "signer_index"
        const val EXTRA_REPLACED_XFP = "replaced_xfp"
        const val EXTRA_WALLET_ID = "wallet_id"
        private const val EXTRA_ONCHAIN_ADD_SIGNER_PARAM = "onchain_add_signer_param"
        private const val EXTRA_IS_ONCHAIN_BACKUP = "is_onchain_backup"

        /**
         * Setup action
         */
        const val SETUP_TAP_SIGNER = 1
        const val ADD_KEY = 2
        const val CHANGE_CVC = 3
        const val RECOVER_NFC = 4
        const val SETUP_SATSCARD = 5
        const val VERIFY_TAP_SIGNER = 7
        const val CREATE_BACK_UP_KEY = 8

        /**
         * @param masterSignerId need to setup satscard
         * @param hasWallet need to setup satscard
         */
        fun navigate(
            activity: Activity,
            setUpAction: Int,
            fromMembershipFlow: Boolean = false,
            masterSignerId: String? = null,
            backUpFilePath: String? = null,
            hasWallet: Boolean = false,
            slot: SatsCardSlot? = null,
        ) {
            activity.startActivity(
                buildIntent(
                    activity = activity,
                    setUpAction = setUpAction,
                    fromMembershipFlow = fromMembershipFlow,
                    masterSignerId = masterSignerId,
                    backUpFilePath = backUpFilePath,
                    hasWallet = hasWallet,
                    slot = slot
                )
            )
        }

        fun buildIntent(
            activity: Activity,
            setUpAction: Int,
            fromMembershipFlow: Boolean = false,
            masterSignerId: String? = null,
            backUpFilePath: String? = null,
            hasWallet: Boolean = false,
            slot: SatsCardSlot? = null,
            groupId: String = "",
            signerIndex: Int = 0,
            replacedXfp: String = "",
            walletId: String = "",
            keyId: String = "",
            onChainAddSignerParam: OnChainAddSignerParam? = null,
            isOnChainBackUp: Boolean = false,
        ) = Intent(activity, NfcSetupActivity::class.java).apply {
            putExtra(EXTRA_ACTION, setUpAction)
            putExtra(EXTRA_MASTER_SIGNER_ID, masterSignerId)
            putExtra(EXTRA_HAS_WALLET, hasWallet)
            putExtra(EXTRA_FROM_MEMBERSHIP_FLOW, fromMembershipFlow)
            putExtra(EXTRA_BACKUP_FILE_PATH, backUpFilePath)
            putExtra(EXTRA_SATSCARD_SLOT, slot)
            putExtra(EXTRA_GROUP_ID, groupId)
            putExtra(EXTRA_SIGNER_INDEX, signerIndex)
            putExtra(EXTRA_REPLACED_XFP, replacedXfp)
            putExtra(EXTRA_WALLET_ID, walletId)
            putExtra(EXTRA_KEY_ID, keyId)
            putExtra(EXTRA_ONCHAIN_ADD_SIGNER_PARAM, onChainAddSignerParam)
            putExtra(EXTRA_IS_ONCHAIN_BACKUP, isOnChainBackUp)
        }
    }
}