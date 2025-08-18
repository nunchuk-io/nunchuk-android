package com.nunchuk.android.compose.miniscript

import com.nunchuk.android.core.miniscript.ScriptNodeType
import com.nunchuk.android.model.ScriptNode
import com.nunchuk.android.model.TimeLock
import com.nunchuk.android.type.MiniscriptTimelockBased
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.ceil

val ScriptNode.displayName: String
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
                        parts.add("${days}d")
                    }
                    if (hours > 0) {
                        parts.add("${hours}h")
                    }
                    if (minutes > 0) {
                        parts.add("${minutes}m")
                    }
                    
                    if (parts.isEmpty()) {
                        "After 0d"
                    } else {
                        "After ${parts.joinToString(" ")}"
                    }
                }
                else -> { // block height
                    val formattedBlocks = String.format("%,d", timeLock.value)
                    if (timeLock.value == 1L) "After 1 block" else "After $formattedBlocks blocks"
                }
            }
        }
        ScriptNodeType.AFTER.name -> {
            val timeLock = this.timeLock ?: TimeLock()
            when {
                timeLock.isTimestamp() -> {
                    val targetDate = calculateDateFromSeconds(timeLock.value)
                    "After $targetDate"
                }
                else -> { // block height
                    val formattedBlocks = String.format("%,d", timeLock.value)
                    if (timeLock.value == 1L) "After 1 block" else "After block $formattedBlocks"
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
        ScriptNodeType.THRESH.name -> "Thresh ${this.k}/${this.subs.size}"
        ScriptNodeType.MULTI.name -> "Multisig ${this.k}/${this.keys.size}"
        ScriptNodeType.OR_TAPROOT.name -> "OR"
        ScriptNodeType.MUSIG.name -> "MuSig"
        else -> "Unknown"
    }

val ScriptNode.descriptionText: String
    get() = when (this.type) {
        ScriptNodeType.PK.name -> "Public key"
        ScriptNodeType.OLDER.name -> "From the time the coins are received."
        ScriptNodeType.AFTER.name -> {
            val timeLock = this.timeLock ?: TimeLock()
            when {
                timeLock.isTimestamp() -> {
                    val currentTimeSeconds = System.currentTimeMillis() / 1000
                    val diff = timeLock.value - currentTimeSeconds
                    val daysFromNow = ceil(diff / 86400.0).toInt()
                    // Don't display description if daysFromNow is less than 0
                    if (daysFromNow <= 0) {
                        ""
                    } else {
                        val dayText = if (daysFromNow == 1) "day" else "days"
                        if (daysFromNow == 1) "1 $dayText from today." else "$daysFromNow $dayText from today."
                    }
                }
                else -> { // block height - provide a generic description since we don't have currentBlockHeight
                    "When the specified block height is reached."
                }
            }
        }
        ScriptNodeType.HASH160.name -> "Requires a preimage that hashes to a given value with HASH160"
        ScriptNodeType.HASH256.name -> "Requires a preimage that hashes to a given value with SHA256"
        ScriptNodeType.RIPEMD160.name -> "Requires a preimage that hashes to a given value with RIPEMD160"
        ScriptNodeType.SHA256.name -> "Requires a preimage that hashes to a given value with SHA256"
        ScriptNodeType.AND.name -> "Both conditions must be satisfied."
        ScriptNodeType.OR.name -> "Only one condition needs to be satisfied."
        ScriptNodeType.ANDOR.name -> " If the first condition is met, the second must also be met. If the first condition isn't met, the third must be met instead.\n(Note: Timelocks cannot revoke an earlier spend path.)"
        ScriptNodeType.THRESH.name -> "Requires M of N conditions."
        ScriptNodeType.MULTI.name -> "Requires M of N keys."
        ScriptNodeType.OR_TAPROOT.name -> "Only one tapscript needs to be satisfied."
        ScriptNodeType.MUSIG.name -> "Better privacy and lower fees. Requires all keys."
        else -> ""
    }

fun ScriptNode.getAfterBlockDescription(currentBlockHeight: Int): String {
    val timeLock = this.timeLock ?: TimeLock()
    return if (this.type == ScriptNodeType.AFTER.name && !timeLock.isTimestamp()) {
        val blockDiff = timeLock.value - currentBlockHeight
        // Don't display description if blockDiff is less than 0
        if (blockDiff < 0) {
            ""
        } else {
            val formattedBlockDiff = String.format("%,d", blockDiff)
            if (blockDiff == 1L) "1 block from the current block." else "$formattedBlockDiff blocks from the current block."
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
    
    val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
    val dateString = dateFormat.format(calendar.time)
    
    // Only show time if hour and minute are not both 0
    return if (hour == 0 && minute == 0) {
        dateString
    } else {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
        val timeString = timeFormat.format(calendar.time)
        "$dateString $timeString"
    }
}

fun TimeLock.isTimestamp(): Boolean {
    return this.based == MiniscriptTimelockBased.TIME_LOCK
} 