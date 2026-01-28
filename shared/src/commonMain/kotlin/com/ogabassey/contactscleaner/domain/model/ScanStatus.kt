package com.ogabassey.contactscleaner.domain.model

sealed class ScanStatus {
    data class Progress(val progress: Float, val message: String? = null) : ScanStatus()
    data class Success(val result: ScanResult) : ScanStatus()
    data class Error(val message: String) : ScanStatus()
}
