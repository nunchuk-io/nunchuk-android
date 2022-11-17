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

package com.nunchuk.android.core.util

import com.nunchuk.android.model.Transaction
import com.nunchuk.android.type.TransactionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionExtensionsTest {

    @Test
    fun testTransactionSorted() {
        val listTransaction = listOf(
            Transaction(
                txId = "TX_CONFIRMED_1",
                blockTime = System.currentTimeMillis() - 1000,
                status = TransactionStatus.CONFIRMED
            ),
            Transaction(
                txId = "TX_READY_BROADCAST",
                status = TransactionStatus.READY_TO_BROADCAST
            ),
            Transaction(
                txId = "TX_PENDING_CONFIRMATION",
                status = TransactionStatus.PENDING_CONFIRMATION
            ),
            Transaction(
                txId = "TX_REPLACED_1",
                replacedByTxid = "TX_CONFIRMED_2",
                status = TransactionStatus.REPLACED
            ),
            Transaction(
                txId = "TX_PENDING_SIGNATURES",
                status = TransactionStatus.PENDING_SIGNATURES
            ),
            Transaction(
                txId = "TX_CONFIRMED_2",
                blockTime = System.currentTimeMillis() - 900,
                status = TransactionStatus.CONFIRMED
            ),
            Transaction(
                txId = "TX_CONFIRMED_3",
                blockTime = System.currentTimeMillis() - 800,
                status = TransactionStatus.CONFIRMED
            ),
            Transaction(
                txId = "TX_REPLACED_2",
                replacedByTxid = "TX_CONFIRMED_4",
                blockTime = System.currentTimeMillis() - 2000,
                status = TransactionStatus.REPLACED
            ),
            Transaction(
                txId = "TX_CONFIRMED_4",
                blockTime = System.currentTimeMillis() - 700,
                status = TransactionStatus.CONFIRMED
            ),
            Transaction(
                txId = "TX_NETWORK_REJECTED",
                status = TransactionStatus.NETWORK_REJECTED
            )
        )

        val sortedList = listTransaction.sorted()

        // 1st
        assertEquals(TransactionStatus.PENDING_SIGNATURES, sortedList.first().status)

        // 2nd
        assertEquals(TransactionStatus.READY_TO_BROADCAST, sortedList[1].status)

        // 3rd
        assertEquals(TransactionStatus.NETWORK_REJECTED, sortedList[2].status)

        // 4th
        assertEquals(TransactionStatus.PENDING_CONFIRMATION, sortedList[3].status)

        // 5th
        assertEquals("TX_CONFIRMED_4", sortedList[4].txId)
        assertEquals(TransactionStatus.CONFIRMED, sortedList[4].status)

        // 6th
        assertEquals("TX_REPLACED_2", sortedList[5].txId)
        assertEquals(TransactionStatus.REPLACED, sortedList[5].status)

        // 7th
        assertEquals("TX_CONFIRMED_3", sortedList[6].txId)
        assertEquals(TransactionStatus.CONFIRMED, sortedList[6].status)

        // 8th
        assertEquals("TX_CONFIRMED_2", sortedList[7].txId)
        assertEquals(TransactionStatus.CONFIRMED, sortedList[7].status)

        // 9th
        assertEquals("TX_REPLACED_1", sortedList[8].txId)
        assertEquals(TransactionStatus.REPLACED, sortedList[8].status)

        // 10th
        assertEquals("TX_CONFIRMED_1", sortedList[9].txId)
        assertEquals(TransactionStatus.CONFIRMED, sortedList[9].status)
    }

    @Test
    fun testTruncatedAddress() {
        assertEquals("", "".truncatedAddress())
        assertEquals("37NF", "37NF".truncatedAddress())
        assertEquals("37NFX8K", "37NFX8K".truncatedAddress())
        assertEquals("37NFX...zbyZ", "37NFX8KWAQbaodUG6pE1hNUH1dXgkpzbyZ".truncatedAddress())
    }

}