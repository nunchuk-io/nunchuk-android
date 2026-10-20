package com.nunchuk.android.settings.displaysettings

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcRadioButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.showNunchukSnackbar
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.locale.AppLanguage
import com.nunchuk.android.core.locale.AppLanguageManager
import com.nunchuk.android.settings.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.Normalizer
import java.util.Locale

/**
 * Language picker. A selection applies immediately (AppCompat recreates the activity in the new
 * language) and the picker stays open; Back returns to Display settings.
 */
@Composable
fun LanguageScreen() {
    val context = LocalContext.current
    val appLocale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.ENGLISH
    var selected by remember { mutableStateOf(AppLanguageManager.selectedLanguage()) }
    val deviceLanguage = remember { AppLanguageManager.deviceLanguage(context) }
    val deviceLocale = remember { AppLanguageManager.deviceLocale(context) }
    // English names stay searchable whatever the app language is.
    val englishNames = remember(context) {
        val englishConfig = Configuration(context.resources.configuration).apply { setLocale(Locale.ENGLISH) }
        val englishResources = context.createConfigurationContext(englishConfig).resources
        AppLanguage.supported.associateWith { englishResources.getString(it.nameRes) }
    }
    val failedMessage = stringResource(R.string.nc_change_language_failed)
    val snackState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LanguageContent(
        selected = selected,
        deviceHelper = when {
            selected != null -> stringResource(
                R.string.nc_language_device_language,
                deviceLanguage?.let { stringResource(it.nameRes) } ?: deviceLocale.getDisplayName(appLocale)
            )

            deviceLanguage != null -> stringResource(
                R.string.nc_language_currently_using,
                stringResource(deviceLanguage.nameRes)
            )

            else -> stringResource(R.string.nc_language_device_fallback, deviceLocale.getDisplayName(appLocale))
        },
        englishNames = englishNames,
        snackState = snackState,
        onSelect = { language ->
            if (language == selected) return@LanguageContent
            keyboardController?.hide()
            focusManager.clearFocus()
            runCatching { AppLanguageManager.setLanguage(language) }
                .onSuccess { selected = language }
                .onFailure {
                    Timber.e(it)
                    coroutineScope.launch {
                        snackState.showNunchukSnackbar(message = failedMessage, type = NcToastType.ERROR)
                    }
                }
        },
    )
}

@Composable
private fun LanguageContent(
    selected: AppLanguage? = null,
    deviceHelper: String = "",
    englishNames: Map<AppLanguage, String> = emptyMap(),
    snackState: SnackbarHostState = remember { SnackbarHostState() },
    onSelect: (AppLanguage?) -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    val localizedNames = AppLanguage.supported.associateWith { stringResource(it.nameRes) }
    val languages = remember(query, localizedNames, englishNames) {
        val needle = query.normalizeForSearch()
        if (needle.isEmpty()) {
            AppLanguage.supported
        } else {
            AppLanguage.supported.filter { language ->
                listOfNotNull(language.nativeName, englishNames[language], localizedNames[language])
                    .any { it.normalizeForSearch().contains(needle) }
            }
        }
    }
    val listState = rememberLazyListState()

    // Keep the chosen language in view when the picker opens and after the activity is recreated.
    LaunchedEffect(Unit) {
        val index = AppLanguage.supported.indexOf(selected)
        if (index < 0) return@LaunchedEffect
        val visible = snapshotFlow { listState.layoutInfo.visibleItemsInfo }.first { it.isNotEmpty() }
        if (visible.none { it.index == index }) {
            listState.scrollToItem(index)
        }
    }

    NunchukTheme {
        NcScaffold(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            snackState = snackState,
            topBar = {
                NcTopAppBar(
                    title = stringResource(R.string.nc_language),
                    textStyle = NunchukTheme.typography.titleLarge
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                LanguageSearchField(
                    query = query,
                    onQueryChange = { query = it },
                )

                LanguageRow(
                    title = stringResource(R.string.nc_follow_device_language),
                    subtitle = deviceHelper,
                    selected = selected == null,
                    onClick = {
                        query = ""
                        onSelect(null)
                    },
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.strokePrimary,
                )

                if (languages.isEmpty()) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        text = stringResource(R.string.nc_no_languages_found),
                        style = NunchukTheme.typography.body.copy(color = MaterialTheme.colorScheme.textSecondary),
                        textAlign = TextAlign.Center,
                    )
                } else {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        items(languages, key = { it.name }) { language ->
                            LanguageRow(
                                title = language.nativeName,
                                subtitle = localizedNames[language]?.takeIf { it != language.nativeName },
                                selected = selected == language,
                                onClick = {
                                    query = ""
                                    onSelect(language)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val clearSearchLabel = stringResource(R.string.nc_clear_search)
    NcTextField(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        title = "",
        value = query,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        placeholder = { Text(text = stringResource(R.string.nc_search_languages)) },
        rightContent = {
            if (query.isEmpty()) {
                NcIcon(
                    painter = painterResource(R.drawable.ic_search_dark),
                    contentDescription = null,
                )
            } else {
                NcIcon(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(role = Role.Button) { onQueryChange("") }
                        .semantics { contentDescription = clearSearchLabel },
                    painter = painterResource(R.drawable.ic_close_circle),
                    contentDescription = null,
                )
            }
        },
        onValueChange = onQueryChange,
    )
}

@Composable
private fun LanguageRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = NunchukTheme.typography.body)
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    text = subtitle,
                    style = NunchukTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.textSecondary),
                )
            }
        }
        // The row carries the selection semantics, so the button itself takes no click.
        NcRadioButton(selected = selected, onClick = null)
    }
}

/** Case- and accent-insensitive form, so "espanol" finds Español. */
private fun String.normalizeForSearch(): String =
    Normalizer.normalize(trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)

@PreviewLightDark
@Composable
private fun LanguageContentPreview() {
    LanguageContent(deviceHelper = "Currently using English")
}
