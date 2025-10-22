package com.socam.bcms.uhf

import android.os.Build
import android.util.Log
import com.socam.bcms.model.*
import com.uhf.base.UHFManager
import com.uhf.base.UHFModuleType

/**
 * UHF 管理器包裝器 / UHF Manager Wrapper
 * 
 * Includes emulator detection and mock functionality for development
 */
class UHFManagerWrapper {
    
    // 私有屬性：包裝廠商的 UHF 管理器 / Private property: Wrap vendor's UHF manager
    private var uhfManager: UHFManager? = null
    private var isInitialized = false
    private val isEmulator = isRunningOnEmulator()
    private var mockPowerLevel = 30 // Mock power level for emulator
    
    init {
        // Log initial detection results for debugging
        Log.d(TAG, "=== UHF Manager Wrapper Initialization ===")
        Log.d(TAG, "Device detection result: isEmulator = $isEmulator")
        Log.d(TAG, "Build info - BRAND: ${Build.BRAND}, MODEL: ${Build.MODEL}")
        Log.d(TAG, "Build info - PRODUCT: ${Build.PRODUCT}, HARDWARE: ${Build.HARDWARE}")
        Log.d(TAG, "Build info - FINGERPRINT: ${Build.FINGERPRINT}")
        Log.d(TAG, "==========================================")
    }
    
