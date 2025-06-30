package com.nunchuk.android.app.miniscript.contractpolicy

import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcSnackbarVisuals
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.lightGray
import com.nunchuk.android.core.miniscript.MultisignType
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.MiniscriptTimelockBased
import com.nunchuk.android.type.MiniscriptTimelockType
import kotlinx.serialization.Serializable
import java.util.Date
import java.util.Locale

@Serializable
data class MiniscriptConfigTemplate(
    val multisignType: Int
)

fun NavGraphBuilder.miniscriptConfigTemplateDestination(
    addressType: AddressType,
    onNext: (String) -> Unit
) {
    composable<MiniscriptConfigTemplate> { navBackStackEntry ->
        val viewModel = hiltViewModel<MiniscriptConfigTemplateViewModel>()

        val data: MiniscriptConfigTemplate = navBackStackEntry.toRoute()

        val multisignType =
            MultisignType.entries.find { it.ordinal == data.multisignType }
                ?: MultisignType.FLEXIBLE

        MiniscriptConfigTemplateScreen(
            addressType = addressType,
            multisignType = multisignType,
            viewModel = viewModel,
            onContinueClick = { template ->
                onNext(template)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniscriptConfigTemplateScreen(
    addressType: AddressType = AddressType.ANY,
    multisignType: MultisignType = MultisignType.FLEXIBLE,
    viewModel: MiniscriptConfigTemplateViewModel = hiltViewModel(),
    onContinueClick: (String) -> Unit = {}
) {
    var showEditPolicyBottomSheet by remember { mutableStateOf(false) }
    var showEditTimelockBottomSheet by remember { mutableStateOf(false) }
    var timelockData by remember {
        mutableStateOf(
            TimelockData(
                timelockType = MiniscriptTimelockType.ABSOLUTE,
                timeUnit = MiniscriptTimelockBased.TIME_LOCK,
                value = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
            )
        )
    }

    val dateFormat = remember { SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()) }
    val timeLockText by remember {
        derivedStateOf {
            if (timelockData.timelockType == MiniscriptTimelockType.ABSOLUTE && timelockData.timeUnit == MiniscriptTimelockBased.TIME_LOCK) {
                dateFormat.format(Date(timelockData.value))
            } else {
                val suffixText =
                    if (timelockData.timelockType == MiniscriptTimelockType.RELATIVE && timelockData.timeUnit == MiniscriptTimelockBased.TIME_LOCK) {
                        " days"
                    } else {
                        " blocks"
                    }
                "${timelockData.value}$suffixText"
            }
        }
    }

    var m by remember { mutableIntStateOf(2) }
    var n by remember { mutableIntStateOf(3) }
    var newM by remember { mutableIntStateOf(3) }
    var newN by remember { mutableIntStateOf(3) }

    var editingInitialPolicy by remember { mutableStateOf(true) }

    // Set default values based on multisignType
    LaunchedEffect(multisignType) {
        when (multisignType) {
            MultisignType.FLEXIBLE -> {
                m = 2
                n = 3
                newM = 3
                newN = 3
            }

            MultisignType.DECAYING -> {
                m = 2
                n = 3
                newM = 1
                newN = 3
            }

            MultisignType.EXPANDING -> {
                m = 2
                n = 2
                newM = 2
                newN = 3
            }

            else -> {}
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.event) {
        when (val event = uiState.event) {
            is MiniscriptConfigTemplateEvent.TemplateCreated -> {
                onContinueClick(event.template)
                viewModel.onEventHandled()
            }

            is MiniscriptConfigTemplateEvent.ShowError -> {
                snackbarHostState.showSnackbar(
                    NcSnackbarVisuals(
                        message = event.message,
                        type = NcToastType.ERROR
                    )
                )
                viewModel.onEventHandled()
            }

            null -> {}
        }
    }

    val title = when (multisignType) {
        MultisignType.FLEXIBLE -> "Flexible multisig"
        MultisignType.EXPANDING -> "Expanding multisig"
        MultisignType.DECAYING -> "Decaying multisig"
        else -> ""
    }

    val reuseSigner = remember { mutableStateOf(multisignType == MultisignType.FLEXIBLE) }

    NunchukTheme {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = Modifier
                .navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = title,
                    textStyle = NunchukTheme.typography.titleLarge,
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    NcPrimaryDarkButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val time =
                                if ((timelockData.timelockType == MiniscriptTimelockType.ABSOLUTE
                                    && timelockData.timeUnit == MiniscriptTimelockBased.TIME_LOCK)
                                ) {
                                    timelockData.value / 1000 // Convert milliseconds to seconds
                                } else if (timelockData.timelockType == MiniscriptTimelockType.RELATIVE
                                    && timelockData.timeUnit == MiniscriptTimelockBased.TIME_LOCK
                                ) {
                                    timelockData.value * 24 * 60 * 60 // Convert days to seconds
                                } else {
                                    timelockData.value
                                }
                            viewModel.createMiniscriptTemplateBySelection(
                                multisignType = multisignType.ordinal,
                                newM = newM,
                                newN = newN,
                                n = n,
                                m = m,
                                timelockType = timelockData.timelockType.ordinal,
                                timeUnit = when (timelockData.timeUnit) {
                                    MiniscriptTimelockBased.TIME_LOCK -> 0
                                    MiniscriptTimelockBased.HEIGHT_LOCK -> 1
                                    else -> 0
                                },
                                time = time,
                                addressType = addressType,
                                reuseSigner = reuseSigner.value,
                            )
                        }
                    ) {
                        Text(text = "Continue")
                    }
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                val spacing = 12.dp
                val chipModifier = Modifier
                    .padding(end = 4.dp)
                    .background(Color(0xFFFFF1CC), shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)

                Text(
                    text = "Contract policy:",
                    style = NunchukTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = spacing)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.lightGray)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextChip("$m of $n", chipModifier) {
                                editingInitialPolicy = true
                                showEditPolicyBottomSheet = true
                            }
                            Text(
                                text = "multisig wallet",
                                style = NunchukTheme.typography.body
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "will automatically change to",
                            style = NunchukTheme.typography.body
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            when (multisignType) {
                                MultisignType.FLEXIBLE -> {
                                    TextChipLineContent(
                                        contentBeginning = {
                                            Text(
                                                text = "a ",
                                                style = NunchukTheme.typography.body
                                            )
                                        },
                                        modifier = chipModifier,
                                        text = "$newM of $newN",
                                        contentEnd = {
                                            Text(
                                                text = " multisig wallet",
                                                style = NunchukTheme.typography.body
                                            )
                                        },
                                        onClick = {
                                            editingInitialPolicy = false
                                            showEditPolicyBottomSheet = true
                                        }
                                    )
                                }

                                MultisignType.EXPANDING -> {
                                    TextChipLineContent(
                                        contentBeginning = {
                                            Text(
                                                text = "a $m of ",
                                                style = NunchukTheme.typography.body
                                            )
                                        },
                                        modifier = chipModifier,
                                        text = "$newN",
                                        contentEnd = {
                                            Text(
                                                text = " multisig wallet",
                                                style = NunchukTheme.typography.body
                                            )
                                        },
                                        onClick = {
                                            editingInitialPolicy = false
                                            showEditPolicyBottomSheet = true
                                        }
                                    )
                                }

                                MultisignType.DECAYING -> {
                                    TextChipLineContent(
                                        contentBeginning = {
                                            Text(
                                                text = "a ",
                                                style = NunchukTheme.typography.body
                                            )
                                        },
                                        modifier = chipModifier,
                                        text = "$newM",
                                        contentEnd = {
                                            Text(
                                                text = " of $n multisig wallet",
                                                style = NunchukTheme.typography.body
                                            )
                                        },
                                        onClick = {
                                            editingInitialPolicy = false
                                            showEditPolicyBottomSheet = true
                                        }
                                    )
                                }

                                else -> {}
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Column {
                            TextChipLineContent(
                                modifier = chipModifier,
                                text = "after $timeLockText",
                                contentEnd = {
                                    if (timelockData.timelockType == MiniscriptTimelockType.RELATIVE) {
                                        Text(
                                            text = " from the time the coins",
                                            style = NunchukTheme.typography.body
                                        )
                                    }
                                },
                                onClick = {
                                    showEditTimelockBottomSheet = true
                                }
                            )

                            if (timelockData.timelockType == MiniscriptTimelockType.RELATIVE) {
                                Text(
                                    text = " are received.",
                                    style = NunchukTheme.typography.body,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }

                val isCheckboxDisabled =
                    timelockData.timelockType == MiniscriptTimelockType.RELATIVE

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .alpha(if (isCheckboxDisabled) 0.4f else 1f)
                ) {
                    Checkbox(
                        checked = reuseSigner.value,
                        onCheckedChange = if (isCheckboxDisabled) null else {
                            { reuseSigner.value = it }
                        },
                        enabled = !isCheckboxDisabled
                    )
                    Text(
                        modifier = Modifier.padding(start = 4.dp),
                        text = "Reuse keys across multisig policies",
                        style = NunchukTheme.typography.body
                    )
                }
            }

            if (showEditPolicyBottomSheet) {
                val config = when {
                    editingInitialPolicy -> {
                        EditPolicyConfig(
                            initialM = m,
                            initialN = n,
                            showTotalKeys = true,
                            showRequiredKeys = true,
                            minM = if (multisignType == MultisignType.DECAYING) newM + 1 else 1,
                            maxM = 5,
                            minN = 2,
                            maxN = if (multisignType == MultisignType.EXPANDING) newN - 1 else 5
                        )
                    }

                    multisignType == MultisignType.FLEXIBLE -> {
                        EditPolicyConfig(
                            initialM = newM,
                            initialN = newN,
                            showTotalKeys = true,
                            showRequiredKeys = true,
                            minM = 1,
                            maxM = 5,
                            minN = 2,
                            maxN = 5
                        )
                    }

                    multisignType == MultisignType.EXPANDING -> {
                        EditPolicyConfig(
                            initialN = newN,
                            showTotalKeys = true,
                            showRequiredKeys = false,
                            minN = n + 1,
                            maxN = 5
                        )
                    }

                    multisignType == MultisignType.DECAYING -> {
                        EditPolicyConfig(
                            initialM = newM,
                            showTotalKeys = false,
                            showRequiredKeys = true,
                            minM = 1,
                            maxM = m - 1
                        )
                    }

                    else -> EditPolicyConfig()
                }

                EditPolicyBottomSheet(
                    config = config,
                    onDismiss = { showEditPolicyBottomSheet = false },
                    onSave = { newMValue, newNValue ->
                        if (editingInitialPolicy) {
                            m = newMValue
                            n = newNValue
                        } else {
                            when (multisignType) {
                                MultisignType.FLEXIBLE -> {
                                    newM = newMValue
                                    newN = newNValue
                                }

                                MultisignType.EXPANDING -> {
                                    newN = newNValue
                                }

                                MultisignType.DECAYING -> {
                                    newM = newMValue
                                }

                                else -> {}
                            }
                        }
                        showEditPolicyBottomSheet = false
                    }
                )
            }

            if (showEditTimelockBottomSheet) {
                EditTimelockBottomSheet(
                    currentBlockHeight = uiState.currentBlockHeight.toLong(),
                    initialData = timelockData,
                    onDismiss = { showEditTimelockBottomSheet = false },
                    onSave = { data ->
                        timelockData = data
                        showEditTimelockBottomSheet = false
                    }
                )
            }
        }
    }
}

@Composable
fun TextChipLineContent(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit = {},
    contentBeginning: @Composable () -> Unit = {},
    contentEnd: @Composable () -> Unit = {},
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        contentBeginning()
        TextChip(
            text = text,
            modifier = modifier.clickable { onClick() },
            onClick = onClick
        )
        contentEnd()
    }
}

@Composable
fun TextChip(text: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Row(
        modifier = modifier.clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = NunchukTheme.typography.body,
            textDecoration = TextDecoration.Underline,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Edit",
            modifier = Modifier.size(16.dp)
        )
    }
}

