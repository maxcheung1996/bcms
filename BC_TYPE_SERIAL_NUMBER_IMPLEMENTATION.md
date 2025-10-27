# BC Type-Specific Serial Number Implementation

## Overview
This document describes the implementation of BC type-specific serial numbers for tag generation. Each BC type (MIC, ALW, TID, etc.) now maintains its own independent 4-digit serial number counter instead of using a single global counter.

---

## Problem Statement
Previously, the app used a **single** 4-digit serial number counter (YYYY) for **all** BC types. This caused issues because:
- Each BC type should have independent counters
- Tag numbers include BC type codes (107 for MIC, 102 for ALW, 103 for TID)
- Different projects may have different numbers of BC types
- Serial numbers need to be synced from server per BC type

---

## Solution Architecture

### Tag Number Format (Unchanged)
```
Prefix + MainContract + Version + Reserved + BCTypeCode + ContractNo + DeviceID + SerialNumber
Example: 34180 + 03 + 3 + 0 + 107 + 210573 + 01 + 0123
         ^^^^^   ^^   ^   ^   ^^^   ^^^^^^   ^^   ^^^^
         |       |    |   |   |     |        |    |
         |       |    |   |   |     |        |    SerialNumber (YYYY, per BC type)
         |       |    |   |   |     |        DeviceID (XX, shared)
         |       |    |   |   |     ContractNo (6 digits)
         |       |    |   |   BCTypeCode (3 digits)
         |       |    |   Reserved (1 digit)
         |       |    Version (1 digit)
         |       MainContract (2 digits)
         Prefix (5 digits, configurable)
```

### What Changed
- **Before**: Single 4-digit serial number shared by all BC types
- **After**: Each BC type has its own 4-digit serial number

---

## Implementation Details

### 1. Database Schema (`BCTypeSerialNumbers.sq`)
Created new SQLDelight table to store BC type serial numbers:

```sql
CREATE TABLE IF NOT EXISTS BCTypeSerialNumbers (
    bc_type TEXT PRIMARY KEY NOT NULL,      -- "MIC", "ALW", "TID"
    bc_type_code TEXT NOT NULL,             -- "107", "102", "103"
    serial_number TEXT NOT NULL,            -- "0001", "0123", etc.
    updated_date INTEGER NOT NULL           -- Unix timestamp
);
```

**Key Operations**:
- `selectSerialNumberByBcType`: Get serial number for a specific BC type
- `insertOrReplace`: Upsert BC type serial number from server
- `incrementSerialNumber`: Auto-increment serial number for a BC type
- `selectAll`: Get all BC type serial numbers

---

### 2. API DTOs (`BCTypeSerialNumberDto.kt`)
Created DTOs for API communication:

```kotlin
data class BCTypeSerialNumberDto(
    val bcType: String,              // "MIC", "ALW", "TID"
    val bcTypeCode: String,          // "107", "102", "103"
    val latestSerialNumber: String   // "0001", "0123", etc.
)

data class BCTypeSerialNumbersResponse(
    val tagNumbers: List<BCTypeSerialNumberDto>
)
```

**Example API Response**:
```json
{
  "tagNumbers": [
    { "bcType": "MIC", "bcTypeCode": "107", "latestSerialNumber": "0123" },
    { "bcType": "ALW", "bcTypeCode": "102", "latestSerialNumber": "0456" },
    { "bcType": "TID", "bcTypeCode": "103", "latestSerialNumber": "0789" }
  ]
}
```

---

### 3. API Service (`SyncApiService.kt`)
Added new endpoint to fetch BC type serial numbers:

```kotlin
@GET("Rfids/{projId}/BCTypes/LatestSerialNumbers")
suspend fun getBCTypeSerialNumbers(
    @Path("projId") projId: String
): Response<BCTypeSerialNumbersResponse>
```

**Endpoint**: `GET /Rfids/{projId}/BCTypes/LatestSerialNumbers`
- Returns all BC types with their latest serial numbers for the project

---

### 4. DatabaseManager (`DatabaseManager.kt`)
Added BC type-specific serial number methods:

#### New Methods
```kotlin
// Get serial number for a specific BC type
fun getSerialNumberByBcType(bcType: String): String?

// Update/insert BC type serial number from server
fun updateBcTypeSerialNumber(bcType: String, bcTypeCode: String, serialNumber: String)

// Increment serial number for a specific BC type
fun incrementBcTypeSerialNumber(bcType: String): String?

// Get all BC type serial numbers
fun getAllBcTypeSerialNumbers(): List<BCTypeSerialNumbers>

// Check if BC type serial number exists
fun bcTypeSerialNumberExists(bcType: String): Boolean

// Initialize BC type serial numbers with "0000" (signals need to fetch from server)
fun initializeBcTypeSerialNumbers()
```

