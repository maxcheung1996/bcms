package com.socam.bcms

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.tencent.mmkv.MMKV
import com.uhf.base.UHFManager
import com.uhf.base.UHFModuleType

/**
 * 模組選擇活動 / Module Selection Activity
 * 
 * Kotlin 概念解釋 / Kotlin Concepts Explained:
 * 1. when 表達式處理點擊事件 / when expression for handling clicks
 * 2. 擴展函數概念 / Extension function concepts  
 * 3. 智慧轉換 / Smart casting
 * 4. 字串比較和判斷 / String comparison and checking
 */
class SelectActivity : AppCompatActivity() {
    
    // 私有屬性 / Private properties
    private lateinit var mmkv: MMKV
    private lateinit var umModuleBtn: Button
    private lateinit var slrModuleBtn: Button  
    private lateinit var rmModuleBtn: Button
    private lateinit var gxModuleBtn: Button
    
    companion object {
        private const val TAG = "SelectActivity"
        private const val CURRENT_UHF_MODULE = "uhf_module_type"  // MMKV 鍵值 / MMKV key
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select)
        
        Log.d(TAG, "SelectActivity 啟動 / SelectActivity started")
        
        // 初始化 / Initialize
        initializeViews()
        initializeMMKV()
        checkSavedModuleType()
    }
    
    /**
     * 初始化視圖 / Initialize Views
     * 
     * Kotlin 概念: lateinit
     * - 稍後才初始化的非空屬性 / Non-null property initialized later
     * - 避免使用可空類型 / Avoid using nullable types
     */
    private fun initializeViews() {
        umModuleBtn = findViewById(R.id.umModule)
        slrModuleBtn = findViewById(R.id.slrModule)
        rmModuleBtn = findViewById(R.id.rmModule)
        gxModuleBtn = findViewById(R.id.gxModule)
        
        // 設定點擊事件 / Setup click events
        setupClickListeners()
    }
    
    /**
     * 設定點擊監聽器 / Setup Click Listeners
     * 
     * Kotlin 概念: Lambda 表達式 / Lambda expressions  
     * - { } 語法比 new OnClickListener() 簡潔
     * - { } syntax cleaner than new OnClickListener()
     */
    private fun setupClickListeners() {
        umModuleBtn.setOnClickListener { onModuleSelected(UHFModuleType.UM_MODULE) }
        slrModuleBtn.setOnClickListener { onModuleSelected(UHFModuleType.SLR_MODULE) }
        rmModuleBtn.setOnClickListener { onModuleSelected(UHFModuleType.RM_MODULE) }
        gxModuleBtn.setOnClickListener { onModuleSelected(UHFModuleType.GX_MODULE) }
    }
    
    /**
     * 初始化 MMKV / Initialize MMKV
     */
    private fun initializeMMKV() {
        mmkv = MMKV.defaultMMKV() ?: run {
            Log.e(TAG, "MMKV 初始化失敗 / MMKV initialization failed")
            return
        }
        Log.d(TAG, "MMKV 初始化成功 / MMKV initialized successfully")
    }
    
    /**
     * 檢查已儲存的模組類型 / Check Saved Module Type
     * 
     * Kotlin 概念: 空值安全處理 / Null safety handling
     * 1. ?. 安全呼叫：僅在不是 null 時呼叫 / Safe call: only call if not null
     * 2. ?: Elvis 運算子：提供預設值 / Elvis operator: provide default value
     * 3. let { } 範圍函數：僅在不是 null 時執行 / let scope function: execute only if not null
     */
    private fun checkSavedModuleType() {
        val savedModuleType = mmkv.decodeString(CURRENT_UHF_MODULE, "") ?: ""
        
        Log.d(TAG, "已儲存的模組類型 / Saved module type: $savedModuleType")
        
        // Kotlin 空值安全：使用 isNotEmpty() 前先確保不是 null
        // Kotlin null safety: Ensure not null before using isNotEmpty()
        if (savedModuleType.isNotEmpty()) {
            // 已經選擇過模組，直接跳轉到主頁面
            // Module already selected, navigate directly to main page
            navigateToMainWithModule(savedModuleType)
        } else {
            // 顯示模組選擇按鈕 / Show module selection buttons
            showModuleButtons()
        }
    }
    
    /**
     * 顯示模組選擇按鈕 / Show Module Selection Buttons
     * 
     * Kotlin 概念: 範圍函數 apply { }
     * - 在物件上執行多個操作 / Execute multiple operations on object  
     */
    private fun showModuleButtons() {
        Log.d(TAG, "顯示模組選擇按鈕 / Showing module selection buttons")
        
        // 使用 apply 同時設定多個屬性 / Use apply to set multiple properties
        umModuleBtn.apply {
            visibility = View.VISIBLE
            alpha = 0f
            animate().alpha(1f).duration = 300  // 淡入動畫 / Fade in animation
        }
        
        slrModuleBtn.apply {
            visibility = View.VISIBLE
            alpha = 0f
            animate().alpha(1f).setStartDelay(100).duration = 300
        }
        
        rmModuleBtn.apply {
            visibility = View.VISIBLE
            alpha = 0f
            animate().alpha(1f).setStartDelay(200).duration = 300
        }
        
        gxModuleBtn.apply {
            visibility = View.VISIBLE
            alpha = 0f
            animate().alpha(1f).setStartDelay(300).duration = 300
        }
    }
    
    /**
     * 處理模組選擇 / Handle Module Selection
     * 
     * Kotlin 概念: 函數參數和命名 / Function parameters and naming
     */
    private fun onModuleSelected(moduleType: UHFModuleType) {
        Log.d(TAG, "選擇模組類型 / Selected module type: $moduleType")
        
        // 儲存選擇的模組類型 / Save selected module type
        saveModuleType(moduleType)
        
        // 初始化選擇的 UHF 模組 / Initialize selected UHF module
        initializeSelectedModule(moduleType)
        
        // 導航到主頁面 / Navigate to main page
        navigateToMain()
    }
    
    /**
     * 儲存模組類型到 MMKV / Save Module Type to MMKV
     */
    private fun saveModuleType(moduleType: UHFModuleType) {
        try {
            mmkv.encode(CURRENT_UHF_MODULE, moduleType.name)
            Log.d(TAG, "模組類型已儲存 / Module type saved: ${moduleType.name}")
        } catch (e: Exception) {
            Log.e(TAG, "儲存模組類型失敗 / Failed to save module type", e)
        }
    }
    
    /**
     * 初始化選擇的模組 / Initialize Selected Module
     */
    private fun initializeSelectedModule(moduleType: UHFModuleType) {
        try {
            val app = BCMSApp.instance
            val uhfManager = app.uhfManager
            
            Log.d(TAG, "初始化 UHF 管理器 / Initializing UHF manager with: $moduleType")
            uhfManager.initialize(moduleType)
            
        } catch (e: Exception) {
            Log.e(TAG, "UHF 管理器初始化失敗 / UHF manager initialization failed", e)
        }
    }
    
    /**
     * 導航到主頁面 / Navigate to Main Page
     * 
     * Kotlin 概念: Intent 建立和導航 / Intent creation and navigation
     */
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            // 清除 SelectActivity 從堆疊 / Clear SelectActivity from stack
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        startActivity(intent)
        finish()  // 結束目前活動 / Finish current activity
    }
    
    /**
     * 使用已儲存的模組類型導航 / Navigate with Saved Module Type
     * 
     * Kotlin 概念: try-catch 表達式 / try-catch expression
     */
    private fun navigateToMainWithModule(savedModuleType: String) {
        try {
            // 將字串轉換為列舉 / Convert string to enum
            val moduleType = UHFModuleType.valueOf(savedModuleType)
            
            Log.d(TAG, "使用已儲存的模組類型 / Using saved module type: $moduleType")
            
            // 初始化並導航 / Initialize and navigate
            initializeSelectedModule(moduleType)
            navigateToMain()
            
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "無效的模組類型，顯示選擇畫面 / Invalid module type, showing selection screen")
            showModuleButtons()
        }
    }
}
