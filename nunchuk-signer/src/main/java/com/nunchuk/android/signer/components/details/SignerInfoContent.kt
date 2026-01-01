package com.nunchuk.android.signer.components.details

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunchuk.android.compose.HighlightMessageType
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcInputDialog
import com.nunchuk.android.compose.dialog.NcInputType
import com.nunchuk.android.compose.greyDark
import com.nunchuk.android.compose.latoBold
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.util.formatMMMddyyyyDate
import com.nunchuk.android.core.util.toReadableDrawableResId
import com.nunchuk.android.core.util.toReadableString
import com.nunchuk.android.model.HealthCheckHistory
import com.nunchuk.android.model.KeyHealthType
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.signer.R
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.utils.healthCheckLabel
import com.nunchuk.android.utils.healthCheckTimeColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import timber.log.Timber
import java.util.Locale
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

@Composable
fun SignerInfoContent(
    uiState: SignerInfoState = SignerInfoState(),
    isPrimaryKey: Boolean = false,
    onBackClicked: () -> Unit = {},
    onMoreClicked: () -> Unit = {},
    onEditClicked: () -> Unit = {},
    onHealthCheckClicked: () -> Unit = {},
    onBackupKeyClicked: () -> Unit = {},
    onHistoryItemClick: (HealthCheckHistory) -> Unit = {},
    onViewSeedPhraseClicked: (String?) -> Unit = {},
    onPassphraseSubmitted: (String) -> Unit = {},
    onPassphraseConsume: () -> Unit = {},
) {
    val context = LocalContext.current
    val label by remember(uiState.lastHealthCheckTimeMillis) {
        derivedStateOf {
            uiState.lastHealthCheckTimeMillis.healthCheckLabel(context)
        }
    }
    val color = uiState.lastHealthCheckTimeMillis.healthCheckTimeColor()
    val isMyKey = uiState.masterSigner?.isVisible ?: uiState.remoteSigner?.isVisible ?: false
    val isHotKey = uiState.masterSigner?.isNeedBackup == true
    val isSoftwareSigner = uiState.masterSigner?.type == SignerType.SOFTWARE
    val needPassphrase = uiState.masterSigner?.device?.needPassPhraseSent == true
    var showSecurityTimeoutDialog by remember { mutableStateOf(false) }
    var showPassphraseDialog by remember { mutableStateOf(false) }
    var remainingTimeMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(uiState.passphrase) {
        if (!uiState.passphrase.isNullOrEmpty()) {
            if (uiState.seedPhraseViewTimestamp == null) {
                showSecurityTimeoutDialog = true
            }
            onViewSeedPhraseClicked(uiState.passphrase)
            onPassphraseConsume()
        }
    }

    LaunchedEffect(uiState.seedPhraseViewTimestamp) {
        val timestamp = uiState.seedPhraseViewTimestamp
        while (isActive) {
            if (timestamp == null) {
                remainingTimeMs = 0L
            } else {
                val currentTime = System.currentTimeMillis()
                val elapsedTime = currentTime - timestamp
                val remaining = timeoutDurationMs - elapsedTime
                remainingTimeMs = if (remaining > 0) remaining else 0L
            }
            delay(30.seconds)
        }
    }

    NunchukTheme {
        Scaffold(
            modifier = Modifier.Companion.navigationBarsPadding(),
            topBar = {
                Column(
                    modifier = Modifier.Companion
                        .background(
                            brush = Brush.Companion.linearGradient(
                                colors = listOf(
                                    colorResource(id = R.color.nc_primary_light_color),
                                    colorResource(id = R.color.nc_primary_dark_color)
                                ),
                                start = Offset(0f, 1000f),
                                end = Offset(0f, 0f)
                            ),
                            shape = RectangleShape
                        ),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Companion.CenterHorizontally
                ) {
                    NcTopAppBar(
                        textStyle = NunchukTheme.typography.titleLarge.copy(color = Color.Companion.White),
                        backgroundColor = Color.Companion.Transparent,
                        title = stringResource(id = R.string.nc_text_signer_info),
                        tintColor = Color.Companion.White,
                        onBackPress = onBackClicked,
                        actions = {
                            IconButton(onClick = onMoreClicked) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_more_horizontal),
                                    contentDescription = "More",
                                    tint = Color.Companion.White
                                )
                            }
                        }
                    )

                    if (isHotKey) {
                        NcHintMessage(
                            modifier = Modifier.Companion
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .clickable {
                                    onBackupKeyClicked()
                                },
                            type = HighlightMessageType.WARNING,
                            content = {
                                val annotatedText = buildAnnotatedString {
                                    append(stringResource(R.string.nc_please_back_up_your_key))
                                    append(" ")
                                    pushStringAnnotation(
                                        tag = "DO_IT_NOW",
                                        annotation = "do_it_now"
                                    )
                                    withStyle(
                                        style = SpanStyle(
                                            textDecoration = TextDecoration.Companion.Underline
                                        )
                                    ) {
                                        append(stringResource(R.string.nc_do_it_now))
                                    }
                                    pop()
                                }
                                Text(
                                    text = annotatedText,
                                    style = NunchukTheme.typography.titleSmall.copy(
                                        color = colorResource(
                                            R.color.nc_primary_dark_color
                                        )
                                    )
                                )
                            }
                        )
                    }

                    val resId = if (uiState.masterSigner != null) {
                        uiState.masterSigner.toReadableDrawableResId(isPrimaryKey = isPrimaryKey)
                    } else uiState.remoteSigner?.toReadableDrawableResId()
                    resId?.let {
                        NcCircleImage(
                            modifier = Modifier.Companion.padding(top = 12.dp),
                            resId = resId,
                            size = 96.dp,
                            iconSize = 60.dp,
                            iconTintColor = colorResource(id = R.color.nc_grey_g7),
                            color = if (uiState.assistedWalletIds.isEmpty()) Color.Companion.White else color
                        )
                    }

                    Row(
                        modifier = Modifier.Companion
                            .clickable { onEditClicked() }
                            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                        verticalAlignment = Alignment.Companion.CenterVertically,
                    ) {
                        Text(
                            text = uiState.signerName,
                            style = NunchukTheme.typography.heading.copy(color = Color.Companion.White)
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit),
                            contentDescription = "",
                            tint = Color.Companion.White,
                            modifier = Modifier.Companion
                                .padding(start = 8.dp)
                                .size(18.dp)
                        )
                    }

                    Row(modifier = Modifier.Companion.padding(top = 8.dp)) {
                        if (isPrimaryKey) {
                            Text(
                                modifier = Modifier.Companion
                                    .background(
                                        color = colorResource(id = R.color.nc_fill_beewax),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 8.dp),
                                text = stringResource(id = R.string.nc_signer_type_primary_key),
                                style = TextStyle(
                                    color = MaterialTheme.colorScheme.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Companion.W500,
                                    fontFamily = latoBold
                                )
                            )
                        }
                        val signerType =
                            uiState.masterSigner?.type?.toReadableString(
                                LocalContext.current,
                                false
                            )
                                ?: uiState.remoteSigner?.type?.toReadableString(
                                    LocalContext.current,
                                    isPrimaryKey
                                )
                                ?: ""
                        Text(
                            modifier = Modifier.Companion
                                .padding(start = 8.dp)
                                .background(
                                    color = colorResource(id = R.color.nc_grey_g2),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            text = signerType,
                            style = TextStyle(
                                color = colorResource(R.color.nc_grey_g7),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Companion.W500,
                                fontFamily = latoBold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.Companion.height(24.dp))
                }
            },
            bottomBar = {
                Column(
                    modifier = Modifier.Companion.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (isMyKey) {
                        NcPrimaryDarkButton(
                            modifier = Modifier.Companion
                                .fillMaxWidth(),
                            onClick = onHealthCheckClicked
                        ) {
                            Text(
                                text = if (isHotKey) stringResource(id = R.string.nc_txt_health_check) else stringResource(
                                    id = R.string.nc_txt_run_health_check
                                )
                            )
                        }
                    }
                    if (isSoftwareSigner) {
                        val viewText = if (uiState.hasXprv) {
                            stringResource(id = R.string.nc_view_xprv)
                        } else {
                            stringResource(id = R.string.nc_view_seed_phrase)
                        }

                        val buttonText = if (remainingTimeMs > 0) {
                            val totalMinutes = (remainingTimeMs / (60 * 1000)).toInt()
                            val hours = totalMinutes / 60
                            val minutes = totalMinutes % 60
                            val timeString =
                                String.Companion.format(
                                    Locale.getDefault(),
                                    "%02d:%02d",
                                    hours,
                                    minutes
                                )
                            "$viewText in $timeString"
                        } else {
                            viewText
                        }

                        if (uiState.seedPhraseViewTimestamp != null && remainingTimeMs <= 0) {
                            NcOutlineButton(
                                modifier = Modifier.Companion.fillMaxWidth(),
                                onClick = {
                                    if (needPassphrase) {
                                        showPassphraseDialog = true
                                    } else {
                                        onViewSeedPhraseClicked(null)
                                    }
                                }
                            ) {
                                Text(text = buttonText)
                            }
                        } else {
                            val isEnabled = remainingTimeMs <= 0
                            TextButton(
                                modifier = Modifier.Companion.fillMaxWidth(),
                                enabled = isEnabled,
                                onClick = {
                                    if (needPassphrase) {
                                        showPassphraseDialog = true
                                    } else {
                                        onViewSeedPhraseClicked(null)
                                        showSecurityTimeoutDialog = true
                                    }
                                }
                            ) {
                                Text(
                                    text = buttonText,
                                    style = NunchukTheme.typography.title.copy(
                                        color = if (isEnabled) {
                                            MaterialTheme.colorScheme.textPrimary
                                        } else {
                                            MaterialTheme.colorScheme.textSecondary
                                        }
                                    )
                                )
                            }
                        }
                    }
                }
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.Companion
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                if (isMyKey) {
                    uiState.nfcCardId?.let {
                        item {
                            Spacer(modifier = Modifier.Companion.height(16.dp))
                            Text(
                                text = stringResource(R.string.nc_card_id),
                                style = NunchukTheme.typography.titleSmall
                            )
                            Text(
                                modifier = Modifier.Companion.padding(top = 4.dp),
                                text = uiState.nfcCardId,
                                style = NunchukTheme.typography.body
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.Companion.height(24.dp))
                        Text(
                            text = stringResource(R.string.nc_text_signer_spec),
                            style = NunchukTheme.typography.titleSmall
                        )
                        val keySpec = if (uiState.masterSigner != null) {
                            uiState.masterSigner.device.masterFingerprint
                        } else if (uiState.remoteSigner != null) {
                            uiState.remoteSigner.descriptor
                        } else {
                            ""
                        }
                        Text(
                            modifier = Modifier.Companion.padding(top = 4.dp),
                            text = keySpec,
                            style = NunchukTheme.typography.body
                        )
                    }
                }
                if (uiState.assistedWalletIds.isNotEmpty()) {
                    if (isMyKey) {
                        item {
                            Spacer(modifier = Modifier.Companion.height(24.dp))
                            HorizontalDivider()
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.Companion.height(24.dp))
                        Column {
                            Row(
                                modifier = Modifier.Companion
                                    .background(
                                        color = color,
                                        shape = RoundedCornerShape(size = 20.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(
                                    4.dp,
                                    Alignment.Companion.Start
                                ),
                                verticalAlignment = Alignment.Companion.CenterVertically,
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_health_check_dark),
                                    contentDescription = "",
                                    tint = colorResource(R.color.nc_grey_g7)
                                )
                                Text(
                                    text = label,
                                    style = NunchukTheme.typography.bodySmall.copy(
                                        color = colorResource(
                                            R.color.nc_grey_g7
                                        )
                                    )
                                )
                            }

                            Text(
                                modifier = Modifier.Companion.padding(top = 16.dp),
                                text = stringResource(R.string.nc_health_check_history),
                                style = NunchukTheme.typography.title
                            )
                            if (uiState.healthCheckHistories.isNullOrEmpty()) {
                                Text(
                                    modifier = Modifier.Companion.padding(top = 16.dp),
                                    text = stringResource(R.string.nc_no_history),
                                    style = NunchukTheme.typography.body
                                )
                            }
                        }

                    }

                    items(uiState.healthCheckHistories.orEmpty()) {
                        HealthCheckHistoryItem(it, onHistoryItemClick = { onHistoryItemClick(it) })
                    }
                }
            }
        }

        if (showPassphraseDialog) {
            NcInputDialog(
                title = stringResource(id = R.string.nc_transaction_enter_passphrase),
                inputType = NcInputType.PASSWORD,
                isMaskedInput = true,
                onConfirmed = { passphrase ->
                    showPassphraseDialog = false
                    onPassphraseSubmitted(passphrase)
                },
                onCanceled = {
                    showPassphraseDialog = false
                },
                onDismiss = {
                    showPassphraseDialog = false
                }
            )
        }

        if (showSecurityTimeoutDialog) {
            SecurityTimeoutDialog(
                remainingTimeMs = remainingTimeMs,
                isXprv = uiState.hasXprv,
                onDismiss = {
                    showSecurityTimeoutDialog = false
                },
                onConfirm = {
                    showSecurityTimeoutDialog = false
                },
            )
        }
    }
}


