package com.socam.bcms.model

/**
 * 注意: UHFModuleType 使用廠商提供的 com.uhf.base.UHFModuleType
 * Note: UHFModuleType uses vendor-provided com.uhf.base.UHFModuleType
 * 
 * 這裡我們定義其他 UHF 相關的 enums / Here we define other UHF-related enums
 */

/**
 * 記憶體庫類型 / Memory Bank Type  
 * 
 * Kotlin 概念: 構造函數參數 / Constructor parameters
 * - val value: Int 同時宣告屬性和構造函數參數
 * - Declares both property and constructor parameter
 */
enum class MemoryBank(val value: Int) {
    RESERVED(0),    // 保留區 (存取密碼) / Reserved (access passwords)
    EPC(1),         // 電子產品碼 / Electronic Product Code  
    TID(2),         // 標籤識別碼 / Tag Identifier
    USER(3)         // 使用者資料 / User Data
}

/**
 * 掃描設定 / Scanning Settings
 */
data class ScanSettings(
    val power: Int = 33,           // 發射功率 / Transmission power
    val frequency: Int = 1,        // 頻率區域 / Frequency region 
    val sessionMode: Int = 0,      // 會話模式 / Session mode
    val readMode: Int = 0,         // 讀取模式 / Read mode
    val protocolType: Int = 1      // 協議類型 (1:ISO, 2:GB) / Protocol type
) {
    /**
     * 驗證設定值 / Validate settings
     * Kotlin 概念: when 表達式 (類似 switch，但更強大)
     * Kotlin Concept: when expression (like switch, but more powerful)
     */
    fun isValid(): Boolean {
        return when {
            power < 5 || power > 33 -> false        // 功率範圍檢查 / Power range check
            frequency < 0 || frequency > 3 -> false // 頻率範圍檢查 / Frequency range check
            sessionMode < 0 || sessionMode > 3 -> false
            else -> true                             // 其他情況都正確 / All other cases are valid
        }
    }
}

/**
 * 清單掃描結果 / Inventory Scan Result
 * 
 * Kotlin 概念: 資料類別用於統計 / Data class for statistics
 * - 自動產生 equals, hashCode, toString / Auto-generated equals, hashCode, toString  
 * - copy() 方法用於建立修改版本 / copy() method for creating modified versions
 */
data class InventoryResult(
    val uniqueTags: Int,
    val totalReads: Long, 
    val readRate: Long,
    val duration: Long
) {
    /**
     * 格式化顯示 / Formatted Display
     */
    fun getDisplayText(): String = 
        "標籤: $uniqueTags | 讀取: $totalReads | 速率: ${readRate}/s | 時間: ${duration/1000}s"
}
