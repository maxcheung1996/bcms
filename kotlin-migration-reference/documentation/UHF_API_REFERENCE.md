# UHF API Reference Guide

## 🔌 UHFManager Core API Methods

This document explains the key methods from the vendor's UHFManager class that you'll need to use in your Kotlin implementation.

## 🏭 Hardware Control Methods

### Power Management
```java
// Initialize UHF hardware - CRITICAL FIRST STEP
boolean powerOn()

// Shutdown UHF hardware - CRITICAL LAST STEP  
boolean powerOff()

// Set transmission power (5-33 dBm)
boolean powerSet(int power)

// Get current power level
int powerGet()

// Check if device has trigger button
boolean isIfHaveTrigger()
```

### Module Information
```java
// Get hardware version string
String hardwareVerGet()

// Get firmware version
String firmwareVerGet()

// Get UHF module type string
String getUHFModuleType()

// Get module temperature (RM modules only)
String getModuleTemp()
```

## 📡 Scanning Operations

### Inventory (Bulk Scanning)
```java
// Start continuous tag scanning
boolean startInventoryTag()

// Stop tag scanning
boolean stopInventory()

// Read tag data from internal buffer
String[] readTagFromBuffer()
// Returns: [TID/EPC, EPC, RSSI, Temperature(RM only)]

// Set inventory mode for SLR modules  
boolean slrInventoryModeSet(int mode)
// Modes: 0=Single, 2=Dual, 3=Dense, 4=Max, 5=User, 6=Auto
```

### Individual Tag Operations
```java
// Read data from specific tag memory bank
String readTag(String password, int filterBank, int filterAddress, 
               int filterLength, String filterData, int memoryBank, 
               int startAddress, int length)

// Write data to specific tag memory bank
boolean writeTag(String password, int filterBank, int filterAddress,
                 int filterLength, String filterData, int memoryBank,
                 int startAddress, int length, String data)

// Write data directly to EPC memory  
boolean writeDataToEpc(String password, int startAddress, int length, String data)
```

## 🔒 Security Operations

### Tag Locking
```java
// Lock specific memory regions
boolean lockTag(String password, int lockType, int lockRegion)

// Lock 6B protocol tags
boolean lockTag6B(int address, String tid)
```

### Tag Killing
```java
// Permanently disable tag
boolean killTag(String password, String killPassword)
```

## ⚙️ Configuration Methods

### Frequency Management
```java
// Set frequency region (0=840MHz, 1=920MHz, 2=EU, 3=US)
boolean frequencyModeSet(int region)

// Get current frequency region
int frequencyModeGetNotFixedFreq()

// Set specific frequency range
int frequenceRange_Set(int startChannel, int channelCount, int[] frequencies, int region)

// Get current frequency range
int[] frequenceRange_Get()
```

### Reading Mode Configuration
```java
// Configure what data to read from tags
boolean readTagModeSet(int mode, int startAddress, int length, int option)
// Modes:
// 0 = EPC only
// 1 = TID only  
// 2 = USER data
// 3 = TID + USER
// 4 = EPC + TID + USER
// 5 = RFU (Reserved for Future Use)
```

### Session Management
```java
// Set session mode (0-3)
boolean sessionModeSet(int session)

// Get current session mode
int sessionModeGet()
```

### Protocol Configuration
```java
// Set RFID protocol (0=ISO18000-6C, 1=GB, 2=6B)
boolean setRFIDProtocolStandard(int protocol)

// Get current protocol
int getRFIDProtocolStandard()
```

## 🔋 Power Control

### Advanced Power Settings
```java
// Set separate read/write power levels
boolean setReadWritePower(int readPower, int writePower)

// Get current read/write power levels  
int[] getReadWritePower()

// Set power transmission for testing
boolean powerTransmitted(int power)
```

### Device Configuration
```java
// Change device configuration
boolean changeConfig(boolean enable)

// Set high baud rate (SLR modules only)
boolean isHighUHFBaud(boolean enable)
```

## 🌡️ Environmental Controls (RM Module Only)

### Temperature Management
```java
// Open cooling fan
boolean openFan()

// Close cooling fan  
boolean closeFan()
```

## 📊 Data Processing

