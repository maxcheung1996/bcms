package com.socam.bcms.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.socam.bcms.BCMSApp
import com.socam.bcms.uhf.UHFManagerWrapper
import kotlinx.coroutines.launch

/**
 * 基礎片段類別 / Base Fragment Class
 * 
 * Kotlin 概念解釋 / Kotlin Concepts Explained:
 * 1. abstract class: 抽象類別，不能直接實例化
 *    abstract class: Cannot be instantiated directly
 *    
 * 2. open vs abstract: 
 *    - open: 可以被覆寫 / Can be overridden
 *    - abstract: 必須被覆寫 / Must be overridden
 *    
 * 3. 屬性委託: by lazy 延遲初始化 / Property delegation: by lazy initialization
 */
abstract class BaseFragment : Fragment() {
    
    companion object {
        private const val TAG = "BaseFragment"
    }
    
    /**
     * UHF 管理器引用 / UHF Manager Reference
     * 
     * Kotlin 概念: protected 可見性
     * - 子類別可以存取 / Subclasses can access
     * - 比 Java 的 protected 更清晰 / Clearer than Java's protected
     */
    protected val uhfManager: UHFManagerWrapper by lazy {
        BCMSApp.instance.uhfManager
    }
    
    /**
     * 應用程式實例 / Application Instance
     */
    protected val app: BCMSApp by lazy {
        BCMSApp.instance
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "${this::class.simpleName} 視圖已建立 / View created")
        
        // 初始化片段 / Initialize fragment
        initializeFragment(view)
        
        // 設定 UHF 事件監聽 / Setup UHF event listeners
        setupUHFListeners()
    }
    
    /**
     * 抽象方法：子類別必須實作 / Abstract method: Must be implemented by subclasses
     */
    abstract fun initializeFragment(view: View)
    
    /**
     * 虛擬方法：子類別可以覆寫 / Virtual method: Can be overridden by subclasses
     * 
     * Kotlin 概念: open 函數
     * - 允許子類別覆寫 / Allows subclass override
     * - 預設情況下 Kotlin 函數是 final / By default Kotlin functions are final
     */
    open fun setupUHFListeners() {
        // 預設實作，子類別可以覆寫 / Default implementation, subclasses can override
        Log.d(TAG, "${this::class.simpleName} UHF 監聽器設定完成 / UHF listeners setup completed")
    }
    
    /**
     * 協程輔助函數 / Coroutine Helper Functions
     * 
     * Kotlin 概念: 擴展函數 / Extension functions
     * - 為現有類別新增功能 / Add functionality to existing classes
     * - 不需要修改原始類別 / No need to modify original class
     */
    protected fun runOnUHFThread(action: suspend () -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                action()
            } catch (e: Exception) {
                Log.e(TAG, "${this@BaseFragment::class.simpleName} UHF 操作錯誤 / UHF operation error", e)
                handleUHFError(e)
            }
        }
    }
    
    /**
     * 處理 UHF 錯誤 / Handle UHF Errors
     * 
     * Kotlin 概念: 虛擬函數與預設實作 / Virtual function with default implementation
     */
    open fun handleUHFError(error: Exception) {
        Log.w(TAG, "UHF 錯誤 / UHF Error: ${error.message}")
        // 子類別可以覆寫此方法來提供特定的錯誤處理
        // Subclasses can override this method to provide specific error handling
    }
    
    /**
     * 安全的 UHF 操作 / Safe UHF Operations
     * 
     * Kotlin 概念: 內聯函數修正 / Inline function correction
     * - 移除 inline 以避免存取私有成員問題 / Remove inline to avoid private member access issues
     */
    protected fun safeUHFOperation(operation: () -> Unit) {
        try {
            if (uhfManager.isReady()) {
                operation()
            } else {
                Log.w(TAG, "UHF 管理器尚未準備就緒 / UHF manager not ready")
            }
        } catch (e: Exception) {
            Log.e(TAG, "UHF 操作失敗 / UHF operation failed", e)
            handleUHFError(e)
        }
    }
    
    /**
     * 記錄片段生命週期 / Log Fragment Lifecycle
     */
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "${this::class.simpleName} 已恢復 / Resumed")
    }
    
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "${this::class.simpleName} 已暫停 / Paused")
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "${this::class.simpleName} 視圖已銷毀 / View destroyed")
    }
}
