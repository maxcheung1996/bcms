package com.socam.bcms.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Request DTO for fetching BC Type Serial Numbers from backend
 * POST SerialNo/Latest/
 */
data class BCTypeSerialNumberRequest(
    @SerializedName("ProjId")
    val projId: String,
    
    @SerializedName("DeviceId")
    val deviceId: String,
    
    @SerializedName("Username")
    val username: String
)

/**
 * Response DTO for BC Type Serial Number from API
 * Used during login to sync latest serial numbers for each BC type
 */
data class BCTypeSerialNumberDto(
    @SerializedName("BCType")
    val bcType: String,              // "MIC", "ALW", "TID"
    
    @SerializedName("ProjId")
    val projId: String,              // Project ID
    
    @SerializedName("GunNum")
    val gunNum: String,              // Gun number (maps to Device ID)
    
    @SerializedName("SerialNo")
    val serialNo: String             // 4-digit format: "0001", "0123", etc.
)

/**
 * Response list for BC Type Serial Numbers API
 * Returns array of BC type serial numbers
 */
typealias BCTypeSerialNumbersResponse = List<BCTypeSerialNumberDto>

