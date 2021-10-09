package com.nunchuk.android.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import com.nunchuk.android.persistence.BaseDao
import com.nunchuk.android.persistence.TABLE_CONTACT
import com.nunchuk.android.persistence.entity.ContactEntity
import io.reactivex.Flowable

@Dao
interface ContactDao : BaseDao<ContactEntity> {

    @Query("SELECT * FROM $TABLE_CONTACT WHERE account_id = :accountId")
    fun getContacts(accountId: String): Flowable<List<ContactEntity>>

    @Query("DELETE FROM $TABLE_CONTACT WHERE id IN (:contactIds) AND account_id = :accountId")
    fun deleteItems(accountId: String, contactIds: List<String>)

}
