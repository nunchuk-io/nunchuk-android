package com.nunchuk.android.main.membership.byzantine.healthcheck

sealed class HealthCheckEvent

data class HealthCheckState(val loading: Boolean = false, val error: String = "")