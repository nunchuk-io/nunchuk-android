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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.compose.NcCheckBox
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcSnackbarVisuals
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.fillBeewax
import com.nunchuk.android.compose.lightGray
import com.nunchuk.android.core.miniscript.MultisignType
import com.nunchuk.android.transaction.components.schedule.timezone.toTimeZoneDetail
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.MiniscriptTimelockBased
import com.nunchuk.android.type.MiniscriptTimelockType
import kotlinx.serialization.Serializable
import java.text.DecimalFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val MAX_REQUIRED_KEYS = 20
private const val MAX_TOTAL_KEYS = 20

@Serializable
data class MiniscriptConfigTemplate(
    val multisignType: Int
)

fun NavGraphBuilder.miniscriptConfigTemplateDestination(
    addressType: AddressType,
    onNext: (String) -> Unit
) {
    composable<MiniscriptConfigTemplate> { navBackStackEntry ->

        val data: MiniscriptConfigTemplate = navBackStackEntry.toRoute()

        val multisignType =
            MultisignType.entries.find { it.ordinal == data.multisignType }
                ?: MultisignType.FLEXIBLE

        MiniscriptConfigTemplateContainer(
            addressType = addressType,
            multisignType = multisignType,
            onContinueClick = { template ->
                onNext(template)
            }
        )
    }
}

