package com.nunchuk.android.main.membership.byzantine.select

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioOption
import com.nunchuk.android.compose.NcTag
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.border
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.model.GroupWalletType
import com.nunchuk.android.main.membership.model.desc
import com.nunchuk.android.main.membership.model.title
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.widget.NCInfoDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SelectGroupFragment : MembershipFragment() {
    private val viewMode: SelectGroupViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SelectGroupScreen(
                    onMoreClicked = ::handleShowMore,
                    onContinueClicked = { groupType ->
                        if (viewMode.checkGroupTypeAvailable(groupType)) {
                            findNavController().navigate(
                                SelectGroupFragmentDirections.actionSelectGroupFragmentToSelectWalletSetupFragment(
                                    groupType = groupType.name
                                )
                            )
                        } else {
                            NCInfoDialog(requireActivity()).init(
                                message = getString(
                                    R.string.nc_run_out_of_byzantine_wallet,
                                    if (groupType.isPro) getString(R.string.nc_pro)
                                    else getString(R.string.nc_standard)
                                ),
                                btnYes = getString(R.string.nc_take_me_there),
                                btnInfo = getString(R.string.nc_text_got_it),
                                onYesClick = {
                                    requireActivity().openExternalLink("https://nunchuk.io/my-plan")
                                }
                            ).show()
                        }
                    },
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewMode.event) {
            when (it) {
                is SelectGroupEvent.Loading -> showOrHideLoading(it.isLoading)
            }
        }
    }
}

@Composable
private fun SelectGroupScreen(
    onContinueClicked: (GroupWalletType) -> Unit = {},
    onMoreClicked: () -> Unit = {},
) {
    SelectGroupContent(onContinueClicked = onContinueClicked, onMoreClicked = onMoreClicked)
}

@Composable
private fun SelectGroupContent(
    onContinueClicked: (GroupWalletType) -> Unit = {},
    onMoreClicked: () -> Unit = {},
) {
    var selectedType by rememberSaveable { mutableStateOf(GroupWalletType.TWO_OF_FOUR_MULTISIG) }
    NunchukTheme {
        Scaffold(modifier = Modifier
            .navigationBarsPadding()
            .statusBarsPadding(),
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = {
                        onContinueClicked(selectedType)
                    }
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            }, topBar = {
                NcTopAppBar(title = "", elevation = 0.dp, actions = {
                    IconButton(onClick = onMoreClicked) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_more),
                            contentDescription = "More icon"
                        )
                    }
                })
            }) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.nc_main_select_group_wallet_type),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_main_what_type_of_group_wallet),
                    style = NunchukTheme.typography.body
                )
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(GroupWalletType.values()) {
                        GroupWalletTypeOptionView(type = it, isSelected = selectedType == it) {
                            selectedType = it
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupWalletTypeOptionView(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    type: GroupWalletType,
    onClick: () -> Unit = {}
) {
    NcRadioOption(modifier = modifier.fillMaxWidth(), isSelected = isSelected, onClick = onClick) {
        Row {
            if (type.isPro) {
                ProBadgePlan(modifier = Modifier.padding(end = 4.dp))
            } else {
                StandardBadgePlan(modifier = Modifier.padding(end = 4.dp))
            }
            if (type == GroupWalletType.TWO_OF_FOUR_MULTISIG || type == GroupWalletType.TWO_OF_THREE) {
                NcTag(
                    modifier = Modifier.padding(bottom = 4.dp),
                    label = stringResource(id = R.string.nc_recommended)
                )
            }
        }
        Text(text = stringResource(id = type.title), style = NunchukTheme.typography.title)
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = stringResource(id = type.desc),
            style = NunchukTheme.typography.body
        )
    }
}

@Composable
fun ProBadgePlan(modifier: Modifier) {
    Row(
        modifier = modifier
            .background(
                color = colorResource(id = R.color.nc_beeswax_dark),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_check_badge),
            tint = MaterialTheme.colors.surface,
            contentDescription = "Badge Icon"
        )
        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = stringResource(id = R.string.nc_pro),
            style = NunchukTheme.typography.bold.copy(
                color = MaterialTheme.colors.surface,
                fontSize = 10.sp
            )
        )
    }
}

@Composable
fun StandardBadgePlan(modifier: Modifier) {
    Row(
        modifier = modifier
            .border(
                color = MaterialTheme.colors.border,
                shape = RoundedCornerShape(20.dp),
                width = 1.dp,
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_check_badge),
            tint = MaterialTheme.colors.primary,
            contentDescription = "Badge Icon"
        )
        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = stringResource(id = R.string.nc_standard),
            style = NunchukTheme.typography.bold.copy(fontSize = 10.sp)
        )
    }
}

@Preview
@Composable
private fun SelectGroupScreenPreview() {
    SelectGroupContent(

    )
}