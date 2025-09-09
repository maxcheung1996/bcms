# BCMS - Batch Content Management System

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![Material Design 3](https://img.shields.io/badge/UI-Material%20Design%203-purple.svg)](https://m3.material.io)
[![License](https://img.shields.io/badge/License-Private-red.svg)]()

A production-level Android application for UHF RFID tag management and batch processing with offline-first capabilities.

## 🏗️ Architecture

This application follows **Clean Architecture** principles with **MVI (Model-View-Intent)** pattern for robust, maintainable, and testable code.

```
┌─────────────────┬─────────────────┬─────────────────┐
│   Presentation  │     Domain      │      Data       │
│    (UI Layer)   │ (Business Logic)│  (Data Sources) │
├─────────────────┼─────────────────┼─────────────────┤
│ • Activities    │ • AuthManager   │ • Repositories  │
│ • Fragments     │ • Use Cases     │ • API Services  │
│ • ViewModels    │ • Entities      │ • Database      │
│ • UI Components │ • Interfaces    │ • Local Storage │
└─────────────────┴─────────────────┴─────────────────┘
```

### Key Architectural Patterns

- **MVI Pattern**: Unidirectional data flow for predictable state management
- **Repository Pattern**: Abstract data layer with multiple data sources
- **Dependency Injection**: Clean separation of concerns
- **Offline-First**: Local database as single source of truth
- **Clean Architecture**: Separation of concerns across layers

## 🛠️ Tech Stack

### Core Technologies
- **Language**: Kotlin 1.5.32
- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 30 (Android 11)
- **Gradle**: 7.4.2
- **AGP**: 7.0.4

### Key Dependencies

#### UI & Navigation
- **Material Design Components**: 1.4.0
- **Navigation Component**: 2.3.5
- **ViewBinding**: Enabled
- **ConstraintLayout**: 2.1.0

#### Database & Storage
- **SQLDelight**: 1.5.3 - Type-safe SQL database
- **MMKV**: 1.2.11 - High-performance key-value storage

#### Networking
- **Retrofit**: 2.9.0 - Type-safe HTTP client
- **OkHttp Logging**: 4.9.1 - Network request logging
- **Gson**: 2.8.8 - JSON serialization

#### Asynchronous Programming
- **Kotlin Coroutines**: 1.5.1
- **Lifecycle Components**: 2.3.1

#### Security
- **AndroidX Security**: 1.0.0 - Encrypted SharedPreferences

#### Hardware Integration
- **UHF RFID**: Custom vendor libraries
  - `UHFJar_V1.4.05.aar`
  - `iscanuserapi.jar`

## 📁 Project Structure

```
app/src/main/
├── java/com/socam/bcms/
│   ├── data/
│   │   ├── api/                 # API interfaces and clients
│   │   ├── database/            # Database managers
│   │   ├── dto/                 # Data transfer objects
│   │   └── repository/          # Repository implementations
│   ├── domain/                  # Business logic layer
│   │   └── AuthManager.kt       # Authentication business logic
│   ├── model/                   # Data models
│   ├── presentation/            # UI layer
│   │   ├── auth/                # Authentication flow
│   │   ├── main/                # Main navigation
│   │   └── modules/             # Feature modules
│   ├── uhf/                     # UHF RFID integration
│   └── BCMSApp.kt              # Application class
├── res/
│   ├── layout/                  # XML layouts
│   ├── navigation/              # Navigation graphs
│   ├── values/                  # Colors, strings, themes
│   └── drawable/                # Icons and images
└── sqldelight/com/socam/bcms/database/
    ├── User.sq                  # User authentication schema
    ├── Tag.sq                   # Tag management schema
    ├── TagBatch.sq             # Batch processing schema
    ├── ApiToken.sq             # API token management
    └── SyncStatus.sq           # Data synchronization tracking
```

## 🚀 Features

### ✅ Implemented Features

#### Authentication System
- **Offline Authentication**: Local user validation without internet
- **Predefined Users**: Admin, Demo, Operator roles
- **Secure Password Storage**: Salted & hashed passwords
- **Token Management**: JWT-like token handling for API calls

#### Main Navigation
- **Material Design 3 UI**: Modern, accessible interface
- **Four Core Modules**:
  - 🏷️ **Tag Activation**: Individual tag processing
  - 📱 **Tag Single Scan**: Quick single tag operations
  - 📦 **Batch Process**: Multiple tag batch operations
  - ⚙️ **Settings**: Application configuration
- **Data Synchronization**: Manual sync with remote servers

#### Data Management
- **Offline-First Database**: All operations work without internet
- **Multi-Environment Support**: Development & Production APIs
- **Automatic Data Persistence**: Local storage with sync capabilities
- **UHF RFID Integration**: Hardware tag reading capabilities

### 🔄 Environment Configuration

#### Development Environment
- **Base URL**: `https://dev.socam.com/iot/api`
- **Debug Logging**: Enabled
- **Network Interceptors**: Request/Response logging

#### Production Environment
- **Base URL**: `https://micservice.shuion.com.hk/api`
- **Optimized Performance**: Release configurations
- **Security**: Enhanced API security measures

## 🗄️ Database Schema

### Core Tables

#### Users (`User.sq`)
```sql
CREATE TABLE User (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    salt TEXT NOT NULL,
    role TEXT NOT NULL,
    is_active INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

#### Tags (`Tag.sq`)
```sql
CREATE TABLE Tag (
    id TEXT PRIMARY KEY,
    epc TEXT NOT NULL,
    status TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    synced_at INTEGER
);
```

#### API Tokens (`ApiToken.sq`)
```sql
CREATE TABLE ApiToken (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    token TEXT NOT NULL,
    expires_at INTEGER,
    is_active INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (user_id) REFERENCES User(id)
);
```

## 🔧 Setup Instructions

### Prerequisites
- **JDK 15**: Required for compilation
- **Android Studio**: Arctic Fox or newer
- **Android SDK**: Level 30+

### Installation Steps

1. **Clone Repository**
```bash
git clone <repository-url>
cd BCMS
```

2. **Set Java Version**
```bash
export JAVA_HOME=/path/to/jdk-15
```

3. **Build Project**
```bash
./gradlew clean assembleDebug
```

4. **Install Dependencies**
```bash
./gradlew generateDebugDatabaseInterface
```

### Configuration

#### Local Properties
Create `local.properties`:
```properties
sdk.dir=/path/to/Android/Sdk
```

#### Environment Setup
The app automatically configures environments:
- **Development**: Default environment for testing
- **Production**: Switch via settings or environment toggle

## 🔐 Authentication

### Default Users

| Username | Password | Role | Access Level |
|----------|----------|------|--------------|
| `admin` | `admin123` | Administrator | Full access |
| `demo` | `password` | Demo User | Limited access |
| `operator` | `operator123` | Operator | Standard access |

### Login Flow
1. **Local Validation**: Check credentials against local database
2. **Token Generation**: Create session token for API calls
3. **Role-Based Access**: Different permissions per user type
4. **Session Management**: Secure token storage and validation

## 🌐 API Integration

### Centralized API Service
```kotlin
interface ApiService {
    @GET("tags")
    suspend fun getTagsList(): Response<ApiResponse<List<TagDto>>>
    
    @POST("tags/activate")
    suspend fun activateTag(@Body request: TagActivationRequest): Response<ApiResponse<TagDto>>
    
    @POST("batch/process")
    suspend fun processBatch(@Body request: BatchProcessRequest): Response<ApiResponse<BatchResult>>
}
```

### Authentication Interceptor
All API calls automatically include:
```
Authorization: Bearer <user-token>
Content-Type: application/json
```

## 📱 UHF RFID Integration

### Hardware Support
- **UHF Reader**: Vendor-specific implementation
- **Tag Operations**: Read, Write, Lock, Kill
- **Batch Processing**: Multiple tag operations
- **Real-time Feedback**: Audio and visual indicators

### UHF Manager Wrapper
```kotlin
class UHFManagerWrapper {
    fun startInventory(): Boolean
    fun stopInventory(): Boolean
    fun readTag(epc: String): TagData?
    fun writeTag(epc: String, data: String): Boolean
}
```

## 🔄 Data Synchronization

### Sync Strategy
- **Manual Sync**: User-initiated via sync button
- **Conflict Resolution**: Last-write-wins with timestamps
- **Offline Queue**: Store operations for later sync
- **Retry Logic**: Automatic retry for failed operations

### Sync Status Tracking
```kotlin
enum class SyncStatus {
    PENDING,    // Waiting to sync
    SYNCING,    // Currently syncing
    SYNCED,     // Successfully synced
    FAILED      // Sync failed, retry needed
}
```

## 🎨 UI/UX Design

### Material Design 3
- **Color Scheme**: Professional green theme
- **Typography**: Roboto font family
- **Components**: Modern Material components
- **Accessibility**: Full accessibility support
- **Dark Mode**: Automatic dark/light theme switching

### Navigation Pattern
```
AuthActivity (Login) → MainActivity (Main Navigation)
                    ├── Tag Activation Module
                    ├── Single Scan Module  
                    ├── Batch Process Module
                    └── Settings Module
```

## 🧪 Testing Strategy

### Unit Tests
- **Repository Tests**: Data layer validation
- **ViewModel Tests**: Business logic verification  
- **Database Tests**: SQLDelight query testing

### Integration Tests
- **API Tests**: Network layer integration
- **Database Integration**: End-to-end data flow
- **UHF Hardware Tests**: Device communication

## 📋 Development Guidelines

### Code Standards
- **Kotlin Style Guide**: Follow official Kotlin conventions
- **Clean Architecture**: Maintain layer separation
- **SOLID Principles**: Follow object-oriented design principles
- **Documentation**: KDoc for all public APIs

### Git Workflow
- **Feature Branches**: Individual features in separate branches
- **Code Review**: All changes require review
- **Conventional Commits**: Standardized commit messages

### Performance Guidelines
- **Offline-First**: Minimize network dependencies
- **Memory Management**: Proper lifecycle handling
- **Battery Optimization**: Efficient background processing
- **Database Optimization**: Indexed queries and efficient schemas

## 🚧 Known Issues & Limitations

### Current Limitations
- **Manual Sync Only**: No automatic background sync
- **Single User Session**: One user logged in at a time
- **Basic Error Handling**: Needs enhanced error recovery

### Future Enhancements
- [ ] Background synchronization
- [ ] Multi-user support
- [ ] Advanced analytics
- [ ] Cloud backup
- [ ] Advanced UHF features

## 📞 Support

### Development Team
- **Architecture**: Clean Architecture with MVI pattern
- **Platform**: Android Native (Kotlin)
- **Database**: SQLDelight with offline-first approach
- **Integration**: UHF RFID hardware support

### Contact Information
For technical support or questions about the codebase, please refer to the development team or create an issue in the project repository.

---

## 📄 License

This project is proprietary software. All rights reserved.

---

**Built with ❤️ using Kotlin and Material Design 3**
