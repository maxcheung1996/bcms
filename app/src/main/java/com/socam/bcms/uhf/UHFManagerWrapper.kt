package com.socam.bcms.uhf

import android.util.Log
import com.socam.bcms.model.*
import com.uhf.base.UHFManager
import com.uhf.base.UHFModuleType

/**
 * UHF 管理器包裝器 / UHF Manager Wrapper
 * 
 * Kotlin 概念解釋 / Kotlin Concepts Explained:
 * 1. class: Kotlin 類別宣告
 *    class: Kotlin class declaration
 *    
 * 2. private var: 私有可變屬性
 *    private var: Private mutable property
 *    
 * 3. 可空類型 ?: UHFManager? 表示可能是 null
 *    Nullable type: UHFManager? means can be null
 *    
 * 4. 安全呼叫 ?.: 如果物件不是 null 才呼叫方法  
 *    Safe call ?.: Only call method if object is not null
 */
class UHFManagerWrapper {
    
    // 私有屬性：包裝廠商的 UHF 管理器 / Private property: Wrap vendor's UHF manager
    private var uhfManager: UHFManager? = null
    private var isInitialized = false
    
    companion object {
        private const val TAG = "UHFManagerWrapper"  // Log 標籤 / Log tag
    }
    
    /**
     * 初始化 UHF 硬體 / Initialize UHF Hardware
     * 
     * Kotlin 概念: 函數回傳型別 / Function return type
     * - fun functionName(): ReturnType
     */
    fun initialize(moduleType: com.uhf.base.UHFModuleType): Boolean {
        return try {
            Log.d(TAG, "正在初始化 UHF 模組: $moduleType / Initializing UHF module: $moduleType")
            
            // 呼叫廠商 API / Call vendor API
            uhfManager = UHFManager.getUHFImplSigleInstance(moduleType)
            isInitialized = true
            
            Log.d(TAG, "UHF 管理器初始化成功 / UHF Manager initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "UHF 初始化失敗 / UHF initialization failed", e)
            false
        }
    }
    
    /**
     * 開啟 UHF 電源 / Power On UHF
     * 
     * Kotlin 概念: Elvis 運算子 ?: / Elvis operator ?:
     * - uhfManager?.powerOn() ?: false 
     * - 如果 uhfManager 是 null，回傳 false
     * - If uhfManager is null, return false
     */
    fun powerOn(): Boolean {
        Log.d(TAG, "開啟 UHF 電源 / Powering on UHF")
        return uhfManager?.powerOn() ?: false
    }
    
    /**
     * 關閉 UHF 電源 / Power Off UHF
     */
    fun powerOff(): Boolean {
        Log.d(TAG, "關閉 UHF 電源 / Powering off UHF")
        return uhfManager?.powerOff() ?: false
    }
    
    /**
     * 開始清單掃描 / Start Inventory Scanning
     */
    fun startInventory(): Boolean {
        Log.d(TAG, "開始 RFID 清單掃描 / Starting RFID inventory")
        return uhfManager?.startInventoryTag() ?: false
    }
    
    /**
     * 停止掃描 / Stop Scanning
     */
    fun stopInventory(): Boolean {
        Log.d(TAG, "停止 RFID 掃描 / Stopping RFID scanning")
        return uhfManager?.stopInventory() ?: false
    }
    
    /**
     * 從緩衝區讀取標籤資料 / Read Tag Data from Buffer
     * 
     * Kotlin 概念解釋 / Kotlin Concepts:
     * 1. let { }: 僅在物件不是 null 時執行區塊
     *    let { }: Execute block only if object is not null
     *    
     * 2. if 表達式: Kotlin 的 if 可以回傳值
     *    if expression: Kotlin's if can return values
     *    
     * 3. toIntOrNull(): 安全轉換，失敗時回傳 null
     *    toIntOrNull(): Safe conversion, returns null on failure
     */
    fun readTagFromBuffer(): TagData? {
        val tagDataArray = uhfManager?.readTagFromBuffer()
        
        return tagDataArray?.let { data ->
            if (data.size >= 3) {
                TagData(
                    tid = data[0],
                    epc = data[1],
                    rssi = parseRssi(data[2])
                )
            } else {
                Log.w(TAG, "標籤資料不完整 / Incomplete tag data: ${data.size} elements")
                null
            }
        }
    }
    
    /**
     * 解析 RSSI 值 / Parse RSSI Value
     * 
     * Kotlin 概念: when 表達式 / when expression
     * - 類似 Java switch，但更強大 / Like Java switch but more powerful
     * - 可以有複雜條件 / Can have complex conditions
     */
    private fun parseRssi(rssiHex: String): Int {
        return try {
            when (UHFManager.getType()) {
                com.uhf.base.UHFModuleType.UM_MODULE,
                com.uhf.base.UHFModuleType.RM_MODULE -> {
                    // UM 和 RM 模組需要特殊計算 / UM and RM modules need special calculation
                    val hb = rssiHex.substring(0, 2).toInt(16)
                    val lb = rssiHex.substring(2, 4).toInt(16)
                    ((hb - 256 + 1) * 256 + (lb - 256)) / 10
                }
                else -> {
                    // 其他模組直接轉換 / Other modules direct conversion
                    rssiHex.toInt()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "RSSI 解析失敗 / RSSI parsing failed: $rssiHex", e)
            -99  // 預設錯誤值 / Default error value
        }
    }
    
    /**
     * 設定傳輸功率 / Set Transmission Power
     * 
     * Kotlin 概念: 單行函數 / Single-line function
     * = 可以取代 { return } / = can replace { return }
     */
    fun setPower(power: Int): Boolean = uhfManager?.powerSet(power) ?: false
    
    /**
     * 取得目前功率 / Get Current Power
     */
    fun getPower(): Int = uhfManager?.powerGet() ?: -1
    
    /**
     * 設定頻率區域 / Set Frequency Region
     */
    fun setFrequency(region: Int): Boolean = uhfManager?.frequencyModeSet(region) ?: false
    
    /**
     * 檢查初始化狀態 / Check Initialization Status
     */
    fun isReady(): Boolean = isInitialized && uhfManager != null
}
