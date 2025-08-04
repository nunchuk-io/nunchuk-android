package com.nunchuk.android.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import java.util.TimeZone

data class SimpleTimeZone(
    val id: String,
    val displayName: String,
    val offsetText: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NcTimeZoneSelectDialog(
    sheetState: SheetState = rememberModalBottomSheetState(),
    onDismissRequest: () -> Unit,
    onTimeZoneSelected: (SimpleTimeZone) -> Unit,
) {
    var search by remember { mutableStateOf(TextFieldValue("")) }
    var timeZones by remember { mutableStateOf<List<SimpleTimeZone>>(emptyList()) }
    var filteredTimeZones by remember { mutableStateOf<List<SimpleTimeZone>>(emptyList()) }

    LaunchedEffect(Unit) {
        val zones = TimeZone.getAvailableIDs()
            .mapNotNull { id ->
                try {
                    val timeZone = TimeZone.getTimeZone(id)
                    val tokens = id.replace("_", " ").split("/")
                    if (tokens.size == 2) {
                        val offsetMinutes = timeZone.rawOffset / (1000 * 60)
                        val offsetHours = offsetMinutes / 60
                        val offsetMins = offsetMinutes % 60
                        val offsetText = if (offsetMins == 0) {
                            "GMT${if (offsetHours >= 0) "+" else ""}$offsetHours"
                        } else {
                            "GMT${if (offsetHours >= 0) "+" else ""}$offsetHours:${String.format("%02d", Math.abs(offsetMins))}"
                        }
                        
                        SimpleTimeZone(
                            id = id,
                            displayName = "${tokens[1].replace("_", " ")} (${tokens[0]})",
                            offsetText = offsetText
                        )
                    } else null
                } catch (e: Exception) {
                    null
                }
            }
            .sortedBy { it.displayName }
            
        timeZones = zones
        filteredTimeZones = zones
    }

    LaunchedEffect(search.text) {
        filteredTimeZones = if (search.text.isEmpty()) {
            timeZones
        } else {
            timeZones.filter { 
                it.displayName.contains(search.text, ignoreCase = true) ||
                it.offsetText.contains(search.text, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        onDismissRequest = onDismissRequest,
        tonalElevation = 0.dp,
        content = {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Select Timezone",
                    style = NunchukTheme.typography.title,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                NcTextField(
                    title = "Search",
                    value = search.text,
                    onValueChange = { search = search.copy(text = it) },
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filteredTimeZones) { timeZone ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    onTimeZoneSelected(timeZone)
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = timeZone.displayName,
                                style = NunchukTheme.typography.body
                            )
                            Text(
                                text = timeZone.offsetText,
                                style = NunchukTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        },
        dragHandle = { }
    )
}