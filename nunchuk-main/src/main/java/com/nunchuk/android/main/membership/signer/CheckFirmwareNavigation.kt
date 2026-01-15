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
import com.nunchuk.android.main.membership.key.list.SelectSignerBottomSheet
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragmentArgs
import com.nunchuk.android.main.membership.onchaintimelock.checkfirmware.CheckFirmwareEvent
import com.nunchuk.android.main.membership.onchaintimelock.checkfirmware.CheckFirmwareViewModel
import com.nunchuk.android.nav.args.CheckFirmwareArgs
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import kotlinx.serialization.Serializable

@Serializable
data class CheckFirmwareDestination(
    val signerTagName: String,
    val walletId: String = "",
    val groupId: String = ""
)

fun NavGraphBuilder.checkFirmwareDestination(
    onChainAddSignerParam: OnChainAddSignerParam?,
    onMoreClicked: () -> Unit = {},
    onFilteredSignersReady: (SignerModel) -> Unit = {},
    onOpenNextScreen: (SignerTag) -> Unit = {}
) {
    composable<CheckFirmwareDestination> { backStackEntry ->
        val destination = backStackEntry.toRoute<CheckFirmwareDestination>()
        val signerTag = SignerTag.valueOf(destination.signerTagName)
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
            onOpenNextScreen = { onOpenNextScreen(signerTag) }
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
        NunchukTheme {
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
                args = TapSignerListBottomSheetFragmentArgs(
                    signers = filteredSigners.toTypedArray(),
                    type = when (args.signerTag) {
                        SignerTag.COLDCARD -> SignerType.COLDCARD_NFC
                        SignerTag.JADE -> SignerType.AIRGAP
                        else -> SignerType.UNKNOWN
                    }
                )
            )
        }
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
                                contentDescription = "More icon"
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
                    text = "Check your $signerName firmware",
                    style = NunchukTheme.typography.heading
                )
                val context = LocalContext.current
                val annotatedText = buildAnnotatedString {
                    append(
                        "Your $signerName must support Miniscript for on-chain timelocks. Please make sure the device is running "
                    )
                    if (firmwareVersion.isNotEmpty()) {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.W700)) {
                            val versionText = if (firmwareVersion.contains("or above", ignoreCase = true)) {
                                "$firmwareVersion."
                            } else {
                                "$firmwareVersion or above."
                            }
                            append(versionText)
                        }
                    } else {
                        append("the latest firmware version.")
                    }
                    append("\n\n")
                    append("To check or update $signerName firmware, please follow ")
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
                        if (args.signerTag == SignerTag.JADE) {
                            append("Blockstream's instructions")
                        } else {
                            append("$signerName's instructions")
                        }
                    }
                    pop()
                    append(".")
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
