package com.socam.bcms.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.socam.bcms.BCMSApp
import com.socam.bcms.R
import com.socam.bcms.model.TagData
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

/**
 * UHF 掃描背景服務 / UHF Scanning Background Service
 * 
 * Kotlin 概念解釋 / Kotlin Concepts Explained:
 * 1. Service: Android 背景服務 / Android background service
 * 2. Flow: 響應式資料流 / Reactive data streams
 * 3. StateFlow: 有狀態的資料流 / Stateful data streams  
 * 4. Channel: 協程間通訊 / Inter-coroutine communication
 * 5. SupervisorJob: 子協程失敗不影響父協程 / Child failures don't affect parent
 */
class UHFScanningService : Service() {
    
    companion object {
        private const val TAG = "UHFScanningService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "uhf_scanning_channel"
        
        // Service 動作 / Service actions
        const val ACTION_START_SCANNING = "start_scanning"
        const val ACTION_STOP_SCANNING = "stop_scanning"
        const val ACTION_UPDATE_POWER = "update_power"
        const val EXTRA_POWER_LEVEL = "power_level"
    }
    
    /**
     * 服務範圍 / Service Scope
     * 
     * Kotlin 概念: SupervisorJob + CoroutineScope
     * - SupervisorJob: 一個子協程失敗不會取消其他協程 / One child failure doesn't cancel others
     * - Dispatchers.Default: CPU 密集型工作的調度器 / Dispatcher for CPU-intensive work
     */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    /**
     * 私有資料流 / Private Data Flows
     * 
     * Kotlin 概念: MutableStateFlow vs MutableSharedFlow
     * - StateFlow: 保持最新狀態，新訂閱者會收到目前值 / Keeps latest state, new subscribers get current value
     * - SharedFlow: 事件流，不保持狀態 / Event stream, doesn't keep state
     */
    private val _scanningState = MutableStateFlow(false)
    private val _tagDataFlow = MutableSharedFlow<TagData>(extraBufferCapacity = 100)
    private val _scanStatistics = MutableStateFlow(ScanStatistics(0, 0, 0.0))
    
    /**
     * 公開資料流 / Public Data Flows
     */
    val scanningState: StateFlow<Boolean> = _scanningState.asStateFlow()
    val tagDataFlow: SharedFlow<TagData> = _tagDataFlow.asSharedFlow()
    val scanStatistics: StateFlow<ScanStatistics> = _scanStatistics.asStateFlow()
    
    /**
     * Channel 用於控制指令 / Channel for control commands
     * 
     * Kotlin 概念: Channel
     * - 協程間傳遞資料的管道 / Pipe for passing data between coroutines
     * - Channel.UNLIMITED: 無限緩衝 / Unlimited buffer
     */
    private val controlChannel = Channel<ControlCommand>(Channel.UNLIMITED)
    
    /**
     * 掃描統計資料類別 / Scan Statistics Data Class
     */
    data class ScanStatistics(
        val uniqueTags: Int,
        val totalReads: Long,
        val readRate: Double
    )
    
    /**
     * 控制指令密封類別 / Control Command Sealed Class
     * 
     * Kotlin 概念: sealed class
     * - 限制繼承的類別集合 / Limited set of inheriting classes
     * - 編譯器知道所有可能的子類型 / Compiler knows all possible subtypes
     * - when 表達式可以是窮盡的 / when expressions can be exhaustive
     */
    sealed class ControlCommand {
        object StartScanning : ControlCommand()
        object StopScanning : ControlCommand()
        data class UpdatePower(val power: Int) : ControlCommand()
        object GetStatistics : ControlCommand()
    }
    
