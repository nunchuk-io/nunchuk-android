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
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.components.backup.BACKUP_OPTIONS
import com.nunchuk.android.signer.components.backup.BackUpOption
import com.nunchuk.android.signer.components.backup.BackUpOptionType
import com.nunchuk.android.signer.components.backup.VerifyBackUpOptionContent
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ColdCardVerifyBackUpOptionFragment : MembershipFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                ColdCardVerifyBackUpOptionScreen(membershipStepManager) {
                    handleVerifyClicked(it)
                }
            }
        }
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
                        requireActivity().finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun ColdCardVerifyBackUpOptionScreen(
    membershipStepManager: MembershipStepManager,
    onContinueClicked: (BackUpOption) -> Unit = {}
) {
    val remainingTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()

    VerifyBackUpOptionContent(
        onContinueClicked = onContinueClicked,
        options = BACKUP_OPTIONS,
        remainingTime = remainingTime
    )
}