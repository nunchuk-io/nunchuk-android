/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.core.wallet

import android.app.Activity
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.ActiveWallet
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.getWalletColors
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.compose.isLimitAccess
import com.nunchuk.android.compose.provider.WalletExtendedListProvider
import com.nunchuk.android.core.R
import com.nunchuk.android.core.base.BaseComposeBottomSheet
import com.nunchuk.android.core.util.SavedAddressFlow
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.SavedAddress
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.utils.parcelable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * Show wallets (assisted + free) and saved addresses in a bottom sheet.
 */
@AndroidEntryPoint
class WalletComposeBottomSheet : BaseComposeBottomSheet() {

    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel by viewModels<WalletsBottomSheetViewModel>()

    private val exclusiveWalletIds by lazy {
        requireArguments().getStringArrayList(
            EXTRA_EXCLUSIVE_WALLET_IDS
        ).orEmpty()
    }
    private val walletIds by lazy {
        requireArguments().getStringArrayList(EXTRA_WALLET_IDS).orEmpty()
    }
    private val exclusiveAddresses by lazy {
        requireArguments().getStringArrayList(
            EXTRA_EXCLUSIVE_ADDRESSES
        ).orEmpty()
    }
    private val configArgs by lazy { requireArguments().parcelable<ConfigArgs>(EXTRA_CONFIG) }

    private val addressLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                viewModel.getSavedAddresses()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                NunchukTheme {
                    AssistedWalletScreen(viewModel = viewModel,
                        title = configArgs?.title,
                        onWalletClick = { wallet ->
                            setFragmentResult(
                                TAG, Bundle().apply {
                                    putParcelable(
                                        RESULT,
                                        WalletBottomSheetResult(
                                            walletId = wallet.wallet.id,
                                            walletName = wallet.wallet.name
                                        )
                                    )
                                }
                            )
                            dismissAllowingStateLoss()
                        },
                        onAddAddressClick = { isCreate ->
                            navigator.openSavedAddressScreen(
                                launcher = addressLauncher,
                                activityContext = requireActivity(),
                                flow = if (isCreate) SavedAddressFlow.CREATE else SavedAddressFlow.LIST
                            )
                        },
                        onSelectAddressClick = {
                            setFragmentResult(
                                TAG, Bundle().apply {
                                    putParcelable(
                                        RESULT,
                                        WalletBottomSheetResult(savedAddress = it)
                                    )
                                }
                            )
                            dismissAllowingStateLoss()
                        })
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.init(configArgs, walletIds, exclusiveWalletIds, exclusiveAddresses)
    }

    companion object {
        const val TAG = "WalletComposeBottomSheet"
        private const val EXTRA_EXCLUSIVE_WALLET_IDS = "extra_exclusive_wallet_ids"
        private const val EXTRA_EXCLUSIVE_ADDRESSES = "extra_exclusive_addresses"
        private const val EXTRA_WALLET_IDS = "wallet_ids"
        private const val EXTRA_CONFIG = "extra_config"
        const val RESULT = "WALLET_BOTTOM_SHEET_RESULT"

        fun show(
            fragmentManager: FragmentManager,
            exclusiveAssistedWalletIds: List<String> = emptyList(),
            exclusiveAddresses: List<String> = emptyList(),
            assistedWalletIds: List<String> = emptyList(),
            configArgs: ConfigArgs,
        ) = WalletComposeBottomSheet().apply {
            arguments = Bundle().apply {
                putStringArrayList(
                    EXTRA_EXCLUSIVE_WALLET_IDS,
                    ArrayList(exclusiveAssistedWalletIds)
                )
                putStringArrayList(
                    EXTRA_EXCLUSIVE_ADDRESSES,
                    ArrayList(exclusiveAddresses)
                )
                putStringArrayList(EXTRA_WALLET_IDS, ArrayList(assistedWalletIds))
                putParcelable(EXTRA_CONFIG, configArgs)
            }
            show(fragmentManager, TAG)
        }

        val SHOW_ADDRESS = 1 shl 0 // 0001
        val SHOW_DEACTIVATED_WALLETS = 1 shl 1 // 0010

        fun fromFlags(vararg options: Int): Int {
            var combinedFlags = 0
            for (option in options) {
                combinedFlags = combinedFlags or option
            }
            return combinedFlags
        }
    }

    @Parcelize
    data class ConfigArgs(
        val title: String? = null,
        val flags: Int = 0
    ) : Parcelable {
        fun isShowAddress(): Boolean = flags and SHOW_ADDRESS != 0
        fun isShowDeactivatedWallets(): Boolean = flags and SHOW_DEACTIVATED_WALLETS != 0
    }
}

