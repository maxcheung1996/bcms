package com.socam.bcms.data.dto

/**
 * Request DTO for fetching device serial number from backend
 */
data class SerialNumberRequest(
    val device_mac_address: String,
    val user_id: String,
    val project_id: String
)

/**
 * Response DTO for serial number from backend
 */
data class SerialNumberResponse(
    val serial_number: String,  // Format: "0001" to "9999"
    val success: Boolean,
    val message: String? = null
)

