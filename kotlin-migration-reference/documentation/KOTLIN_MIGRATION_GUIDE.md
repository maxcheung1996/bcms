# UHF RFID Scanner - Kotlin Migration Guide

## 📋 Project Overview

This guide contains all the necessary information to migrate the UHFDemo Java application to a modern Kotlin Android application while preserving all UHF RFID scanning functionality.

### 🎯 Target Features to Implement

1. **RFID Tag Inventory** - Bulk scanning and reading multiple tags
2. **Individual Tag Search** - Finding and locating specific tags with signal strength  
3. **Tag Reading/Writing** - Read and write data to different memory banks (EPC, TID, USER, RESERVED)
4. **Tag Locking/Unlocking** - Security operations to protect tag data
5. **Tag Killing** - Permanently disabling tags
6. **Settings Management** - Power control, frequency settings, protocol configuration

### 🏗️ Architecture Overview

The original Java app follows this structure:
```
SelectActivity (Module Selection) 
    ↓
MainActivity (Fragment Container)
    ├── LeftFragment (Main Inventory)
    ├── RightFragment (Settings Container)
    ├── SearchFragment (Individual Tag Search)
    └── InventoryFragment (Alternative Inventory View)
```

## 🔧 Technical Requirements

### Android Configuration
- **Minimum SDK Version:** 23 (Android 6.0)
- **Target SDK Version:** 30+ (recommended 34)
- **Compile SDK Version:** 34+
- **Java Compatibility:** 1.8 (for vendor libraries)
- **Kotlin Version:** 1.9.0+

### Essential Dependencies
```gradle
// Core Android
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'androidx.fragment:fragment-ktx:1.6.2'
implementation 'androidx.activity:activity-ktx:1.8.2'
implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'

// Vendor Libraries (CRITICAL - DO NOT CHANGE VERSIONS)
implementation files('libs/UHFJar_V1.4.05.aar')
implementation files('libs/iscanuserapi.jar')

// Data Persistence
implementation 'com.tencent:mmkv:1.2.11'

// Excel Export
implementation 'net.sourceforge.jexcelapi:jxl:2.6.12'

// Coroutines
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
```

### Critical Gradle Configuration
```gradle
android {
    compileSdk 34
    
    defaultConfig {
        minSdk 23
        targetSdk 34
    }
    
    // CRITICAL: Keep for vendor libraries
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = '1.8'
    }
    
    // CRITICAL: For JNI libraries
    sourceSets {
        main {
            jniLibs.srcDirs = ['libs']
        }
    }
}
```

## 📱 Required Permissions

Copy these permissions to your AndroidManifest.xml:
```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WAKE_LOCK"/>
```

## 🔌 UHF Module Types Explained

### UM Module (Universal Module)
- **Use Case:** Basic RFID scanning
- **Features:** Standard tag inventory, basic read/write
- **Power Range:** 0-25 or 5-30 (depending on firmware)

### SLR Module (Serial Link Reader)
- **Use Case:** High-performance applications  
- **Sub-types:**
  - SLR5100: Basic model with limited features
  - SLR7100/3100/3600/3700: Advanced models with full features
- **Features:** Advanced inventory modes, higher scanning speed
- **Power Range:** 5-30

### RM Module (RFID Module)
- **Use Case:** Industrial applications with environmental monitoring
- **Features:** Temperature monitoring, protocol switching (ISO/GB)
- **Power Range:** 10-33
- **Special:** Fan control for temperature management

### GX Module (GuoXin Module)
- **Use Case:** Specialized applications requiring 6B protocol
- **Features:** 6B protocol support, custom inventory modes
- **Power Range:** Varies by implementation

## 💾 Vendor Library Integration

### UHFManager Class Structure
The UHFManager (from UHFJar_V1.4.05.aar) provides these key methods:

**Hardware Control:**
- `powerOn()` - Initialize UHF hardware
- `powerOff()` - Shutdown UHF hardware  
- `powerSet(int)` - Set transmission power
- `powerGet()` - Get current power level

**Inventory Operations:**
- `startInventoryTag()` - Begin tag scanning
- `stopInventory()` - Stop tag scanning
- `readTagFromBuffer()` - Get scanned tag data

**Tag Operations:**
- `readTag(...)` - Read specific tag memory
- `writeTag(...)` - Write data to tag memory
- `lockTag(...)` - Lock tag memory regions
- `killTag(...)` - Permanently disable tag

