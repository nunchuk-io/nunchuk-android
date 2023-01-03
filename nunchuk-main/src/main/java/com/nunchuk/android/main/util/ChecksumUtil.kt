/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.main.util

import org.apache.commons.codec.DecoderException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


object ChecksumUtil {
    const val ALGORITHM = "SHA-256"
    @Throws(NoSuchAlgorithmException::class)
    fun getChecksum(bytes: ByteArray?): String {
        val digest = MessageDigest.getInstance(ALGORITHM)
        val encodedhash = digest.digest(bytes)
        return bytesToHex(encodedhash)
    }

    fun verifyChecksum(bytes: ByteArray?, checksum: String): Boolean {
        return try {
            val digest = MessageDigest.getInstance(ALGORITHM)
            val encodedhash = digest.digest(bytes)
            MessageDigest.isEqual(encodedhash, decodeHex(checksum.toCharArray()))
        } catch (ex: Exception) {
            false
        }
    }

    private fun bytesToHex(hash: ByteArray): String {
        val hexString = StringBuilder(2 * hash.size)
        for (i in hash.indices) {
            val hex = Integer.toHexString(0xff and hash[i].toInt())
            if (hex.length == 1) {
                hexString.append('0')
            }
            hexString.append(hex)
        }
        return hexString.toString()
    }

    @Throws(DecoderException::class)
    fun decodeHex(data: CharArray): ByteArray {
        val out = ByteArray(data.size shr 1)
        val outOffset = 0
        val len = data.size
        if (len and 0x01 != 0) {
            throw DecoderException("Odd number of characters.")
        }
        val outLen = len shr 1
        if (out.size - outOffset < outLen) {
            throw DecoderException("Output array is not large enough to accommodate decoded data.")
        }

        // two characters form the hex value.
        var i = outOffset
        var j = 0
        while (j < len) {
            var f = toDigit(data[j], j) shl 4
            j++
            f = f or toDigit(data[j], j)
            j++
            out[i] = (f and 0xFF).toByte()
            i++
        }
        return out
    }

    @Throws(DecoderException::class)
    internal fun toDigit(ch: Char, index: Int): Int {
        val digit = ch.digitToIntOrNull(16) ?: -1
        if (digit == -1) {
            throw DecoderException("Illegal hexadecimal character $ch at index $index")
        }
        return digit
    }
}