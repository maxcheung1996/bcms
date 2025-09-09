# Kotlin Implementation Examples

## 🎯 Core Kotlin Classes to Implement

### 1. Application Class
```kotlin
class UHFApp : Application() {
    
    companion object {
        lateinit var instance: UHFApp
            private set
        
        // Global settings (converted from MyApp.java static variables)
        var isOpenSound = false
        var currentInvtDataType = -1
        var protocolType = 1
        var powerSize = 5
        var maxPower = 33
    }
    
    val uhfManager: UHFManagerWrapper by lazy { UHFManagerWrapper() }
    private lateinit var soundPool: SoundPool
    private var soundID: Int = 0
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize MMKV for settings persistence
        MMKV.initialize(this)
        
        // Setup sound pool for scanning feedback
        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            ).build()
        soundID = soundPool.load(this, R.raw.beep, 1)
        
        // Initialize crash handler
        MyCrashHandler.getInstance().init(this)
    }
    
    fun playSound() {
        soundPool.play(soundID, 1f, 1f, 0, 0, 1f)
    }
}
```

### 2. UHF Manager Wrapper
```kotlin
class UHFManagerWrapper {
    private var uhfManager: UHFManager? = null
    private var isInitialized = false
    
    fun initialize(moduleType: UHFModuleType): Boolean {
        return try {
            uhfManager = UHFManager.getUHFImplSigleInstance(moduleType)
            isInitialized = true
            Log.d("UHF", "UHF Manager initialized for module: $moduleType")
            true
        } catch (e: Exception) {
            Log.e("UHF", "Failed to initialize UHF Manager", e)
            false
        }
    }
    
    fun powerOn(): Boolean {
        return uhfManager?.powerOn() ?: false
    }
    
    fun powerOff(): Boolean {
        return uhfManager?.powerOff() ?: false
    }
    
    fun startInventory(): Boolean {
        return uhfManager?.startInventoryTag() ?: false
    }
    
    fun stopInventory(): Boolean {
        return uhfManager?.stopInventory() ?: false
    }
    
    fun readTagFromBuffer(): TagData? {
        val tagDataArray = uhfManager?.readTagFromBuffer()
        return tagDataArray?.let { data ->
            if (data.size >= 3) {
                TagData(
                    tid = data[0],
                    epc = data[1], 
                    rssi = parseRssi(data[2]),
                    temperature = if (data.size > 3) data[3].toIntOrNull() else null,
                    timestamp = System.currentTimeMillis()
                )
            } else null
        }
    }
    
    private fun parseRssi(rssiHex: String): Int {
        return try {
            if (UHFManager.getType() == UHFModuleType.UM_MODULE || 
                UHFManager.getType() == UHFModuleType.RM_MODULE) {
                val hb = rssiHex.substring(0, 2).toInt(16)
                val lb = rssiHex.substring(2, 4).toInt(16)
                ((hb - 256 + 1) * 256 + (lb - 256)) / 10
            } else {
                rssiHex.toInt()
            }
        } catch (e: Exception) {
            -99 // Default error value
        }
    }
    
    // Add more wrapper methods as needed...
    fun setPower(power: Int): Boolean = uhfManager?.powerSet(power) ?: false
    fun getPower(): Int = uhfManager?.powerGet() ?: -1
    fun setFrequency(region: Int): Boolean = uhfManager?.frequencyModeSet(region) ?: false
}
```

### 3. Tag Data Model
```kotlin
data class TagData(
    val tid: String,
    val epc: String,
    val rssi: Int,
    val temperature: Int? = null,
    val timestamp: Long
) {
    val isValidRssi: Boolean get() = rssi > -100 && rssi < 0
    val signalStrength: Int get() = ((rssi + 100) * 100 / 70).coerceIn(0, 100)
}

data class InventoryResult(
    val uniqueTags: Int,
    val totalReads: Long,
    val readRate: Long,
    val duration: Long
) {
    val averageRate: Double get() = if (duration > 0) totalReads.toDouble() / (duration / 1000.0) else 0.0
}
```