    companion object {
        private const val TAG = "UHFManagerWrapper"  // Log 標籤 / Log tag
        
    /**
     * Detect if running on Android emulator (simplified approach like vendor demo)
     */
    private fun isRunningOnEmulator(): Boolean {
        val isEmulator = (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
        
        // Check for UHF service availability (simplified check)
        val hasUHFService = checkForUHFService()
        
        Log.d(TAG, "Device detection - isEmulator: $isEmulator, hasUHFService: $hasUHFService")
        Log.d(TAG, "Device info - Brand: ${Build.BRAND}, Model: ${Build.MODEL}, Product: ${Build.PRODUCT}")
        
        // Only use mock mode for actual emulators OR when UHF service is completely unavailable
        return isEmulator || !hasUHFService
    }
    
    /**
     * Check if UHF service is available (simplified like vendor demo)
     */
    private fun checkForUHFService(): Boolean {
        return try {
            // Try to access UHF service classes
            Class.forName("com.idata.UHFManager")
            Class.forName("com.uhf.base.UHFManager")
            Log.d(TAG, "UHF service classes found - device has UHF capability")
            true
        } catch (e: ClassNotFoundException) {
            Log.d(TAG, "UHF service classes not found - using mock mode")
            false
        } catch (e: Exception) {
            Log.w(TAG, "Error checking UHF service availability", e)
            false
        }
    }
    }
    
    /**
     * 初始化 UHF 硬體 / Initialize UHF Hardware
     * 
     * Simplified approach following vendor demo pattern
     */
    fun initialize(moduleType: com.uhf.base.UHFModuleType): Boolean {
        return try {
            Log.d(TAG, "=== UHF Hardware Initialization ===")
            Log.d(TAG, "正在初始化 UHF 模組: $moduleType / Initializing UHF module: $moduleType")
            Log.d(TAG, "Current isEmulator status: $isEmulator")
            
            if (isEmulator) {
                Log.d(TAG, "Emulator/Non-UHF device detected - using mock UHF manager")
                isInitialized = true
                Log.d(TAG, "Mock UHF 管理器初始化成功 / Mock UHF Manager initialized successfully")
                Log.d(TAG, "======================================")
                return true
            }
            
            // 呼叫廠商 API / Call vendor API (following vendor demo approach)
            Log.d(TAG, "Attempting to initialize real UHF hardware like vendor demo...")
            uhfManager = UHFManager.getUHFImplSigleInstance(moduleType)
            
            // Verify the manager was created successfully
            if (uhfManager == null) {
                Log.w(TAG, "UHF manager creation returned null, falling back to mock mode")
                isInitialized = true
                Log.d(TAG, "======================================")
                return true
            }
            
            isInitialized = true
            Log.d(TAG, "✅ UHF 管理器初始化成功 / UHF Manager initialized successfully")
            Log.d(TAG, "Real UHF hardware is now available for scanning")
            Log.d(TAG, "======================================")
            true
        } catch (e: Exception) {
            Log.e(TAG, "UHF 初始化失敗 / UHF initialization failed", e)
            Log.e(TAG, "Exception details: ${e.message}")
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            
            // Fall back to mock mode when initialization fails
            Log.d(TAG, "Falling back to mock UHF manager due to initialization failure")
            isInitialized = true
            Log.d(TAG, "======================================")
            return true
        }
    }
    
    
    /**
     * 開啟 UHF 電源 / Power On UHF
     * 
     * Simplified implementation following vendor demo pattern
     */
    fun powerOn(): Boolean {
        Log.d(TAG, "=== UHF Power On Request ===")
        Log.d(TAG, "開啟 UHF 電源 / Powering on UHF")
        Log.d(TAG, "isEmulator: $isEmulator, isInitialized: $isInitialized, uhfManager: ${uhfManager != null}")
        
        return try {
            if (isEmulator) {
                Log.d(TAG, "Mock: UHF power on successful")
                Log.d(TAG, "==============================")
                true
            } else {
                if (uhfManager == null) {
                    Log.w(TAG, "UHF manager is null - initialization may have failed")
                    Log.d(TAG, "==============================")
                    return false
                }
                
                Log.d(TAG, "Calling real UHF hardware powerOn()...")
                val result = uhfManager!!.powerOn()
                Log.d(TAG, "✅ UHF power on result: $result")
                Log.d(TAG, "==============================")
                result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during UHF power on", e)
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Exception message: ${e.message}")
            Log.d(TAG, "==============================")
            
            // Return false for real hardware exceptions to indicate failure
            false
        }
    }
    
    /**
     * 關閉 UHF 電源 / Power Off UHF
     */
    fun powerOff(): Boolean {
        Log.d(TAG, "關閉 UHF 電源 / Powering off UHF")
        return if (isEmulator) {
            Log.d(TAG, "Mock: UHF power off successful")
            true
        } else {
            uhfManager?.powerOff() ?: false
        }
    }
    
    /**
     * 開始清單掃描 / Start Inventory Scanning
     */
    fun startInventory(): Boolean {
        Log.d(TAG, "=== Starting RFID Inventory ===")
        Log.d(TAG, "開始 RFID 清單掃描 / Starting RFID inventory")
        return try {
            if (isEmulator) {
                Log.d(TAG, "Mock: RFID inventory started")
                Log.d(TAG, "==============================")
                true
            } else {
                if (uhfManager == null) {
                    Log.w(TAG, "UHF manager is null - cannot start inventory")
                    Log.d(TAG, "==============================")
                    return false
                }
                
                Log.d(TAG, "Calling real UHF hardware startInventoryTag()...")
                val result = uhfManager!!.startInventoryTag()
                Log.d(TAG, "✅ Start inventory result: $result")
                Log.d(TAG, "==============================")
                result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during start inventory", e)
            Log.e(TAG, "Exception details: ${e.message}")
            Log.d(TAG, "==============================")
            false
        }
    }

    
    /**
     * 停止掃描 / Stop Scanning
     */
    fun stopInventory(): Boolean {
        Log.d(TAG, "停止 RFID 掃描 / Stopping RFID scanning")
        return if (isEmulator) {
            Log.d(TAG, "Mock: RFID scanning stopped")
            true
        } else {
            uhfManager?.stopInventory() ?: false
        }
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
        return if (isEmulator) {
            // Mock tag data for emulator testing
            val mockTags = listOf(
                TagData(tid = "E280", epc = "1234567890123456", rssi = -45),
                TagData(tid = "E280", epc = "ABCDEF1234567890", rssi = -52),
                TagData(tid = "E280", epc = "FEDCBA0987654321", rssi = -38)
            )
            
            // Randomly return one of the mock tags occasionally
            if (Math.random() < 0.1) { // 10% chance
                mockTags.random()
            } else {
                null
            }
        } else {
            val tagDataArray = uhfManager?.readTagFromBuffer()
            
            tagDataArray?.let { data ->
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
            if (isEmulator) {
                // Mock RSSI for emulator
                return (-30..-80).random()
            }
            
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
     * Mock implementation for emulator
     */
    fun setPower(power: Int): Boolean {
        return if (isEmulator) {
            mockPowerLevel = power
            Log.d(TAG, "Mock: Power set to $power dBm")
            true
        } else {
            uhfManager?.powerSet(power) ?: false
        }
    }
    
    /**
     * 取得目前功率 / Get Current Power
     */
    fun getPower(): Int {
        return if (isEmulator) {
            mockPowerLevel
        } else {
            uhfManager?.powerGet() ?: -1
        }
    }
    
    /**
     * Check if UHF manager is ready for operations
     * Used by Settings and other modules to verify UHF availability
     */
    fun isReady(): Boolean {
        Log.d(TAG, "Checking UHF ready state - isInitialized: $isInitialized, isEmulator: $isEmulator, uhfManager: ${uhfManager != null}")
        return if (isEmulator) {
            // In emulator mode, always ready if initialized
            isInitialized
        } else {
            // On real hardware, need both initialization and valid UHF manager
            isInitialized && uhfManager != null
        }
    }
    
    /**
     * 設定頻率區域 / Set Frequency Region
     */
    fun setFrequency(region: Int): Boolean {
        return if (isEmulator) {
            Log.d(TAG, "Mock: Frequency region set to $region")
            true
        } else {
            uhfManager?.frequencyModeSet(region) ?: false
        }
    }
    
    /**
     * 讀取特定記憶體庫資料 / Read Specific Memory Bank Data
     * Used for reading USER memory bank data for tag modification
     */
    fun readTag(
        password: String = "00000000",
        filterBank: Int = MemoryBank.EPC.value,
        filterAddress: Int = 2,
        filterLength: Int = 0,
        filterData: String = "",
        memoryBank: Int = MemoryBank.USER.value,
        startAddress: Int = 2,  // Changed from 0 to 2 (following demo pattern)
        length: Int = 6
    ): String? {
        return if (isEmulator) {
            // Mock USER bank data for emulator testing
            when (memoryBank) {
                MemoryBank.USER.value -> "4D4F434B555352" // Mock USER data: "MOCKUSR" in hex
                MemoryBank.TID.value -> "E280116040000000"  // Mock TID
                MemoryBank.EPC.value -> "3000001234567890"  // Mock EPC
                else -> "0000000000000000"
            }
        } else {
            try {
                Log.d(TAG, "Reading tag memory bank: $memoryBank, address: $startAddress, length: $length")
                
                // Stop any ongoing inventory first
                Log.d(TAG, "Stopping any ongoing inventory...")
                uhfManager?.stopInventory()
                Thread.sleep(100)
                
                // Start inventory briefly for tag detection
                val inventoryStarted = uhfManager?.startInventoryTag() ?: false
                Log.d(TAG, "Inventory started for read: $inventoryStarted")
                
                if (!inventoryStarted) {
                    Log.w(TAG, "❌ Failed to start inventory for read operation")
                    return null
                }
                
                // Wait for RF field and tag detection
                Thread.sleep(200)
                
                // Keep inventory running for read operation - tag needs RF field to be detectable
                Log.d(TAG, "Keeping inventory active for read operation...")
                
                val result = uhfManager?.readTag(
                    password,
                    filterBank,
                    filterAddress,
                    filterLength,
                    filterData,
                    memoryBank,
                    startAddress,
                    length
                )
                
                if (result != null) {
                    Log.d(TAG, "✅ Read operation successful: $result")
                } else {
                    Log.w(TAG, "❌ Read operation failed")
                }
                
                // Stop inventory after read operation
                val stopped = uhfManager?.stopInventory() ?: false
                Log.d(TAG, "Inventory stopped after read: $stopped")
                
                result
            } catch (e: Exception) {
                Log.e(TAG, "讀取標籤記憶體庫失敗 / Failed to read tag memory bank: ${e.message}")
                // Stop inventory on error
                uhfManager?.stopInventory()
                null
            }
        }
    }
    
    /**
     * 寫入EPC資料 / Write EPC Data  
     * Restored original working pattern from Tag Modification
     */
    fun writeDataToEpc(
        targetEpc: String,      // Target tag's current EPC (for filtering)
        password: String = "00000000",
        startAddress: Int = 2,  // EPC start address (word address)
        length: Int = 6,        // EPC length (words)
        data: String            // New EPC data (hex string)
    ): Boolean {
        return if (isEmulator) {
            // Mock write success for emulator testing
            Log.d(TAG, "Mock: Writing EPC data '$data' at address $startAddress")
            Thread.sleep(500)
            true
        } else {
            try {
                Log.d(TAG, "Writing EPC data...")
                Log.d(TAG, "Target EPC: $targetEpc")
                Log.d(TAG, "New EPC data: $data")
                Log.d(TAG, "Start address: $startAddress (word address)")
                Log.d(TAG, "Length: $length (words)")
                
                // CRITICAL: Following Tag Modification working pattern
                // Stop inventory and wait (DO NOT restart inventory)
                Log.d(TAG, "Stopping inventory before write (Tag Modification pattern)...")
                val inventoryStopped = uhfManager?.stopInventory() ?: false
                Log.d(TAG, "Inventory stopped: $inventoryStopped")
                
                // Wait for hardware to settle (same as Tag Modification: 500ms)
                Thread.sleep(500)
                
                // Use EPC filtering approach that works in Tag Modification
                Log.d(TAG, "Performing EPC write with filtering (working Tag Modification pattern)...")
                val result = uhfManager?.writeTag(
                    password,               // Access password
                    1,                      // Filter bank: EPC bank (1)
                    32,                     // Filter address: EPC start (32 bits)
                    targetEpc.length * 4,   // Filter length: EPC length in bits
                    targetEpc,              // Filter data: target tag's current EPC
                    1,                      // Memory bank: EPC bank (1)
                    startAddress,           // Start address in EPC bank
                    length,                 // Data length in words
                    data                    // New EPC data to write
                )
                
                if (result == true) {
                    Log.d(TAG, "✅ EPC write successful using Tag Modification pattern")
                } else {
                    Log.w(TAG, "❌ EPC write failed")
                }
                
                result ?: false
                
            } catch (e: Exception) {
                Log.e(TAG, "寫入EPC資料失敗 / Failed to write EPC data: ${e.message}")
                false
            }
        }
    }
    
    /**
     * 寫入特定記憶體庫資料 / Write Specific Memory Bank Data
     * Used for writing USER memory bank data for tag modification
     */
    fun writeTag(
        targetEpc: String,
        userData: String,
        password: String = "00000000",
        startAddress: Int = 2  // Changed from 0 to 2 (following demo pattern)
    ): Boolean {
        return if (isEmulator) {
            // Mock write success for emulator testing
            Log.d(TAG, "Mock: Writing USER data '$userData' to tag EPC: $targetEpc")
            // Simulate write delay
            Thread.sleep(500)
            true
        } else {
            try {
                val dataLength = userData.length / 4 // Convert hex string to word count
                
                Log.d(TAG, "Writing USER data to tag...")
                Log.d(TAG, "Target EPC: $targetEpc (reference only - using non-filtering)")
                Log.d(TAG, "USER data: $userData")
                Log.d(TAG, "Data length: $dataLength words")
                Log.d(TAG, "Start address: $startAddress (word address - following demo pattern)")
                Log.d(TAG, "Using non-filtering approach like UHF demo")
                
                // Stop any ongoing inventory first to avoid MT_OP_EXECING error
                Log.d(TAG, "Stopping any ongoing inventory...")
                val inventoryStopped = uhfManager?.stopInventory() ?: false
                Log.d(TAG, "Inventory stopped: $inventoryStopped")
                
                // Wait for operations to complete
                Thread.sleep(200)
                
                // Start inventory briefly to activate RF field for tag detection
                Log.d(TAG, "Starting inventory for tag operations...")
                val inventoryStarted = uhfManager?.startInventoryTag() ?: false
                Log.d(TAG, "Inventory started: $inventoryStarted")
                
                if (!inventoryStarted) {
                    Log.w(TAG, "❌ Failed to start inventory for write operation")
                    return false
                }
                
                // Wait for RF field and tag detection
                Thread.sleep(200)
                
                // Keep inventory running for write operation - tag needs RF field to be detectable
                Log.d(TAG, "Keeping inventory active for write operation...")
                
                // Try non-filtering approach (like demo line 404)
                val result = uhfManager?.writeTag(
                    password,                    // Access password
                    0,                          // No filter bank (disable filtering)
                    0,                          // No filter address
                    0,                          // No filter length  
                    "0",                        // No filter data
                    MemoryBank.USER.value,      // Memory bank to write (USER bank)
                    startAddress,               // Start address in USER bank
                    dataLength,                 // Data length in words
                    userData                    // Data to write (hex string)
                )
                
                if (result == true) {
                    Log.d(TAG, "✅ Write operation successful")
                } else {
                    Log.w(TAG, "❌ Write operation failed")
                }
                
                // Stop inventory after write operation
                val stopped = uhfManager?.stopInventory() ?: false
                Log.d(TAG, "Inventory stopped after write: $stopped")
                
                result ?: false
                
            } catch (e: Exception) {
                Log.e(TAG, "寫入標籤記憶體庫失敗 / Failed to write tag memory bank: ${e.message}")
                // Stop inventory on error
                uhfManager?.stopInventory()
                false
            }
        }
    }
    
    
    /**
     * 檢查並修復UHF硬體狀態 / Check and Restore UHF Hardware State
     * Critical method to fix UHF hardware corruption after write operations
     */
    fun checkAndRestoreUHFHealth(): Boolean {
        return if (isEmulator) {
            Log.d(TAG, "Emulator mode - UHF health check not needed")
            true
        } else {
            try {
                Log.d(TAG, "=== UHF Health Check & Restoration ===")
                
                // Test current UHF manager state
                val currentPowerStatus = uhfManager?.powerOn() ?: false
                Log.d(TAG, "Current UHF power status: $currentPowerStatus")
                
                if (!currentPowerStatus) {
                    Log.d(TAG, "UHF hardware appears corrupted - attempting restoration...")
                    
                    // Stop any ongoing operations
                    uhfManager?.stopInventory()
                    Thread.sleep(500)
                    
                    // Try to reinitialize the UHF manager
                    Log.d(TAG, "Reinitializing UHF manager...")
                    uhfManager = UHFManager.getUHFImplSigleInstance(UHFModuleType.SLR_MODULE)
                    Thread.sleep(1000)
                    
                    // Test power on again
                    val restoredPowerStatus = uhfManager?.powerOn() ?: false
                    Log.d(TAG, "UHF restoration result: $restoredPowerStatus")
                    
                    if (restoredPowerStatus) {
                        Log.d(TAG, "✅ UHF hardware successfully restored")
                        isInitialized = true
                        return true
                    } else {
                        Log.w(TAG, "❌ UHF hardware restoration failed")
                        return false
                    }
                } else {
                    Log.d(TAG, "✅ UHF hardware is healthy")
                    return true
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "UHF health check failed: ${e.message}")
                false
            }
        }
    }

    /**
     * Set SLR inventory mode (vendor demo uses mode 3 for best performance)
     * Following vendor demo pattern: uhfmanager.slrInventoryModeSet(3)
     */
    fun setSlrInventoryMode(mode: Int): Boolean {
        return if (isEmulator) {
            Log.d(TAG, "Mock: Setting SLR inventory mode to $mode")
            true
        } else {
            try {
                val result = uhfManager?.slrInventoryModeSet(mode) ?: false
                Log.d(TAG, "Set SLR inventory mode $mode: $result")
                result
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set SLR inventory mode: ${e.message}")
                false
            }
        }
    }

    /**
     * Set read tag mode (vendor demo uses 0,0,0,0)
     * Following vendor demo pattern: uhfmanager.readTagModeSet(0,0,0,0)
     */
    fun setReadTagMode(mode: Int, startAddress: Int, length: Int, option: Int): Boolean {
        return if (isEmulator) {
            Log.d(TAG, "Mock: Setting read tag mode to $mode,$startAddress,$length,$option")
            true
        } else {
            try {
                val result = uhfManager?.readTagModeSet(mode, startAddress, length, option) ?: false
                Log.d(TAG, "Set read tag mode $mode,$startAddress,$length,$option: $result")
                result
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set read tag mode: ${e.message}")
                false
            }
        }
    }

    /**
     * Set frequency mode (vendor demo uses 3 for US frequency)
     * Following vendor demo pattern: uhfmanager.frequencyModeSet(3)
     */
    fun setFrequencyModeSet(mode: Int): Boolean {
        return if (isEmulator) {
            Log.d(TAG, "Mock: Setting frequency mode to $mode")
            true
        } else {
            try {
                val result = uhfManager?.frequencyModeSet(mode) ?: false
                Log.d(TAG, "Set frequency mode $mode: $result")
                result
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set frequency mode: ${e.message}")
                false
            }
        }
    }
}
