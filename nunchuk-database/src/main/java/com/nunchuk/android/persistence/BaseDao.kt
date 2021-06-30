package com.nunchuk.android.persistence

import androidx.room.*

@Dao
interface BaseDao<in T> {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(item: T): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(items: List<T>): List<Long>

    @Update
    fun update(item: T): Int

    @Update
    fun update(items: List<T>): Int

    @Delete
    fun delete(item: T): Int

}

@Transaction
fun <T> BaseDao<T>.updateOrInsert(item: T): Long {
    val insertResult = insert(item)
    return if (insertResult == -1L) {
        val updateResult = update(item).toLong()
        updateResult
    } else insertResult
}

@Transaction
fun <T> BaseDao<T>.updateOrInsert(items: List<T>) {
    val insertResults = insert(items)
    val updates = insertResults.indices
        .filter { insertResults[it] == -1L }
        .map { items[it] }
    if (updates.isNotEmpty()) {
        update(updates)
    }
}