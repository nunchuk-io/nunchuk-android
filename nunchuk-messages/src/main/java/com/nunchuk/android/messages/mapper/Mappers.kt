package com.nunchuk.android.messages.mapper

import com.nunchuk.android.messages.api.UserResponse

fun List<UserResponse>.toEntities() {
    map(UserResponse::toEntity)
}

fun UserResponse.toEntity() {
}
