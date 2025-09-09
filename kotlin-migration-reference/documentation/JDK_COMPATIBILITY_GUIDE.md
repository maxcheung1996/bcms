# JDK Compatibility Guide for UHF Kotlin Migration

## 🎯 Recommended JDK Configuration

### **⭐ PROVEN WORKING CONFIGURATION ⭐**

**Use the EXACT setup that makes your current UHFDemo work:**
- ✅ **Java 15** (switched from Java 17 to Java 15 for vendor support)
- ✅ **Android Gradle Plugin 3.6.3** (vendor's original version)
- ✅ **Gradle 6.7.1** (compatible with AGP 3.6.3)
- ✅ **NDK Version 21.4.7075529** (available and tested)

## ⚠️ Critical Compatibility Requirements

### **Why NOT JDK 21?**
1. **Vendor Library Compatibility**: UHFJar_V1.4.05.aar was compiled with older Java versions
2. **Native Library Support**: UHF hardware libraries include JNI components that need tested JDK versions
3. **Android Studio Stability**: JDK 21 is very new and may cause unexpected issues
4. **Production Safety**: Stick with proven combinations for hardware-dependent apps

### **Recommended Setup:**

**⭐ Recommended: Azul Java 15 (Proven Working)**
```bash
# Use the SAME Azul Java 15 that makes your current UHFDemo work
# Set in Android Studio: File > Project Structure > SDK Location > JDK Location  
# Select your Azul Java 15 path
```

**Alternative Options (if Azul 15 not available):**
- JDK 11 (very stable)
- JDK 17 (modern but safe)
- **Avoid JDK 21** (too new for vendor libraries)

## 🔧 Gradle Configuration (CRITICAL)

**Regardless of your JDK version, you MUST set these in your app's build.gradle:**

```gradle
android {
    compileSdk 34
    
    // CRITICAL: Must be Java 1.8 for vendor UHF libraries
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    
    // CRITICAL: Kotlin must target Java 1.8 bytecode  
    kotlinOptions {
        jvmTarget = '1.8'
    }
}
```

## 🎯 Why This Configuration Works

### **JDK vs Bytecode Target:**
- **Your JDK (11/17)**: The development environment and build tools
- **Target Bytecode (1.8)**: What your code compiles to for compatibility

```
Your Code (Kotlin) → JDK 11/17 (Build Tools) → Java 1.8 Bytecode → Android Device
                                                      ↑
                              This matches vendor library requirements
```

### **Benefits:**
- ✅ Use modern JDK for development tools
- ✅ Compile to Java 1.8 bytecode for vendor library compatibility  
- ✅ Full Kotlin language features available
- ✅ Modern Android development experience
- ✅ Hardware libraries work correctly

## 🚨 What Happens If You Use JDK 21?

### **Potential Issues:**
- UHF libraries may fail to initialize
- Native JNI calls might break
- Unexpected runtime crashes
- Hard-to-debug compatibility problems
- Android Studio build issues

### **Error Examples:**
```
UnsupportedClassVersionError
NoClassDefFoundError  
UnsatisfiedLinkError (for native libraries)
```

## 🔧 How to Change JDK in Android Studio

### **Method 1: Project Structure**
1. Go to **File → Project Structure**
2. Select **SDK Location** on left
3. Change **JDK Location** to JDK 11 or 17 path
4. Click **Apply** and **OK**

### **Method 2: gradle.properties**
Add this line to your `gradle.properties`:
```properties
org.gradle.java.home=/path/to/jdk-11
```

### **Method 3: Terminal/Command Line**
```bash
# Set JAVA_HOME environment variable
export JAVA_HOME=/path/to/jdk-11

# Verify version
java -version
javac -version
```

## 📋 Verification Checklist

After changing JDK, verify everything works:

### ✅ Build Verification
- [ ] Project builds without errors
- [ ] Vendor libraries (.aar/.jar) are recognized
- [ ] No class version conflicts
- [ ] APK builds successfully

### ✅ Runtime Verification  
- [ ] App installs on device
- [ ] UHF libraries initialize correctly
- [ ] Hardware scanning functions work
- [ ] No runtime exceptions related to class loading

### ✅ Development Environment
- [ ] Android Studio runs smoothly
- [ ] Code completion works for vendor classes
- [ ] Debugging works correctly
- [ ] Build performance is acceptable

## 🎯 Final Recommendation

### **⭐ For Your Kotlin Project (UPDATED):**

1. **Use Azul Java 15** (exactly the same as your working UHFDemo project)
2. **Keep compileOptions to JavaVersion.VERSION_1_8** (for vendor library compatibility)
3. **Set kotlinOptions jvmTarget to '1.8'** (for bytecode compatibility)
4. **Test vendor library initialization immediately**

### **Why Azul Java 15 is Perfect:**
- ✅ **Proven Working** - Your current project builds successfully with it
- ✅ **Vendor Compatibility** - UHF libraries are confirmed working
- ✅ **Kotlin Compatible** - Supports all Kotlin language features  
- ✅ **Hardware Tested** - Already verified with your scanning device

### **Alternative if Azul 15 Not Available:**
- Regular JDK 15 (Oracle/OpenJDK)
- JDK 11 or JDK 17 as backup
- **Definitely avoid JDK 21** (too new for vendor libraries)

## 💡 Quick Test

After changing JDK, test this simple initialization:
```kotlin
try {
    val uhfManager = UHFManager.getUHFImplSigleInstance(UHFModuleType.UM_MODULE)
    val powerResult = uhfManager.powerOn()
    Log.d("UHF_TEST", "Power on result: $powerResult")
} catch (e: Exception) {
    Log.e("UHF_TEST", "UHF initialization failed", e)
}
```

If this works without errors, your JDK configuration is correct!

---

**Bottom Line: Use JDK 11 or 17, but always compile to Java 1.8 bytecode for vendor library compatibility. This gives you the best of both worlds - modern development tools with hardware compatibility.** 🎯
