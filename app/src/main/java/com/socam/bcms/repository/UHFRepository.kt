package com.socam.bcms.repository

import android.content.Context
import android.util.Log
import com.socam.bcms.BCMSApp
import com.socam.bcms.model.TagData
import com.socam.bcms.service.UHFServiceHelper
import com.socam.bcms.service.UHFScanningService
import kotlinx.coroutines.flow.*

/**
 * UHF 資料儲存庫 / UHF Data Repository
 * 
 * Kotlin 概念解釋 / Kotlin Concepts Explained:
 * 1. Repository 模式: 資料層抽象 / Repository pattern: Data layer abstraction
 * 2. Flow 組合: 合併多個資料流 / Flow composition: Combining multiple data streams
 * 3. 狀態共享: 多個 UI 組件共享狀態 / State sharing: Multiple UI components sharing state
 * 4. 單一資料來源: 所有資料通過此類 / Single source of truth: All data through this class
 */
class UHFRepository(private val context: Context) {
    
    companion object {
        private const val TAG = "UHFRepository"
        
        /**
         * 單例實例 / Singleton instance
         * 
         * Kotlin 概念: lazy 單例模式 / lazy singleton pattern
         * - thread-safe 且只建立一次 / thread-safe and created only once
         */
        @Volatile
        private var INSTANCE: UHFRepository? = null
        
        fun getInstance(context: Context): UHFRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UHFRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // 本地掃描資料 / Local scanning data
    private val _localScannedTags = MutableStateFlow<Map<String, TagData>>(emptyMap())
    private val _localScanningState = MutableStateFlow(false)
    
    /**
     * 組合資料流 / Combined Data Flows
     * 
     * Kotlin 概念: Flow 轉換和組合 / Flow transformation and combination
     * - combine() 合併多個 Flow / Combine multiple Flows
     * - map() 轉換資料 / Transform data
     * - distinctUntilChanged() 去除重複 / Remove duplicates
     */
    val scanningState: Flow<Boolean> = combine(
        _localScanningState,
        UHFServiceHelper.isServiceConnected
    ) { localScanning, serviceConnected ->
        localScanning && serviceConnected
    }.distinctUntilChanged()
    
    val scannedTags: Flow<Map<String, TagData>> = _localScannedTags
    
    /**
     * 初始化儲存庫 / Initialize Repository
     */
    fun initialize() {
        Log.d(TAG, "初始化 UHF 儲存庫 / Initializing UHF repository")
        UHFServiceHelper.startScanningService(context)
    }
    
    /**
     * 開始掃描 / Start Scanning
     * 
     * Kotlin 概念: 服務控制 / Service control
     */
    suspend fun startScanning(): Boolean {
        return try {
            Log.d(TAG, "開始掃描請求 / Start scanning request")
            
            _localScanningState.value = true
            UHFServiceHelper.startScanning()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "開始掃描失敗 / Start scanning failed", e)
            _localScanningState.value = false
            false
        }
    }
    
    /**
     * 停止掃描 / Stop Scanning
     */
    suspend fun stopScanning(): Boolean {
        return try {
            Log.d(TAG, "停止掃描請求 / Stop scanning request")
            
            _localScanningState.value = false
            UHFServiceHelper.stopScanning()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "停止掃描失敗 / Stop scanning failed", e)
            false
        }
    }
    
    /**
     * 更新功率 / Update Power
     */
    suspend fun setPower(power: Int): Boolean {
        return try {
            if (power in 5..33) {
                UHFServiceHelper.updatePower(power)
                BCMSApp.powerSize = power
                Log.d(TAG, "功率更新 / Power updated: $power")
                true
            } else {
                Log.w(TAG, "無效功率值 / Invalid power value: $power")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "功率設定失敗 / Power setting failed", e)
            false
        }
    }
    
    /**
     * 新增標籤到本地快取 / Add Tag to Local Cache
     * 
     * 這個方法用於從服務接收標籤資料 / This method is for receiving tag data from service
     */
    fun addTagData(tagData: TagData) {
        val currentTags = _localScannedTags.value.toMutableMap()
        currentTags[tagData.epc] = tagData
        _localScannedTags.value = currentTags
        
        Log.d(TAG, "新增標籤資料 / Added tag data: ${tagData.epc}")
    }
    
    /**
     * 清除掃描資料 / Clear Scan Data
     */
    fun clearScanData() {
        _localScannedTags.value = emptyMap()
        Log.d(TAG, "掃描資料已清除 / Scan data cleared")
    }
    
    /**
     * 取得 CSV 資料 / Get CSV Data
     */
    fun getCSVData(): String {
        val tags = _localScannedTags.value
        
        return buildString {
            append("TID,EPC,RSSI,Timestamp\n")
            
            tags.values
                .sortedByDescending { it.timestamp }
                .forEach { tag ->
                    append("${tag.tid},${tag.epc},${tag.rssi},${tag.timestamp}\n")
                }
        }
    }
    
    /**
     * 清理資源 / Cleanup Resources
     */
    fun cleanup() {
        UHFServiceHelper.cleanup()
        Log.d(TAG, "儲存庫資源已清理 / Repository resources cleaned up")
    }
}