### 4. Scanning Service with Coroutines
```kotlin
class RFIDScanningService(
    private val uhfManager: UHFManagerWrapper
) {
    private var scanningJob: Job? = null
    private var isScanning = false
    
    private val _scanResults = MutableSharedFlow<TagData>()
    val scanResults: SharedFlow<TagData> = _scanResults.asSharedFlow()
    
    private val _scanStats = MutableStateFlow(InventoryResult(0, 0, 0, 0))
    val scanStats: StateFlow<InventoryResult> = _scanStats.asStateFlow()
    
    private var startTime = 0L
    private var totalReads = 0L
    private val tagMap = mutableMapOf<String, Int>()
    
    suspend fun startScanning() {
        if (isScanning) return
        
        if (!uhfManager.startInventory()) {
            throw Exception("Failed to start UHF inventory")
        }
        
        isScanning = true
        startTime = System.currentTimeMillis()
        totalReads = 0
        tagMap.clear()
        
        scanningJob = CoroutineScope(Dispatchers.IO).launch {
            var lastStatsUpdate = System.currentTimeMillis()
            var readsSinceLastUpdate = 0L
            
            while (isActive && isScanning) {
                val tagData = uhfManager.readTagFromBuffer()
                
                if (tagData != null) {
                    totalReads++
                    readsSinceLastUpdate++
                    
                    // Count unique tags
                    val key = if (UHFApp.currentInvtDataType == 1 || 
                                 UHFApp.currentInvtDataType == 2 || 
                                 UHFApp.currentInvtDataType == 4) {
                        tagData.tid
                    } else {
                        tagData.epc
                    }
                    
                    tagMap[key] = (tagMap[key] ?: 0) + 1
                    
                    // Emit tag data
                    _scanResults.emit(tagData)
                    
                    // Update statistics every second
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastStatsUpdate >= 1000) {
                        val duration = currentTime - startTime
                        val currentRate = readsSinceLastUpdate
                        
                        _scanStats.value = InventoryResult(
                            uniqueTags = tagMap.size,
                            totalReads = totalReads,
                            readRate = currentRate,
                            duration = duration
                        )
                        
                        lastStatsUpdate = currentTime
                        readsSinceLastUpdate = 0
                    }
                }
                
                // Small delay to prevent tight loop
                delay(10)
            }
        }
    }
    
    fun stopScanning() {
        isScanning = false
        scanningJob?.cancel()
        uhfManager.stopInventory()
        
        // Final stats update
        val duration = System.currentTimeMillis() - startTime
        _scanStats.value = InventoryResult(
            uniqueTags = tagMap.size,
            totalReads = totalReads,
            readRate = 0,
            duration = duration
        )
    }
}
```

### 5. Main Activity with ViewBinding
```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var scanningService: RFIDScanningService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        scanningService = RFIDScanningService(UHFApp.instance.uhfManager)
        
        setupFragments()
        requestPermissions()
        acquireWakeLock()
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Handle hardware trigger buttons
        when (keyCode) {
            KeyEvent.KEYCODE_F8,
            KeyEvent.KEYCODE_F4,
            KeyEvent.KEYCODE_BUTTON_4,
            KeyEvent.KEYCODE_PROG_RED,
            KeyEvent.KEYCODE_BUTTON_3 -> {
                toggleScanning()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
    
    private fun toggleScanning() {
        lifecycleScope.launch {
            try {
                if (scanningService.isScanning) {
                    scanningService.stopScanning()
                } else {
                    scanningService.startScanning()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to toggle scanning", e)
                Toast.makeText(this@MainActivity, "Scanning error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch {
            scanningService.stopScanning()
            UHFApp.instance.uhfManager.powerOff()
        }
        releaseWakeLock()
    }
}
```

