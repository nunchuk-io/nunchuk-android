package com.nunchuk.android.signer.mk4.inheritance.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import androidx.fragment.app.viewModels
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.signer.mk4.Mk4Activity
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.signer.R
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.signer.components.backup.BACKUP_OPTIONS
import com.nunchuk.android.signer.components.backup.BackUpOption
import com.nunchuk.android.signer.components.backup.BackUpOptionType
import com.nunchuk.android.signer.components.backup.VerifyBackUpOptionContent
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ColdCardVerifyBackUpOptionFragment : MembershipFragment() {

    private val viewModel: ColdCardVerifyBackUpOptionViewModel by viewModels()

    private val mk4Activity by lazy { requireActivity() as Mk4Activity }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                ColdCardVerifyBackUpOptionScreen(
                    membershipStepManager = membershipStepManager,
                    signerTag = mk4Activity.signerTag,
                ) {
                    handleVerifyClicked(it)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { requireActivity().finish() }
    }


    private fun handleVerifyClicked(option: BackUpOption) {
        when (option.type) {
            BackUpOptionType.BY_APP -> {
                findNavController().navigate(
                    ColdCardVerifyBackUpOptionFragmentDirections.actionColdCardVerifyBackUpOptionFragmentToColdCardVerifyBackupViaAppFragment()
                )
            }

            BackUpOptionType.BY_MYSELF -> {
                findNavController().navigate(
                    ColdCardVerifyBackUpOptionFragmentDirections.actionColdCardVerifyBackUpOptionFragmentToColdCardVerifyBackupMySelfIntroFragment()
                )
            }

            BackUpOptionType.SKIP -> {
                NCWarningDialog(requireActivity()).showDialog(
                    title = getString(R.string.nc_confirmation),
                    message = getString(R.string.nc_skip_back_up_desc),
                    onYesClick = {
                        // An off-chain inheritance key has to record the skip, otherwise the half
                        // it covers stays unresolved and the key list keeps asking for it.
                        val claimOption = mk4Activity.claimOption
                        if (claimOption != null) {
                            viewModel.skipVerification(
                                groupId = mk4Activity.groupId,
                                masterSignerId = mk4Activity.xfp,
                                verificationMethod = claimOption,
                            )
                        } else {
                            requireActivity().finish()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ColdCardVerifyBackUpOptionScreen(
    membershipStepManager: MembershipStepManager,
    signerTag: SignerTag,
    onContinueClicked: (BackUpOption) -> Unit = {}
) {
    val remainingTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()

    VerifyBackUpOptionContent(
        onContinueClicked = onContinueClicked,
        // Checking the backup in the app decrypts it with `verifyColdCardBackup`, which
        // understands Coldcard's format only — another vendor's backup would fail it for the
        // wrong reason, so that option is not offered.
        options = if (signerTag == SignerTag.COLDCARD) {
            BACKUP_OPTIONS
        } else {
            BACKUP_OPTIONS.filter { it.type != BackUpOptionType.BY_APP }
        },
        remainingTime = remainingTime
    )
}