@Composable
private fun AssistedWalletScreen(
    viewModel: WalletsBottomSheetViewModel,
    title: String?,
    onWalletClick: (WalletExtended) -> Unit = {},
    onAddAddressClick: (Boolean) -> Unit = {},
    onSelectAddressClick: (SavedAddress) -> Unit = {},
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    AssistedWalletContent(
        uiState = uiState,
        title = title,
        onWalletClick = onWalletClick,
        onSelectAddressClick = onSelectAddressClick,
        onAddAddressClick = {
            onAddAddressClick(uiState.savedAddresses.isEmpty())
        }
    )
}

@Composable
private fun AssistedWalletContent(
    uiState: WalletsBottomSheetState,
    title: String? = null,
    onWalletClick: (WalletExtended) -> Unit = {},
    onAddAddressClick: () -> Unit = {},
    onSelectAddressClick: (SavedAddress) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            )
            .nestedScroll(rememberNestedScrollInteropConnection())
    ) {

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (uiState.config.isShowAddress()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = true),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NcIcon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(id = R.drawable.ic_saved_address),
                                contentDescription = ""
                            )
                            Text(
                                text = stringResource(id = R.string.nc_saved_addresses),
                                modifier = Modifier.padding(start = 8.dp),
                                style = NunchukTheme.typography.title
                            )
                        }
                        Text(
                            modifier = Modifier.clickable { onAddAddressClick() },
                            text = if (uiState.savedAddresses.isEmpty()) stringResource(id = R.string.nc_add) else stringResource(
                                id = R.string.nc_edit
                            ),
                            style = NunchukTheme.typography.title,
                            textDecoration = TextDecoration.Underline,
                        )
                    }
                }

                items(uiState.savedAddresses) { address ->
                    AddressItem(
                        address = address.label,
                        onClick = { onSelectAddressClick(address) }
                    )
                }
            }

            if (uiState.wallets.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.config.isShowAddress()) {
                            Row(
                                modifier = Modifier.weight(1f, fill = true),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NcIcon(
                                    modifier = Modifier.size(24.dp),
                                    painter = painterResource(id = R.drawable.ic_wallet_small),
                                    contentDescription = ""
                                )
                                Text(
                                    text = stringResource(id = R.string.nc_your_wallets),
                                    modifier = Modifier.padding(start = 8.dp),
                                    style = NunchukTheme.typography.title
                                )
                            }
                        } else {
                            Text(
                                text = uiState.config.title
                                    ?: stringResource(R.string.nc_select_a_wallet),
                                style = NunchukTheme.typography.title
                            )
                        }
                    }
                }
            }

            items(uiState.walletUiModels) { ui ->
                AssistedWallet(
                    walletsExtended = ui.wallet,
                    isAssistedWallet = ui.isAssistedWallet,
                    walletStatus = ui.walletStatus,
                    role = ui.role.name,
                    group = ui.group,
                    onWalletClick = {
                        onWalletClick(ui.wallet)
                    }
                )
            }
        }
    }
}

@Composable
fun AddressItem(
    address: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.greyLight,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            text = address, style = NunchukTheme.typography.body
        )
    }
}

@Composable
fun AssistedWallet(
    walletsExtended: WalletExtended,
    isAssistedWallet: Boolean,
    role: String,
    group: ByzantineGroup?,
    walletStatus: String,
    onWalletClick: () -> Unit = {}
) {
    val isLimitAccess = isLimitAccess(group, role, walletStatus)

    val colors = getWalletColors(
        wallet = walletsExtended.wallet,
        isAssistedWallet = isAssistedWallet,
        hasGroup = group != null,
        isLimitAccess = isLimitAccess,
        isFreeGroupWallet = false
    )
    Column(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(8.dp))
            .background(colorResource(id = R.color.nc_grey_light))
            .clickable(onClick = onWalletClick, enabled = true)
            .fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = colors, start = Offset.Zero, end = Offset.Infinite
                    )
                )
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            ActiveWallet(
                walletsExtended = walletsExtended,
                hideWalletDetail = false,
                isAssistedWallet = isAssistedWallet,
                role = role,
                useLargeFont = false,
                walletStatus = walletStatus
            )
        }
    }
}

@Preview
@Composable
fun AssistedWalletContentPreview(
    @PreviewParameter(WalletExtendedListProvider::class) wallets: List<WalletExtended>,
) {
    NunchukTheme {
        AssistedWalletContent(
            uiState = WalletsBottomSheetState(
                wallets = wallets, config = WalletComposeBottomSheet.ConfigArgs(
                    flags = WalletComposeBottomSheet.fromFlags(
                        WalletComposeBottomSheet.SHOW_ADDRESS
                    )
                )
            )
        )
    }
}