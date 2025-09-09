package com.socam.bcms.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.socam.bcms.BCMSApp
import com.socam.bcms.model.TagData
import com.socam.bcms.model.InventoryResult
import com.socam.bcms.repository.UHFRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

/**
 * UHF 掃描 ViewModel (進階版) / UHF Scanning ViewModel (Advanced)
 * 
 * Kotlin 概念解釋 / Kotlin Concepts Explained:
 * 1. AndroidViewModel: 可存取 Application Context 的 ViewModel
 * 2. Repository 模式: 資料層抽象 / Repository pattern: Data layer abstraction
 * 3. Flow 轉 LiveData: asLiveData() 轉換 / Flow to LiveData conversion
 * 4. Channel 通訊: 協程間資料傳遞 / Channel communication
 * 5. 進階協程: timeout, cancel, exception handling
 */
class UHFViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "UHFViewModel"
        private const val SCAN_TIMEOUT_MS = 30_000L
    }
    
    // Repository 實例
    private val repository = UHFRepository.getInstance(application)
    
    // 錯誤處理 Channel
    private val errorChannel = Channel<String>(Channel.UNLIMITED)
    
    // Flow 轉 LiveData
    val isScanning: LiveData<Boolean> = repository.scanningState.asLiveData()
    val scannedTags: LiveData<Map<String, TagData>> = repository.scannedTags.asLiveData()
    
    // 錯誤訊息
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    // 功率等級
    private val _powerLevel = MutableLiveData<Int>(BCMSApp.powerSize)
    val powerLevel: LiveData<Int> = _powerLevel
    
    // 掃描統計
    private val _inventoryResult = MutableLiveData<InventoryResult>()
    val inventoryResult: LiveData<InventoryResult> = _inventoryResult
    
    init {
        repository.initialize()
        startErrorHandling()
        startStatisticsUpdates()
        Log.d(TAG, "UHFViewModel 初始化完成")
    }
    
    /**
     * 開始掃描 / Start Scanning
     */
    fun startScanning() {
        if (isScanning.value == true) return
        
        viewModelScope.launch {
            try {
                withTimeout(SCAN_TIMEOUT_MS) {
                    val result = repository.startScanning()
                    if (!result) {
                        sendError("掃描啟動失敗")
                    }
                }
            } catch (e: TimeoutCancellationException) {
                sendError("掃描啟動超時")
            } catch (e: Exception) {
                sendError("掃描錯誤: ${e.message}")
            }
        }
    }
    
    /**
     * 停止掃描 / Stop Scanning
     */
    fun stopScanning() {
        if (isScanning.value != true) return
        
        viewModelScope.launch {
            try {
                val result = repository.stopScanning()
                if (!result) {
                    sendError("掃描停止失敗")
                }
            } catch (e: Exception) {
                sendError("停止錯誤: ${e.message}")
            }
        }
    }
    
    /**
     * 設定功率 / Set Power
     */
    fun setPower(power: Int) {
        viewModelScope.launch {
            try {
                val result = repository.setPower(power)
                if (result) {
                    _powerLevel.value = power
                } else {
                    sendError("功率設定失敗")
                }
            } catch (e: Exception) {
                sendError("功率錯誤: ${e.message}")
            }
        }
    }
    
    /**
     * 清除掃描資料 / Clear Scan Data
     */
    fun clearScanData() {
        repository.clearScanData()
        _inventoryResult.value = InventoryResult(0, 0, 0, 0)
    }
    
    /**
     * 取得 CSV 資料 / Get CSV Data
     */
    fun getCSVData(): String = repository.getCSVData()
    
    /**
     * 啟動錯誤處理 / Start Error Handling
     */
    private fun startErrorHandling() {
        viewModelScope.launch {
            errorChannel.consumeAsFlow().collect { error ->
                _errorMessage.value = error
            }
        }
    }
    
    /**
     * 發送錯誤到 Channel / Send Error to Channel
     */
    private suspend fun sendError(error: String) {
        errorChannel.trySend(error)
    }
    
    /**
     * 啟動統計更新 / Start Statistics Updates
     */
    private fun startStatisticsUpdates() {
        viewModelScope.launch {
            combine(
                repository.scanningState,
                repository.scannedTags
            ) { isScanning, tags ->
                if (isScanning && tags.isNotEmpty()) {
                    InventoryResult(
                        uniqueTags = tags.size,
                        totalReads = tags.size.toLong(),
                        readRate = 0L,
                        duration = 0L
                    )
                } else {
                    InventoryResult(0, 0, 0, 0)
                }
            }.collect { result ->
                _inventoryResult.value = result
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel 被清除")
        
        if (isScanning.value == true) {
            viewModelScope.launch {
                repository.stopScanning()
            }
        }
        
        repository.cleanup()
        errorChannel.close()
    }
}