### Tag Data Array Format
When `readTagFromBuffer()` returns data, the array contains:
- `[0]` = TID or primary data (depends on read mode)
- `[1]` = EPC data
- `[2]` = RSSI value (signal strength)  
- `[3]` = Temperature (RM modules only)

### RSSI Calculation (for UM/RM modules)
```java
// Convert RSSI hex to dBm
String rssi = tagData[2];
int Hb = Integer.parseInt(rssi.substring(0, 2), 16);
int Lb = Integer.parseInt(rssi.substring(2, 4), 16);
int rssi_dBm = ((Hb - 256 + 1) * 256 + (Lb - 256)) / 10;
```

## 🎯 Memory Bank Constants

Use these constants for memory bank operations:
```java
public enum BankEnum {
    RESERVED(0),  // Access/Kill passwords
    UII(1),       // EPC memory  
    TID(2),       // Tag identifier
    USER(3);      // User data
}
```

## ⚠️ Critical Implementation Notes

### 1. Hardware Initialization Sequence
```java
// MUST follow this exact order:
1. UHFManager.getUHFImplSigleInstance(moduleType)
2. uhfManager.powerOn()  
3. uhfManager.getModuleInfo() // For module detection
4. uhfManager.startInventoryTag() // To begin scanning
```

### 2. Threading Requirements
- **NEVER call UHF methods on UI thread**
- Use background thread for all UHF operations
- Post results back to UI thread for display

### 3. Module-Specific Behavior
```java
// Different modules require different handling:
if (UHFManager.getType() == UHFModuleType.RM_MODULE) {
    // RM modules support temperature monitoring
    uhfManager.getRFIDProtocolStandard();
} else if (UHFManager.getType() == UHFModuleType.SLR_MODULE) {
    // SLR modules have advanced inventory modes  
    uhfManager.slrInventoryModeSet(3);
}
```

### 4. Error Handling
- Always check return values from UHF methods
- `null` return values indicate operation failure
- Boolean methods return `false` for failure

### 5. Resource Cleanup
```java
// MUST call when app exits:
uhfManager.stopInventory();
uhfManager.powerOff();
```

## 🔧 Kotlin Conversion Tips

### 1. Null Safety
```kotlin
// Java: String[] tagData = uhfManager.readTagFromBuffer();
// Kotlin: val tagData: Array<String>? = uhfManager.readTagFromBuffer()

tagData?.let { data ->
    // Process tag data safely
}
```

### 2. Coroutines vs Threads
```kotlin
// Replace Java Thread with Coroutines
class RFIDScanningService {
    private var scanningJob: Job? = null
    
    suspend fun startScanning() = withContext(Dispatchers.IO) {
        while (isActive) {
            val tagData = uhfManager.readTagFromBuffer()
            tagData?.let { data ->
                withContext(Dispatchers.Main) {
                    updateUI(data)
                }
            }
            delay(10)
        }
    }
}
```

### 3. Data Classes
```kotlin
data class TagData(
    val tid: String,
    val epc: String, 
    val rssi: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class ScanSettings(
    val power: Int = 33,
    val frequency: Int = 1,
    val sessionMode: Int = 0,
    val readMode: Int = 0
)
```

### 4. Enum Classes
```kotlin
enum class UHFModuleType {
    UM_MODULE,
    SLR_MODULE, 
    RM_MODULE,
    GX_MODULE
}

enum class MemoryBank(val value: Int) {
    RESERVED(0),
    EPC(1),
    TID(2), 
    USER(3)
}
```

## 📝 Implementation Checklist

### ✅ Core UHF Integration
- [ ] UHFManager wrapper class
- [ ] Module type detection
- [ ] Hardware initialization
- [ ] Power management

### ✅ Scanning Engine  
- [ ] Continuous scanning service
- [ ] Tag data processing
- [ ] Callback mechanism
- [ ] Error handling

### ✅ UI Components
- [ ] Module selection screen
- [ ] Main scanning interface  
- [ ] Settings management
- [ ] Data export functionality

### ✅ Hardware Features
- [ ] Physical trigger button support
- [ ] Battery monitoring
- [ ] Wake lock management
- [ ] Device-specific optimizations

---

This API reference provides all the essential information needed to implement UHF RFID functionality in Kotlin while maintaining compatibility with the vendor's hardware and libraries.
