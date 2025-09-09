# UHF RFID Scanner - Kotlin Project Scope

## 🎯 Project Goal
Create a modern Kotlin Android application that provides UHF RFID scanning capabilities for industrial scanning devices, replacing the existing Java implementation while maintaining full functionality compatibility.

## 📱 Target Application Features

### Core Functionality (MUST HAVE)
1. **Module Selection Screen**
   - Support for UM, SLR, RM, GX module types
   - Automatic module detection and configuration
   - Persistent module selection storage

2. **RFID Tag Inventory**
   - Bulk scanning of multiple RFID tags
   - Real-time tag count and read rate display
   - Duplicate tag handling and counting
   - Export functionality to Excel format

3. **Individual Tag Search**
   - Single tag detection and tracking
   - Signal strength (RSSI) visualization
   - Audio feedback for proximity indication
   - Real-time signal strength bar

4. **Tag Data Management**
   - Display EPC, TID, USER data, and RSSI values
   - Support for different data reading modes:
     - EPC Only
     - TID Only  
     - EPC + TID
     - EPC + USER
     - EPC + TID + USER
     - EPC + RFU (Reserved for Future Use)
     - RFU Only

5. **Tag Operations**
   - **Read Tag:** Read data from specific memory banks
   - **Write Tag:** Write data to tag memory banks
   - **Lock Tag:** Protect memory regions from modification
   - **Kill Tag:** Permanently disable tags

6. **Settings Management**
   - **Power Control:** Adjust transmission power (5-33 dBm range)
   - **Frequency Settings:** Regional frequency band selection
   - **Protocol Selection:** ISO18000-6C or GB standards
   - **Inventory Modes:** Different scanning optimization modes
   - **Session Management:** Configure tag session parameters

### Hardware Integration (MUST HAVE)
1. **Physical Trigger Support**
   - Handle hardware scan button presses
   - Support multiple key codes (F8, F4, BUTTON_4, etc.)
   - Toggle scanning with trigger button

2. **Power Management**
   - Battery level monitoring
   - Low power protection
   - Wake lock management
   - Automatic power optimization

3. **Device-Specific Features**
   - Temperature monitoring (for RM modules)
   - Fan control (for RM modules)
   - Charging status detection

### User Experience (SHOULD HAVE)
1. **Modern UI/UX**
   - Material Design 3 components
   - Clean, intuitive interface
   - Responsive design for different screen sizes

2. **Real-time Feedback**
   - Live tag count updates
   - Visual scanning status indicators
   - Audio feedback for successful scans

3. **Data Visualization**
   - Clear tag information display
   - Signal strength visualization
   - Scan statistics and timing

## 🚫 Out of Scope

### Features NOT to Implement
1. **Network Connectivity** - Focus on local device functionality
2. **Cloud Integration** - Keep data local to device
3. **Multi-device Support** - Single device operation only
4. **Custom Hardware Modifications** - Use vendor APIs as-is
5. **Advanced Analytics** - Basic scan statistics only

### Technologies NOT to Use
1. **Compose UI** - Keep with traditional Views for vendor library compatibility
2. **Room Database** - Use MMKV for simple data persistence
3. **Retrofit/Networking** - No network features needed
4. **Camera Integration** - Focus on UHF scanning only

## 📋 Success Criteria

### Functional Requirements
- ✅ All original Java app features working in Kotlin
- ✅ Support for all UHF module types (UM, SLR, RM, GX)
- ✅ Hardware trigger buttons functioning correctly
- ✅ Tag read/write operations successful
- ✅ Settings persistence working
- ✅ Excel export functionality operational

### Performance Requirements  
- ✅ Scanning rate: Minimum 50 tags/second (hardware dependent)
- ✅ UI responsiveness: <100ms for user interactions
- ✅ Memory usage: <100MB during normal operation
- ✅ Battery life: No degradation from original app

### Quality Requirements
- ✅ Crash-free operation during normal usage
- ✅ Proper resource cleanup on app exit
- ✅ Consistent behavior across different module types
- ✅ Clear error messages for user guidance

## 🔄 Migration Strategy

### Phase 1: Foundation (Days 1-2)
- Project setup and configuration
- Vendor library integration
- Basic UHF initialization

### Phase 2: Core Scanning (Days 3-5)
- UHF manager wrapper creation
- Scanning service implementation
- Tag data processing

### Phase 3: User Interface (Days 6-8)
- Activity and fragment conversion
- UI component implementation
- Navigation setup

### Phase 4: Tag Operations (Days 9-11)
- Read/write functionality
- Lock/unlock/kill operations
- Settings management

### Phase 5: Testing & Polish (Days 12-14)
- Hardware testing
- Performance optimization
- Bug fixes and refinements

## 📚 Reference Materials

### Essential Files to Review
1. `UHFManager` class usage patterns
2. `GetRFIDThread.java` - Threading implementation
3. `MainActivity.java` - App initialization sequence
4. `LeftFragment.java` - Main inventory implementation  
5. `PoweFrequencyFragment.java` - Settings management

### Key Concepts to Understand
1. **Module Detection:** How app identifies hardware type
2. **Hardware Initialization:** Power-on sequence and configuration
3. **Tag Reading Loop:** Continuous scanning mechanism
4. **Data Processing:** Tag data parsing and filtering
5. **Power Management:** Battery and wake lock handling

## ⚠️ Critical Warnings

### NEVER Modify These:
- Vendor library files (.aar, .jar)
- Hardware initialization sequence
- Module type detection logic
- Key event handling codes

### Always Preserve:
- Exact power-on/power-off procedures
- Thread safety in tag reading
- Battery management logic
- Hardware-specific configurations

---

This scope document ensures the migration maintains full compatibility with the original hardware while modernizing the codebase for better maintainability and development experience.