@PreviewLightDark
@Composable
private fun MiniscriptConfigTemplateScreenPreview() {
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = "Flexible multisig",
                    textStyle = NunchukTheme.typography.titleLarge,
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                val spacing = 12.dp
                val chipModifier = Modifier
                    .padding(end = 4.dp)
                    .background(Color(0xFFFFF1CC), shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)

                Text(
                    text = "Contract policy:",
                    style = NunchukTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = spacing)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.lightGray)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextChip("2 of 3", chipModifier) {}
                            Text(
                                text = "multisig wallet",
                                style = NunchukTheme.typography.body
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "will automatically change to",
                            style = NunchukTheme.typography.body
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextChipLineContent(
                                contentBeginning = {
                                    Text(
                                        text = "a ",
                                        style = NunchukTheme.typography.body
                                    )
                                },
                                modifier = chipModifier,
                                text = "3 of 3",
                                contentEnd = {
                                    Text(
                                        text = " multisig wallet",
                                        style = NunchukTheme.typography.body
                                    )
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextChip("after 30 days", chipModifier) {}
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Checkbox(
                        checked = true,
                        onCheckedChange = {}
                    )
                    Text(
                        modifier = Modifier.padding(start = 4.dp),
                        text = "Reuse keys across multisig policies",
                        style = NunchukTheme.typography.body
                    )
                }
            }
        }
    }
}