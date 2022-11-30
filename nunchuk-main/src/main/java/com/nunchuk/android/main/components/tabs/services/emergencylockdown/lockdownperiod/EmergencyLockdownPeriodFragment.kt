package com.nunchuk.android.main.components.tabs.services.emergencylockdown.lockdownperiod

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.*
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.signer.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EmergencyLockdownPeriodFragment : Fragment() {
    private val viewModel: EmergencyLockdownPeriodViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                LockdownPeriodScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when(event) {
                is LockdownPeriodEvent.ContinueClick -> {
                    findNavController().navigate(EmergencyLockdownPeriodFragmentDirections.actionLockdownPeriodFragmentToLockdownConfirmFragment())
                }
                is LockdownPeriodEvent.Loading -> TODO()
            }
        }
    }
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
private fun LockdownPeriodScreen(
    viewModel: EmergencyLockdownPeriodViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LockdownPeriodContent(
        state.options,
        onContinueClicked = viewModel::onContinueClicked
    )
}

@Composable
private fun LockdownPeriodContent(
    options: List<PeriodOption> = emptyList(),
    onContinueClicked: () -> Unit = {}
) = NunchukTheme {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            NcTopAppBar(title = "")
            Text(
                modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                text = stringResource(R.string.nc_emergency_lockdown_period_title),
                style = NunchukTheme.typography.heading
            )
            Text(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                text = stringResource(R.string.nc_emergency_lockdown_period_desc),
                style = NunchukTheme.typography.body,
            )
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(options) { item ->
                    OptionItem(
                        modifier = Modifier.fillMaxWidth(),
                        isSelected = item.isSelected,
                        label = item.title
                    ) {

                    }
                }
            }
            Spacer(modifier = Modifier.weight(1.0f))
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
//                enabled = options.any { it.isSelected },
                onClick = onContinueClicked,
            ) {
                Text(text = stringResource(id = R.string.nc_text_continue))
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun OptionItem(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier, onClick = onClick,
        border = BorderStroke(
            width = 2.dp, color = Color(0xFFDEDEDE)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = isSelected, onClick = onClick)
            Text(text = label, style = NunchukTheme.typography.title)
        }
    }
}

@Preview
@Composable
private fun UploadBackUpTapSignerScreenPreview() {
    NunchukTheme {
        LockdownPeriodContent(
            options = listOf(
                PeriodOption(
                    "1 day",
                    false
                ),
                PeriodOption(
                    "3 days",
                    false
                ),
            )
        )
    }
}