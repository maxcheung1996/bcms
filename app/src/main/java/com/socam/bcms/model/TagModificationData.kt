package com.socam.bcms.model

/**
 * Data class for Tag Modification scan results
 * Contains all tag data including EPC content for status modification
 */
data class TagModificationData(
    val tid: String,
    val epc: String, 
    val rssiRaw: String,
    val rssiDbm: Int,
    val epcData: String?,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Format RSSI for display with both raw and dBm values
     */
    fun getFormattedRssi(): String {
        return "Raw: $rssiRaw | dBm: ${rssiDbm}"
    }
    
    /**
     * Check if this tag has stronger signal than another
     * Higher RSSI (less negative) = stronger signal = closer distance
     */
    fun isStrongerThan(other: TagModificationData): Boolean {
        return this.rssiDbm > other.rssiDbm
    }
    
    /**
     * Format EPC data for display
     */
    fun getFormattedEpcData(): String {
        return epcData ?: "No EPC data"
    }
    
    /**
     * Get the first 2 characters of EPC for status detection
     */
    fun getEpcStatus(): String {
        return (epcData ?: epc).replace(" ", "").take(2).uppercase()
    }
    
    /**
     * Determine if tag is Active or Inactive based on EPC prefix
     * Active: EPC starts with "34"
     * Removed: EPC starts with "00"
     * Inactive: EPC starts with neither "34" nor "00"
     */
    fun getTagStatus(): TagStatus {
        val epcToCheck = (epcData ?: epc).uppercase().replace(" ", "")
        return when {
            epcToCheck.startsWith("34") -> TagStatus.ACTIVE
            else -> TagStatus.INACTIVE // Starts with 00 or neither = Inactive
        }
    }
    
    /**
     * Get status display name with color info
     */
    fun getStatusDisplayInfo(): StatusDisplayInfo {
        return when (getTagStatus()) {
            TagStatus.ACTIVE -> StatusDisplayInfo("ACTIVE", "#4CAF50") // Green
            TagStatus.INACTIVE -> StatusDisplayInfo("INACTIVE", "#F44336") // Red
        }
    }
}

/**
 * Tag status enumeration for filtering
 */
enum class TagStatus {
    ACTIVE,   // Starts with "34"
    INACTIVE  // Starts with "00" (REMOVED) or neither "34" nor "00"
}

/**
 * Scan mode selection for Tag Modification screen
 */
enum class ScanMode {
    SINGLE,   // Single scan mode with write functionality
    MULTIPLE  // Multiple scan mode with read-only list view
}

/**
 * Status display information with color
 */
data class StatusDisplayInfo(
    val displayName: String,
    val colorHex: String
)

/**
 * Tag status options for EPC modification
 * Using first 2 hex characters (1 byte) to store status
 */
object TagStatusOptions {
    const val ACTIVATED_PREFIX = "34"        // "34" prefix for active tags
    const val REMOVED_PREFIX = "00"          // "00" prefix for removed tags
    
    val OPTIONS = listOf(
        TagStatusOption("ACTIVE", ACTIVATED_PREFIX, "Tag is activated and in use (34 prefix)"),
        TagStatusOption("REMOVE", REMOVED_PREFIX, "Tag is removed from service (00 prefix)")
    )
}

/**
 * Single tag status option
 */
data class TagStatusOption(
    val displayName: String,
    val hexValue: String, 
    val description: String
)

/**
 * UI state for Tag Modification screen
 */
data class TagModificationUiState(
    val powerLevel: Int = 20,
    val isScanning: Boolean = false,
    val isTriggerPressed: Boolean = false,
    val statusMessage: String = "Ready to scan. Press and hold trigger to scan.",
    
    // Scan mode selection
    val currentScanMode: ScanMode = ScanMode.SINGLE,
    
    // Single Scan mode data (independent)
    val lastScanResult: TagModificationData? = null,
    val selectedStatusOption: TagStatusOption? = null,
    val isWriting: Boolean = false,
    val canWrite: Boolean = false, // True when tag is scanned and trigger is released
    
    // Multiple Scan mode data (independent)
    val multipleScanResults: List<TagModificationData> = emptyList(),
    
    // Shared state
    val errorMessage: String? = null,
    val needsFocusRestore: Boolean = false, // Signal Fragment to restore focus after operations
    
    // Real-time filtering options (apply to both modes)
    val showActiveFilter: Boolean = true, // Show Active tags (starts with 34)
    val showInactiveFilter: Boolean = true // Show Inactive tags (starts with 00 or neither 34/00)
)
