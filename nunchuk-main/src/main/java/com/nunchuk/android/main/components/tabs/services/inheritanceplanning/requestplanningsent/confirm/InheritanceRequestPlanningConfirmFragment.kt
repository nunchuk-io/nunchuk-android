package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.requestplanningsent.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InheritanceRequestPlanningConfirmFragment : Fragment() {
    private val viewModel: InheritanceRequestPlanningConfirmViewModel by viewModels()
    private val walletId: String
        get() = arguments?.getString(ARG_WALLET_ID).orEmpty()
    private val groupId: String
        get() = arguments?.getString(ARG_GROUP_ID).orEmpty()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                InheritanceRequestPlanningConfirmScreen(
                    onCancel = {
                        requireActivity().finish()
                    },
                    onContinue = {
                        viewModel.requestInheritancePlanning(
                            walletId = walletId,
                            groupId = groupId
                        )
                    }
                )
            }
        }
    }

    companion object {
        const val REQUEST_KEY = "InheritanceRequestPlanningConfirmFragment"
        const val EXTRA_REQUEST_SUCCESS = "EXTRA_REQUEST_SUCCESS"
        private const val ARG_WALLET_ID = "wallet_id"
        private const val ARG_GROUP_ID = "group_id"
    }
}


@Composable
internal fun InheritanceRequestPlanningConfirmScreen(
    onCancel: () -> Unit,
    onContinue: () -> Unit,
) {
    InheritanceRequestPlanningConfirmContent(
        onCancelRequest = onCancel,
        onContinue = onContinue,
    )
}

@Composable
private fun InheritanceRequestPlanningConfirmContent(
    onCancelRequest: () -> Unit = {},
    onContinue: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding(),
            topBar = {
                NcTopAppBar(title = "")
            },
            bottomBar = {
                Column {
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                        onClick = { onContinue() },
                    ) {
                        Text(text = stringResource(id = R.string.nc_text_yes))
                    }

                    TextButton(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        onClick = onCancelRequest
                    ) {
                        Text(
                            text = stringResource(id = R.string.nc_cancel),
                            style = NunchukTheme.typography.title
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.nc_set_up_inheritance_plan),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(id = R.string.nc_inheritance_planning_request_confirm_desc),
                    style = NunchukTheme.typography.body
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun InheritanceRequestPlanningConfirmScreenPreview() {
    InheritanceRequestPlanningConfirmContent()
}
