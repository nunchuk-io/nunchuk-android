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

package com.nunchuk.android.signer.mk4

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.nav.args.SetupMk4Args
import com.nunchuk.android.share.ColdcardAction
import com.nunchuk.android.signer.R
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Mk4Activity : BaseNfcActivity<ActivityNavigationBinding>() {

    private val viewModel by viewModels<Mk4ViewModel>()

    private val args by lazy { SetupMk4Args.deserializeFrom(intent) }
    
    val action: ColdcardAction by lazy(LazyThreadSafetyMode.NONE) { args.action }
    val groupId: String by lazy { args.groupId }
    val newIndex by lazy { args.newIndex }
    val xfp by lazy { args.xfp.orEmpty() }
    val replacedXfp by lazy { args.replacedXfp }
    val walletId by lazy { args.walletId }
    val signerType by lazy { args.signerType ?: SignerType.NFC }
    private val backUpFilePath by lazy { args.backUpFilePath.orEmpty() }
    val keyId by lazy { args.keyId.orEmpty() }
    val keyName by lazy { args.keyName.orEmpty() }
    val backUpFileName by lazy { args.backUpFileName.orEmpty() }
    val isFromAddKey by lazy { args.isFromAddKey }
    val isMembershipFlow by lazy { args.fromMembershipFlow }
    val quickWalletParam by lazy { args.quickWalletParam }
    val onChainAddSignerParam by lazy { args.onChainAddSignerParam }

    override fun initializeBinding(): ActivityNavigationBinding {
        return ActivityNavigationBinding.inflate(layoutInflater).also {
            enableEdgeToEdge()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initStartDestination()
    }

    private fun initStartDestination() {
        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment)
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.mk4_navigation)
        viewModel.setOrUpdate(
            ColdCardBackUpParam(
                xfp = xfp,
                keyType = signerType,
                filePath = backUpFilePath,
                keyName = keyName,
                backUpFileName = backUpFileName,
                keyId = keyId,
                isRequestAddOrReplaceKey = action != ColdcardAction.UPLOAD_BACKUP,
                groupId = groupId
            )
        )
        when (action) {
            ColdcardAction.CREATE -> {
                if (isFromAddKey || onChainAddSignerParam != null) {
                    graph.setStartDestination(R.id.coldCardIntroFragment)
                } else {
                    graph.setStartDestination(R.id.mk4IntroFragment)
                }
            }
            ColdcardAction.RECOVER_KEY -> graph.setStartDestination(R.id.coldcardRecoverFragment)
            ColdcardAction.RECOVER_SINGLE_SIG_WALLET,
            ColdcardAction.RECOVER_MULTI_SIG_WALLET,
            ColdcardAction.PARSE_SINGLE_SIG_WALLET,
            ColdcardAction.PARSE_MULTISIG_WALLET,
            -> graph.setStartDestination(R.id.mk4IntroFragment)

            ColdcardAction.INHERITANCE_PASSPHRASE_QUESTION -> graph.setStartDestination(R.id.coldCardPassphraseQuestionFragment)
            ColdcardAction.VERIFY_KEY -> graph.setStartDestination(R.id.coldCardVerifyBackUpOptionFragment)
            ColdcardAction.UPLOAD_BACKUP -> graph.setStartDestination(R.id.coldCardBackUpIntroFragment)
        }
        navHostFragment.navController.setGraph(graph, intent.extras)
        navHostFragment.navController.addOnDestinationChangedListener { _, destination, _ ->
            WindowCompat.setDecorFitsSystemWindows(
                window,
                destination.id == R.id.addMk4NameFragment
            )
        }
    }

    companion object {
        fun navigate(
            activity: Activity,
            args: SetupMk4Args
        ) {
            activity.startActivity(buildIntent(activity, args))
        }

        fun buildIntent(
            activity: Activity,
            args: SetupMk4Args
        ): Intent {
            return Intent(activity, Mk4Activity::class.java).apply {
                putExtras(args.buildBundle())
            }
        }
    }
}