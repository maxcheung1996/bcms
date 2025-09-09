package com.socam.bcms

import android.app.Application
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.socam.bcms.uhf.UHFManagerWrapper
import com.socam.bcms.data.database.DatabaseManager
import com.tencent.mmkv.MMKV

/**
 * 應用程式類別 / Application Class
 * 
 * Kotlin 概念解釋 / Kotlin Concepts Explained:
 * 1. object vs class: 
 *    - object: 單例模式，自動建立唯一實例 / Singleton pattern, auto-creates unique instance
 *    - class: 需要手動建立實例 / Need to manually create instances
 *    
 * 2. companion object: 類似 Java static，但更靈活
 *    companion object: Like Java static, but more flexible
 *    
 * 3. lateinit var: 延遲初始化，稍後才設定值
 *    lateinit var: Late initialization, set value later
 *    
 * 4. lazy { }: 延遲建立，第一次使用時才建立
 *    lazy { }: Lazy creation, only create when first used
 */
class BCMSApp : Application() {
    
    companion object {
        // 全域應用程式實例 / Global app instance
        lateinit var instance: BCMSApp
            private set    // 只能在這個類別內部設定 / Can only be set inside this class
        
        private const val TAG = "BCMSApp"
        
        // 全域設定值 (轉換自 MyApp.java 的 static 變數)
        // Global settings (converted from MyApp.java static variables)
        var isOpenSound = false              // 是否開啟聲音 / Enable sound
        var currentInvtDataType = -1         // 目前清單資料類型 / Current inventory data type
        var protocolType = 1                 // 協議類型 (1:ISO, 2:GB) / Protocol type
        var powerSize = 5                    // 功率大小 / Power size
        var maxPower = 33                    // 最大功率 / Maximum power
        var isASCII = false                  // 是否以 ASCII 顯示 / Display as ASCII
    }
    
    /**
     * UHF 管理器 - 使用 lazy 延遲初始化
     * UHF Manager - Using lazy initialization
     * 
     * Kotlin 概念: by lazy { } 
     * - 第一次存取時才建立物件 / Object created only on first access
     * - 執行緒安全 / Thread-safe
     * - 只建立一次 / Created only once
     */
    val uhfManager: UHFManagerWrapper by lazy { UHFManagerWrapper() }
    
    /**
     * 資料庫管理器 - Database Manager
     */
    val databaseManager: DatabaseManager by lazy { DatabaseManager.getInstance(this) }
    
    // 聲音播放器 / Sound player
    private lateinit var soundPool: SoundPool
    private var soundID: Int = 0
    
    /**
     * 應用程式啟動時呼叫 / Called when app starts
     */
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        Log.d(TAG, "BCMS 應用程式啟動 / BCMS Application starting")
        
        // 初始化 MMKV 儲存 / Initialize MMKV storage
        initializeMMKV()
        
        // 初始化資料庫 / Initialize database
        initializeDatabase()
        
        // 設定聲音系統 / Setup sound system  
        setupSoundPool()
        
        Log.d(TAG, "應用程式初始化完成 / Application initialization completed")
    }
    
    /**
     * 初始化 MMKV 資料儲存 / Initialize MMKV Data Storage
     * 
     * MMKV: 騰訊開發的高效能鍵值儲存 / Tencent's high-performance key-value storage
     */
    private fun initializeMMKV() {
        try {
            MMKV.initialize(this)
            Log.d(TAG, "MMKV 初始化成功 / MMKV initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "MMKV 初始化失敗 / MMKV initialization failed", e)
        }
    }
    
    /**
     * 初始化 SQLDelight 資料庫 / Initialize SQLDelight Database
     */
    private fun initializeDatabase() {
        try {
            databaseManager.initializeDatabase()
            Log.d(TAG, "資料庫初始化成功 / Database initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "資料庫初始化失敗 / Database initialization failed", e)
        }
    }
    
    /**
     * 設定聲音池 / Setup Sound Pool
     * 
     * Kotlin 概念: apply { } 
     * - 在物件上執行多個操作 / Execute multiple operations on object
     * - 返回原物件 / Returns original object
     */
    private fun setupSoundPool() {
        try {
            soundPool = SoundPool.Builder().apply {
                setMaxStreams(10)                    // 最大同時播放數 / Max concurrent streams
                setAudioAttributes(
                    AudioAttributes.Builder().apply {
                        setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    }.build()
                )
            }.build()
            
            // 載入嗶聲檔案 (如果存在) / Load beep sound file (if exists)
            // soundID = soundPool.load(this, R.raw.beep, 1)
            Log.d(TAG, "聲音池設定完成 / Sound pool setup completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "聲音池設定失敗 / Sound pool setup failed", e)
        }
    }
    
    /**
     * 播放掃描聲音 / Play Scan Sound
     * 
     * Kotlin 概念: 函數可見性 / Function visibility
     * - fun (無修飾符) = public / fun (no modifier) = public
     */
    fun playSound() {
        if (isOpenSound && soundID != 0) {
            try {
                soundPool.play(soundID, 1f, 1f, 0, 0, 1f)
                Log.d(TAG, "播放掃描聲音 / Playing scan sound")
            } catch (e: Exception) {
                Log.w(TAG, "聲音播放失敗 / Sound playback failed", e)
            }
        }
    }
    
    /**
     * 儲存設定到 MMKV / Save Settings to MMKV
     * 
     * Kotlin 概念: 智慧轉換 / Smart casting
     * - Kotlin 自動判斷類型 / Kotlin automatically determines types
     */
    fun saveSettings() {
        try {
            val mmkv = MMKV.defaultMMKV()
            mmkv?.apply {
                encode("openSound", isOpenSound)
                encode("dataType", currentInvtDataType)  
                encode("protocol", protocolType)
                encode("power", powerSize)
            }
            Log.d(TAG, "設定已儲存 / Settings saved")
        } catch (e: Exception) {
            Log.e(TAG, "設定儲存失敗 / Settings save failed", e)
        }
    }
    
    /**
     * 從 MMKV 載入設定 / Load Settings from MMKV
     */
    fun loadSettings() {
        try {
            val mmkv = MMKV.defaultMMKV()
            mmkv?.let { storage ->
                isOpenSound = storage.decodeBool("openSound", false)
                currentInvtDataType = storage.decodeInt("dataType", -1)
                protocolType = storage.decodeInt("protocol", 1)
                powerSize = storage.decodeInt("power", 5)
            }
            Log.d(TAG, "設定已載入 / Settings loaded")
        } catch (e: Exception) {
            Log.e(TAG, "設定載入失敗 / Settings load failed", e)
        }
    }
}
