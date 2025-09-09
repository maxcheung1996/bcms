package com.socam.bcms.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.socam.bcms.BCMSApp
import com.socam.bcms.R
import com.socam.bcms.model.TagData
import com.socam.bcms.viewmodel.UHFViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 左側片段：RFID 掃描控制 / Left Fragment: RFID Scanning Control
 * 
 * Kotlin 概念解釋 / Kotlin Concepts Explained:
 * 1. 繼承抽象類別 / Inheriting abstract class
 * 2. 覆寫抽象方法 / Overriding abstract methods  
 * 3. Handler 與 Coroutines 的整合 / Handler and Coroutines integration
 * 4. MutableList 與資料更新 / MutableList and data updates
 */
class LeftFragment : BaseFragment() {
    
    companion object {
        private const val TAG = "LeftFragment"
        
        /**
         * Kotlin 概念: companion object 中的工廠方法 / Factory method in companion object
         * - 類似 Java static 方法 / Like Java static method
         * - 但可以存取私有構造函數 / But can access private constructors
         */
        fun newInstance(): LeftFragment {
            return LeftFragment().apply {
                arguments = Bundle()  // 可以傳遞參數 / Can pass parameters
            }
        }
    }
    
    // UI 元件 / UI Components
    private lateinit var readRFIDBtn: Button
    private lateinit var clearDataBtn: Button
    private lateinit var exportBtn: Button
    private lateinit var tagNumbersTV: TextView
    private lateinit var readNumbersTV: TextView
    private lateinit var useTimesTV: TextView
    private lateinit var powerET: EditText
    private lateinit var setPowerBtn: Button
    private lateinit var scanResultsLV: ListView
    
    /**
     * 共享 ViewModel / Shared ViewModel
     * 
     * Kotlin 概念: 委託屬性 / Delegated properties
     * - by activityViewModels() 自動建立和管理 ViewModel
     * - by activityViewModels() automatically creates and manages ViewModel
     * - 在同一個 Activity 的所有 Fragment 間共享
     * - Shared among all Fragments in the same Activity
     */
    private val uhfViewModel: UHFViewModel by activityViewModels()
    
    // Handler 用於 UI 更新 / Handler for UI updates
    private val uiHandler = Handler(Looper.getMainLooper())
    
