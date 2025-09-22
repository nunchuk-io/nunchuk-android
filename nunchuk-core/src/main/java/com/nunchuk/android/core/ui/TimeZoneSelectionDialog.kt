package com.nunchuk.android.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.R
import java.util.TimeZone

@Composable
fun TimeZoneSelectionDialog(
    onDismissRequest: () -> Unit,
    onTimeZoneSelected: (TimeZoneDetail) -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            var search by remember { mutableStateOf("") }
            var timeZones by remember { mutableStateOf<List<TimeZoneDetail>>(emptyList()) }
            var filteredTimeZones by remember { mutableStateOf<List<TimeZoneDetail>>(emptyList()) }

            LaunchedEffect(Unit) {
                val zones = TimeZone.getAvailableIDs().mapNotNull { id ->
                    id.toTimeZoneDetail()
                }.sortedBy { it.offset }
                    
                timeZones = zones
                filteredTimeZones = zones
            }

            LaunchedEffect(search) {
                filteredTimeZones = if (search.isEmpty()) {
                    timeZones
                } else {
                    timeZones.filter { timeZone ->
                        timeZone.city.contains(search)
                                || timeZone.country.contains(search)
                                || timeZone.offset.contains(search)
                    }
                }
            }

            NcTopAppBar(
                title = "Select Time zone",
                textStyle = NunchukTheme.typography.title,
            )

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 16.dp)
                    .scale(scaleX = 1f, scaleY = 0.9f),
                value = search,
                onValueChange = {
                    search = it
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

            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredTimeZones) { timeZone ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onTimeZoneSelected(timeZone)
                            }
                            .padding(vertical = 8.dp, horizontal = 16.dp),
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
} 