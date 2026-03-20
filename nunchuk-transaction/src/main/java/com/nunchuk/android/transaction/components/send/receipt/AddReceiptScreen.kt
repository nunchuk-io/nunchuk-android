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

package com.nunchuk.android.transaction.components.send.receipt

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.fillInputText
import com.nunchuk.android.compose.lightGray
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.core.util.MAX_NOTE_LENGTH
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.send.batchtransaction.SelectAddressType
import com.nunchuk.android.utils.MaxLengthTransformation

@Composable
internal fun AddReceiptScreen(
    state: AddReceiptState,
    sweepType: SweepType,
    isWithdrawFlow: Boolean,
    event: AddReceiptEvent?,
    selectAddressType: Int = SelectAddressType.NONE.ordinal,
    selectAddressName: String = "",
    onAddressChange: (String) -> Unit = {},
    onPrivateNoteChange: (String) -> Unit = {},
    onCreateTransactionClick: () -> Unit = {},
    onCustomizeTransactionClick: () -> Unit = {},
    onScanClick: () -> Unit = {},
    onDropdownClick: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onEventHandled: () -> Unit = {},
    onParseBtcUri: (String) -> Unit = {},
) {
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val invalidAddressMessage = stringResource(R.string.nc_transaction_invalid_address)
    val requiredMessage = stringResource(R.string.nc_text_required)

    LaunchedEffect(event) {
        when (event) {
            AddReceiptEvent.AddressRequiredEvent -> {
                errorMessage = requiredMessage
                onEventHandled()
            }

            AddReceiptEvent.InvalidAddressEvent -> {
                errorMessage = invalidAddressMessage
                onEventHandled()
            }

            is AddReceiptEvent.ParseBtcUriEvent -> {
                onEventHandled()
            }

            is AddReceiptEvent.AcceptedAddressEvent,
            is AddReceiptEvent.Loading,
            is AddReceiptEvent.ShowError,
            AddReceiptEvent.NoOp,
            null -> Unit
        }
    }

    AddReceiptContent(
        address = state.address,
        privateNote = state.privateNote,
        sweepType = sweepType,
        isWithdrawFlow = isWithdrawFlow,
        selectAddressType = selectAddressType,
        selectAddressName = selectAddressName,
        errorMessage = errorMessage,
        onAddressChange = { address ->
            errorMessage = null
            onAddressChange(address)
        },
        onPrivateNoteChange = onPrivateNoteChange,
        onCreateTransactionClick = onCreateTransactionClick,
        onCustomizeTransactionClick = onCustomizeTransactionClick,
        onScanClick = onScanClick,
        onDropdownClick = {
            if (selectAddressType != SelectAddressType.NONE.ordinal) {
                onClearSelection()
            } else {
                onDropdownClick()
            }
        },
        onBackClick = onBackClick,
        onParseBtcUri = onParseBtcUri,
    )
}