#### Usage Example
```kotlin
// Get MIC serial number
val micSerialNumber = databaseManager.getSerialNumberByBcType("MIC") // "0123"

// Increment MIC serial number
databaseManager.incrementBcTypeSerialNumber("MIC") // "0124"

// Update from server
databaseManager.updateBcTypeSerialNumber("MIC", "107", "0500")
```

---

### 5. LoginViewModel (`LoginViewModel.kt`)
Updated login flow to fetch BC type serial numbers:

#### New Flow
1. User logs in successfully
2. App initializes BC type serial numbers with "0000" (if not exists)
3. App checks if any BC type has "0000" serial number
4. If yes, fetch all BC type serial numbers from server
5. Update local database with fetched serial numbers

#### Code Changes
```kotlin
private suspend fun initializeSerialNumber(user: User) {
    // Initialize BC type serial numbers with "0000" if they don't exist
    databaseManager.initializeBcTypeSerialNumbers()
    
    // Get all BC type serial numbers
    val bcTypeSerialNumbers = databaseManager.getAllBcTypeSerialNumbers()
    
    // Check if any BC type needs fetching from server
    val needsServerFetch = bcTypeSerialNumbers.isEmpty() || 
        bcTypeSerialNumbers.any { it.serial_number == "0000" }
    
    if (needsServerFetch) {
        fetchBcTypeSerialNumbersFromServer(user)
    }
}

private suspend fun fetchBcTypeSerialNumbersFromServer(user: User) {
    val response = apiService.getBCTypeSerialNumbers(BuildConfig.PROJECT_ID)
    
    if (response.isSuccessful && response.body() != null) {
        response.body()!!.tagNumbers.forEach { bcTypeDto ->
            databaseManager.updateBcTypeSerialNumber(
                bcType = bcTypeDto.bcType,
                bcTypeCode = bcTypeDto.bcTypeCode,
                serialNumber = bcTypeDto.latestSerialNumber
            )
        }
    }
}
```

---

### 6. TagActivationViewModel (`TagActivationViewModel.kt`)
Updated tag number generation to use BC type-specific serial numbers:

#### Code Changes
```kotlin
private suspend fun generateTagNumber(bcType: String): String? {
    // ... (prefix, version, contract setup) ...
    
    // UPDATED: Get BC type-specific serial number
    val serialNumber = databaseManager.getSerialNumberByBcType(bcType) ?: "0001"
    
    val autoIncrement = "$deviceId$serialNumber"
    
    // UPDATED: Increment BC type-specific serial number
    databaseManager.incrementBcTypeSerialNumber(bcType)
    
    // Build tag number
    val tagNumber = "$prefix$mainContract$version$reserved$bcTypeCode$contractNo$autoIncrement"
    
    return tagNumber
}
```

---

## Data Flow

### Login Flow
```
User Login
    ↓
Initialize BC Type Serial Numbers (create with "0000" if not exists)
    ↓
Check if any BC type has "0000"
    ↓
    Yes → Fetch from API → Update Local DB
    ↓
    No → Use existing values
    ↓
Login Success
```

### Tag Activation Flow
```
User Activates Tag
    ↓
Generate Tag Number
    ↓
Get BC Type-Specific Serial Number from DB
    ↓
Build Tag Number: Prefix + ... + BCTypeCode + ... + DeviceID + SerialNumber
    ↓
Increment BC Type-Specific Serial Number
    ↓
Write Tag Number to Tag
    ↓
Save to Database
```

---

## Example Scenarios

### Scenario 1: First Login
```
Initial State: BCTypeSerialNumbers table is empty

Login:
1. initializeBcTypeSerialNumbers() creates records:
   - MIC: "0000", ALW: "0000", TID: "0000"
   
2. Detect "0000" values → fetch from server
   
3. Server returns:
   - MIC: "0123", ALW: "0456", TID: "0789"
   
4. Update local database:
   - MIC: "0123", ALW: "0456", TID: "0789"
```

### Scenario 2: Tag Activation (MIC)
```
User selects BC Type: MIC
User activates tag

generateTagNumber("MIC"):
1. Get MIC serial number: "0123"
2. Build tag: 341800330107210573010123
3. Increment MIC serial number: "0124"
4. Write to tag and save to database

Next MIC activation will use: "0124"
```

### Scenario 3: Tag Activation (ALW)
```
User selects BC Type: ALW
User activates tag

generateTagNumber("ALW"):
1. Get ALW serial number: "0456"
2. Build tag: 341800330102210573010456
3. Increment ALW serial number: "0457"
4. Write to tag and save to database

Next ALW activation will use: "0457"
MIC serial number remains: "0124" (independent)
```

