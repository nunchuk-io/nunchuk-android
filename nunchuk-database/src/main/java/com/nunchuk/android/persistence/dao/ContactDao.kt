package com.nunchuk.android.persistence.dao

import androidx.room.Dao
import com.nunchuk.android.persistence.BaseDao
import com.nunchuk.android.persistence.entity.ContactEntity

@Dao
interface ContactDao : BaseDao<ContactEntity>
