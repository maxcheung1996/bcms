# App Update Feature Documentation

## Overview

The BCMS app now includes an in-app update feature that allows users to download and install new versions of the app directly from the Settings screen.

## Features

- **One-Click Update**: Users can update the app with a single button click
- **Download Progress**: Visual feedback during download with Snackbar notifications
- **Automatic Installation Prompt**: After download completes, the Android installation prompt opens automatically
- **Multi-Language Support**: Update feature text available in English, Traditional Chinese, and Simplified Chinese
- **Error Handling**: Graceful error handling with user-friendly error messages

## How It Works

1. User navigates to **Settings** screen
2. User clicks **Update App** button in the Actions section
3. Confirmation dialog appears asking if user wants to download and install
4. User clicks **Download** to proceed
5. APK file downloads to device's Downloads folder
6. Android system prompts user to install the downloaded APK
7. User approves installation and app updates

## Implementation Details

### Files Modified/Created

1. **`ApkDownloader.kt`** - Utility class for downloading and installing APK files
2. **`SettingsViewModel.kt`** - Added update download state management
3. **`SettingsFragment.kt`** - Added UI handling for update button and download progress
4. **`settings_actions.xml`** - Added Update App button to layout
5. **`AndroidManifest.xml`** - Added necessary permissions and FileProvider configuration
6. **`file_provider_paths.xml`** - FileProvider configuration for sharing APK files
7. **String resources** - Added translations in English, Traditional Chinese, and Simplified Chinese

### Required Permissions

The following permissions are already included in `AndroidManifest.xml`:

```xml
<!-- App update permissions -->
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

## Configuring the Update URL

### Current Configuration (Sample URL)

The app currently uses a sample URL that needs to be replaced with your actual APK download URL:

**Location**: `SettingsFragment.kt` line 434

```kotlin
val updateUrl = "https://example.com/bcms_update.apk" // Sample URL - replace with real URL
viewModel.startAppUpdate(updateUrl)
```

### How to Update the URL

#### Option 1: Hardcoded URL (Simple, for fixed update server)

Replace the sample URL in `SettingsFragment.kt`:

```kotlin
private fun showUpdateAppConfirmation(): Unit {
    val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
    builder.setTitle(getString(R.string.update_app_title))
    builder.setMessage(getString(R.string.update_app_message))
    builder.setPositiveButton(getString(R.string.download)) { _, _ ->
        // Replace this URL with your actual APK download URL
        val updateUrl = "https://your-server.com/downloads/bcms_update.apk"
        viewModel.startAppUpdate(updateUrl)
    }
    builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
        dialog.dismiss()
    }
    builder.show()
}
```

#### Option 2: Database Configuration (Dynamic, recommended)

Store the update URL in the database for easy updates:

1. Add to `AppSettings.sq`:
```sql
INSERT INTO AppSettings (setting_key, setting_value, setting_type, description, is_user_configurable)
VALUES ('app_update_url', 'https://your-server.com/downloads/bcms_update.apk', 'STRING', 'URL for downloading app updates', 0);
```

2. Update `SettingsViewModel.kt` to load from database:
```kotlin
private suspend fun loadUpdateUrl(): String = withContext(Dispatchers.IO) {
    try {
        val urlSetting = databaseManager.database.appSettingsQueries
            .selectSettingByKey("app_update_url")
            .executeAsOneOrNull()
        urlSetting?.setting_value ?: "https://example.com/bcms_update.apk"
    } catch (e: Exception) {
        "https://example.com/bcms_update.apk"
    }
}

fun startAppUpdateFromSettings(): Unit {
    viewModelScope.launch {
        val updateUrl = loadUpdateUrl()
        startAppUpdate(updateUrl)
    }
}
```

3. Update `SettingsFragment.kt`:
```kotlin
builder.setPositiveButton(getString(R.string.download)) { _, _ ->
    viewModel.startAppUpdateFromSettings()
}
```

#### Option 3: Remote Configuration (Advanced, most flexible)

Fetch the update URL from your backend API:

1. Add API endpoint to `SyncApiService.kt`:
```kotlin
@GET("/api/app-version")
suspend fun getLatestAppVersion(): AppVersionDto