**Configuration:**
- `frequencyModeSet(int)` - Set frequency region
- `sessionModeSet(int)` - Set scanning session mode
- `readTagModeSet(...)` - Configure tag reading mode

## 🧵 Threading Model

### Original Java Implementation
- Uses `GetRFIDThread` class extending Thread
- Continuous polling loop for tag detection
- Callback mechanism for UI updates

### Recommended Kotlin Implementation
```kotlin
class RFIDScanningService {
    private var scanningJob: Job? = null
    
    suspend fun startScanning(callback: (TagData) -> Unit) {
        scanningJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val tagData = uhfManager.readTagFromBuffer()
                if (tagData != null) {
                    withContext(Dispatchers.Main) {
                        callback(parseTagData(tagData))
                    }
                }
                delay(10) // Prevent tight loop
            }
        }
    }
    
    fun stopScanning() {
        scanningJob?.cancel()
    }
}
```

## 🏷️ Tag Data Structure

RFID tags contain multiple memory banks:
- **EPC (Electronic Product Code):** Primary identifier
- **TID (Tag Identifier):** Unique tag serial number
- **USER:** Custom application data
- **RESERVED:** Access passwords and kill passwords

### Data Format Example
```kotlin
data class TagData(
    val epc: String,      // "1234567890ABCDEF12345678"
    val tid: String,      // "E2801160600002040000002F"
    val userData: String, // Custom data
    val rssi: String,     // Signal strength "-45"
    val timestamp: Long   // When tag was read
)
```

## 🎛️ Hardware Key Handling

The scanning device has physical trigger buttons that generate KeyEvents:
- **F8, F4, BUTTON_4, PROG_RED, BUTTON_3:** Start/stop scanning
- **BUTTON_2:** Barcode scanning (if available)

### Implementation
```kotlin
override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
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
```

## ⚡ Power Management

### Battery Monitoring
The device requires special battery management:
- Monitor low battery states
- Automatic scanning shutdown on low power
- Wake lock management to prevent sleep

### Power Control Integration
```kotlin
class PowerManager {
    fun acquireWakeLock(context: Context)
    fun releaseWakeLock()
    fun monitorBatteryLevel(callback: (Int) -> Unit)
}
```

## 📊 Data Export Features

The app supports exporting scan results to Excel format:
- Uses JXL library for Excel generation
- Exports different data combinations based on scan mode
- Timestamps and file naming conventions

## 🚨 Critical Migration Notes

### DO NOT CHANGE:
1. **Vendor library versions** - UHFJar_V1.4.05.aar MUST remain as-is
2. **Hardware initialization sequence** - Follow exact power-on procedure
3. **Module detection logic** - Each module type requires specific handling
4. **Key event codes** - Hardware-specific button mappings

### CAN BE MODERNIZED:
1. **UI Framework** - Use modern Android UI patterns
2. **Threading** - Replace Java Thread with Coroutines  
3. **Data Binding** - Use ViewBinding/DataBinding
4. **Architecture** - Implement MVVM with ViewModel
5. **Navigation** - Use Navigation Component

## 📋 Implementation Phases

### Phase 1: Core Setup (Priority: HIGH)
1. Project configuration and dependencies
2. Vendor library integration
3. Basic UHF initialization

### Phase 2: Scanning Engine (Priority: HIGH)  
1. UHF manager wrapper
2. Scanning service with coroutines
3. Tag data models and parsing

### Phase 3: UI Components (Priority: MEDIUM)
1. Module selection activity
2. Main activity with fragments
3. Inventory and search screens

### Phase 4: Tag Operations (Priority: MEDIUM)
1. Read/write functionality
2. Lock/unlock operations  
3. Kill tag implementation

### Phase 5: Advanced Features (Priority: LOW)
1. Settings management
2. Data export functionality
3. UI enhancements

## 🔍 Testing Strategy

### Device Testing Requirements:
- Test on actual UHF scanning hardware
- Verify each module type (UM/SLR/RM/GX)
- Test physical trigger buttons
- Validate power management
- Test tag read/write operations

### Performance Targets:
- **Scanning Rate:** 100+ tags per second (hardware dependent)
- **Response Time:** <100ms for tag detection
- **Battery Life:** Maintain original device battery performance
- **Memory Usage:** <100MB for normal operation

---

**⚠️ IMPORTANT:** This migration preserves all original functionality while modernizing the codebase. The vendor libraries are black-box dependencies that MUST be used as-is. Focus on creating clean Kotlin wrappers around the existing Java APIs.
