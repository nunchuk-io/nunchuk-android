package com.nunchuk.android.main.membership.byzantine.groupchathistory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.RadioButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.byzantine.groupdashboard.GroupDashboardEvent
import com.nunchuk.android.main.membership.byzantine.selectrole.ByzantineSelectRoleFragment
import com.nunchuk.android.main.membership.byzantine.selectrole.ByzantineSelectRoleFragment.Companion.EXTRA_ROLE
import com.nunchuk.android.model.HistoryPeriod
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GroupChatHistoryFragment : Fragment() {

    private val viewModel: GroupChatHistoryViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                GroupChatHistoryScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is GroupChatHistoryEvent.Error -> showError(message = event.message)
                is GroupChatHistoryEvent.Loading -> showOrHideLoading(loading = event.loading)
                is GroupChatHistoryEvent.UpdateGroupChatSuccess -> {
                    setFragmentResult(
                        REQUEST_KEY,
                        bundleOf(EXTRA_HISTORY_PERIOD to event.historyPeriod)
                    )
                    findNavController().popBackStack()
                }
            }
        }
    }

    companion object {
        const val REQUEST_KEY = "GroupChatHistoryFragment"
        const val EXTRA_HISTORY_PERIOD = "EXTRA_HISTORY_PERIOD"
    }
}

@Composable
fun GroupChatHistoryScreen(
    viewModel: GroupChatHistoryViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GroupChatHistoryScreenContent(
        periods = state.historyPeriods,
        selectPeriodId = state.selectedHistoryPeriodId,
        onSaveClick = viewModel::saveHistoryPeriod,
        onCheckedChange = viewModel::setHistoryPeriod,
    )
}

@Composable
fun GroupChatHistoryScreenContent(
    periods: List<HistoryPeriod> = emptyList(),
    selectPeriodId: String = "",
    onCheckedChange: (String) -> Unit = {},
    onSaveClick: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .navigationBarsPadding()
                .statusBarsPadding(),
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = selectPeriodId.isNotEmpty(),
                    onClick = onSaveClick
                ) {
                    Text(text = stringResource(id = R.string.nc_text_save))
                }
            },
            topBar = {
                NcTopAppBar(
                    title = stringResource(id = R.string.nc_manage_group_chat_history),
                    isBack = false,
                    textStyle = NunchukTheme.typography.titleLarge,
                    actions = {
                        Spacer(modifier = Modifier.size(LocalViewConfiguration.current.minimumTouchTargetSize))
                    })
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxHeight()
            ) {
                Text(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp),
                    text = stringResource(id = R.string.nc_keep_group_chat_history),
                    style = NunchukTheme.typography.title
                )
                LazyColumn(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxHeight(),
                ) {
                    items(periods) { period ->
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = period.displayName,
                                style = NunchukTheme.typography.body,
                                modifier = Modifier
                                    .weight(1f, true)
                                    .padding(end = 12.dp)
                            )

                            RadioButton(selected = selectPeriodId == period.id, onClick = {
                                onCheckedChange(period.id)
                            })
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun GroupChatHistoryScreenContentPreview() {
    val periods = listOf(
        HistoryPeriod(
            id = "1",
            displayName = "30 days",
            enabled = true,
            interval = "30 days",
            intervalCount = 1
        )
    )
    GroupChatHistoryScreenContent(periods = periods)
}