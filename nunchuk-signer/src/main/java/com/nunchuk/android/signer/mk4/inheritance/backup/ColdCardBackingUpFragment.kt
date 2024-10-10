package com.nunchuk.android.signer.mk4.inheritance.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.signer.components.backup.BackingUpContent
import com.nunchuk.android.signer.components.backup.BackingUpEvent
import com.nunchuk.android.signer.components.backup.BackingUpState
import com.nunchuk.android.signer.components.backup.BackingUpViewModel
import com.nunchuk.android.signer.mk4.Mk4Activity
import com.nunchuk.android.signer.mk4.Mk4ViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ColdCardBackingUpFragment : MembershipFragment() {
    private val viewModel: BackingUpViewModel by viewModels()
    private val args: ColdCardBackingUpFragmentArgs by navArgs()
    private val mk4ViewModel: Mk4ViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                ColdCardBackingUpScreen(viewModel, membershipStepManager)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val replacedXfp = (requireActivity() as Mk4Activity).replacedXfp
        val walletId = (requireActivity() as Mk4Activity).walletId
        val newIndex = (requireActivity() as Mk4Activity).newIndex
        viewModel.init(
            isAddNewKey = true,
            groupId = (requireActivity() as Mk4Activity).groupId,
            signerIndex = if (newIndex == -1) 0 else newIndex,
            replacedXfp = replacedXfp.orEmpty(),
            walletId = walletId.orEmpty(),
            masterSignerId = mk4ViewModel.coldCardBackUpParam.xfp,
            filePath = args.filePath,
            signerType = mk4ViewModel.coldCardBackUpParam.keyType,
            keyName = mk4ViewModel.coldCardBackUpParam.keyName
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.upload()
        flowObserver(viewModel.event) {
            when (it) {
                BackingUpEvent.OnContinueClicked -> {
                    val replacedXfp = (requireActivity() as Mk4Activity).replacedXfp
                    mk4ViewModel.setOrUpdate(
                        mk4ViewModel.coldCardBackUpParam.copy(
                            filePath = viewModel.getServerFilePath(),
                            backUpFileName = viewModel.getBackUpFileName(),
                            keyId = if (replacedXfp.isNullOrEmpty().not()) mk4ViewModel.coldCardBackUpParam.keyId else ""
                        )
                    )
                    findNavController().navigate(
                        ColdCardBackingUpFragmentDirections.actionColdCardBackingUpFragmentToColdCardVerifyBackUpOptionFragment(),
                        NavOptions.Builder()
                            .setPopUpTo(findNavController().graph.startDestinationId, true).build()
                    )
                }

                is BackingUpEvent.ShowError -> {
                    showError(it.message)
                }

                is BackingUpEvent.KeyVerified -> {
                    NcToastManager.scheduleShowMessage(it.message)
                    requireActivity().finish()
                }
            }
        }
    }
}

@Composable
private fun ColdCardBackingUpScreen(
    viewModel: BackingUpViewModel = viewModel(),
    membershipStepManager: MembershipStepManager
) {
    val state: BackingUpState by viewModel.state.collectAsStateWithLifecycle()
    val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
    BackingUpContent(
        onContinueClicked = viewModel::onContinueClicked,
        percentage = state.percent,
        isError = state.isError,
        remainTime = remainTime,
        title = "Backing up COLDCARD",
        description = "An encrypted backup for the COLDCARD will be stored on the server. "
    )
}