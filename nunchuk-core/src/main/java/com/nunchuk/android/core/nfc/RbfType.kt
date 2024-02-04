package com.nunchuk.android.core.nfc

@JvmInline
value class RbfType(val value: Int) {
    companion object {
        val ReplaceFee = RbfType(1)
        val CancelTransaction = RbfType(2)
        
        fun fromValue(value: Int): RbfType {
            return when (value) {
                1 -> ReplaceFee
                2 -> CancelTransaction
                else -> throw IllegalArgumentException("Unknown value $value")
            }
        }
    }
}