---

## Testing Checklist

### Database Tests
- [ ] BCTypeSerialNumbers table created successfully
- [ ] Can insert BC type serial numbers
- [ ] Can query BC type serial numbers by BC type
- [ ] Can increment BC type serial numbers
- [ ] Can update BC type serial numbers from server

### API Tests
- [ ] API returns correct BC type serial numbers
- [ ] API handles missing project gracefully
- [ ] App handles API failures gracefully (uses local counters)

### Login Tests
- [ ] First login initializes BC type serial numbers with "0000"
- [ ] Login fetches BC type serial numbers from server
- [ ] Login updates local database with fetched values
- [ ] Login handles API failures gracefully

### Tag Activation Tests
- [ ] Tag activation uses correct BC type serial number
- [ ] Tag activation increments correct BC type serial number
- [ ] Different BC types maintain independent serial numbers
- [ ] MIC activation doesn't affect ALW/TID counters
- [ ] ALW activation doesn't affect MIC/TID counters
- [ ] TID activation doesn't affect MIC/ALW counters

### Edge Cases
- [ ] What if BC type not found in database? (Fallback to "0001")
- [ ] What if API returns empty list? (Use local counters)
- [ ] What if new BC type is added? (Initialize with "0000" on next login)
- [ ] What if serial number reaches 9999? (Roll over to 0000 or error?)

---

## Migration Notes

### Existing Installations
For apps upgrading from the old single-counter system:
1. Old `serial_number` in `AppSettings` table is **not** affected
2. New `BCTypeSerialNumbers` table will be created
3. On first login after upgrade:
   - BC type serial numbers initialized with "0000"
   - Fetched from server with latest values
   - Each BC type starts from server's latest value

### Backward Compatibility
- Old serial number methods still exist in `DatabaseManager`
- Can be used for testing or migration purposes
- New code uses BC type-specific methods

---

## Files Modified

### New Files
- `app/src/main/sqldelight/com/socam/bcms/database/BCTypeSerialNumbers.sq`
- `app/src/main/java/com/socam/bcms/data/dto/BCTypeSerialNumberDto.kt`

### Modified Files
- `app/src/main/java/com/socam/bcms/data/api/SyncApiService.kt`
- `app/src/main/java/com/socam/bcms/data/database/DatabaseManager.kt`
- `app/src/main/java/com/socam/bcms/presentation/login/LoginViewModel.kt`
- `app/src/main/java/com/socam/bcms/presentation/modules/TagActivationViewModel.kt`

---

## API Requirements (Backend)

The backend needs to implement the following endpoint:

### Endpoint
```
GET /Rfids/{projId}/BCTypes/LatestSerialNumbers
```

### Parameters
- `projId` (path): Project ID (UUID)

### Response Format
```json
{
  "tagNumbers": [
    {
      "bcType": "MIC",
      "bcTypeCode": "107",
      "latestSerialNumber": "0123"
    },
    {
      "bcType": "ALW",
      "bcTypeCode": "102",
      "latestSerialNumber": "0456"
    },
    {
      "bcType": "TID",
      "bcTypeCode": "103",
      "latestSerialNumber": "0789"
    }
  ]
}
```

### Business Logic
- Return all BC types for the given project
- `latestSerialNumber` should be the highest 4-digit serial number used for each BC type
- Format: 4-digit string with leading zeros (e.g., "0001", "0123", "1234")
- Backend should query all tags for each BC type and extract the highest serial number

---

## Future Enhancements

### Potential Improvements
1. **Serial Number Sync**: Periodic sync of serial numbers (not just at login)
2. **Conflict Resolution**: Handle conflicts if multiple devices activate tags offline
3. **Serial Number Range**: Pre-allocate ranges to devices (e.g., Device 01: 0001-0100, Device 02: 0101-0200)
4. **Serial Number Analytics**: Track serial number usage per BC type
5. **Serial Number Reset**: Admin feature to reset serial numbers for a BC type

### Database Optimizations
1. Add index on `bc_type` for faster lookups (already primary key)
2. Add index on `updated_date` for sync queries
3. Add foreign key to `MasterCategories` table (optional)

---

## Conclusion

This implementation provides independent serial number management for each BC type while maintaining the existing tag number format and workflow. The solution is:
- ✅ Scalable (supports any number of BC types)
- ✅ Flexible (server controls latest serial numbers)
- ✅ Reliable (graceful fallbacks for API failures)
- ✅ Clean (follows existing code patterns)
- ✅ Maintainable (clear separation of concerns)

All changes are backward compatible and don't affect existing functionality.

