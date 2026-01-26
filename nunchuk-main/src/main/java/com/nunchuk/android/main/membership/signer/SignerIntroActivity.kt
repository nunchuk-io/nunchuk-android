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

package com.nunchuk.android.main.membership.signer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.compose.NcSelectableBottomSheet
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.R
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.portal.PortalDeviceArgs
import com.nunchuk.android.core.portal.PortalDeviceFlow
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.signer.KeyFlow
import com.nunchuk.android.core.signer.OnChainAddSignerParam
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.membership.key.list.SelectSignerBottomSheet
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragmentArgs
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.nav.args.AddAirSignerArgs
import com.nunchuk.android.nav.args.SetupMk4Args
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.signer.KeyType
import com.nunchuk.android.signer.SignerIntroEvent
import com.nunchuk.android.signer.SignerIntroViewModel
import com.nunchuk.android.signer.mk4.Mk4Activity
import com.nunchuk.android.signer.tapsigner.NfcSetupActivity
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.parcelableArrayList
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SignerIntroActivity : BaseComposeActivity(), BottomSheetOptionListener {

    @Inject
    lateinit var membershipStepManager: MembershipStepManager

    private val supportedSigners: List<SupportedSigner> by lazy {
        intent.parcelableArrayList<SupportedSigner>(EXTRA_SUPPORTED_SIGNERS).orEmpty()
    }
    private val keyFlow by lazy { intent.getIntExtra(EXTRA_KEY_FLOW, KeyFlow.NONE) }
    private val onChainAddSignerParam by lazy {
        intent.parcelable<OnChainAddSignerParam>(EXTRA_ONCHAIN_ADD_SIGNER_PARAM)
    }
    private val isClaiming by lazy {
        onChainAddSignerParam?.isClaiming == true
    }

    private val viewModel: SignerIntroViewModel by viewModels()

    private val signerResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val signer = result.data?.parcelable<SignerModel>(GlobalResultKey.EXTRA_SIGNER)
            if (signer != null) {
                returnSigner(signer)
            }
        }
    }

    private val recoverSeedLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val mnemonic = result.data?.getStringExtra(GlobalResultKey.MNEMONIC).orEmpty()
            val passphrase = result.data?.getStringExtra(GlobalResultKey.PASSPHRASE).orEmpty()
            if (mnemonic.isNotEmpty()) {
                val signerCount = viewModel.state.value.allSigners.size + 1
                val signerName = "Inheritance key #$signerCount"
                viewModel.createSoftwareSignerFromMnemonic(mnemonic, passphrase, signerName)
            }
        }
    }

    private val xprvLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val xprv = result.data?.getStringExtra(GlobalResultKey.XPRV).orEmpty()
            if (xprv.isNotEmpty()) {
                val signerCount = viewModel.state.value.allSigners.size + 1
                val signerName = "Inheritance key #$signerCount"
                viewModel.createSoftwareSignerFromXprv(xprv, signerName)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        observeEvent()

        viewModel.init(
            onChainAddSignerParam = onChainAddSignerParam,
            supportedSigners = supportedSigners,
            keyFlow = keyFlow
        )

        setContentView(ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val navHostController = rememberNavController()
                var showSignerBottomSheet by remember { mutableStateOf(false) }
                var filteredSigners by remember { mutableStateOf<List<SignerModel>>(emptyList()) }
                var signerType by remember { mutableStateOf<SignerType?>(null) }
                var signerTag by remember { mutableStateOf<SignerTag?>(null) }
                var showRecoverSheet by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    viewModel.event.collect { event ->
                        when (event) {
                            is SignerIntroEvent.ShowFilteredSigners -> {
                                filteredSigners = event.signers
                                signerType = event.type
                                signerTag = event.tag
                                showSignerBottomSheet = true
                            }

                            is SignerIntroEvent.OpenSetupSigner -> {
                                when (event.type) {
                                    SignerType.NFC -> navigateToSetupTapSigner()
                                    SignerType.COLDCARD_NFC -> openSetupMk4()
                                    SignerType.SOFTWARE -> {
                                        if (isClaiming) {
                                            showRecoverSheet = true
                                        } else {
                                            createNewSoftware()
                                        }
                                    }

                                    SignerType.AIRGAP -> {
                                        when (event.tag) {
                                            SignerTag.JADE -> openAddAirSignerForJade()
                                            else -> handleSelectAddAirgapType(event.tag)
                                        }
                                    }

                                    else -> { /* no-op */
                                    }
                                }
                            }

                            else -> {}
                        }
                    }
                }

                NunchukTheme {
                    NavHost(
                        navController = navHostController,
                        startDestination = SignerIntroDestination
                    ) {
                        signerIntroDestination(
                            viewModel = viewModel,
                            keyFlow = keyFlow,
                            onChainAddSignerParam = onChainAddSignerParam,
                            onClick = { keyType: KeyType ->
                                when (keyType) {
                                    KeyType.TAPSIGNER -> handleTapSignerSelection()
                                    KeyType.COLDCARD -> handleColdCardSelection(navHostController)
                                    KeyType.JADE -> handleJadeSelection(navHostController)
                                    KeyType.PORTAL -> openPortalScreen()
                                    KeyType.SEEDSIGNER -> handleSelectAddAirgapType(SignerTag.SEEDSIGNER)
                                    KeyType.KEYSTONE -> handleSelectAddAirgapType(SignerTag.KEYSTONE)
                                    KeyType.FOUNDATION -> handleSelectAddAirgapType(SignerTag.PASSPORT)
                                    KeyType.SOFTWARE -> showSoftwareSigners()
                                    KeyType.GENERIC_AIRGAP -> openAddAirSignerIntroScreen()
                                    KeyType.LEDGER -> handleHardwareSignerSelection(SignerTag.LEDGER)
                                    KeyType.BITBOX -> handleHardwareSignerSelection(SignerTag.BITBOX)
                                    KeyType.TREZOR -> handleHardwareSignerSelection(SignerTag.TREZOR)
                                    else -> {}
                                }
                            },
                            onMoreClicked = ::handleShowMore,
                        )

                        checkFirmwareDestination(
                            onChainAddSignerParam = onChainAddSignerParam,
                            onMoreClicked = ::handleShowMore,
                            onFilteredSignersReady = { signer ->
                                returnSigner(signer)
                            },
                            onOpenNextScreen = { signerTag ->
                                when (signerTag) {
                                    SignerTag.COLDCARD -> openSetupMk4()
                                    SignerTag.JADE -> openAddAirSignerForJade()
                                    else -> {}
                                }
                            }
                        )
                    }

                    if (showSignerBottomSheet && filteredSigners.isNotEmpty()) {
                        SelectSignerBottomSheet(
                            args = TapSignerListBottomSheetFragmentArgs(
                                signers = filteredSigners.toTypedArray(),
                                type = signerType ?: SignerType.UNKNOWN,
                                description = "",
                                ignoreIndexCheckForAcctX = true
                            ),
                            onDismiss = {
                                showSignerBottomSheet = false
                            },
                            onAddExistKey = { signer ->
                                showSignerBottomSheet = false
                                returnSigner(signer)
                            },
                            onAddNewKey = {
                                showSignerBottomSheet = false
                                signerType?.let { signerType ->
                                    viewModel.createNewSigner(signerType, signerTag)
                                }
                            }
                        )
                    }

                    if (showRecoverSheet) {
                        NcSelectableBottomSheet(
                            options = listOf(
                                stringResource(R.string.nc_recover_key_via_seed),
                                stringResource(R.string.nc_recover_key_via_xprv),
                                stringResource(R.string.nc_recover_tapsigner_key_from_backup),
                            ),
                            onSelected = {
                                when (it) {
                                    0 -> onRecoverSeedClicked()
                                    1 -> onRecoverXprvClicked()
                                    2 -> onRecoverTapSignerClicked()
                                }
                                showRecoverSheet = false
                            },
                            onDismiss = {
                                showRecoverSheet = false
                            },
                        )
                    }
                }
            }
        })
    }

    private fun onRecoverSeedClicked() {
        navigator.openRecoverSeedScreen(
            launcher = recoverSeedLauncher,
            activityContext = this,
            keyFlow = KeyFlow.ADD_AND_RETURN_PASSPHRASE
        )
    }

    private fun onRecoverXprvClicked() {
        navigator.openAddSoftwareSignerScreen(
            activityContext = this,
            keyFlow = KeyFlow.ADD_AND_RETURN,
            launcher = xprvLauncher,
            masterSignerId = ""
        )
    }

    private fun onRecoverTapSignerClicked() {
        navigator.openRecoverTapSigner(
            launcher = signerResultLauncher,
            activity = this,
            fromMembershipFlow = true
        )
    }

    private fun handleTapSignerSelection() {
        if (onChainAddSignerParam != null) {
            viewModel.showExistingSignerOrCreateNew(SignerType.NFC)
        } else {
            navigateToSetupTapSigner()
        }
    }

    private fun handleColdCardSelection(navController: NavHostController) {
        val onChainAddSignerParam = onChainAddSignerParam
        if (onChainAddSignerParam == null || onChainAddSignerParam.isVerifyBackupSeedPhrase()) {
            openSetupMk4()
        } else if (onChainAddSignerParam.isAddInheritanceOffChainSigner()) {
            viewModel.showExistingSignerOrCreateNew(SignerType.COLDCARD_NFC, SignerTag.COLDCARD)
        } else {
            navController.navigate(
                CheckFirmwareDestination(
                    signerTagName = SignerTag.COLDCARD.name,
                    walletId = walletId,
                    groupId = groupId
                )
            )
        }
    }

    private fun handleJadeSelection(navController: NavHostController) {
        if (onChainAddSignerParam == null || onChainAddSignerParam?.isVerifyBackupSeedPhrase() == true) {
            handleSelectAddAirgapType(SignerTag.JADE)
        } else {
            navController.navigate(
                CheckFirmwareDestination(
                    signerTagName = SignerTag.JADE.name,
                    walletId = walletId,
                    groupId = groupId
                )
            )
        }
    }

    private fun openAddAirSignerForJade() {
        navigator.openAddAirSignerScreen(
            activityContext = this,
            args = AddAirSignerArgs(
                isMembershipFlow = onChainAddSignerParam != null,
                tag = SignerTag.JADE,
                groupId = groupId,
                walletId = walletId,
                replacedXfp = onChainAddSignerParam?.replaceInfo?.replacedXfp,
                onChainAddSignerParam = onChainAddSignerParam,
                step = membershipStepManager.currentStep
            )
        )
        finish()
    }

    private fun handleHardwareSignerSelection(tag: SignerTag) {
        if (onChainAddSignerParam != null && onChainAddSignerParam?.isClaiming == false) {
            val intent = Intent().apply {
                putExtra(GlobalResultKey.EXTRA_SIGNER_TAG, tag)
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        } else if (onChainAddSignerParam?.isClaiming == true) {
            navigator.openAddDesktopKey(
                this,
                signerTag = tag,
                step = MembershipStep.SETUP_INHERITANCE,
                isInheritanceKey = true,
                magic = onChainAddSignerParam?.magic.orEmpty()
            )
        }
    }

    private fun handleSelectAddAirgapType(tag: SignerTag?) {
        val args = AddAirSignerArgs(
            isMembershipFlow = onChainAddSignerParam != null,
            tag = tag,
            groupId = groupId,
            walletId = walletId,
            onChainAddSignerParam = onChainAddSignerParam,
            step = membershipStepManager.currentStep
        )

        if (onChainAddSignerParam != null) {
            navigator.openAddAirSignerScreenForResult(
                launcher = signerResultLauncher,
                activityContext = this,
                args = args
            )
        } else {
            navigator.openAddAirSignerScreen(
                activityContext = this,
                args = args
            )
            finish()
        }
    }

    private fun openSetupMk4() {
        val args = SetupMk4Args(
            fromMembershipFlow = onChainAddSignerParam != null,
            isFromAddKey = true,
            groupId = groupId,
            walletId = walletId,
            replacedXfp = onChainAddSignerParam?.replaceInfo?.replacedXfp,
            onChainAddSignerParam = onChainAddSignerParam,
        )

        if (onChainAddSignerParam != null) {
            signerResultLauncher.launch(
                Mk4Activity.buildIntent(
                    activity = this,
                    args = args
                )
            )
        } else {
            navigator.openSetupMk4(
                activity = this,
                args = args
            )
            finish()
        }
    }

    private fun openPortalScreen() {
        navigator.openPortalScreen(
            activity = this,
            args = PortalDeviceArgs(
                type = PortalDeviceFlow.SETUP,
                isMembershipFlow = walletId.isNotEmpty() || onChainAddSignerParam != null,
                walletId = walletId,
                groupId = groupId,
            )
        )
        finish()
    }

    private fun openAddAirSignerIntroScreen() {
        navigator.openAddAirSignerScreen(
            activityContext = this,
            args = AddAirSignerArgs(
                isMembershipFlow = onChainAddSignerParam != null,
                groupId = groupId,
                walletId = walletId,
                onChainAddSignerParam = onChainAddSignerParam,
            )
        )
        finish()
    }

    private fun showSoftwareSigners() {
        if (onChainAddSignerParam != null && onChainAddSignerParam?.isClaiming == true) {
            viewModel.showExistingSignerOrCreateNew(SignerType.SOFTWARE)
        } else {
            createNewSoftware()
        }
    }

    private fun createNewSoftware() {
        val primaryKeyFlow =
            if (walletId.isNotEmpty()) KeyFlow.REPLACE_KEY_IN_FREE_WALLET else keyFlow
        navigator.openAddSoftwareSignerScreen(
            activityContext = this,
            keyFlow = primaryKeyFlow,
            groupId = groupId,
            walletId = walletId,
        )
        finish()
    }

    private fun navigateToSetupTapSigner() {
        val onChainAddSignerParam = onChainAddSignerParam
        if (onChainAddSignerParam != null) {
            signerResultLauncher.launch(
                NfcSetupActivity.buildIntent(
                    activity = this,
                    setUpAction = NfcSetupActivity.SETUP_TAP_SIGNER,
                    walletId = walletId,
                    groupId = groupId,
                    fromMembershipFlow = true,
                    onChainAddSignerParam = onChainAddSignerParam
                )
            )
        } else {
            startActivity(
                NfcSetupActivity.buildIntent(
                    activity = this,
                    setUpAction = NfcSetupActivity.SETUP_TAP_SIGNER,
                    walletId = walletId,
                    groupId = groupId,
                )
            )
            finish()
        }
    }

    private fun observeEvent() {
        flowObserver(viewModel.event) { event ->
            when (event) {
                is SignerIntroEvent.RestartWizardSuccess -> {
                    navigator.openMembershipActivity(
                        activityContext = this,
                        groupStep = MembershipStage.NONE,
                        isPersonalWallet = membershipStepManager.isPersonalWallet(),
                        isClearTop = true,
                        quickWalletParam = null
                    )
                    setResult(RESULT_OK)
                    finish()
                }

                is SignerIntroEvent.CreateSoftwareSignerSuccess -> {
                    returnSigner(event.signer)
                }

                is SignerIntroEvent.Error -> {
                }

                else -> {}
            }
        }
    }

    private fun returnSigner(signer: SignerModel) {
        val intent = Intent().apply {
            putExtra(GlobalResultKey.EXTRA_SIGNER, signer)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun handleShowMore() {
        val options = mutableListOf<SheetOption>()
        options.add(
            SheetOption(
                type = SheetOptionType.TYPE_RESTART_WIZARD,
                label = getString(R.string.nc_restart_wizard)
            )
        )
        options.add(
            SheetOption(
                type = SheetOptionType.TYPE_EXIT_WIZARD,
                label = getString(R.string.nc_exit_wizard)
            )
        )
        BottomSheetOption.newInstance(options).show(supportFragmentManager, "BottomSheetOption")
    }

    override fun onOptionClicked(option: SheetOption) {
        if (option.type == SheetOptionType.TYPE_RESTART_WIZARD) {
            NCWarningDialog(this).showDialog(
                title = getString(R.string.nc_confirmation),
                message = getString(R.string.nc_confirm_restart_wizard),
                onYesClick = {
                    viewModel.resetWizard(membershipStepManager.localMembershipPlan, groupId)
                }
            )
        } else if (option.type == SheetOptionType.TYPE_EXIT_WIZARD) {
            NCInfoDialog(this).showDialog(
                message = getString(R.string.nc_resume_wizard_desc),
                onYesClick = {
                    finish()
                }
            )
        }
    }

    // replace key in free wallet
    private val walletId by lazy { intent.getStringExtra(EXTRA_WALLET_ID).orEmpty() }

    // group sandbox id
    private val groupId by lazy { intent.getStringExtra(EXTRA_GROUP_ID).orEmpty() }

    companion object {
        private const val EXTRA_WALLET_ID = "wallet_id"
        private const val EXTRA_GROUP_ID = "group_id"
        private const val EXTRA_SUPPORTED_SIGNERS = "supported_signers"
        private const val EXTRA_KEY_FLOW = "key_flow"
        private const val EXTRA_ONCHAIN_ADD_SIGNER_PARAM = "onchain_add_signer_param"

        fun start(
            activityContext: Context,
            walletId: String? = null,
            groupId: String? = null,
            supportedSigners: List<SupportedSigner>? = null,
            @KeyFlow.PrimaryFlowInfo keyFlow: Int = KeyFlow.NONE,
            onChainAddSignerParam: OnChainAddSignerParam? = null,
        ) {
            activityContext.startActivity(
                buildIntent(
                    activityContext,
                    walletId,
                    groupId,
                    supportedSigners,
                    keyFlow,
                    onChainAddSignerParam
                )
            )
        }

        fun buildIntent(
            activityContext: Context,
            walletId: String? = null,
            groupId: String? = null,
            supportedSigners: List<SupportedSigner>? = null,
            @KeyFlow.PrimaryFlowInfo keyFlow: Int = KeyFlow.NONE,
            onChainAddSignerParam: OnChainAddSignerParam? = null,
        ): Intent {
            return Intent(activityContext, SignerIntroActivity::class.java).apply {
                putExtra(EXTRA_WALLET_ID, walletId)
                putExtra(EXTRA_GROUP_ID, groupId)
                putExtra(EXTRA_KEY_FLOW, keyFlow)
                putExtra(EXTRA_ONCHAIN_ADD_SIGNER_PARAM, onChainAddSignerParam)
                supportedSigners?.let {
                    putParcelableArrayListExtra(EXTRA_SUPPORTED_SIGNERS, ArrayList(it))
                }
            }
        }
    }
}