val timeoutDurationMs = 2.hours.inWholeMilliseconds

@Composable
fun HealthCheckHistoryItem(history: HealthCheckHistory, onHistoryItemClick: () -> Unit = {}) {
    Column {
        Row(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .clickable { onHistoryItemClick() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = history.createdTimeMillis.formatMMMddyyyyDate,
                    style = NunchukTheme.typography.body,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val status = when (history.type) {
                    KeyHealthType.HEALTH_CHECK.name, KeyHealthType.DUMMY_TRANSACTION.name -> {
                        "Health check succeeded"
                    }

                    KeyHealthType.TRANSACTION.name -> {
                        "Transaction signed"
                    }

                    else -> {
                        ""
                    }
                }
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = status,
                    style = NunchukTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.greyDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (history.type == KeyHealthType.TRANSACTION.name) {
                Image(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(id = R.drawable.ic_right_arrow_dark),
                    contentDescription = "Arrow"
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun SignerInfoScreenPreview() {
    SignerInfoContent(
        uiState = SignerInfoState(
            signerName = "Key",
            masterSigner = MasterSigner(
                type = SignerType.NFC
            ),
        )
    )
}

@PreviewLightDark
@Composable
private fun SignerInfoAssistedScreenPreview() {
    SignerInfoContent(
        uiState = SignerInfoState(
            signerName = "Key",
            masterSigner = MasterSigner(
                type = SignerType.SOFTWARE
            ),
            assistedWalletIds = listOf("abc")
        )
    )
}