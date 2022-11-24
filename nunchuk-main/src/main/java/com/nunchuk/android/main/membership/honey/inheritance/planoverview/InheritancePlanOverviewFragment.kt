package com.nunchuk.android.main.membership.honey.inheritance.planoverview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nunchuk.android.compose.NCLabelWithIndex
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InheritancePlanOverviewFragment : Fragment() {
    private val viewModel: InheritancePlanOverviewViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                InheritancePlanOverviewScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when(event) {
                        InheritancePlanOverviewEvent.OnContinueClicked -> TODO()
                    }
                }
        }
    }
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
private fun InheritancePlanOverviewScreen(viewModel: InheritancePlanOverviewViewModel = viewModel()) {
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    InheritancePlanOverviewContent(remainTime, viewModel::onContinueClicked)
}

@Composable
private fun InheritancePlanOverviewContent(
    remainTime: Int = 0,
    onContinueClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                Modifier
                    .padding(innerPadding)
                    .navigationBarsPadding()
            ) {
                NcTopAppBar(
                    title = stringResource(
                        id = R.string.nc_estimate_remain_time,
                        remainTime
                    ),
                )
                Text(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_plan_overview),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_plan_overview_desc),
                    style = NunchukTheme.typography.body
                )
                NCLabelWithIndex(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    index = 1,
                    label = stringResource(R.string.nc_magical_phrase),
                )
                NCLabelWithIndex(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    index = 2,
                    label = stringResource(R.string.nc_backup_password),
                )
                NCLabelWithIndex(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    index = 3,
                    label = stringResource(R.string.nc_activation_date),
                )
                Text(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_plan_overview_bottom_desc),
                    style = NunchukTheme.typography.body
                )
                Spacer(modifier = Modifier.weight(1.0f))
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(id = com.nunchuk.android.signer.R.string.nc_text_continue))
                }
            }
        }
    }
}

@Preview
@Composable
private fun InheritancePlanOverviewScreenPreview() {
    InheritancePlanOverviewContent()
}