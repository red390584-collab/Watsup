package com.yourapp.whatsappclone.data

/** High-level connection status surfaced to the UI. */
sealed class ConnectionStatus {
    object Idle : ConnectionStatus()
    object NeedsLink : ConnectionStatus()
    object Connecting : ConnectionStatus()
    data class QrReady(val payload: String) : ConnectionStatus()
    object Linked : ConnectionStatus()
    data class Disconnected(val reason: String) : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}

/** State machine for the phone-number (OTP) linking flow. */
sealed class PhoneLinkState {
    object Idle : PhoneLinkState()
    object RequestingCode : PhoneLinkState()
    data class CodeRequested(val phoneNumber: String) : PhoneLinkState()
    object Verifying : PhoneLinkState()
    object Linked : PhoneLinkState()
    data class Error(val message: String) : PhoneLinkState()
}
