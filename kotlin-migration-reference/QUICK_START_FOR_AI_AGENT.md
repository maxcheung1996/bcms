# 🤖 Quick Start Guide for AI Agent

## 📋 Mission Brief
Convert the provided Java UHF RFID scanner app to modern Kotlin while preserving ALL functionality. The target is an industrial Android scanning device with UHF RFID capabilities.

## 🎯 What You're Building
A Kotlin Android app with these core features:
1. **RFID Tag Inventory** - Bulk scan multiple tags simultaneously
2. **Individual Tag Search** - Find specific tags with signal strength
3. **Tag Read/Write** - Access different memory banks (EPC, TID, USER)
4. **Tag Lock/Kill** - Security operations
5. **Settings** - Power, frequency, protocol configuration
6. **Module Support** - UM, SLR, RM, GX hardware modules

## 🏗️ Architecture Overview
```
User selects UHF module type → Initialize hardware → Main app with fragments
├── Inventory: Bulk scanning with real-time results
├── Search: Individual tag with signal strength  
├── Settings: Power, frequency, protocol config
└── Operations: Read, write, lock, kill tags
```

## 📚 Essential Reading Order
1. `documentation/PROJECT_SCOPE.md` - What to build
2. `documentation/UHF_API_REFERENCE.md` - How vendor API works
3. `documentation/KOTLIN_EXAMPLES.md` - Concrete implementation patterns
4. `documentation/IMPLEMENTATION_CHECKLIST.md` - Step-by-step guide

## 🔧 Technical Stack

### 🚨 CRITICAL: Use User's Proven Working Configuration
```
✅ Java 15 (Azul or regular)
✅ Android Gradle Plugin 3.6.3  
✅ Gradle 6.7.1
✅ NDK 21.4.7075529
✅ Compile/Target SDK 30
✅ Java 1.8 compatibility for vendor libraries
```

### Must Use (Hardware Requirements)
- **UHFJar_V1.4.05.aar** - Vendor UHF library (in vendor-libraries/)
- **iscanuserapi.jar** - Barcode scanning library
- **Exact gradle configuration** - See CRITICAL_CONFIG_UPDATE.md

### Available Stack (Limited by AGP 3.6.3)
- **Kotlin 1.8.22** with coroutines (1.5.2)
- **kotlin-android-extensions** for UI (no ViewBinding)
- **Basic StateFlow** for reactive programming  
- **AndroidX libraries** (compatible versions only)
- **Fragment-based architecture**

## 📁 Key Reference Files

### Critical Java Files to Study
- `MyApp.java` - Shows app initialization and UHF setup
- `MainActivity.java` - Fragment management and key handling
- `GetRFIDThread.java` - **MOST IMPORTANT** - Shows scanning loop logic
- `LeftFragment.java` - Main inventory implementation
- `SelectActivity.java` - Module selection and initialization

### Essential Implementation Patterns
```kotlin
// 1. UHF Initialization (from SelectActivity.java)
UHFManager.getUHFImplSigleInstance(moduleType)
uhfManager.powerOn()

// 2. Continuous Scanning (from GetRFIDThread.java)  
while (scanning) {
    val tagData = uhfManager.readTagFromBuffer()
    if (tagData != null) {
        processTagData(tagData)
    }
}

// 3. Hardware Triggers (from MainActivity.java)
override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    if (keyCode == KeyEvent.KEYCODE_F8) toggleScanning()
}
```

## ⚡ Quick Implementation Strategy

### Phase 1: Get Scanning Working (Priority 1)
1. Copy gradle configs from `gradle-configs/`
2. Copy vendor libraries to `app/libs/`
3. Create UHFManagerWrapper based on `documentation/KOTLIN_EXAMPLES.md`
4. Implement basic scanning service with coroutines
5. Test on hardware device

### Phase 2: Build UI (Priority 2)  
1. Convert module selection (SelectActivity.java → Kotlin)
2. Build main activity with fragment container
3. Create inventory fragment with RecyclerView
4. Add start/stop scanning buttons

### Phase 3: Add Features (Priority 3)
1. Tag read/write operations (reference ReadOrWriteTagFragment.java)
2. Settings management (reference PoweFrequencyFragment.java)  
3. Individual tag search (reference SearchFragment.java)
4. Data export functionality

## 🚨 Critical Warnings

### NEVER Change These:
- ❌ Vendor library files (.aar, .jar)
- ❌ UHF method signatures or parameters
- ❌ Hardware initialization sequence
- ❌ Key event codes for trigger buttons

### Always Preserve:
- ✅ Exact UHF power-on/power-off procedure
- ✅ Thread safety for UHF operations
- ✅ Battery management logic
- ✅ Module-specific configurations (UM vs SLR vs RM vs GX)

## 📊 Success Metrics
- ✅ All original features working
- ✅ Hardware trigger buttons functional  
- ✅ Scanning rate ≥50 tags/second
- ✅ Support for all module types
- ✅ Modern Kotlin codebase

## 🔍 Quick Debug Checklist
If something doesn't work:
1. Check gradle configuration matches `gradle-configs/build.gradle.app`
2. Verify vendor libraries are in `app/libs/` and recognized
3. Ensure Java 1.8 compatibility is set
4. Check UHF initialization follows exact sequence from `MyApp.java`
5. Verify permissions are added to manifest

## 💡 Implementation Tips
- Start with the simplest module type (UM_MODULE) for initial testing
- Use `MLog.e()` statements like the original for debugging
- Test each UHF operation individually before combining
- Keep UI updates on Main thread, UHF operations on background thread
- Reference the original Java files extensively - they contain working logic

## 🎯 End Goal
A production-ready Kotlin app that works identically to the original Java version but with modern architecture, better maintainability, and enhanced user experience.

---

**This package contains everything you need for a successful migration. The original Java app is fully functional - your job is to recreate its exact functionality using modern Kotlin patterns. Good luck! 🚀**
