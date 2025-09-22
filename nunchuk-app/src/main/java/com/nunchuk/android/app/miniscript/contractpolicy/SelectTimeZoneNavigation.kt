package com.nunchuk.android.app.miniscript.contractpolicy

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.schedule.timezone.SelectTimeZoneViewModel
import com.nunchuk.android.core.ui.TimeZoneDetail
import kotlinx.serialization.Serializable

@Serializable
object MiniscriptSelectTimeZone

fun NavGraphBuilder.miniscriptSelectTimeZoneDestination(
    navController: NavController
) {
    composable<MiniscriptSelectTimeZone> {
        val viewModel = hiltViewModel<SelectTimeZoneViewModel>()

        SelectTimeZoneScreen(
            viewModel = viewModel,
            navController = navController
        )
    }
}

@Composable
fun SelectTimeZoneScreen(
    viewModel: SelectTimeZoneViewModel = hiltViewModel(),
    navController: NavController
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val onBackPressDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    // Handle back press
    androidx.compose.runtime.LaunchedEffect(Unit) {
        onBackPressDispatcher?.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navController.popBackStack()
            }
        })
    }

    NunchukTheme {
        Scaffold(
            topBar = {
                NcTopAppBar(
                    title = "Select Timezone"
                )
            }
        ) { innerPadding ->
            SelectTimeZoneContent(
                timezones = state.timezones,
                onSearch = viewModel::onSearch,
                onTimeZoneClicked = { timeZone ->
                    // Save the selected timezone to navigation state
                    navController.currentBackStackEntry?.savedStateHandle?.set("selectedTimeZone", timeZone)
                    navController.popBackStack()
                },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
fun SelectTimeZoneContent(
    timezones: List<TimeZoneDetail> = emptyList(),
    onSearch: (value: String) -> Unit = {},
    onTimeZoneClicked: (zone: TimeZoneDetail) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var search by remember { mutableStateOf(TextFieldValue("")) }

    Column(modifier = modifier) {
        // Search field
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .scale(scaleX = 1f, scaleY = 0.9f),
            value = search,
            onValueChange = {
                search = it
                onSearch(it.text)
            },
            placeholder = {
                Text(
                    text = "Search time zone",
                    style = NunchukTheme.typography.body.copy(
                        color = colorResource(id = R.color.nc_boulder_color)
                    )
                )
            },
            shape = RoundedCornerShape(44.dp),
        )

        // Timezone list
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(timezones) { timeZone ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTimeZoneClicked(timeZone) }
                        .padding(16.dp)
                ) {
                    Text(
                        text = timeZone.city,
                        style = NunchukTheme.typography.body
                    )
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = timeZone.country,
                        style = NunchukTheme.typography.bodySmall
                    )
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = timeZone.offset,
                        style = NunchukTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun SelectTimeZoneScreenPreview() {
    SelectTimeZoneContent()
}