    // ListView 適配器 / ListView Adapter
    private lateinit var tagsAdapter: ArrayAdapter<String>
    private val tagDisplayList = mutableListOf<String>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_left, container, false)
    }
    
    /**
     * 實作抽象方法：初始化片段 / Implement abstract method: Initialize fragment
     * 
     * Kotlin 概念: override 關鍵字
     * - 明確表示覆寫父類別方法 / Explicitly indicates overriding parent method
     */
    override fun initializeFragment(view: View) {
        Log.d(TAG, "初始化左側片段 / Initializing left fragment")
        
        initializeViews(view)
        setupListView()
        setupClickListeners()
        observeViewModel()  // 觀察 ViewModel 變化 / Observe ViewModel changes
    }
    
    /**
     * 初始化視圖元件 / Initialize View Components
     * 
     * Kotlin 概念: with 範圍函數 / with scope function
     * - 在物件上下文中執行多個操作 / Execute multiple operations in object context
     */
    private fun initializeViews(view: View) {
        with(view) {
            readRFIDBtn = findViewById(R.id.read_RFID)
            clearDataBtn = findViewById(R.id.clear_Data)
            exportBtn = findViewById(R.id.export)
            tagNumbersTV = findViewById(R.id.tagNumbers)
            readNumbersTV = findViewById(R.id.readNumbers)
            useTimesTV = findViewById(R.id.useTimes)
            powerET = findViewById(R.id.et_power)
            setPowerBtn = findViewById(R.id.btn_power)
            scanResultsLV = findViewById(R.id.scan_results)
        }
        
        // 設定初始功率值 / Set initial power value
        powerET.setText(BCMSApp.powerSize.toString())
    }
    
    /**
     * 設定 ListView / Setup ListView
     * 
     * Kotlin 概念: 集合操作 / Collection operations
     * - mutableListOf(): 建立可變清單 / Create mutable list
     * - ArrayAdapter: 簡單的 ListView 適配器 / Simple ListView adapter
     */
    private fun setupListView() {
        tagsAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            tagDisplayList
        )
        scanResultsLV.adapter = tagsAdapter
        
        // 長按清除單個項目 / Long press to clear single item
        scanResultsLV.setOnItemLongClickListener { _, _, position, _ ->
            if (position < tagDisplayList.size) {
                val epc = tagDisplayList[position].split(" | ")[0].substringAfter("EPC: ")
                
                // 從 ViewModel 移除標籤 / Remove tag from ViewModel
                // TODO: 實作 ViewModel.removeTag() 方法 / Implement ViewModel.removeTag() method
                
                showToast("已移除標籤 / Tag removed: $epc")
                true
            } else false
        }
    }
    
    /**
     * 設定點擊監聽器 / Setup Click Listeners
     * 
     * Kotlin 概念: ViewModel 方法呼叫 / ViewModel method calls
     * - 透過 ViewModel 管理狀態 / Manage state through ViewModel  
     * - UI 只負責觸發事件 / UI only responsible for triggering events
     */
    private fun setupClickListeners() {
        readRFIDBtn.setOnClickListener { 
            if (uhfViewModel.isScanning.value == true) {
                uhfViewModel.stopScanning()
            } else {
                uhfViewModel.startScanning()
            }
        }
        
        clearDataBtn.setOnClickListener { uhfViewModel.clearScanData() }
        exportBtn.setOnClickListener { exportDataFromViewModel() }
        
        setPowerBtn.setOnClickListener { 
            val power = powerET.text.toString().toIntOrNull() ?: 5
            uhfViewModel.setPower(power)
        }
    }
    
    /**
     * 觀察 ViewModel 變化 / Observe ViewModel Changes
     * 
     * Kotlin 概念: LiveData 觀察者 / LiveData observers
     * - observe() 註冊觀察者 / Register observers with observe()
     * - 自動處理生命週期 / Automatically handles lifecycle
     * - Lambda 表達式作為觀察者 / Lambda expressions as observers
     */
    private fun observeViewModel() {
        Log.d(TAG, "設定 ViewModel 觀察者 / Setting up ViewModel observers")
        
        // 觀察掃描狀態 / Observe scanning state
        uhfViewModel.isScanning.observe(viewLifecycleOwner) { isScanning ->
            updateScanButton(isScanning)
            Log.d(TAG, "掃描狀態變更 / Scanning state changed: $isScanning")
        }
        
        // 觀察掃描到的標籤 / Observe scanned tags
        uhfViewModel.scannedTags.observe(viewLifecycleOwner) { tags ->
            updateTagsList(tags)
            Log.d(TAG, "標籤清單更新 / Tags list updated: ${tags.size} tags")
        }
        
        // 觀察清單統計 / Observe inventory statistics
        uhfViewModel.inventoryResult.observe(viewLifecycleOwner) { result ->
            updateStatistics(result)
        }
        
        // 觀察錯誤訊息 / Observe error messages  
        uhfViewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                showToast(errorMessage)
            }
        }
        
        // 觀察功率變化 / Observe power changes
        uhfViewModel.powerLevel.observe(viewLifecycleOwner) { power ->
            powerET.setText(power.toString())
        }
    }
    
    /**
     * 更新掃描按鈕 / Update Scan Button
     * 
     * Kotlin 概念: UI 狀態反應 / UI state reaction
     * - 根據 ViewModel 狀態更新 UI / Update UI based on ViewModel state  
     */
    private fun updateScanButton(isScanning: Boolean) {
        readRFIDBtn.text = if (isScanning) {
            "🛑 停止掃描 / Stop Scanning"
        } else {
            "📡 開始掃描 / Start Scanning"
        }
        
        readRFIDBtn.setBackgroundColor(
            if (isScanning) 0xFFFF5722.toInt() else 0xFF4CAF50.toInt()
        )
    }
    
    /**
     * 更新標籤清單 / Update Tags List
     * 
     * Kotlin 概念: Map 轉換為 List / Map to List conversion  
     * - values 取得所有值 / values gets all values
     * - map{} 轉換每個元素 / map{} transforms each element
     */
    private fun updateTagsList(tags: Map<String, TagData>) {
        tagDisplayList.clear()
        
        // 將 Map 轉換為顯示清單 / Convert Map to display list
        tags.values
            .sortedByDescending { it.timestamp }  // 最新的在前面 / Latest first
            .take(100)  // 限制 100 項 / Limit to 100 items
            .forEach { tag ->
                tagDisplayList.add(tag.getDisplayText())
            }
        
        tagsAdapter.notifyDataSetChanged()
    }
    
    /**
     * 更新統計資訊 / Update Statistics
     * 
     * Kotlin 概念: 資料類別解構 / Data class destructuring
     * - 直接提取屬性 / Direct property extraction
     */
    private fun updateStatistics(result: com.socam.bcms.model.InventoryResult) {
        tagNumbersTV.text = "標籤數量 / Tag Count: ${result.uniqueTags}"
        readNumbersTV.text = "讀取次數 / Read Count: ${result.totalReads}"  
        useTimesTV.text = "用時 / Duration: ${result.duration / 1000}s (${result.readRate}/s)"
    }
    
    /**
     * 從 ViewModel 匯出資料 / Export Data from ViewModel
     */
    private fun exportDataFromViewModel() {
        val csvData = uhfViewModel.getCSVData()
        
        if (csvData.lines().size <= 1) {
            showToast("⚠️ 沒有資料可匯出 / No data to export")
            return
        }
        
        Log.d(TAG, "匯出 CSV 資料 / Exporting CSV data")
        showToast("📁 資料已準備匯出 / Data ready for export")
        
        // 這裡可以實作實際的檔案儲存邏輯 / Here can implement actual file saving logic
        Log.d(TAG, "CSV 資料 / CSV Data:\n$csvData")
    }
    

    
    /**
     * 覆寫 UHF 錯誤處理 / Override UHF Error Handling
     * 
     * Kotlin 概念: ViewModel 狀態管理 / ViewModel state management
     * - 透過 ViewModel 處理錯誤 / Handle errors through ViewModel
     */
    override fun handleUHFError(error: Exception) {
        super.handleUHFError(error)
        
        // 透過 ViewModel 停止掃描 / Stop scanning through ViewModel
        if (uhfViewModel.isScanning.value == true) {
            uhfViewModel.stopScanning()
        }
        
        showToast("❌ UHF 錯誤: ${error.message}")
    }
    
    /**
     * 顯示 Toast 訊息 / Show Toast Message
     * 
     * Kotlin 概念: 擴展函數概念預覽 / Preview of extension function concept
     */
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 片段暫停時停止掃描 / Stop scanning when fragment pauses
     * 
     * 注意：現在由 ViewModel 管理掃描狀態 / Note: Scanning state now managed by ViewModel
     */
    override fun onPause() {
        super.onPause()
        // ViewModel 會自動處理生命週期 / ViewModel automatically handles lifecycle
    }
}