data class AppVersionDto(
    val version: String,
    val downloadUrl: String,
    val releaseNotes: String,
    val mandatory: Boolean
)
```

2. Check for updates and show dialog with version info

## Testing the Feature

### Testing with a Sample APK

1. **Create a test APK**:
   - Build your app: `./gradlew assembleDebug`
   - Find APK in: `app/build/outputs/apk/debug/app-debug.apk`

2. **Host the APK**:
   - Upload to a web server (e.g., your backend server)
   - Or use a temporary file hosting service for testing
   - Get the direct download URL

3. **Update the URL** in the code (as described above)

4. **Test the flow**:
   - Open app → Settings
   - Click "Update App"
   - Click "Download"
   - Wait for download to complete
   - Verify installation prompt appears
   - Install and verify app updates

### Testing Considerations

- **Network**: Ensure device has internet connection
- **Storage**: Ensure device has enough storage space
- **Permissions**: On first use, device may prompt for installation permission
- **File Size**: Large APKs will take longer to download
- **HTTPS**: Use HTTPS URLs for security (Android may block HTTP downloads on newer versions)

## User Experience Flow

1. **Settings Screen**
   ```
   [Actions Section]
   ├── [Update App] ← New button
   └── [Logout]
   ```

2. **Click Update App**
   ```
   ┌─────────────────────────────┐
   │  App Update                  │
   ├─────────────────────────────┤
   │  A new version is available. │
   │  Would you like to download  │
   │  and install it now?         │
   ├─────────────────────────────┤
   │  [Cancel]      [Download]    │
   └─────────────────────────────┘
   ```

3. **Download Progress**
   ```
   [Update App Button shows: "Downloading update..."]
   [Snackbar: "Downloading update..."]
   ```

4. **Download Complete**
   ```
   [Android system installation prompt]
   ```

## Troubleshooting

### Download Fails

- **Check URL**: Ensure the URL is correct and accessible
- **Check Network**: Verify device has internet connection
- **Check Permissions**: Ensure app has necessary permissions
- **Check Storage**: Ensure device has enough storage space

### Installation Doesn't Start

- **Check FileProvider**: Verify `file_provider_paths.xml` is correctly configured
- **Check Permissions**: App needs `REQUEST_INSTALL_PACKAGES` permission
- **Check APK**: Ensure downloaded APK is valid and not corrupted

### "Unknown Sources" Error

- On some devices, user may need to enable "Install from Unknown Sources" in system settings
- Android 8.0+ handles this per-app, so user will see a permission prompt

## Security Considerations

1. **HTTPS Only**: Always use HTTPS URLs to prevent man-in-the-middle attacks
2. **Signature Verification**: Consider adding APK signature verification
3. **Checksum Verification**: Consider adding checksum validation
4. **Backend Control**: Store URL in backend to prevent hardcoded outdated URLs
5. **Version Check**: Consider checking version before downloading to avoid unnecessary downloads

## Future Enhancements

1. **Version Check**: Check current version vs. available version before showing update prompt
2. **Release Notes**: Show release notes in update dialog
3. **Mandatory Updates**: Force updates for critical security patches
4. **Background Updates**: Download in background and notify when ready
5. **Progress Bar**: Show detailed download progress (percentage, MB downloaded, etc.)
6. **Automatic Check**: Check for updates on app launch
7. **Delta Updates**: Download only changed files for smaller downloads

## Sample Backend API Response

If implementing remote configuration (Option 3), your backend could return:

```json
{
  "latestVersion": "1.2.0",
  "currentVersion": "1.1.0",
  "updateAvailable": true,
  "mandatory": false,
  "downloadUrl": "https://your-server.com/downloads/bcms-v1.2.0.apk",
  "releaseNotes": "- Bug fixes\n- Performance improvements\n- New features",
  "releaseDate": "2024-10-24",
  "fileSize": 25600000,
  "checksum": "sha256:abcd1234..."
}
```

## Contact

For questions or issues with the update feature, contact the development team.

---

**Last Updated**: October 24, 2024
**Feature Version**: 1.0

