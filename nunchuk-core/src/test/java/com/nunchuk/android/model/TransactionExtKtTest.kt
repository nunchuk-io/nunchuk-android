package com.nunchuk.android.model

import com.nunchuk.android.type.TransactionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

internal class TransactionExtKtTest {

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
        assertEquals(TransactionStatus.REPLACED,sortedList[5].status)

        // 7th
        assertEquals("TX_CONFIRMED_3", sortedList[6].txId)
        assertEquals(TransactionStatus.CONFIRMED,sortedList[6].status)

        // 8th
        assertEquals("TX_CONFIRMED_2", sortedList[7].txId)
        assertEquals(TransactionStatus.CONFIRMED,sortedList[7].status)

        // 9th
        assertEquals("TX_REPLACED_1", sortedList[8].txId)
        assertEquals(TransactionStatus.REPLACED, sortedList[8].status)

        // 10th
        assertEquals("TX_CONFIRMED_1", sortedList[9].txId)
        assertEquals(TransactionStatus.CONFIRMED, sortedList[9].status)

    }
}