### 6. Inventory Fragment with StateFlow
```kotlin
class InventoryFragment : Fragment() {
    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var scanningService: RFIDScanningService
    private lateinit var tagAdapter: TagListAdapter
    private val tagList = mutableListOf<TagData>()
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        scanningService = RFIDScanningService(UHFApp.instance.uhfManager)
        setupRecyclerView()
        observeScanning()
        
        binding.btnStartStop.setOnClickListener {
            toggleScanning()
        }
        
        binding.btnClear.setOnClickListener {
            clearData()
        }
    }
    
    private fun setupRecyclerView() {
        tagAdapter = TagListAdapter(tagList)
        binding.recyclerViewTags.adapter = tagAdapter
    }
    
    private fun observeScanning() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Observe scan results
            scanningService.scanResults.collect { tagData ->
                updateTagList(tagData)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            // Observe scan statistics
            scanningService.scanStats.collect { stats ->
                updateStatistics(stats)
            }
        }
    }
    
    private fun updateTagList(tagData: TagData) {
        val existingIndex = tagList.indexOfFirst { it.epc == tagData.epc }
        if (existingIndex >= 0) {
            tagList[existingIndex] = tagData
            tagAdapter.notifyItemChanged(existingIndex)
        } else {
            tagList.add(tagData)
            tagAdapter.notifyItemInserted(tagList.size - 1)
        }
    }
    
    private fun updateStatistics(stats: InventoryResult) {
        binding.apply {
            txtTagCount.text = "Tags: ${stats.uniqueTags}"
            txtReadRate.text = "Rate: ${stats.readRate}/s"
            txtDuration.text = "Time: ${stats.duration}ms"
        }
    }
    
    private fun toggleScanning() {
        lifecycleScope.launch {
            try {
                if (scanningService.isScanning) {
                    scanningService.stopScanning()
                    binding.btnStartStop.text = "Start Inventory"
                } else {
                    scanningService.startScanning()
                    binding.btnStartStop.text = "Stop Inventory" 
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

### 7. Settings Management
```kotlin
class SettingsRepository(private val uhfManager: UHFManagerWrapper) {
    private val mmkv = MMKV.defaultMMKV()
    
    // Power settings
    suspend fun setPower(power: Int): Boolean = withContext(Dispatchers.IO) {
        val success = uhfManager.setPower(power)
        if (success) {
            mmkv.encode("power", power)
            UHFApp.powerSize = power
        }
        success
    }
    
    suspend fun getPower(): Int = withContext(Dispatchers.IO) {
        uhfManager.getPower()
    }
    
    // Frequency settings
    suspend fun setFrequency(region: Int): Boolean = withContext(Dispatchers.IO) {
        val success = uhfManager.setFrequency(region)
        if (success) {
            mmkv.encode("frequency", region)
        }
        success
    }
    
    // Save and restore all settings
    fun saveSettings() {
        mmkv.encode("openSound", UHFApp.isOpenSound)
        mmkv.encode("dataType", UHFApp.currentInvtDataType)
        mmkv.encode("protocol", UHFApp.protocolType)
    }
    
