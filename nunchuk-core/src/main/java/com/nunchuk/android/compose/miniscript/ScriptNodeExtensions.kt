package com.nunchuk.android.compose.miniscript

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.nunchuk.android.core.R
import com.nunchuk.android.core.miniscript.ScriptNodeType
import com.nunchuk.android.model.ScriptNode
import com.nunchuk.android.model.TimeLock
import com.nunchuk.android.type.MiniscriptTimelockBased
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.ceil

private fun Long.toPluralCount(): Int = coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()

val ScriptNode.displayName: String
    @Composable
    get() = when (this.type) {
        ScriptNodeType.PK.name -> "PK"
        ScriptNodeType.OLDER.name -> {
            val timeLock = this.timeLock ?: TimeLock()
            when {
                timeLock.isTimestamp() -> {
                    // Convert seconds to days, hours, minutes format
                    val totalSeconds = timeLock.value
                    val days = totalSeconds / (24 * 60 * 60)
                    val hours = (totalSeconds % (24 * 60 * 60)) / (60 * 60)
                    val minutes = (totalSeconds % (60 * 60)) / 60

                    val parts = mutableListOf<String>()
                    if (days > 0) {
                        parts.add(stringResource(R.string.nc_miniscript_unit_days_short, days))
                    }
                    if (hours > 0) {
                        parts.add(stringResource(R.string.nc_miniscript_unit_hours_short, hours))
                    }
                    if (minutes > 0) {
                        parts.add(stringResource(R.string.nc_miniscript_unit_minutes_short, minutes))
                    }

                    if (parts.isEmpty()) {
                        stringResource(
                            R.string.nc_miniscript_after_value,
                            stringResource(R.string.nc_miniscript_unit_days_short, 0)
                        )
                    } else {
                        stringResource(R.string.nc_miniscript_after_value, parts.joinToString(" "))
                    }
                }

                else -> { // block height
                    val formattedBlocks = String.format("%,d", timeLock.value)
                    pluralStringResource(
                        R.plurals.nc_miniscript_after_blocks,
                        timeLock.value.toPluralCount(),
                        formattedBlocks
                    )
                }
            }
        }

        ScriptNodeType.AFTER.name -> {
            val timeLock = this.timeLock ?: TimeLock()
            when {
                timeLock.isTimestamp() -> {
                    val targetDate = calculateDateFromSeconds(timeLock.value)
                    stringResource(R.string.nc_miniscript_after_value, targetDate)
                }

                else -> { // block height
                    val formattedBlocks = String.format("%,d", timeLock.value)
                    if (timeLock.value == 1L) {
                        pluralStringResource(R.plurals.nc_miniscript_after_blocks, 1, formattedBlocks)
                    } else {
                        stringResource(R.string.nc_miniscript_after_block_height, formattedBlocks)
                    }
                }
            }
        }

        ScriptNodeType.HASH160.name -> "HASH160"
        ScriptNodeType.HASH256.name -> "HASH256"
        ScriptNodeType.RIPEMD160.name -> "RIPEMD160"
        ScriptNodeType.SHA256.name -> "SHA256"
        ScriptNodeType.AND.name -> "AND"
        ScriptNodeType.OR.name -> "OR"
        ScriptNodeType.ANDOR.name -> "AND OR"
        ScriptNodeType.THRESH.name -> stringResource(
            R.string.nc_miniscript_thresh_name,
            this.k.toString(),
            this.subs.size.toString()
        )
        ScriptNodeType.MULTI.name -> stringResource(
            R.string.nc_miniscript_multisig_name,
            this.k.toString(),
            this.keys.size.toString()
        )
        ScriptNodeType.OR_TAPROOT.name -> "OR"
        ScriptNodeType.MUSIG.name -> "MuSig"
        else -> stringResource(R.string.nc_unknown)
    }

