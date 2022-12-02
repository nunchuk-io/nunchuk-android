package com.nunchuk.android.main.membership.honey.inheritance.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import com.nunchuk.android.share.membership.MembershipFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InheritanceSetupIntroFragment : MembershipFragment() {
    private val viewModel: InheritanceSetupIntroViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                InheritanceSetupIntroScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when (event) {
                        InheritanceSetupIntroEvent.OnContinueClicked -> findNavController().navigate(
                            InheritanceSetupIntroFragmentDirections.actionInheritanceSetupIntroFragmentToInheritancePlanOverviewFragment()
                        )
                    }
                }
        }
    }
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
private fun InheritanceSetupIntroScreen(viewModel: InheritanceSetupIntroViewModel = viewModel()) {
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    InheritanceSetupIntroContent(remainTime, viewModel::onContinueClicked)
}

@Composable
private fun InheritanceSetupIntroContent(
    remainTime: Int = 0,
    onContinueClicked: () -> Unit = {}
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
            ) {
                NcImageAppBar(
                    backgroundRes = R.drawable.bg_inheritance,
                    title = stringResource(
                        id = R.string.nc_estimate_remain_time,
                        remainTime
                    ),
                )
                Text(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_setup_inheritance_plan_intro_title),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_setup_inheritance_plan_intro_desc),
                    style = NunchukTheme.typography.body
                )
                Box(
                    modifier = Modifier
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                        .background(
                            color = colorResource(
                                id = R.color.nc_grey_light
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    NcHighlightText(
                        modifier = Modifier.padding(12.dp),
                        style = NunchukTheme.typography.body,
                        text = stringResource(id = R.string.nc_set_up_inheritance_hint)
                    )
                }
                Text(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_inheritance_claim_trustee),
                    style = NunchukTheme.typography.body
                )
                Spacer(modifier = Modifier.weight(1.0f))
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            }
        }
    }
}

@Preview
@Composable
private fun InheritanceSetupIntroScreenPreview() {
    InheritanceSetupIntroContent()
}