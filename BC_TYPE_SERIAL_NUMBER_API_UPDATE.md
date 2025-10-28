# BC Type Serial Number API Integration - Real API Implementation

**Date:** October 28, 2025  
**Status:** ✅ Completed

---

## Overview

Updated the BC Type Serial Number API integration from demo endpoint to production-ready endpoint. The system now calls the real backend API during login to fetch the latest serial numbers for all BC types (MIC, ALW, TID) and syncs them to the local database.

---

## Changes Summary

### 1. Updated DTOs (`BCTypeSerialNumberDto.kt`)

**New Request DTO:**
```kotlin
data class BCTypeSerialNumberRequest(
    @SerializedName("ProjId")
    val projId: String,           // Hardcoded project ID from BuildConfig
    
    @SerializedName("DeviceId")
    val deviceId: String,         // Device ID from BuildConfig (e.g., "01")
    
    @SerializedName("Username")
    val username: String          // Logged-in user's username
)
```

**Updated Response DTO:**
```kotlin
data class BCTypeSerialNumberDto(
    @SerializedName("BCType")
    val bcType: String,           // "MIC", "ALW", "TID"
    
    @SerializedName("ProjId")
    val projId: String,           // Project ID
    
    @SerializedName("GunNum")
    val gunNum: String,           // Gun number (maps to Device ID)
    
    @SerializedName("SerialNo")
    val serialNo: String          // 4-digit format: "0001", "0123", etc.
)

// Response is now a List directly
typealias BCTypeSerialNumbersResponse = List<BCTypeSerialNumberDto>
```

---

### 2. Updated API Service (`SyncApiService.kt`)

**Changed from:**
```kotlin
@GET("Rfids/{projId}/BCTypes/LatestSerialNumbers")
suspend fun getBCTypeSerialNumbers(
    @Path("projId") projId: String
): Response<BCTypeSerialNumbersResponse>
```

**Changed to:**
```kotlin
@POST("SerialNo/Latest/")
suspend fun getBCTypeSerialNumbers(
    @Body request: BCTypeSerialNumberRequest
): Response<BCTypeSerialNumbersResponse>
```

---

### 3. Added DatabaseManager Method

**New Method:**
```kotlin
fun getNumericCodeByBcType(bcType: String): String?
```

**Purpose:** Queries the `BCTypeMapping` table to get the numeric code (bc_type_code) for a given BC type.

**Mapping:**
- `MIC` → `107`
- `ALW` → `102`
- `TID` → `103`

---

### 4. Updated LoginViewModel Logic

**Updated `fetchBcTypeSerialNumbersFromServer()` method:**

**Key Changes:**
1. **Request Construction:**
   - Uses `BuildConfig.PROJECT_ID` (hardcoded per device/APK)
   - Uses `BuildConfig.DEVICE_ID` (e.g., "01")
   - Uses `user.username` (logged-in user)

2. **BCType Mapping:**
   - Queries `BCTypeMapping` table to get numeric code for each BC type
   - Validates BC type exists before updating
   - Logs warning if BC type not found in mapping table

3. **Database Update:**
   - Updates `BCTypeSerialNumbers` table with:
     - `bc_type`: From API response (e.g., "MIC")
     - `bc_type_code`: From BCTypeMapping table (e.g., "107")
     - `serial_number`: From API response (e.g., "0001")

---

## API Endpoint Details

### Real Production API

**Endpoint:** `POST SerialNo/Latest/`

**Full URLs:**
- **Development:** `https://dev.socam.com/iot/api/SerialNo/Latest/`
- **Production:** `https://micservice.shuion.com.hk/api/SerialNo/Latest/`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer {token}
```

**Request Body Example:**
```json
{
    "ProjId": "629F9E29-0B36-4A9E-A2C4-C28969285583",
    "DeviceId": "01",
    "Username": "client_user"
}
```

**Response Example:**
```json
[
    {
        "BCType": "MIC",
        "ProjId": "629F9E29-0B36-4A9E-A2C4-C28969285583",
        "GunNum": "01",
        "SerialNo": "0001"
    },
    {
        "BCType": "ALW",
        "ProjId": "629F9E29-0B36-4A9E-A2C4-C28969285583",
        "GunNum": "01",
        "SerialNo": "0001"
    },
    {
        "BCType": "TID",
        "ProjId": "629F9E29-0B36-4A9E-A2C4-C28969285583",
        "GunNum": "01",
        "SerialNo": "0001"
    }
]
```

---

## Environment Configuration

The API automatically uses the correct base URL based on `EnvironmentConfig.BUILD_ENVIRONMENT`:

**Switch Environment:**
```kotlin
// In EnvironmentConfig.kt
val BUILD_ENVIRONMENT = Environment.DEVELOPMENT  // For dev
// OR
val BUILD_ENVIRONMENT = Environment.PRODUCTION   // For prod
```

**Environment URLs:**
- **DEVELOPMENT:** `https://dev.socam.com/iot/api`
- **PRODUCTION:** `https://micservice.shuion.com.hk/api`

---

## Login Flow with Serial Number Sync

### Step-by-Step Process

1. **User Authentication:**
   - User enters username and password
   - Local database authentication succeeds
   - Hardcoded API token is saved

