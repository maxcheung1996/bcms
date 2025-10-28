# Project ID Centralization Summary

## Overview
All hardcoded project ID references (`629F9E29-0B36-4A9E-A2C4-C28969285583`) have been centralized to use `BuildConfig.PROJECT_ID`, following the same pattern as `BuildConfig.DEVICE_ID`.

## Changes Made

### 1. **AuthManager.kt**
- ✅ Added `import com.socam.bcms.BuildConfig`
- ✅ Updated `createUser()` method to use `BuildConfig.PROJECT_ID` instead of hardcoded value
- **Location**: Line 188

### 2. **DatabaseManager.kt**
- ✅ Added `import com.socam.bcms.BuildConfig`
- ✅ Updated `seedInitialUsers()` - role-based users now use `BuildConfig.PROJECT_ID`
- ✅ Updated `createInitialUser()` default parameter to use `BuildConfig.PROJECT_ID`
- ✅ Updated `seedMasterProjects()` - Anderson Road project now uses `BuildConfig.PROJECT_ID`
- **Locations**: Lines 217, 263, 444

### 3. **SyncApiService.kt**
- ✅ Added `import com.socam.bcms.BuildConfig`
- ✅ Updated companion object constant to reference `BuildConfig.PROJECT_ID`
- ✅ Enhanced deprecation notice with proper import replacement
- **Location**: Line 23

### 4. **User.sq** (SQL Schema)
- ✅ Added comment explaining that SQL cannot reference BuildConfig
- ✅ Documented that the default value should match `BuildConfig.PROJECT_ID`
- **Note**: SQL schemas cannot use Kotlin constants, but actual values are set via Kotlin code

## Verification

### All Usages Now Use BuildConfig.PROJECT_ID
The following files correctly reference `BuildConfig.PROJECT_ID`:
- ✅ `AuthManager.kt`
- ✅ `DatabaseManager.kt`
- ✅ `SyncApiService.kt`
- ✅ `LoginViewModel.kt`
- ✅ `TagActivationViewModel.kt`
- ✅ `SyncViewModel.kt`

### Remaining Hardcoded Value
Only **1 instance** remains hardcoded:
- `User.sq` line 11: SQL default value (cannot reference BuildConfig, documented with comment)

## Benefits

### 1. **Single Source of Truth**
- Project ID is now defined in one place: `BuildConfig.PROJECT_ID`
- Follows the same pattern as `DEVICE_ID`

### 2. **Build Configuration**
- Project ID can be changed per build variant in `build.gradle`
- Supports multiple environments/deployments

### 3. **Consistency**
- All Kotlin code uses the centralized value
- No risk of mismatched project IDs across the codebase

### 4. **Maintainability**
- Easy to change project ID for different deployments
- Clear deprecation notices guide developers to use centralized value

## Build Configuration Reference

The project ID is defined in `app/build.gradle`:

```gradle
android {
    defaultConfig {
        buildConfigField "String", "PROJECT_ID", "\"629F9E29-0B36-4A9E-A2C4-C28969285583\""
        buildConfigField "String", "DEVICE_ID", "\"01\""
    }
}
```

## Migration Complete ✅

All project ID references have been successfully centralized to `BuildConfig.PROJECT_ID`. No further action required.

---
**Date**: 2025-10-28
**Status**: ✅ Complete

