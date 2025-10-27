package com.socam.bcms.data.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO for BC Type Serial Number from API
 * Used during login to sync latest serial numbers for each BC type
 */
data class BCTypeSerialNumberDto(
    @SerializedName("bcType")
    val bcType: String,
    
    @SerializedName("bcTypeCode")
    val bcTypeCode: String,
    
    @SerializedName("latestSerialNumber")
    val latestSerialNumber: String // 4-digit format: "0001", "0123", etc.
)

/**
 * Response wrapper for BC Type Serial Numbers API
 */
data class BCTypeSerialNumbersResponse(
    @SerializedName("tagNumbers")
    val tagNumbers: List<BCTypeSerialNumberDto>
)

