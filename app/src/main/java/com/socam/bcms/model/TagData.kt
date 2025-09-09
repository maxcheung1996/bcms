package com.socam.bcms.model

/**
 * RFID 標籤資料 / RFID Tag Data
 * 
 * Kotlin 概念解釋 / Kotlin Concepts Explained:
 * 1. data class: 資料類別，自動生成 equals, hashCode, toString
 *    data class: Data class, auto-generates equals, hashCode, toString
 *    
 * 2. val vs var: val 不可變，var 可變
 *    val vs var: val immutable, var mutable
 *    
 * 3. 預設參數: count = 1 提供預設值
 *    Default parameters: count = 1 provides default value
 *    
 * 4. 主要建構子參數同時是屬性
 *    Primary constructor parameters are also properties
 */
data class TagData(
    val tid: String,           // Tag ID - 標籤 ID
    val epc: String,           // Electronic Product Code - 電子產品碼  
    val rssi: Int,             // Received Signal Strength Indicator - 接收信號強度
    var count: Int = 1,        // Read count - 讀取次數 (var 因為會增加)
    val timestamp: Long = System.currentTimeMillis() // 時間戳記 / Timestamp
) {
    /**
     * 計算屬性 / Computed Property
     * 
     * Kotlin 概念: get() 自定義 getter
     * - 每次存取時重新計算 / Recalculated on each access
     */
    val displayRssi: String
        get() = "$rssi dBm"

    /**
     * 範例：自定義 toString / Example: Custom toString
     */
    override fun toString(): String {
        return "EPC: $epc, TID: $tid, RSSI: $displayRssi, Count: $count"
    }
    
    /**
     * 用於 ListView 顯示 / For ListView display
     */
    fun getDisplayText(): String {
        return "EPC: $epc | RSSI: $displayRssi | Count: $count"
    }
}