package com.socam.bcms.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UHF 服務助手 / UHF Service Helper
 * 
 * Kotlin 概念解釋 / Kotlin Concepts Explained:
 * 1. object 單例模式 / object singleton pattern
 * 2. ServiceConnection 服務連接 / Service connection
 * 3. 狀態管理與 Flow / State management with Flow
 * 4. 服務生命週期管理 / Service lifecycle management
 */
object UHFServiceHelper {
    
    private const val TAG = "UHFServiceHelper"
    
    // 服務連接狀態 / Service connection state
    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()
    
    // 服務實例 / Service instance
    private var uhfService: UHFScanningService? = null
    private var context: Context? = null
    
    /**
     * 服務連接回調 / Service Connection Callback
     * 
     * Kotlin 概念: 匿名物件實作介面 / Anonymous object implementing interface
     * - object : InterfaceName 語法 / object : InterfaceName syntax
     */
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "UHF 服務已連接 / UHF Service connected")
            // 注意：這裡無法取得服務實例，因為我們使用 startService 而非 bindService
            // Note: Can't get service instance here as we use startService instead of bindService
            _isServiceConnected.value = true
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "UHF 服務已斷開 / UHF Service disconnected") 
            uhfService = null
            _isServiceConnected.value = false
        }
    }
    
    /**
     * 啟動 UHF 掃描服務 / Start UHF Scanning Service
     * 
     * Kotlin 概念: Context 擴展函數概念 / Context extension function concept
     */
    fun startScanningService(context: Context) {
        this.context = context
        Log.d(TAG, "啟動 UHF 掃描服務 / Starting UHF scanning service")
        
        val intent = Intent(context, UHFScanningService::class.java)
        
        // 🔧 修正：使用普通 startService 避免前景服務要求 / Fix: Use normal startService to avoid foreground service requirement
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        
        // 我們不需要 bind，只是啟動服務 / We don't need to bind, just start service
        _isServiceConnected.value = true
    }
    
    /**
     * 發送掃描指令 / Send Scan Command
     */
    fun startScanning() {
        context?.let { ctx ->
            val intent = Intent(ctx, UHFScanningService::class.java).apply {
                action = UHFScanningService.ACTION_START_SCANNING
            }
            ctx.startForegroundService(intent)
            Log.d(TAG, "發送開始掃描指令 / Sending start scanning command")
        }
    }
    
    /**
     * 發送停止指令 / Send Stop Command
     */
    fun stopScanning() {
        context?.let { ctx ->
            val intent = Intent(ctx, UHFScanningService::class.java).apply {
                action = UHFScanningService.ACTION_STOP_SCANNING
            }
            ctx.startService(intent)
            Log.d(TAG, "發送停止掃描指令 / Sending stop scanning command")
        }
    }
    
    /**
     * 更新功率 / Update Power
     */
    fun updatePower(power: Int) {
        context?.let { ctx ->
            val intent = Intent(ctx, UHFScanningService::class.java).apply {
                action = UHFScanningService.ACTION_UPDATE_POWER
                putExtra(UHFScanningService.EXTRA_POWER_LEVEL, power)
            }
            ctx.startService(intent)
            Log.d(TAG, "發送功率更新指令 / Sending power update command: $power")
        }
    }
    
    /**
     * 停止服務 / Stop Service
     */
    fun stopService() {
        context?.let { ctx ->
            val intent = Intent(ctx, UHFScanningService::class.java)
            ctx.stopService(intent)
            _isServiceConnected.value = false
            Log.d(TAG, "停止 UHF 服務 / Stopping UHF service")
        }
    }
    
    /**
     * 清理資源 / Cleanup Resources
     */
    fun cleanup() {
        context = null
        uhfService = null
        _isServiceConnected.value = false
    }
}
