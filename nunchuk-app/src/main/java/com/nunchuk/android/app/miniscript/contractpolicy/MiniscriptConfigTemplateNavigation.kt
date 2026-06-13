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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.annotation.StringRes
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.R
import com.nunchuk.android.compose.NcCheckBox
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcSnackbarVisuals
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.fillBeewax
import com.nunchuk.android.compose.lightGray
import com.nunchuk.android.compose.miniscript.EditTimelockBottomSheet
import com.nunchuk.android.compose.miniscript.TimelockData
import com.nunchuk.android.core.miniscript.MultisignType
import com.nunchuk.android.core.ui.toTimeZoneDetail
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
    onNext: (String, Boolean) -> Unit
) {
    composable<MiniscriptConfigTemplate> { navBackStackEntry ->

        val data: MiniscriptConfigTemplate = navBackStackEntry.toRoute()

        val multisignType =
            MultisignType.entries.find { it.ordinal == data.multisignType }
                ?: MultisignType.FLEXIBLE

        MiniscriptConfigTemplateContainer(
            addressType = addressType,
            multisignType = multisignType,
            onContinueClick = { template, reuseSigner ->
                onNext(template, reuseSigner)
            }
        )
    }
}

@Composable
fun MiniscriptConfigTemplateContainer(
    addressType: AddressType = AddressType.ANY,
    multisignType: MultisignType = MultisignType.FLEXIBLE,
    onContinueClick: (String, Boolean) -> Unit = { _, _ -> }
) {
    val viewModel: MiniscriptConfigTemplateViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.event) {
        when (val event = uiState.event) {
            is MiniscriptConfigTemplateEvent.TemplateCreated -> {
                onContinueClick(event.template, event.reuseSigner)
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
    val context = LocalContext.current
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
                    context.getString(R.string.nc_miniscript_block_number, numberFormatter.format(timelockData.value))
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
                        parts.add(context.getString(R.string.nc_miniscript_duration_days_short, days))
                    }
                    if (hours > 0) {
                        parts.add(context.getString(R.string.nc_miniscript_duration_hours_short, hours))
                    }
                    if (minutes > 0) {
                        parts.add(context.getString(R.string.nc_miniscript_duration_minutes_short, minutes))
                    }
                    
                    if (parts.isEmpty()) {
                        context.getString(R.string.nc_miniscript_duration_days_short, 0)
                    } else {
                        parts.joinToString(" ")
                    }
                }
                // Relative block height - show "{number} blocks"
                timelockData.timelockType == MiniscriptTimelockType.RELATIVE && timelockData.timeUnit == MiniscriptTimelockBased.HEIGHT_LOCK -> {
                    context.resources.getQuantityString(R.plurals.nc_miniscript_blocks_count, timelockData.value.coerceIn(0, Int.MAX_VALUE.toLong()).toInt(), numberFormatter.format(timelockData.value))
                }
                // Default fallback
                else -> {
                    context.resources.getQuantityString(R.plurals.nc_miniscript_blocks_count, timelockData.value.coerceIn(0, Int.MAX_VALUE.toLong()).toInt(), numberFormatter.format(timelockData.value))
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

            MultisignType.ZEN_HODL -> {
                m = 1
                n = 1
                newM = 3
                newN = 3
            }

            else -> {}
        }
    }

    val title = when (multisignType) {
        MultisignType.FLEXIBLE -> stringResource(R.string.nc_miniscript_flexible_multisig)
        MultisignType.EXPANDING -> stringResource(R.string.nc_miniscript_expanding_multisig)
        MultisignType.DECAYING -> stringResource(R.string.nc_miniscript_decaying_multisig)
        MultisignType.ZEN_HODL -> stringResource(R.string.nc_miniscript_zen_hodl)
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
                        Text(text = stringResource(R.string.nc_miniscript_continue))
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
                Text(
                    text = stringResource(R.string.nc_miniscript_spending_rules),
                    style = NunchukTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = spacing, start = 16.dp, end = 16.dp)
                )

                when {
                    multisignType == MultisignType.ZEN_HODL -> {
                        when (timelockData.timelockType) {
                            MiniscriptTimelockType.ABSOLUTE -> {
                                ZenHodlAbsoluteCard(
                                    m = m,
                                    n = n,
                                    timeLockText = timeLockText,
                                    onEditingInitialPolicyChange = { editingInitialPolicy = it },
                                    onShowEditPolicyBottomSheetChange = { showEditPolicyBottomSheet = it },
                                    onShowEditTimelockBottomSheetChange = { showEditTimelockBottomSheet = it }
                                )
                            }
                            MiniscriptTimelockType.RELATIVE -> {
                                ZenHodlRelativeCard(
                                    m = m,
                                    n = n,
                                    timeLockText = timeLockText,
                                    onEditingInitialPolicyChange = { editingInitialPolicy = it },
                                    onShowEditPolicyBottomSheetChange = { showEditPolicyBottomSheet = it },
                                    onShowEditTimelockBottomSheetChange = { showEditTimelockBottomSheet = it }
                                )
                            }
                        }
                    }
                    timelockData.timelockType == MiniscriptTimelockType.ABSOLUTE -> {
                        AbsoluteCard(
                            m = m,
                            n = n,
                            newM = newM,
                            newN = newN,
                            multisignType = multisignType,
                            timeLockText = timeLockText,
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
                            onEditingInitialPolicyChange = { editingInitialPolicy = it },
                            onShowEditPolicyBottomSheetChange = { showEditPolicyBottomSheet = it },
                            onShowEditTimelockBottomSheetChange = { showEditTimelockBottomSheet = it }
                        )
                    }
                }

                if (multisignType != MultisignType.ZEN_HODL) {
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
                            text = stringResource(R.string.nc_miniscript_reuse_keys_across_policies),
                            style = NunchukTheme.typography.body
                        )
                    }
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
                            minN = if (multisignType == MultisignType.FLEXIBLE) 1 else 2,
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
                            minN = 1,
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

                    multisignType == MultisignType.ZEN_HODL -> {
                        EditPolicyConfig(
                            initialM = m,
                            initialN = n,
                            showTotalKeys = true,
                            showRequiredKeys = true,
                            minM = 1,
                            maxM = MAX_REQUIRED_KEYS,
                            minN = 1,
                            maxN = MAX_TOTAL_KEYS
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

                                MultisignType.ZEN_HODL -> {
                                    m = newMValue
                                    n = newNValue
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

sealed interface SentenceArg {
    data class Plain(val text: String) : SentenceArg
    data class Chip(val text: String, val onClick: () -> Unit) : SentenceArg
}

private val sentencePlaceholder = Regex("%(\\d+)\\\$s")
private val chipHorizontalPadding = 8.dp
private val chipVerticalPadding = 4.dp
private val chipIconSpacing = 4.dp
private val chipIconSize = 16.dp

/**
 * Renders a whole translatable sentence whose `%n$s` placeholders are filled with [args], so a
 * translation can put the editable chips wherever its grammar needs them. Chips are inline content
 * sized from their measured text, keeping the pill look of the old chip rows.
 */
@Composable
fun ChipSentence(
    template: String,
    args: List<SentenceArg>,
    modifier: Modifier = Modifier,
) {
    val style = NunchukTheme.typography.body
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val inlineContent = mutableMapOf<String, InlineTextContent>()
    val text = buildAnnotatedString {
        var cursor = 0
        sentencePlaceholder.findAll(template).forEach { match ->
            append(template.substring(cursor, match.range.first))
            when (val arg = args[match.groupValues[1].toInt() - 1]) {
                is SentenceArg.Plain -> append(arg.text)
                is SentenceArg.Chip -> {
                    val id = "chip${match.groupValues[1]}"
                    val textSize = textMeasurer.measure(arg.text, style).size
                    val placeholder = with(density) {
                        Placeholder(
                            width = (textSize.width + (chipHorizontalPadding * 2 + chipIconSpacing + chipIconSize).toPx()).toSp(),
                            height = (maxOf(textSize.height.toFloat(), chipIconSize.toPx()) + (chipVerticalPadding * 2).toPx()).toSp(),
                            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                        )
                    }
                    inlineContent[id] = InlineTextContent(placeholder) {
                        SentenceChip(text = arg.text, onClick = arg.onClick)
                    }
                    appendInlineContent(id, arg.text)
                }
            }
            cursor = match.range.last + 1
        }
        append(template.substring(cursor))
    }
    Text(text = text, style = style, inlineContent = inlineContent, modifier = modifier)
}

@Composable
private fun SentenceChip(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .background(color = MaterialTheme.colorScheme.fillBeewax, shape = RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = chipHorizontalPadding, vertical = chipVerticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = NunchukTheme.typography.body.copy(textDecoration = TextDecoration.Underline),
            maxLines = 1,
        )
        Spacer(modifier = Modifier.width(chipIconSpacing))
        Icon(
            painter = painterResource(R.drawable.ic_edit),
            contentDescription = stringResource(R.string.nc_edit),
            modifier = Modifier.size(chipIconSize)
        )
    }
}

@Composable
private fun SpendingRulesCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.lightGray)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            content()
        }
    }
}

