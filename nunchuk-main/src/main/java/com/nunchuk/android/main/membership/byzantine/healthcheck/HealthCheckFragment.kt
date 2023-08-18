package com.nunchuk.android.main.membership.byzantine.healthcheck

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.membership.byzantine.groupdashboard.GroupDashboardEvent
import com.nunchuk.android.main.membership.byzantine.groupdashboard.GroupDashboardViewModel
import com.nunchuk.android.model.VerificationType
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.membership.MembershipFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HealthCheckFragment : MembershipFragment() {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: GroupDashboardViewModel by activityViewModels()

    private val args: HealthCheckFragmentArgs by navArgs()

    private val signLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {

            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val state by viewModel.state.collectAsStateWithLifecycle()
                HealthCheckContent(
                    state = state,
                    onRequestHealthCheck = viewModel::onRequestHealthCheck,
                    onHealthCheck = viewModel::onHealthCheck
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) {
            if (it is GroupDashboardEvent.RequestHealthCheckSuccess) {
                findNavController().navigate(
                    HealthCheckFragmentDirections.actionHealthCheckFragmentToRequestHealthCheckSentFragment()
                )
            } else if (it is GroupDashboardEvent.GetHealthCheckPayload) {
                navigator.openWalletAuthentication(
                    activityContext = requireActivity(),
                    walletId = args.walletId,
                    requiredSignatures = it.payload.requiredSignatures,
                    type = VerificationType.SIGN_DUMMY_TX,
                    groupId = args.groupId,
                    dummyTransactionId = it.payload.dummyTransactionId,
                    launcher = signLauncher
                )
            }
        }
    }
}