    // 掃描資料 / Scanning data
    private val scannedTags = mutableMapOf<String, TagData>()
    private var totalReadCount = 0L
    private var scanStartTime = 0L
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "UHF 掃描服務建立 / UHF Scanning Service created")
        
        createNotificationChannel()
        
        // 🔧 Android 8.0+ 要求：立即啟動前景服務 / Android 8.0+ requirement: Start foreground service immediately
        startForeground(NOTIFICATION_ID, createIdleNotification())
        
        startControlLoop()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "服務命令接收 / Service command received: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_SCANNING -> {
                serviceScope.launch {
                    controlChannel.send(ControlCommand.StartScanning)
                }
            }
            ACTION_STOP_SCANNING -> {
                serviceScope.launch {
                    controlChannel.send(ControlCommand.StopScanning)
                }
            }
            ACTION_UPDATE_POWER -> {
                val power = intent.getIntExtra(EXTRA_POWER_LEVEL, 5)
                serviceScope.launch {
                    controlChannel.send(ControlCommand.UpdatePower(power))
                }
            }
        }
        
        return START_STICKY  // 服務被殺死後自動重啟 / Auto restart after killed
    }
    
    /**
     * 建立通知頻道 / Create Notification Channel
     * 
     * Kotlin 概念: 擴展函數實際應用 / Extension function practical application
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "UHF 掃描服務 / UHF Scanning Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "UHF RFID 背景掃描狀態 / UHF RFID background scanning status"
                enableVibration(false)
                setSound(null, null)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 啟動控制迴圈 / Start Control Loop
     * 
     * Kotlin 概念: 無限協程迴圈 / Infinite coroutine loop
     * - while (isActive) 檢查協程是否活躍 / Check if coroutine is active
     * - channel.receive() 等待指令 / Wait for commands
     */
    private fun startControlLoop() {
        serviceScope.launch {
            Log.d(TAG, "啟動控制迴圈 / Starting control loop")
            
            while (isActive) {
                try {
                    val command = controlChannel.receive()
                    processCommand(command)
                } catch (e: Exception) {
                    Log.e(TAG, "控制迴圈錯誤 / Control loop error", e)
                    delay(1000)  // 錯誤後等待 1 秒 / Wait 1 second after error
                }
            }
        }
    }
    
    /**
     * 處理控制指令 / Process Control Commands
     * 
     * Kotlin 概念: when 與 sealed class 的完美組合 / Perfect combination of when and sealed class
     * - 編譯器確保處理所有情況 / Compiler ensures all cases are handled
     */
    private suspend fun processCommand(command: ControlCommand) {
        Log.d(TAG, "處理控制指令 / Processing control command: $command")
        
        when (command) {
            is ControlCommand.StartScanning -> startScanning()
            is ControlCommand.StopScanning -> stopScanning()
            is ControlCommand.UpdatePower -> updatePower(command.power)
            is ControlCommand.GetStatistics -> updateStatistics()
        }
    }
    
    /**
     * 開始掃描 / Start Scanning
     */
    private suspend fun startScanning() = withContext(Dispatchers.IO) {
        if (_scanningState.value) {
            Log.w(TAG, "掃描已在進行中 / Scanning already in progress")
            return@withContext
        }
        
        Log.d(TAG, "開始背景掃描 / Starting background scanning")
        
        val uhfManager = BCMSApp.instance.uhfManager
        val powerOnResult = uhfManager.powerOn()
        
        if (!powerOnResult) {
            Log.e(TAG, "UHF 電源啟動失敗 / UHF power on failed")
            return@withContext
        }
        
        val startResult = uhfManager.startInventory()
        if (!startResult) {
            Log.e(TAG, "清單掃描啟動失敗 / Inventory scan start failed") 
            uhfManager.powerOff()
            return@withContext
        }
        
        _scanningState.value = true
        scanStartTime = System.currentTimeMillis()
        scannedTags.clear()
        totalReadCount = 0
        
        // 更新為掃描通知 / Update to scanning notification
        updateNotification(createScanningNotification())
        
        // 啟動掃描迴圈 / Start scanning loop
        startScanningLoop()
    }
    
    /**
     * 停止掃描 / Stop Scanning
     */
    private suspend fun stopScanning() = withContext(Dispatchers.IO) {
        if (!_scanningState.value) {
            Log.w(TAG, "掃描未在進行 / Scanning not in progress")
            return@withContext
        }
        
        Log.d(TAG, "停止背景掃描 / Stopping background scanning")
        
        _scanningState.value = false
        
        val uhfManager = BCMSApp.instance.uhfManager
        uhfManager.stopInventory()
        uhfManager.powerOff()
        
        // 回到閒置通知 / Return to idle notification
        updateNotification(createIdleNotification())
        
        updateStatistics()
    }
    
    /**
     * 掃描迴圈 / Scanning Loop
     * 
     * Kotlin 概念: Flow 建造器 / Flow builder
     * - flow { } 建立自定義資料流 / Create custom data stream
     * - emit() 發送資料 / Send data
     * - collect { } 收集資料 / Collect data
     */
    private fun startScanningLoop() {
        serviceScope.launch {
            Log.d(TAG, "啟動掃描迴圈 / Starting scanning loop")
            
            // 建立掃描資料流 / Create scanning data flow
            val scanFlow = flow {
                while (_scanningState.value && currentCoroutineContext().isActive) {
                    try {
                        val tagData = withContext(Dispatchers.IO) {
                            BCMSApp.instance.uhfManager.readTagFromBuffer()
                        }
                        
                        if (tagData != null) {
                            emit(tagData)  // 發送到資料流 / Emit to data flow
                        }
                        
                        delay(50)  // 50ms 間隔 / 50ms interval
                        
                    } catch (e: Exception) {
                        Log.w(TAG, "掃描迴圈錯誤 / Scanning loop error", e)
                        delay(100)
                    }
                }
            }
            
            // 收集掃描資料 / Collect scan data  
            scanFlow.collect { tagData ->
                processTagData(tagData)
            }
        }
        
        // 統計更新迴圈 / Statistics update loop
        serviceScope.launch {
            while (_scanningState.value && currentCoroutineContext().isActive) {
                updateStatistics()
                delay(1000)  // 每秒更新統計 / Update statistics every second
            }
        }
    }
    
    /**
     * 處理標籤資料 / Process Tag Data
     * 
     * Kotlin 概念: SharedFlow 發送 / SharedFlow emission
     * - tryEmit() 非阻塞發送 / Non-blocking emission
     * - 不會等待收集者 / Doesn't wait for collectors
     */
    private suspend fun processTagData(tagData: TagData) {
        val wasNewTag = !scannedTags.containsKey(tagData.epc)
        
        scannedTags[tagData.epc] = tagData
        totalReadCount++
        
        // 發送到資料流 / Emit to data flow
        _tagDataFlow.tryEmit(tagData)
        
        if (wasNewTag) {
            Log.d(TAG, "發現新標籤 / New tag found: ${tagData.epc}")
            
            // 播放聲音 / Play sound
            if (BCMSApp.isOpenSound) {
                withContext(Dispatchers.Main) {
                    BCMSApp.instance.playSound()
                }
            }
            
            // 更新統計通知 / Update statistics notification
            updateScanningStatistics()
        }
    }
    
    /**
     * 更新統計資料 / Update Statistics
     */
    private suspend fun updateStatistics() {
        val duration = System.currentTimeMillis() - scanStartTime
        val readRate = if (duration > 0) {
            (totalReadCount * 1000.0) / duration
        } else 0.0
        
        val statistics = ScanStatistics(
            uniqueTags = scannedTags.size,
            totalReads = totalReadCount,
            readRate = readRate
        )
        
        _scanStatistics.value = statistics
    }
    
    /**
     * 更新功率 / Update Power
     */
    private suspend fun updatePower(power: Int) = withContext(Dispatchers.IO) {
        try {
            val result = BCMSApp.instance.uhfManager.setPower(power)
            Log.d(TAG, "功率更新 / Power updated: $power, result: $result")
        } catch (e: Exception) {
            Log.e(TAG, "功率更新失敗 / Power update failed", e)
        }
    }
    
    /**
     * 建立閒置通知 / Create Idle Notification
     */
    private fun createIdleNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🛡️ UHF 服務就緒 / UHF Service Ready")
            .setContentText("等待掃描指令 / Waiting for scan commands")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSound(null)
            .setVibrate(null)
            .build()
    }
    
    /**
     * 建立掃描通知 / Create Scanning Notification
     */
    private fun createScanningNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🔄 UHF 掃描進行中 / UHF Scanning Active")
            .setContentText("正在背景掃描 RFID 標籤 / Background RFID tag scanning")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)  // 無法滑除 / Cannot be dismissed
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSound(null)
            .setVibrate(null)
            .build()
    }
    
    /**
     * 更新通知內容 / Update Notification Content
     */
    private fun updateNotification(notification: Notification) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * 更新掃描統計通知 / Update Scanning Statistics Notification
     */
    private fun updateScanningStatistics() {
        if (_scanningState.value) {
            val statistics = _scanStatistics.value
            
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("📡 UHF 掃描進行中 / UHF Scanning Active")
                .setContentText("標籤: ${statistics.uniqueTags} | 讀取: ${statistics.totalReads}")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
            
            updateNotification(notification)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "UHF 掃描服務銷毀 / UHF Scanning Service destroyed")
        
        // 取消所有協程 / Cancel all coroutines
        serviceScope.cancel()
        
        // 確保 UHF 關閉 / Ensure UHF is off
        try {
            BCMSApp.instance.uhfManager.stopInventory()
            BCMSApp.instance.uhfManager.powerOff()
        } catch (e: Exception) {
            Log.e(TAG, "服務銷毀時 UHF 關閉錯誤 / UHF shutdown error during service destroy", e)
        }
    }
}
