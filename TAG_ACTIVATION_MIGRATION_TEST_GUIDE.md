# Tag Activation Migration Test Guide

## Quick Test Steps

### 1. Build and Install
```bash
# Build the APK
./gradlew assembleDebug

# Install on device (replace with your device)
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 2. Verify Migration
Launch the app and check logcat for migration messages:

```bash
adb logcat -s DatabaseManager:D TagActivationViewModel:D
```

**Expected log output:**
```
DatabaseManager: Checking for database migrations...
DatabaseManager: BCTypeSerialNumbers table not found, will create it
DatabaseManager: Creating BCTypeSerialNumbers table...
DatabaseManager: BCTypeSerialNumbers table created successfully
DatabaseManager: Database migrations completed
```

### 3. Test Tag Activation

#### Test Case 1: First Tag Activation
1. Login to the app
2. Go to **Tag Activation** module
3. Select BC Type: **MIC**
4. Scan and activate a tag
5. **Expected:** Tag number ends with `...010001`

#### Test Case 2: Second Tag Activation (Same BC Type)
1. Scan and activate another tag with **MIC**
2. **Expected:** Tag number ends with `...010002`

#### Test Case 3: Third Tag Activation (Same BC Type)
1. Scan and activate another tag with **MIC**
2. **Expected:** Tag number ends with `...010003`

#### Test Case 4: Different BC Type
1. Scan and activate a tag with **ALW**
2. **Expected:** Tag number ends with `...010001` (independent counter)

### 4. Verify in Database

Use Android Device Monitor or ADB to check the database:

```bash
# Pull database from device
adb pull /data/data/com.socam.bcms/databases/bcms_database.db

# Query BCTypeSerialNumbers table
sqlite3 bcms_database.db "SELECT * FROM BCTypeSerialNumbers;"
```

**Expected output:**
```
MIC|107|0004|1234567890
ALW|102|0002|1234567891
```

---

## Expected Log Output

### During Migration (First Launch)
```
DatabaseManager: Checking for database migrations...
DatabaseManager: BCTypeSerialNumbers table not found, will create it
DatabaseManager: Creating BCTypeSerialNumbers table...
DatabaseManager: BCTypeSerialNumbers table created successfully
DatabaseManager: Database migrations completed
```

### During Tag Activation (First Tag)
```
TagActivationViewModel: User selected candidate tag: E41235331107210573010001
TagActivationViewModel: ⚠️ No serial number found for BC type MIC, initializing with 0001
TagActivationViewModel: Generated tag number: 341800331107210573010001 (Prefix: 34180, MainContract: 03, Version: 3, Reserved: 0, BCType: MIC -> 107, Contract: 210573, DeviceID: 01, BCType-SerialNo: 0001 -> XXYYYY: 010001)
DatabaseManager: BC type MIC serial number incremented to: 0002
```

### During Tag Activation (Second Tag)
```
TagActivationViewModel: User selected candidate tag: E41235331107210573010002
TagActivationViewModel: Generated tag number: 341800331107210573010002 (Prefix: 34180, MainContract: 03, Version: 3, Reserved: 0, BCType: MIC -> 107, Contract: 210573, DeviceID: 01, BCType-SerialNo: 0002 -> XXYYYY: 010002)
DatabaseManager: BC type MIC serial number incremented to: 0003
```

---

## Troubleshooting

### Issue: "no such table: BCTypeSerialNumbers" Error

**Cause:** Migration didn't run on app startup

**Solutions:**
1. Check if `initializeDatabase()` is being called
2. Verify `performDatabaseMigrations()` is executing
3. Check logcat for migration errors
4. Try clearing app data and reinstalling:
   ```bash
   adb shell pm clear com.socam.bcms
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

### Issue: Serial Numbers Still Not Incrementing

**Cause:** Row initialization in ViewModel not working

**Solutions:**
1. Check if `generateTagNumber()` logs show row creation:
   ```
   ⚠️ No serial number found for BC type MIC, initializing with 0001
   ```
2. Verify table exists:
   ```bash
   adb shell
   sqlite3 /data/data/com.socam.bcms/databases/bcms_database.db
   .tables
   ```
   Should show `BCTypeSerialNumbers`
3. Check for any SQL errors in logcat

### Issue: Migration Logs Not Showing

**Cause:** Database initialization might be lazy

**Solutions:**
1. Login to the app (this triggers database initialization)
2. Check for migration logs after login
3. If still no logs, add debug breakpoint in `performDatabaseMigrations()`

---

## Success Criteria

✅ Migration logs show table creation on first launch  
✅ No "no such table" errors in logcat  
✅ First tag gets serial number `0001`  
✅ Second tag gets serial number `0002`  
✅ Third tag gets serial number `0003`  
✅ Different BC types have independent counters  
✅ Database shows BCTypeSerialNumbers table with correct data  

---

## Clean Test (Start Fresh)

To test from a completely clean state:

```bash
# Clear all app data
adb shell pm clear com.socam.bcms

# Uninstall the app
adb uninstall com.socam.bcms

# Install fresh build
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk

# Start logcat
adb logcat -s DatabaseManager:D TagActivationViewModel:D AuthManager:D

# Launch the app
adb shell am start -n com.socam.bcms/.presentation.MainActivity
```

This ensures you're testing the migration from scratch.

---

## Quick Validation Commands

```bash
# Check if table exists
adb shell "run-as com.socam.bcms sqlite3 /data/data/com.socam.bcms/databases/bcms_database.db '.tables' | grep BCTypeSerialNumbers"

# View all BC type serial numbers
adb shell "run-as com.socam.bcms sqlite3 /data/data/com.socam.bcms/databases/bcms_database.db 'SELECT * FROM BCTypeSerialNumbers;'"

# Count rows in table
adb shell "run-as com.socam.bcms sqlite3 /data/data/com.socam.bcms/databases/bcms_database.db 'SELECT COUNT(*) FROM BCTypeSerialNumbers;'"

# View last 5 activated tags
adb shell "run-as com.socam.bcms sqlite3 /data/data/com.socam.bcms/databases/bcms_database.db 'SELECT RFIDTagNo, BCType, ActivatedDate FROM RfidModule WHERE IsActivated = 1 ORDER BY ActivatedDate DESC LIMIT 5;'"
```

---

## Test Report Template

```
Date: _____________
Tester: ___________

Migration Test:
[ ] Table created on first launch
[ ] No errors in logcat

Tag Activation Test (MIC):
[ ] Tag 1: ...010001 ✅
[ ] Tag 2: ...010002 ✅
[ ] Tag 3: ...010003 ✅

Tag Activation Test (ALW):
[ ] Tag 1: ...010001 ✅
[ ] Tag 2: ...010002 ✅

Database Verification:
[ ] BCTypeSerialNumbers table exists
[ ] MIC serial number: _______
[ ] ALW serial number: _______

Overall Result: [ ] PASS  [ ] FAIL

Notes:
_______________________
_______________________
```