2. **BC Type Serial Number Initialization:**
   - Check if all BC type serial numbers are "0000" (clean install)
   - If yes, proceed to fetch from server

3. **API Call:**
   - Construct request with:
     - `ProjId`: `BuildConfig.PROJECT_ID`
     - `DeviceId`: `BuildConfig.DEVICE_ID`
     - `Username`: `user.username`
   - Call `POST SerialNo/Latest/`
   - Include Bearer token in Authorization header

4. **Response Processing:**
   - For each BC type in response:
     - Query `BCTypeMapping` table for numeric code
     - Update `BCTypeSerialNumbers` table
     - Log success/warning

5. **Login Complete:**
   - Navigate to MainActivity
   - Serial numbers ready for tag generation

---

## Database Tables Involved

### 1. BCTypeMapping (Read-only reference)
```sql
CREATE TABLE BCTypeMapping (
    bc_type TEXT UNIQUE NOT NULL,       -- "MIC", "ALW", "TID"
    numeric_code TEXT NOT NULL,         -- "107", "102", "103"
    description TEXT,
    is_active INTEGER DEFAULT 1
);
```

### 2. BCTypeSerialNumbers (Updated during login)
```sql
CREATE TABLE BCTypeSerialNumbers (
    bc_type TEXT PRIMARY KEY NOT NULL,  -- "MIC", "ALW", "TID"
    bc_type_code TEXT NOT NULL,         -- "107", "102", "103"
    serial_number TEXT NOT NULL,        -- "0001", "0123", etc.
    updated_date INTEGER NOT NULL
);
```

---

## Testing Checklist

### 1. Clean Install Test
- [ ] Uninstall app completely
- [ ] Install fresh APK
- [ ] Login with valid credentials
- [ ] Verify API call is made
- [ ] Check logcat for serial number updates
- [ ] Verify `BCTypeSerialNumbers` table has correct values

### 2. Existing Install Test
- [ ] Login with app already having serial numbers
- [ ] API call should NOT be made (serial numbers not "0000")
- [ ] Verify existing serial numbers are preserved

### 3. Environment Switch Test
- [ ] Change `EnvironmentConfig.BUILD_ENVIRONMENT` to DEVELOPMENT
- [ ] Verify API calls go to `https://dev.socam.com/iot/api`
- [ ] Change to PRODUCTION
- [ ] Verify API calls go to `https://micservice.shuion.com.hk/api`

### 4. Error Handling Test
- [ ] Test with invalid token
- [ ] Test with network disconnected
- [ ] Verify login still succeeds (graceful degradation)
- [ ] Verify app falls back to local serial numbers starting from "0001"

---

## Logging for Debugging

**Look for these log messages:**

```
LoginViewModel: Fetching BC type serial numbers from API...
LoginViewModel: Request - ProjId: 629F9E29-..., DeviceId: 01, Username: client_user
LoginViewModel: Received 3 BC type serial numbers from server
LoginViewModel: Updated MIC (107) serial number to 0001
LoginViewModel: Updated ALW (102) serial number to 0001
LoginViewModel: Updated TID (103) serial number to 0001
LoginViewModel: BC type serial numbers successfully updated
```

**Error Messages:**
```
LoginViewModel: WARNING - BC type XXX not found in BCTypeMapping table, skipping
LoginViewModel: Failed to fetch BC type serial numbers from API: 401 - Unauthorized
LoginViewModel: Error fetching BC type serial numbers from API: {error message}
```

---

## Configuration Values

### BuildConfig Constants
```kotlin
BuildConfig.PROJECT_ID = "629F9E29-0B36-4A9E-A2C4-C28969285583"
BuildConfig.DEVICE_ID = "01"  // Change per device
```

### BCTypeMapping (Default Data)
```
MIC → 107
ALW → 102
TID → 103
```

---

## Files Modified

1. ✅ `app/src/main/java/com/socam/bcms/data/dto/BCTypeSerialNumberDto.kt`
2. ✅ `app/src/main/java/com/socam/bcms/data/api/SyncApiService.kt`
3. ✅ `app/src/main/java/com/socam/bcms/data/database/DatabaseManager.kt`
4. ✅ `app/src/main/java/com/socam/bcms/presentation/login/LoginViewModel.kt`

---

## Notes

- **Authentication:** Bearer token from login is automatically included in API requests
- **Environment:** Base URL is automatically selected based on `EnvironmentConfig`
- **Graceful Degradation:** Login continues even if API call fails
- **Clean Install Detection:** Only calls API when all serial numbers are "0000"
- **BCType Mapping:** Uses `BCTypeMapping` table for consistent numeric code mapping
- **GunNum Validation:** Response includes GunNum (maps to DeviceId) for verification

---

## Future Enhancements

1. **GunNum Validation:** Add validation to ensure `GunNum` matches `BuildConfig.DEVICE_ID`
2. **Retry Logic:** Add retry mechanism for failed API calls
3. **Manual Sync:** Add UI option to manually sync serial numbers from Settings
4. **Conflict Resolution:** Handle cases where local serial number is ahead of server

---

✅ **Implementation Complete and Ready for Testing**

