package com.nunchuk.android.main.membership.byzantine.select

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.findNavController
import com.nunchuk.android.compose.*
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.model.GroupWalletType
import com.nunchuk.android.main.membership.model.desc
import com.nunchuk.android.main.membership.model.title
import com.nunchuk.android.share.membership.MembershipFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SelectGroupFragment : MembershipFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SelectGroupScreen(
                    onMoreClicked = ::handleShowMore,
                    onContinueClicked = {
                        findNavController().navigate(
                            SelectGroupFragmentDirections.actionSelectGroupFragmentToSelectWalletSetupFragment(groupType = it)
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun SelectGroupScreen(
    onContinueClicked: (String) -> Unit = {},
    onMoreClicked: () -> Unit = {},
) {
    SelectGroupContent(onContinueClicked = onContinueClicked, onMoreClicked = onMoreClicked)
}

@Composable
private fun SelectGroupContent(
    onContinueClicked: (String) -> Unit = {},
    onMoreClicked: () -> Unit = {},
) {
    var selectedType by remember { mutableStateOf(GroupWalletType.TWO_OF_FOUR_MULTISIG) }
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
                        onContinueClicked(selectedType.name)
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
        if (type == GroupWalletType.TWO_OF_FOUR_MULTISIG || type == GroupWalletType.TWO_OF_THREE) {
            NcTag(
                modifier = Modifier.padding(bottom = 4.dp),
                label = stringResource(id = R.string.nc_recommended)
            )
        }
        Text(text = stringResource(id = type.title), style = NunchukTheme.typography.title)
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = stringResource(id = type.desc),
            style = NunchukTheme.typography.body
        )
    }
}

@Preview
@Composable
private fun SelectGroupScreenPreview() {
    SelectGroupContent(

    )
}