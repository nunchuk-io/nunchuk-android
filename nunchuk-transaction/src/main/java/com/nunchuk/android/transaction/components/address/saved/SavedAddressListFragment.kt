package com.nunchuk.android.transaction.components.address.saved

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.HighlightMessageType
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.SavedAddressFlow
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.model.SavedAddress
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.transaction.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SavedAddressListFragment : Fragment() {

    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: SavedAddressListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SavedAddressListScreen(viewModel,
                    onAddNewAddressClick = {
                        findNavController().navigate(
                            SavedAddressListFragmentDirections.actionSavedAddressListFragmentToAddOrEditAddressFragment(
                                address = null,
                                flow = SavedAddressFlow.CREATE
                            )
                        )
                    },
                    onAddressClick = { address ->
                        findNavController().navigate(
                            SavedAddressListFragmentDirections.actionSavedAddressListFragmentToAddOrEditAddressFragment(
                                address = address,
                                flow = SavedAddressFlow.EDIT
                            )
                        )
                    })
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is SavedAddressListEvent.Error -> {
                    showOrHideLoading(false)
                    showError(event.message)
                }

                is SavedAddressListEvent.Loading -> {
                    showOrHideLoading(event.loading)
                }
            }
        }
    }
}

@Composable
fun SavedAddressListScreen(
    viewModel: SavedAddressListViewModel = viewModel(),
    onAddNewAddressClick: () -> Unit = {},
    onAddressClick: (SavedAddress) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SavedAddressListScreenContent(
        addresses = state.addresses,
        onAddNewAddressClick = onAddNewAddressClick,
        onAddressClick = onAddressClick,
    )
}

@Composable
fun SavedAddressListScreenContent(
    addresses: List<SavedAddress> = arrayListOf(),
    onAddNewAddressClick: () -> Unit = {},
    onAddressClick: (SavedAddress) -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .navigationBarsPadding()
                .statusBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = "Saved addresses",
                    textStyle = NunchukTheme.typography.titleLarge,
                    actions = {
                        Spacer(modifier = Modifier.size(LocalViewConfiguration.current.minimumTouchTargetSize))
                    })
            },
            bottomBar = {
                Column {
                    NcHintMessage(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        messages = listOf(
                            ClickAbleText(
                                content = stringResource(id = R.string.nc_saved_address_warning_desc),
                            )
                        ),
                        type = HighlightMessageType.WARNING,
                    )

                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        onClick = onAddNewAddressClick,
                    ) {
                        Text(
                            text = stringResource(id = R.string.nc_add_a_new_address),
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxHeight()
            ) {

                addresses.forEach { address ->
                    AddressItem(
                        address = address.label,
                        onAddressClick = {
                            onAddressClick(address)
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun AddressItem(
    address: String,
    onAddressClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth()
            .clickable { onAddressClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .padding(end = 8.dp)
                .weight(1f, fill = true),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_saved_address),
                contentDescription = ""
            )
            Text(
                text = address,
                modifier = Modifier.padding(start = 8.dp),
                style = NunchukTheme.typography.body
            )
        }
        Icon(
            painter = painterResource(id = R.drawable.ic_arrow),
            contentDescription = ""
        )
    }
}

@Preview
@Composable
private fun SavedAddressListScreenContentPreview() {
    SavedAddressListScreenContent(
        addresses = listOf(
            SavedAddress(label = "nugenthomas@gmail.com", address = "aaaa"),
        )
    )
}