@Composable
fun MiniscriptConfigTemplateContainer(
    addressType: AddressType = AddressType.ANY,
    multisignType: MultisignType = MultisignType.FLEXIBLE,
    onContinueClick: (String) -> Unit = {}
) {
    val viewModel: MiniscriptConfigTemplateViewModel = hiltViewModel()
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

    MiniscriptConfigTemplateScreen(
        multisignType = multisignType,
        currentBlockHeight = uiState.currentBlockHeight,
        snackbarHostState = snackbarHostState,
        onContinueClick = { m, n, newM, newN, timelockData, reuseSigner ->
            val time =
                if ((timelockData.timelockType == MiniscriptTimelockType.ABSOLUTE
                    && timelockData.timeUnit == MiniscriptTimelockBased.TIME_LOCK)
                ) {
                    val localCalendar = Calendar.getInstance()
                    localCalendar.timeInMillis = timelockData.value * 1000
                    // Create a calendar in the selected timezone and set the same date/time components
                    val targetTimezoneCalendar = Calendar.getInstance(TimeZone.getTimeZone(timelockData.timezoneId))
                    targetTimezoneCalendar.set(Calendar.YEAR, localCalendar.get(Calendar.YEAR))
                    targetTimezoneCalendar.set(Calendar.MONTH, localCalendar.get(Calendar.MONTH))
                    targetTimezoneCalendar.set(Calendar.DAY_OF_MONTH, localCalendar.get(Calendar.DAY_OF_MONTH))
                    targetTimezoneCalendar.set(Calendar.HOUR_OF_DAY, localCalendar.get(Calendar.HOUR_OF_DAY))
                    targetTimezoneCalendar.set(Calendar.MINUTE, localCalendar.get(Calendar.MINUTE))
                    targetTimezoneCalendar.set(Calendar.SECOND, 0)
                    targetTimezoneCalendar.set(Calendar.MILLISECOND, 0)

                    // This gives us the UTC timestamp for the selected date/time in the selected timezone
                    targetTimezoneCalendar.timeInMillis / 1000
                } else if (timelockData.timelockType == MiniscriptTimelockType.RELATIVE
                    && timelockData.timeUnit == MiniscriptTimelockBased.TIME_LOCK
                ) {
                    // Use the new fields if available, otherwise fall back to value
                    if (timelockData.days > 0 || timelockData.hours > 0 || timelockData.minutes > 0) {
                        (timelockData.days * 24 * 60 * 60) + (timelockData.hours * 60 * 60) + (timelockData.minutes * 60)
                    } else {
                        timelockData.value // Fall back to legacy value
                    }
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
                reuseSigner = reuseSigner,
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniscriptConfigTemplateScreen(
    multisignType: MultisignType = MultisignType.FLEXIBLE,
    currentBlockHeight: Int = 0,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onContinueClick: (m: Int, n: Int, newM: Int, newN: Int, timelockData: TimelockData, reuseSigner: Boolean) -> Unit = { _, _, _, _, _, _ -> }
) {
    var showEditPolicyBottomSheet by remember { mutableStateOf(false) }
    var showEditTimelockBottomSheet by remember { mutableStateOf(false) }
    var timelockData by remember {
        mutableStateOf(
            TimelockData(
                timelockType = MiniscriptTimelockType.ABSOLUTE,
                timeUnit = MiniscriptTimelockBased.TIME_LOCK,
                value = {
                    // Set to midnight of 30 days from now
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.DAY_OF_YEAR, 30)
                    calendar.timeInMillis / 1000 // Convert to Unix timestamp (seconds)
                }(),
                timezoneId = TimeZone.getDefault().id,
                timezoneCity = TimeZone.getDefault().id.toTimeZoneDetail()?.city ?: "",
                timezoneOffset = TimeZone.getDefault().id.toTimeZoneDetail()?.offset ?: ""
            )
        )
    }

    val dateFormat = remember { SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()) }
    val dateTimeFormat = remember { SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault()) }
    val numberFormatter = remember { DecimalFormat("#,###") }
    val timeLockText by remember {
        derivedStateOf {
            when {
                // Absolute time - show formatted date
                timelockData.timelockType == MiniscriptTimelockType.ABSOLUTE && timelockData.timeUnit == MiniscriptTimelockBased.TIME_LOCK -> {
                    val date = Date(timelockData.value * 1000) // Convert seconds to milliseconds
                    val calendar = Calendar.getInstance().apply { time = date }
                    
                    // Check if time is midnight (00:00)
                    if (calendar.get(Calendar.HOUR_OF_DAY) == 0 && calendar.get(Calendar.MINUTE) == 0) {
                        dateFormat.format(date) // Show only date
                    } else {
                        dateTimeFormat.format(date) // Show date and time
                    }
                }
                // Absolute block height - show "block {number}"
                timelockData.timelockType == MiniscriptTimelockType.ABSOLUTE && timelockData.timeUnit == MiniscriptTimelockBased.HEIGHT_LOCK -> {
                    "block ${numberFormatter.format(timelockData.value)}"
                }
                // Relative time lock - show days, hours, minutes
                timelockData.timelockType == MiniscriptTimelockType.RELATIVE && timelockData.timeUnit == MiniscriptTimelockBased.TIME_LOCK -> {
                    // Use new fields if available, otherwise fall back to legacy value
                    val days = if (timelockData.days > 0 || timelockData.hours > 0 || timelockData.minutes > 0) {
                        timelockData.days
                    } else {
                        timelockData.value / (24 * 60 * 60) // Convert from seconds to days
                    }
                    val hours = if (timelockData.hours > 0 || timelockData.days > 0) {
                        timelockData.hours
                    } else {
                        (timelockData.value % (24 * 60 * 60)) / (60 * 60) // Convert from seconds to hours
                    }
                    val minutes = if (timelockData.minutes > 0 || timelockData.days > 0) {
                        timelockData.minutes
                    } else {
                        (timelockData.value % (60 * 60)) / 60 // Convert from seconds to minutes
                    }
                    
                    val parts = mutableListOf<String>()
                    if (days > 0) {
                        parts.add("${days}d")
                    }
                    if (hours > 0) {
                        parts.add("${hours}h")
                    }
                    if (minutes > 0) {
                        parts.add("${minutes}m")
                    }
                    
                    if (parts.isEmpty()) {
                        "0d"
                    } else {
                        parts.joinToString(" ")
                    }
                }
                // Relative block height - show "{number} blocks"
                timelockData.timelockType == MiniscriptTimelockType.RELATIVE && timelockData.timeUnit == MiniscriptTimelockBased.HEIGHT_LOCK -> {
                    "${numberFormatter.format(timelockData.value)} blocks"
                }
                // Default fallback
                else -> {
                    "${numberFormatter.format(timelockData.value)} blocks"
                }
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

    val title = when (multisignType) {
        MultisignType.FLEXIBLE -> "Flexible multisig"
        MultisignType.EXPANDING -> "Expanding multisig"
        MultisignType.DECAYING -> "Decaying multisig"
        else -> ""
    }

    val reuseSigner = remember { mutableStateOf(false) }

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
                            onContinueClick(m, n, newM, newN, timelockData, reuseSigner.value)
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
                    .navigationBarsPadding()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                val spacing = 12.dp
                val chipModifier = Modifier
                    .padding(end = 4.dp)
                    .background(color = MaterialTheme.colorScheme.fillBeewax, shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)

                Text(
                    text = "Spending rules",
                    style = NunchukTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = spacing, start = 16.dp, end = 16.dp)
                )

                when {
                    timelockData.timelockType == MiniscriptTimelockType.ABSOLUTE -> {
                        AbsoluteCard(
                            m = m,
                            n = n,
                            newM = newM,
                            newN = newN,
                            multisignType = multisignType,
                            timeLockText = timeLockText,
                            chipModifier = chipModifier,
                            onEditingInitialPolicyChange = { editingInitialPolicy = it },
                            onShowEditPolicyBottomSheetChange = { showEditPolicyBottomSheet = it },
                            onShowEditTimelockBottomSheetChange = { showEditTimelockBottomSheet = it }
                        )
                    }
                    timelockData.timelockType == MiniscriptTimelockType.RELATIVE -> {
                        RelativeCard(
                            m = m,
                            n = n,
                            newM = newM,
                            newN = newN,
                            multisignType = multisignType,
                            timeLockText = timeLockText,
                            chipModifier = chipModifier,
                            onEditingInitialPolicyChange = { editingInitialPolicy = it },
                            onShowEditPolicyBottomSheetChange = { showEditPolicyBottomSheet = it },
                            onShowEditTimelockBottomSheetChange = { showEditTimelockBottomSheet = it }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .clickable {
                            reuseSigner.value = !reuseSigner.value
                        }
                ) {
                    CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                        NcCheckBox(
                            checked = reuseSigner.value,
                            onCheckedChange = {
                                reuseSigner.value = it
                            },
                            enabled = true
                        )
                    }
                    Text(
                        text = "Reuse keys across policies (extra XPUBs might be required)",
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
                            maxM = MAX_REQUIRED_KEYS,
                            minN = 2,
                            maxN = if (multisignType == MultisignType.EXPANDING) MAX_TOTAL_KEYS - 1 else MAX_TOTAL_KEYS
                        )
                    }

                    multisignType == MultisignType.FLEXIBLE -> {
                        EditPolicyConfig(
                            initialM = newM,
                            initialN = newN,
                            showTotalKeys = true,
                            showRequiredKeys = true,
                            minM = 1,
                            maxM = MAX_REQUIRED_KEYS,
                            minN = 2,
                            maxN = MAX_TOTAL_KEYS
                        )
                    }

                    multisignType == MultisignType.EXPANDING -> {
                        EditPolicyConfig(
                            initialN = newN,
                            showTotalKeys = true,
                            showRequiredKeys = false,
                            minN = n + 1,
                            maxN = MAX_TOTAL_KEYS
                        )
                    }

                    multisignType == MultisignType.DECAYING -> {
                        EditPolicyConfig(
                            initialM = newM,
                            showTotalKeys = false,
                            showRequiredKeys = true,
                            minM = 1,
                            maxM = n - 1  // Enable "+" button while newM < n - 1
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
                            
                            // For EXPANDING multisig: if keyset 1 (n) >= keyset 2 (newN), 
                            // automatically update keyset 2 to be keyset 1 + 1
                            if (multisignType == MultisignType.EXPANDING && n >= newN) {
                                newN = n + 1
                            }
                            
                            // For DECAYING multisig: if keyset 1 (m) <= keyset 2 (newM),
                            // automatically update keyset 2 to be keyset 1 - 1
                            if (multisignType == MultisignType.DECAYING && m <= newM) {
                                newM = m - 1
                            }
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
                                    
                                    // For DECAYING multisig: if keyset 2 (newM) >= keyset 1 (m),
                                    // automatically update keyset 1 to be keyset 2 + 1
                                    if (newM >= m) {
                                        m = newM + 1
                                    }
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
                    currentBlockHeight = currentBlockHeight.toLong(),
                    initialData = timelockData,
                    snackbarHostState = snackbarHostState,
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

@Composable
fun AbsoluteCard(
    m: Int,
    n: Int,
    newM: Int,
    newN: Int,
    multisignType: MultisignType,
    timeLockText: String,
    chipModifier: Modifier,
    onEditingInitialPolicyChange: (Boolean) -> Unit,
    onShowEditPolicyBottomSheetChange: (Boolean) -> Unit,
    onShowEditTimelockBottomSheetChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.lightGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            TextChipLineContent(
                modifier = chipModifier,
                contentBeginning = {
                    Text(
                        text = "Until ",
                        style = NunchukTheme.typography.body
                    )
                },
                text = timeLockText,
                contentEnd = {
                    Text(
                        text = "spending requires",
                        style = NunchukTheme.typography.body
                    )
                },
                onClick = {
                    onShowEditTimelockBottomSheetChange(true)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextChipLineContent(
                modifier = chipModifier,
                contentBeginning = {
                    Text(
                        text = "signatures from a ",
                        style = NunchukTheme.typography.body
                    )
                },
                text = "$m-of-$n",
                contentEnd = {
                    Text(
                        text = if (n == 1 && m == 1) "singlesig." else "multisig.",
                        style = NunchukTheme.typography.body
                    )
                },
                onClick = {
                    onEditingInitialPolicyChange(true)
                    onShowEditPolicyBottomSheetChange(true)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "After that, spending requires either the same",
                style = NunchukTheme.typography.body
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                when (multisignType) {
                    MultisignType.FLEXIBLE -> {
                        TextChipLineContent(
                            contentBeginning = {
                                Text(
                                    text = "${if (n == 1 && m == 1) "$m‑of‑$n singlesig" else "$m‑of‑$n multisig"} OR a ",
                                    style = NunchukTheme.typography.body
                                )
                            },
                            modifier = chipModifier,
                            text = "$newM-of-$newN",
                            contentEnd = {
                                Text(
                                    text = if (newN == 1 && newM == 1) " singlesig." else " multisig.",
                                    style = NunchukTheme.typography.body
                                )
                            },
                            onClick = {
                                onEditingInitialPolicyChange(false)
                                onShowEditPolicyBottomSheetChange(true)
                            }
                        )
                    }

                    MultisignType.EXPANDING -> {
                        TextChipLineContent(
                            contentBeginning = {
                                Text(
                                    text = "${if (n == 1 && m == 1) "$m‑of‑$n singlesig" else "$m‑of‑$n multisig"} OR a $m of ",
                                    style = NunchukTheme.typography.body
                                )
                            },
                            modifier = chipModifier,
                            text = "$newN",
                            contentEnd = {
                                Text(
                                    text = if (newN == 1 && m == 1) " singlesig." else " multisig.",
                                    style = NunchukTheme.typography.body
                                )
                            },
                            onClick = {
                                onEditingInitialPolicyChange(false)
                                onShowEditPolicyBottomSheetChange(true)
                            }
                        )
                    }

                    MultisignType.DECAYING -> {
                        TextChipLineContent(
                            contentBeginning = {
                                Text(
                                    text = "${if (n == 1 && m == 1) "$m‑of‑$n singlesig" else "$m‑of‑$n multisig"} OR a ",
                                    style = NunchukTheme.typography.body
                                )
                            },
                            modifier = chipModifier,
                            text = "$newM",
                            contentEnd = {
                                Text(
                                    text = if (newM == 1 && n == 1) " of-$n-singlesig." else " of-$n-multisig.",
                                    style = NunchukTheme.typography.body
                                )
                            },
                            onClick = {
                                onEditingInitialPolicyChange(false)
                                onShowEditPolicyBottomSheetChange(true)
                            }
                        )
                    }

                    else -> {}
                }
            }
        }
    }
}

@Composable
fun RelativeCard(
    m: Int,
    n: Int,
    newM: Int,
    newN: Int,
    multisignType: MultisignType,
    timeLockText: String,
    chipModifier: Modifier,
    onEditingInitialPolicyChange: (Boolean) -> Unit,
    onShowEditPolicyBottomSheetChange: (Boolean) -> Unit,
    onShowEditTimelockBottomSheetChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.lightGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            TextChipLineContent(
                modifier = chipModifier,
                contentBeginning = {
                    Text(
                        text = "First ",
                        style = NunchukTheme.typography.body
                    )
                },
                text = timeLockText,
                contentEnd = {
                    Text(
                        text = " after a coin is received:",
                        style = NunchukTheme.typography.body
                    )
                },
                onClick = {
                    onShowEditTimelockBottomSheetChange(true)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "spending that coin requires signatures",
                style = NunchukTheme.typography.body
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextChipLineContent(
                contentBeginning = {
                    Text(
                        text = "from a ",
                        style = NunchukTheme.typography.body
                    )
                },
                modifier = chipModifier,
                text = "$m-of-$n",
                contentEnd = {
                    Text(
                        text = if (n == 1 && m == 1) " singlesig." else " multisig.",
                        style = NunchukTheme.typography.body
                    )
                },
                onClick = {
                    onEditingInitialPolicyChange(true)
                    onShowEditPolicyBottomSheetChange(true)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "After that, the same coin can be spent with",
                style = NunchukTheme.typography.body
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "either the same ${if (n == 1 && m == 1) "$m-of-$n singlesig" else "$m-of-$n multisig"} OR a ",
                style = NunchukTheme.typography.body
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                when (multisignType) {
                    MultisignType.FLEXIBLE -> {
                        TextChipLineContent(
                            modifier = chipModifier,
                            text = "$newM-of-$newN",
                            contentEnd = {
                                Text(
                                    text = if (newN == 1 && newM == 1) " singlesig." else " multisig.",
                                    style = NunchukTheme.typography.body
                                )
                            },
                            onClick = {
                                onEditingInitialPolicyChange(false)
                                onShowEditPolicyBottomSheetChange(true)
                            }
                        )
                    }

                    MultisignType.EXPANDING -> {
                        TextChipLineContent(
                            contentBeginning = {
                                Text(
                                    text = "$m of ",
                                    style = NunchukTheme.typography.body
                                )
                            },
                            modifier = chipModifier,
                            text = "$newN",
                            contentEnd = {
                                Text(
                                    text = if (newN == 1 && m == 1) " singlesig." else " multisig.",
                                    style = NunchukTheme.typography.body
                                )
                            },
                            onClick = {
                                onEditingInitialPolicyChange(false)
                                onShowEditPolicyBottomSheetChange(true)
                            }
                        )
                    }

                    MultisignType.DECAYING -> {
                        TextChipLineContent(
                            modifier = chipModifier,
                            text = "$newM",
                            contentEnd = {
                                Text(
                                    text = if (newM == 1 && n == 1) " of-$n-singlesig." else " of-$n-multisig.",
                                    style = NunchukTheme.typography.body
                                )
                            },
                            onClick = {
                                onEditingInitialPolicyChange(false)
                                onShowEditPolicyBottomSheetChange(true)
                            }
                        )
                    }

                    else -> {}
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun MiniscriptConfigTemplateScreenPreview() {
    NunchukTheme {
        MiniscriptConfigTemplateScreen(
            currentBlockHeight = 850000,
            multisignType = MultisignType.DECAYING
        )
    }
}