/**
 * The "after the timelock" sentence shared by the absolute and relative cards: the same policy OR a
 * new one whose shape depends on [multisignType]. Each card passes its own templates so the lead-in
 * ("spending requires" vs "the same coin can be spent with") stays inside one translatable sentence.
 */
@Composable
private fun AfterTimelockSentence(
    m: Int,
    n: Int,
    newM: Int,
    newN: Int,
    multisignType: MultisignType,
    currentPolicyName: String,
    @StringRes flexibleRes: Int,
    @StringRes expandingRes: Int,
    @StringRes expandingSinglesigRes: Int,
    @StringRes decayingRes: Int,
    @StringRes decayingSinglesigRes: Int,
    onEditNewPolicy: () -> Unit,
) {
    val current = SentenceArg.Plain(currentPolicyName)
    when (multisignType) {
        MultisignType.FLEXIBLE -> ChipSentence(
            template = stringResource(flexibleRes),
            args = listOf(current, SentenceArg.Chip(policyName(newM, newN), onEditNewPolicy)),
        )

        MultisignType.EXPANDING -> ChipSentence(
            template = stringResource(if (newN == 1 && m == 1) expandingSinglesigRes else expandingRes),
            args = listOf(current, SentenceArg.Plain("$m"), SentenceArg.Chip("$newN", onEditNewPolicy)),
        )

        MultisignType.DECAYING -> ChipSentence(
            template = stringResource(if (newM == 1 && n == 1) decayingSinglesigRes else decayingRes),
            args = listOf(current, SentenceArg.Chip("$newM", onEditNewPolicy), SentenceArg.Plain("$n")),
        )

        else -> Unit
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
    onEditingInitialPolicyChange: (Boolean) -> Unit,
    onShowEditPolicyBottomSheetChange: (Boolean) -> Unit,
    onShowEditTimelockBottomSheetChange: (Boolean) -> Unit
) {
    SpendingRulesCard {
        ChipSentence(
            template = stringResource(R.string.nc_miniscript_rule_absolute_initial),
            args = listOf(
                SentenceArg.Chip(timeLockText) { onShowEditTimelockBottomSheetChange(true) },
                SentenceArg.Chip(policyName(m, n)) {
                    onEditingInitialPolicyChange(true)
                    onShowEditPolicyBottomSheetChange(true)
                },
            ),
        )
        AfterTimelockSentence(
            m = m,
            n = n,
            newM = newM,
            newN = newN,
            multisignType = multisignType,
            currentPolicyName = policyNameNb(m, n),
            flexibleRes = R.string.nc_miniscript_rule_absolute_after_flexible,
            expandingRes = R.string.nc_miniscript_rule_absolute_after_expanding,
            expandingSinglesigRes = R.string.nc_miniscript_rule_absolute_after_expanding_singlesig,
            decayingRes = R.string.nc_miniscript_rule_absolute_after_decaying,
            decayingSinglesigRes = R.string.nc_miniscript_rule_absolute_after_decaying_singlesig,
            onEditNewPolicy = {
                onEditingInitialPolicyChange(false)
                onShowEditPolicyBottomSheetChange(true)
            },
        )
    }
}

@Composable
fun ZenHodlAbsoluteCard(
    m: Int,
    n: Int,
    timeLockText: String,
    onEditingInitialPolicyChange: (Boolean) -> Unit,
    onShowEditPolicyBottomSheetChange: (Boolean) -> Unit,
    onShowEditTimelockBottomSheetChange: (Boolean) -> Unit
) {
    SpendingRulesCard {
        ChipSentence(
            template = stringResource(R.string.nc_miniscript_rule_zen_absolute_initial),
            args = listOf(SentenceArg.Chip(timeLockText) { onShowEditTimelockBottomSheetChange(true) }),
        )
        ChipSentence(
            template = stringResource(R.string.nc_miniscript_rule_zen_absolute_after),
            args = listOf(
                SentenceArg.Chip(policyName(m, n)) {
                    onEditingInitialPolicyChange(false)
                    onShowEditPolicyBottomSheetChange(true)
                },
            ),
        )
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
    onEditingInitialPolicyChange: (Boolean) -> Unit,
    onShowEditPolicyBottomSheetChange: (Boolean) -> Unit,
    onShowEditTimelockBottomSheetChange: (Boolean) -> Unit
) {
    SpendingRulesCard {
        ChipSentence(
            template = stringResource(R.string.nc_miniscript_rule_relative_initial),
            args = listOf(
                SentenceArg.Chip(timeLockText) { onShowEditTimelockBottomSheetChange(true) },
                SentenceArg.Chip(policyName(m, n)) {
                    onEditingInitialPolicyChange(true)
                    onShowEditPolicyBottomSheetChange(true)
                },
            ),
        )
        AfterTimelockSentence(
            m = m,
            n = n,
            newM = newM,
            newN = newN,
            multisignType = multisignType,
            currentPolicyName = policyName(m, n),
            flexibleRes = R.string.nc_miniscript_rule_relative_after_flexible,
            expandingRes = R.string.nc_miniscript_rule_relative_after_expanding,
            expandingSinglesigRes = R.string.nc_miniscript_rule_relative_after_expanding_singlesig,
            decayingRes = R.string.nc_miniscript_rule_relative_after_decaying,
            decayingSinglesigRes = R.string.nc_miniscript_rule_relative_after_decaying_singlesig,
            onEditNewPolicy = {
                onEditingInitialPolicyChange(false)
                onShowEditPolicyBottomSheetChange(true)
            },
        )
    }
}

@Composable
fun ZenHodlRelativeCard(
    m: Int,
    n: Int,
    timeLockText: String,
    onEditingInitialPolicyChange: (Boolean) -> Unit,
    onShowEditPolicyBottomSheetChange: (Boolean) -> Unit,
    onShowEditTimelockBottomSheetChange: (Boolean) -> Unit
) {
    SpendingRulesCard {
        ChipSentence(
            template = stringResource(R.string.nc_miniscript_rule_zen_relative_initial),
            args = listOf(SentenceArg.Chip(timeLockText) { onShowEditTimelockBottomSheetChange(true) }),
        )
        ChipSentence(
            template = stringResource(R.string.nc_miniscript_rule_zen_relative_after),
            args = listOf(
                SentenceArg.Chip(policyName(m, n)) {
                    onEditingInitialPolicyChange(false)
                    onShowEditPolicyBottomSheetChange(true)
                },
            ),
        )
    }
}

@Composable
private fun policyName(m: Int, n: Int): String =
    if (n == 1 && m == 1) {
        stringResource(R.string.nc_miniscript_singlesig)
    } else {
        stringResource(R.string.nc_miniscript_m_of_n_multisig, m, n)
    }

@Composable
private fun policyNameNb(m: Int, n: Int): String =
    if (n == 1 && m == 1) {
        stringResource(R.string.nc_miniscript_singlesig)
    } else {
        stringResource(R.string.nc_miniscript_m_of_n_multisig_nb, m, n)
    }

@PreviewLightDark
@Composable
private fun MiniscriptConfigTemplateScreenPreview() {
    NunchukTheme {
        MiniscriptConfigTemplateScreen(
            currentBlockHeight = 850000,
            multisignType = MultisignType.ZEN_HODL
        )
    }
}