    fun restoreSettings() {
        UHFApp.isOpenSound = mmkv.decodeBool("openSound", false)
        UHFApp.currentInvtDataType = mmkv.decodeInt("dataType", 0)
        UHFApp.protocolType = mmkv.decodeInt("protocol", 1)
        UHFApp.powerSize = mmkv.decodeInt("power", 33)
    }
}
```

### 8. Module Selection Activity
```kotlin
class ModuleSelectionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityModuleSelectionBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModuleSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        checkExistingModule()
        setupModuleButtons()
    }
    
    private fun checkExistingModule() {
        val mmkv = MMKV.defaultMMKV()
        val savedModule = mmkv.decodeString("CURRENT_UHF_MODULE", "")
        
        if (savedModule.isNotEmpty()) {
            try {
                val moduleType = UHFModuleType.valueOf(savedModule)
                initializeAndProceed(moduleType)
                return
            } catch (e: Exception) {
                Log.e("ModuleSelection", "Invalid saved module type", e)
            }
        }
        
        // Show module selection buttons
        showModuleSelection()
    }
    
    private fun showModuleSelection() {
        binding.apply {
            btnUmModule.visibility = View.VISIBLE
            btnSlrModule.visibility = View.VISIBLE  
            btnRmModule.visibility = View.VISIBLE
            btnGxModule.visibility = View.VISIBLE
        }
    }
    
    private fun setupModuleButtons() {
        binding.btnUmModule.setOnClickListener { selectModule(UHFModuleType.UM_MODULE) }
        binding.btnSlrModule.setOnClickListener { selectModule(UHFModuleType.SLR_MODULE) }
        binding.btnRmModule.setOnClickListener { selectModule(UHFModuleType.RM_MODULE) }
        binding.btnGxModule.setOnClickListener { selectModule(UHFModuleType.GX_MODULE) }
    }
    
    private fun selectModule(moduleType: UHFModuleType) {
        lifecycleScope.launch {
            try {
                showProgress("Initializing ${moduleType.name}...")
                
                val success = withContext(Dispatchers.IO) {
                    UHFApp.instance.uhfManager.initialize(moduleType)
                }
                
                if (success) {
                    // Save module selection
                    MMKV.defaultMMKV().encode("CURRENT_UHF_MODULE", moduleType.name)
                    
                    hideProgress()
                    proceedToMainActivity()
                } else {
                    hideProgress()
                    showError("Failed to initialize UHF module")
                }
            } catch (e: Exception) {
                hideProgress()
                showError("Error: ${e.message}")
            }
        }
    }
    
    private fun proceedToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
```

## 🔧 Key Conversion Patterns

### Java Static Variables → Kotlin Companion Objects
```kotlin
// Java: public static boolean ifOpenSound = false;
// Kotlin:
companion object {
    var isOpenSound = false
}
```

### Java Threads → Kotlin Coroutines  
```kotlin
// Java: extends Thread with while(flag) loop
// Kotlin: 
CoroutineScope(Dispatchers.IO).launch {
    while (isActive) {
        // Scanning logic
        delay(10)
    }
}
```

### Java Callbacks → Kotlin Flow
```kotlin
// Java: interface BackResult { void postResult(String[] data); }
// Kotlin:
private val _scanResults = MutableSharedFlow<TagData>()
val scanResults: SharedFlow<TagData> = _scanResults.asSharedFlow()
```

### Java findViewById → ViewBinding
```kotlin
// Java: Button btn = findViewById(R.id.button);
// Kotlin: binding.button.setOnClickListener { }
```

## ⚠️ Critical Conversion Notes

1. **Preserve Method Signatures:** When calling UHF methods, keep exact parameter types and order
2. **Thread Safety:** Always use Dispatchers.IO for UHF operations
3. **Lifecycle Management:** Properly handle fragment/activity lifecycle with coroutines
4. **Error Handling:** Wrap UHF calls in try-catch blocks
5. **Resource Cleanup:** Cancel coroutines and release UHF resources properly

## 🎯 Testing Strategy for Kotlin Implementation

```kotlin
class UHFManagerTest {
    private lateinit var uhfManager: UHFManagerWrapper
    
    @Before
    fun setup() {
        uhfManager = UHFManagerWrapper()
        // Initialize with test module type
        uhfManager.initialize(UHFModuleType.UM_MODULE)
    }
    
    @Test
    fun testPowerOperations() {
        assertTrue(uhfManager.powerOn())
        assertTrue(uhfManager.setPower(20))
        assertEquals(20, uhfManager.getPower())
        assertTrue(uhfManager.powerOff())
    }
    
    @Test  
    fun testScanningOperations() {
        uhfManager.powerOn()
        assertTrue(uhfManager.startInventory())
        
        // Test tag reading (requires physical tags)
        val tagData = uhfManager.readTagFromBuffer()
        // tagData can be null if no tags present
        
        assertTrue(uhfManager.stopInventory())
        uhfManager.powerOff()
    }
}
```

This guide provides concrete examples of how to convert the Java implementation to modern Kotlin while preserving all UHF functionality.
