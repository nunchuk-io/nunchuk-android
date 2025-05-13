package com.nunchuk.android.core.util

object CurrencyUtil {
    fun convertCurrency(
        amount: Double,
        fromCurrency: String,
        toCurrency: String,
        rates: Map<String, Double>
    ): Double {
        val rateFrom = rates[fromCurrency]
        val rateTo = rates[toCurrency]

        if (rateFrom == null || rateTo == null || rateFrom == 0.0) {
            return 0.0
        }

        val amountInUSD = amount / rateFrom
        return amountInUSD * rateTo
    }
}