val ScriptNode.descriptionText: String
    @Composable
    get() = when (this.type) {
        ScriptNodeType.PK.name -> stringResource(R.string.nc_miniscript_public_key)
        ScriptNodeType.OLDER.name -> stringResource(R.string.nc_miniscript_older_desc)
        ScriptNodeType.AFTER.name -> {
            val timeLock = this.timeLock ?: TimeLock()
            when {
                timeLock.isTimestamp() -> {
                    val currentTimeSeconds = System.currentTimeMillis() / 1000
                    val diff = timeLock.value - currentTimeSeconds
                    when {
                        diff <= 0 -> ""
                        diff >= 86400 -> {
                            val daysFromNow = ceil(diff / 86400.0).toInt()
                            stringResource(
                                R.string.nc_miniscript_from_today,
                                pluralStringResource(R.plurals.nc_day, daysFromNow, daysFromNow)
                            )
                        }
                        else -> {
                            val totalMinutes = ceil(diff / 60.0).toInt()
                            val hours = totalMinutes / 60
                            val minutes = totalMinutes % 60

                            if (hours >= 24) {
                                stringResource(
                                    R.string.nc_miniscript_from_today,
                                    pluralStringResource(R.plurals.nc_day, 1, 1)
                                )
                            } else {
                                val parts = mutableListOf<String>()
                                if (hours > 0) {
                                    parts.add(pluralStringResource(R.plurals.nc_plural_hour, hours, hours.toString()))
                                }
                                if (minutes > 0) {
                                    parts.add(pluralStringResource(R.plurals.nc_plural_minute, minutes, minutes.toString()))
                                }

                                if (parts.isEmpty()) {
                                    ""
                                } else {
                                    stringResource(R.string.nc_miniscript_from_now, parts.joinToString(" "))
                                }
                            }
                        }
                    }
                }

                else -> { // block height - provide a generic description since we don't have currentBlockHeight
                    stringResource(R.string.nc_miniscript_after_block_desc)
                }
            }
        }

        ScriptNodeType.HASH160.name -> stringResource(R.string.nc_miniscript_hash_desc, "HASH160")
        ScriptNodeType.HASH256.name -> stringResource(R.string.nc_miniscript_hash_desc, "SHA256")
        ScriptNodeType.RIPEMD160.name -> stringResource(R.string.nc_miniscript_hash_desc, "RIPEMD160")
        ScriptNodeType.SHA256.name -> stringResource(R.string.nc_miniscript_hash_desc, "SHA256")
        ScriptNodeType.AND.name -> stringResource(R.string.nc_miniscript_and_desc)
        ScriptNodeType.OR.name -> stringResource(R.string.nc_miniscript_or_desc)
        ScriptNodeType.ANDOR.name -> stringResource(R.string.nc_miniscript_andor_desc)
        ScriptNodeType.THRESH.name -> stringResource(R.string.nc_miniscript_thresh_desc)
        ScriptNodeType.MULTI.name -> stringResource(R.string.nc_miniscript_multi_desc)
        ScriptNodeType.OR_TAPROOT.name -> stringResource(R.string.nc_miniscript_or_taproot_desc)
        ScriptNodeType.MUSIG.name -> stringResource(R.string.nc_miniscript_musig_desc)
        else -> ""
    }

@Composable
fun ScriptNode.getAfterBlockDescription(currentBlockHeight: Int): String {
    val timeLock = this.timeLock ?: TimeLock()
    return if (this.type == ScriptNodeType.AFTER.name && !timeLock.isTimestamp()) {
        val blockDiff = timeLock.value - currentBlockHeight
        // Don't display description if blockDiff is less than 0
        if (blockDiff < 0) {
            ""
        } else {
            val formattedBlockDiff = String.format("%,d", blockDiff)
            pluralStringResource(
                R.plurals.nc_miniscript_blocks_from_current_block,
                blockDiff.toPluralCount(),
                formattedBlockDiff
            )
        }
    } else {
        this.descriptionText
    }
}

private fun calculateDateFromSeconds(timestampSeconds: Long): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestampSeconds * 1000

    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)

    val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
    val dateString = dateFormat.format(calendar.time)

    // Only show time if hour and minute are not both 0
    return if (hour == 0 && minute == 0) {
        dateString
    } else {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeString = timeFormat.format(calendar.time)
        "$dateString $timeString"
    }
}

fun TimeLock.isTimestamp(): Boolean {
    return this.based == MiniscriptTimelockBased.TIME_LOCK
}

fun analyzeMiniscriptForTimelocks(scriptNode: ScriptNode): TimelockInfo {
    var hasRelativeTimelock = false
    var hasAbsoluteTimelock = false

    // Use stack instead of recursion
    val stack = mutableListOf<ScriptNode>()
    stack.add(scriptNode)

    while (stack.isNotEmpty()) {
        val node = stack.removeAt(stack.size - 1)

        when (node.type) {
            ScriptNodeType.OLDER.name -> {
                hasRelativeTimelock = true
            }

            ScriptNodeType.AFTER.name -> {
                hasAbsoluteTimelock = true
            }
        }

        // Add sub-nodes to stack for processing
        stack.addAll(node.subs)
    }

    return TimelockInfo(
        hasRelativeTimelock = hasRelativeTimelock,
        hasAbsoluteTimelock = hasAbsoluteTimelock,
    )
}