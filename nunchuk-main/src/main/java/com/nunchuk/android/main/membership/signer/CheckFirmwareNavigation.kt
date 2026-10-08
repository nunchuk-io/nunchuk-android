package com.nunchuk.android.main.membership.signer

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.signer.OnChainAddSignerParam
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.main.R
import com.nunchuk.android.core.signer.SelectSignerArgs
import com.nunchuk.android.core.signer.SelectSignerBottomSheet
import com.nunchuk.android.main.membership.onchaintimelock.checkfirmware.CheckFirmwareEvent
import com.nunchuk.android.main.membership.onchaintimelock.checkfirmware.CheckFirmwareViewModel
import com.nunchuk.android.nav.args.CheckFirmwareArgs
import com.nunchuk.android.signer.SignerFirmwareDevice
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import kotlinx.serialization.Serializable

@Serializable
data class CheckFirmwareDestination(
    val deviceName: String,
    val walletId: String = "",
    val groupId: String = ""
)

fun NavGraphBuilder.checkFirmwareDestination(
    onChainAddSignerParam: OnChainAddSignerParam?,
    onMoreClicked: () -> Unit = {},
    onFilteredSignersReady: (SignerModel) -> Unit = {},
    onOpenNextScreen: (SignerFirmwareDevice) -> Unit = {}
) {
    composable<CheckFirmwareDestination> { backStackEntry ->
        val destination = backStackEntry.toRoute<CheckFirmwareDestination>()
        val device = SignerFirmwareDevice.valueOf(destination.deviceName)
        val signerTag = device.tag
        val args = CheckFirmwareArgs(
            signerTag = signerTag,
            onChainAddSignerParam = onChainAddSignerParam,
            walletId = destination.walletId,
            groupId = destination.groupId
        )
        CheckFirmwareNavigationScreen(
            args = args,
            onMoreClicked = onMoreClicked,
            onFilteredSignersReady = onFilteredSignersReady,
            onOpenNextScreen = { onOpenNextScreen(device) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckFirmwareNavigationScreen(
    args: CheckFirmwareArgs,
    viewModel: CheckFirmwareViewModel = hiltViewModel(),
    onMoreClicked: () -> Unit = {},
    onFilteredSignersReady: (SignerModel) -> Unit = {},
    onOpenNextScreen: () -> Unit = {}
) {
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    val firmwareVersion by viewModel.firmwareVersion.collectAsStateWithLifecycle()
    val filteredSigners by viewModel.filteredSigners.collectAsStateWithLifecycle()
    var showSignerBottomSheet by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.init(args)
        viewModel.event.collect { event ->
            when (event) {
                is CheckFirmwareEvent.ShowFilteredSigners -> {
                    showSignerBottomSheet = true
                }

                CheckFirmwareEvent.OpenNextScreen -> {
                    onOpenNextScreen()
                }
            }
        }
    }

    CheckFirmwareContent(
        args = args,
        onMoreClicked = onMoreClicked,
        remainTime = remainTime,
        firmwareVersion = firmwareVersion,
        onContinueClicked = viewModel::onContinueClicked
    )

    if (showSignerBottomSheet && filteredSigners.isNotEmpty()) {
        SelectSignerBottomSheet(
            sheetState = bottomSheetState,
            onDismiss = {
                showSignerBottomSheet = false
            },
            onAddExistKey = { signer ->
                showSignerBottomSheet = false
                onFilteredSignersReady(signer)
            },
            onAddNewKey = {
                showSignerBottomSheet = false
                onOpenNextScreen()
            },
            args = SelectSignerArgs(
                signers = filteredSigners,
                type = when (args.signerTag) {
                    SignerTag.COLDCARD -> SignerType.COLDCARD_NFC
                    SignerTag.JADE -> SignerType.AIRGAP
                    else -> SignerType.UNKNOWN
                },
            )
        )
    }
}

@Composable
private fun CheckFirmwareContent(
    args: CheckFirmwareArgs,
    remainTime: Int = 0,
    firmwareVersion: String = "",
    onMoreClicked: () -> Unit = {},
    onContinueClicked: () -> Unit = {},
) {
    val signerName = when (args.signerTag) {
        SignerTag.COLDCARD -> "COLDCARD"
        SignerTag.JADE -> "Jade"
        else -> args.signerTag.name
    }
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcImageAppBar(
                    backgroundRes = R.drawable.bg_check_coldcard_firmware_illustration,
                    title = if (remainTime <= 0) "" else stringResource(
                        id = R.string.nc_estimate_remain_time,
                        remainTime
                    ),
                    actions = {
                        IconButton(onClick = onMoreClicked) {
                            Icon(
                                painter = painterResource(id = com.nunchuk.android.signer.R.drawable.ic_more),
                                contentDescription = stringResource(R.string.nc_membership_cd_more_icon)
                            )
                        }
                    }
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(id = com.nunchuk.android.signer.R.string.nc_text_continue))
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_membership_check_firmware_title, signerName),
                    style = NunchukTheme.typography.heading
                )
                val context = LocalContext.current
                val versionText = when {
                    firmwareVersion.isEmpty() -> ""
                    firmwareVersion.contains("or above", ignoreCase = true) -> "$firmwareVersion."
                    else -> stringResource(R.string.nc_membership_firmware_version_or_above, firmwareVersion)
                }
                val requirementText = if (firmwareVersion.isNotEmpty()) {
                    stringResource(R.string.nc_membership_firmware_requirement_version, signerName, versionText)
                } else {
                    stringResource(R.string.nc_membership_firmware_requirement_latest, signerName)
                }
                val linkText = if (args.signerTag == SignerTag.JADE) {
                    stringResource(R.string.nc_membership_firmware_blockstream_instructions)
                } else {
                    stringResource(R.string.nc_membership_firmware_signer_instructions, signerName)
                }
                val updateText = stringResource(R.string.nc_membership_firmware_update_instructions, signerName, linkText)
                val annotatedText = buildAnnotatedString {
                    if (firmwareVersion.isNotEmpty()) {
                        val versionIndex = requirementText.lastIndexOf(versionText)
                        append(requirementText.substring(0, versionIndex))
                        withStyle(style = SpanStyle(fontWeight = FontWeight.W700)) {
                            append(versionText)
                        }
                        append(requirementText.substring(versionIndex + versionText.length))
                    } else {
                        append(requirementText)
                    }
                    append("\n\n")
                    val linkIndex = updateText.lastIndexOf(linkText)
                    append(updateText.substring(0, linkIndex))
                    pushStringAnnotation(
                        tag = "URL",
                        annotation = when (args.signerTag) {
                            SignerTag.COLDCARD -> "https://coldcard.com/docs/upgrade/"
                            SignerTag.JADE -> "https://help.blockstream.com/hc/en-us/articles/4408030503577-Update-Jade-firmware"
                            else -> ""
                        }
                    )
                    withStyle(
                        style = SpanStyle(
                            color = Color(0xFF1976D2),
                            textDecoration = TextDecoration.None
                        )
                    ) {
                        append(linkText)
                    }
                    pop()
                    append(updateText.substring(linkIndex + linkText.length))
                }

                ClickableText(
                    modifier = Modifier.padding(16.dp),
                    text = annotatedText,
                    style = NunchukTheme.typography.body,
                    onClick = { offset ->
                        annotatedText.getStringAnnotations(
                            tag = "URL",
                            start = offset,
                            end = offset
                        ).firstOrNull()?.let { annotation ->
                            val intent = Intent(Intent.ACTION_VIEW, annotation.item.toUri())
                            context.startActivity(intent)
                        }
                    }
                )
            }
        }
    }
}