@Composable
private fun AddReceiptContent(
    address: String = "",
    privateNote: String = "",
    sweepType: SweepType = SweepType.NONE,
    isWithdrawFlow: Boolean = false,
    selectAddressType: Int = SelectAddressType.NONE.ordinal,
    selectAddressName: String = "",
    errorMessage: String? = null,
    onAddressChange: (String) -> Unit = {},
    onPrivateNoteChange: (String) -> Unit = {},
    onCreateTransactionClick: () -> Unit = {},
    onCustomizeTransactionClick: () -> Unit = {},
    onScanClick: () -> Unit = {},
    onDropdownClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onParseBtcUri: (String) -> Unit = {},
) {
    val title = when (sweepType) {
        SweepType.NONE -> stringResource(R.string.nc_transaction_new)
        SweepType.SWEEP_TO_NUNCHUK_WALLET,
        SweepType.UNSEAL_SWEEP_TO_NUNCHUK_WALLET -> stringResource(R.string.nc_withdraw_to_a_wallet)

        SweepType.SWEEP_TO_EXTERNAL_ADDRESS,
        SweepType.UNSEAL_SWEEP_TO_EXTERNAL_ADDRESS -> stringResource(R.string.nc_withdraw_to_an_address)
    }

    val addressLabel = if (sweepType != SweepType.NONE) {
        stringResource(R.string.nc_enter_recipient_address)
    } else {
        stringResource(R.string.nc_transaction_enter_receipt)
    }

    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .systemBarsPadding()
                .imePadding(),
            topBar = {
                NcTopAppBar(
                    title = title,
                    textStyle = NunchukTheme.typography.titleLarge,
                    onBackPress = onBackClick,
                    actions = {
                        if (!isWithdrawFlow) {
                            NcIcon(
                                modifier = Modifier
                                    .size(LocalViewConfiguration.current.minimumTouchTargetSize)
                                    .clickable { onScanClick() }
                                    .padding(12.dp),
                                painter = painterResource(id = R.drawable.ic_qrcode_dark),
                                contentDescription = stringResource(R.string.nc_scan_qr)
                            )
                        } else {
                            Spacer(modifier = Modifier.size(LocalViewConfiguration.current.minimumTouchTargetSize))
                        }
                    }
                )
            },
            bottomBar = {
                Column {
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        onClick = onCreateTransactionClick,
                    ) {
                        Text(text = stringResource(id = R.string.nc_create_transaction))
                    }
                    NcOutlineButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp)
                            .height(48.dp),
                        onClick = onCustomizeTransactionClick,
                    ) {
                        Text(text = stringResource(R.string.nc_customize_transaction))
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                if (selectAddressType == SelectAddressType.NONE.ordinal) {
                    NcTextField(
                        title = addressLabel,
                        value = address,
                        inputBoxHeight = 70.dp,
                        error = errorMessage,
                        onValueChange = { value ->
                            onAddressChange(value)
                            if (value.startsWith("bitcoin:", ignoreCase = true)) {
                                onParseBtcUri(value)
                            }
                        },
                        rightContent = {
                            if (isWithdrawFlow) {
                                NcIcon(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable { onScanClick() },
                                    painter = painterResource(id = R.drawable.ic_qrcode_dark),
                                    contentDescription = ""
                                )
                            } else {
                                NcIcon(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable { onDropdownClick() },
                                    painter = painterResource(id = R.drawable.ic_arrow_drop_down),
                                    contentDescription = ""
                                )
                            }
                        }
                    )
                } else {
                    Text(
                        text = addressLabel,
                        style = NunchukTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .height(70.dp)
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.strokePrimary,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .background(
                                color = MaterialTheme.colorScheme.fillInputText,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NcIcon(
                            modifier = Modifier.size(24.dp),
                            painter = if (selectAddressType == SelectAddressType.WALLET.ordinal) {
                                painterResource(id = R.drawable.ic_wallet_small)
                            } else {
                                painterResource(id = R.drawable.ic_saved_address)
                            },
                            contentDescription = ""
                        )

                        Text(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .weight(1f),
                            text = selectAddressName,
                            style = NunchukTheme.typography.body
                        )

                        NcIcon(
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { onDropdownClick() },
                            painter = painterResource(id = R.drawable.ic_close_circle),
                            contentDescription = ""
                        )
                    }
                }

                if (address.isNotBlank() && selectAddressType == SelectAddressType.NONE.ordinal) {
                    InspectAddressView(address = address)
                }

                if (!isWithdrawFlow) {
                    Spacer(modifier = Modifier.height(16.dp))
                    NcTextField(
                        title = stringResource(id = R.string.nc_transaction_note),
                        value = privateNote,
                        maxLength = MAX_NOTE_LENGTH,
                        enableMaxLength = true,
                        visualTransformation = MaxLengthTransformation(maxLength = MAX_NOTE_LENGTH),
                        onValueChange = {
                            if (it.length <= MAX_NOTE_LENGTH) {
                                onPrivateNoteChange(it)
                            }
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun InspectAddressView(
    address: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Unspecified,
) {
    val context = LocalContext.current
    val andaleMono = remember {
        FontFamily(
            typeface = Typeface.createFromAsset(context.assets, "AndaleMono-Regular.ttf")
        )
    }
    val chunks = remember(address) { address.chunked(4) }
    val primaryColor = MaterialTheme.colorScheme.textPrimary
    val secondaryColor = MaterialTheme.colorScheme.textSecondary
    val resolvedBackgroundColor = if (backgroundColor == Color.Unspecified) {
        MaterialTheme.colorScheme.lightGray
    } else {
        backgroundColor
    }

    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(
                color = resolvedBackgroundColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        maxItemsInEachRow = 6,
    ) {
        chunks.forEachIndexed { index, chunk ->
            Text(
                text = chunk,
                style = TextStyle(
                    fontFamily = andaleMono,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (index % 2 == 0) primaryColor else secondaryColor,
                ),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun AddReceiptContentPreview() {
    AddReceiptContent(
        address = "bc1qqvqhe05av90992q8xuephny96jp7md26ktpd0l",
    )
}

@PreviewLightDark
@Composable
private fun AddReceiptContentSelectedPreview() {
    AddReceiptContent(
        selectAddressType = SelectAddressType.WALLET.ordinal,
        selectAddressName = "My Wallet",
    )
}
