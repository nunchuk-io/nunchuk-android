package com.nunchuk.android.messages.model

data class Contact(val name: String, val email: String)

object ContactsProvider {
    fun contacts() = listOf(
        Contact("Hugo Nguyen", "hugonguyen@gmail.com"),
        Contact("Khoa Pham", "khoapham@gmail.com"),
        Contact("Peter Schiff", "peterschiff@gmail.com")
    )
}