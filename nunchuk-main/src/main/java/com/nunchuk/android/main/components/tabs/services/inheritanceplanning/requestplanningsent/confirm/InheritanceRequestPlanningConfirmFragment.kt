package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.requestplanningsent.confirm

import android.app.Activity
import android.content.Intent
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
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.util.showSuccess
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.membership.byzantine.groupchathistory.GroupChatHistoryFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class InheritanceRequestPlanningConfirmFragment : Fragment() {
    private val viewModel: InheritanceRequestPlanningConfirmViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                InheritanceRequestPlanningConfirmScreen(
                    onCancel = {
                        findNavController().popBackStack()
                    },
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when (event) {
                        is InheritanceRequestPlanningConfirmEvent.Loading -> {
                            showOrHideLoading(event.isLoading)
                        }

                        is InheritanceRequestPlanningConfirmEvent.Error -> {
                            showSuccess(event.message)
                        }

                        InheritanceRequestPlanningConfirmEvent.RequestInheritanceSuccess -> {
                            requireActivity().setResult(Activity.RESULT_OK, Intent().apply {
                                putExtra(InheritancePlanningActivity.RESULT_REQUEST_PLANNING, true)
                            })
                            findNavController().navigate(
                                InheritanceRequestPlanningConfirmFragmentDirections
                                    .actionInheritanceRequestPlanningConfirmFragmentToInheritanceRequestPlanningSentSuccessFragment()
                            )
                        }
                    }
                }
        }
    }

    companion object {
        const val REQUEST_KEY = "InheritanceRequestPlanningConfirmFragment"
        const val EXTRA_REQUEST_SUCCESS = "EXTRA_REQUEST_SUCCESS"
    }
}


@Composable
private fun InheritanceRequestPlanningConfirmScreen(
    viewModel: InheritanceRequestPlanningConfirmViewModel = viewModel(),
    onCancel: () -> Unit,
) {
    InheritanceRequestPlanningConfirmContent(
        onCancelRequest = onCancel,
        onContinue = viewModel::requestInheritancePlanning,
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

@Preview
@Composable
private fun InheritanceRequestPlanningConfirmScreenPreview() {
    InheritanceRequestPlanningConfirmContent()
}