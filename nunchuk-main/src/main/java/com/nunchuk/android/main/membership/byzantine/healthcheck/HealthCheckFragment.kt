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
import com.nunchuk.android.core.util.hideLoading
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.util.showSuccess
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.byzantine.groupdashboard.GroupDashboardEvent
import com.nunchuk.android.main.membership.byzantine.groupdashboard.GroupDashboardViewModel
import com.nunchuk.android.model.VerificationType
import com.nunchuk.android.model.byzantine.DummyTransactionType
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.utils.serializable
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
                viewModel.getKeysStatus()
                val type =
                    it.data?.serializable<DummyTransactionType>(GlobalResultKey.EXTRA_DUMMY_TX_TYPE)
                if (type == DummyTransactionType.HEALTH_CHECK_PENDING || type == DummyTransactionType.HEALTH_CHECK_REQUEST) {
                    val xfp =
                        it.data?.getStringExtra(GlobalResultKey.EXTRA_HEALTH_CHECK_XFP).orEmpty()
                    viewModel.getSignerName(xfp)?.let { name ->
                        showSuccess(
                            message = getString(
                                R.string.nc_txt_run_health_check_success_event,
                                name
                            )
                        )
                    }
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
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
            when (it) {
                is GroupDashboardEvent.RequestHealthCheckSuccess -> {
                    viewModel.getKeysStatus()
                    findNavController().navigate(
                        HealthCheckFragmentDirections.actionHealthCheckFragmentToRequestHealthCheckSentFragment()
                    )
                }

                is GroupDashboardEvent.GetHealthCheckPayload -> {
                    hideLoading()
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

                is GroupDashboardEvent.Loading -> showOrHideLoading(it.loading)
                is GroupDashboardEvent.Error -> showError(message = it.message)
                else -> Unit
            }
        }
    }
}