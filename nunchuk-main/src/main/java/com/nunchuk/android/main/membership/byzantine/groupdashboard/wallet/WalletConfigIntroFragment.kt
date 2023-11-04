package com.nunchuk.android.main.membership.byzantine.groupdashboard.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.byzantine.groupdashboard.GroupDashboardViewModel
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.widget.NCWarningDialog

class WalletConfigIntroFragment : MembershipFragment() {
    private val args: WalletConfigIntroFragmentArgs by navArgs()
    private val activityViewModel: GroupDashboardViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WalletConfigIntroContent(
                    onDoneClick = {
                        if (args.isClaimFlow) {
                            showConfirmDialog()
                        } else {
                            findNavController().popBackStack()
                        }
                    },
                    onClaimAnotherKeyClick = {
                        findNavController().popBackStack()
                    },
                    isClaimFlow = args.isClaimFlow
                )
            }
        }
    }

    private fun showConfirmDialog() {
        NCWarningDialog(requireActivity()).showDialog(
            title = getString(R.string.nc_confirmation),
            message = getString(R.string.nc_confirm_claim_done_msg),
            onYesClick = {
                activityViewModel.dismissCurrentAlert()
                findNavController().popBackStack(R.id.groupDashboardFragment, false)
            },
        )
    }
}

@Composable
private fun WalletConfigIntroContent(
    onDoneClick: () -> Unit = {},
    onClaimAnotherKeyClick: () -> Unit = {},
    isClaimFlow: Boolean = false
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .navigationBarsPadding()
                .statusBarsPadding(),
            bottomBar = {
                Column {
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        onClick = onDoneClick
                    ) {
                        Text(
                            text = stringResource(id = R.string.nc_text_done)
                        )
                    }
                    if (isClaimFlow) {
                        NcOutlineButton(
                            modifier = Modifier
                                .padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = 16.dp
                                )
                                .fillMaxWidth(),
                            onClick = onClaimAnotherKeyClick
                        ) {
                            Text(text = stringResource(R.string.nc_claim_another_key))
                        }
                    }
                }

            },
            topBar = {
                NcTopAppBar(title = "", isBack = false)
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                NcCircleImage(
                    modifier = Modifier
                        .size(96.dp)
                        .align(Alignment.CenterHorizontally),
                    iconSize = 60.dp,
                    iconTintColor = MaterialTheme.colorScheme.primary,
                    color = MaterialTheme.colorScheme.greyLight,
                    resId = R.drawable.ic_backup,
                )
                Text(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_accessing_the_wallet_configuration),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = stringResource(R.string.nc_access_wallet_configuration_desc),
                    style = NunchukTheme.typography.body
                )
            }
        }
    }
}

@Preview
@Composable
fun WalletConfigIntroContentPreview(
) {
    WalletConfigIntroContent()
}