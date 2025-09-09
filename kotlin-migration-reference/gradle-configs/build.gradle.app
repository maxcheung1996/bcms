apply plugin: 'com.android.application'
apply plugin: 'kotlin-android'
apply plugin: 'kotlin-android-extensions'  // For AGP 3.6.3 compatibility

android {
    compileSdkVersion 30  // Keep same as original working project
    ndkVersion "21.4.7075529"  // CRITICAL: Use your proven working NDK version
    
    defaultConfig {
        applicationId "com.uhf.kotlindemo"
        minSdkVersion 23
        targetSdkVersion 30  // Keep same as original
        versionCode 1
        versionName "1.0.0"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
        debug {
            debuggable true
        }
    }

    // CRITICAL: Required for vendor UHF libraries
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = '1.8'
    }

    // CRITICAL: Required for vendor JNI libraries  
    sourceSets {
        main {
            jniLibs.srcDirs = ['libs']
        }
    }

    // Enable ViewBinding for modern UI
    buildFeatures {
        viewBinding true
    }

    // Required for vendor library packaging
    packagingOptions {
        exclude 'META-INF/proguard/androidx-annotations.pro'
    }

    lintOptions {
        disable 'GoogleAppIndexingWarning'
        abortOnError false
    }
}

dependencies {
    implementation "org.jetbrains.kotlin:kotlin-stdlib:1.8.22"
    
    // AndroidX Core - Compatible with AGP 3.6.3
    implementation 'androidx.appcompat:appcompat:1.2.0'
    implementation 'androidx.core:core-ktx:1.3.2'
    implementation 'androidx.fragment:fragment-ktx:1.2.5'
    implementation 'androidx.constraintlayout:constraintlayout:2.0.4'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.2.0'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.2.0'
    
    // Material Design - Compatible version
    implementation 'com.google.android.material:material:1.2.1'
    
    // Coroutines for async operations - Compatible version
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2'
    
    // CRITICAL: Vendor UHF Libraries - DO NOT CHANGE VERSIONS
    implementation fileTree(dir: 'libs', include: ['*.jar','*.aar'])
    // Specifically:
    // - UHFJar_V1.4.05.aar (Main UHF functionality)
    // - iscanuserapi.jar (Barcode scanning support)
    
    // Data Persistence (Same as original)
    implementation 'com.tencent:mmkv:1.2.11'
    
    // Excel Export (Same as original)
    implementation 'net.sourceforge.jexcelapi:jxl:2.6.12'
    
    // Testing
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}

// CRITICAL NOTES:
// 1. Keep Java 1.8 compatibility for vendor libraries
// 2. Must include vendor JAR/AAR files in libs folder
// 3. JNI libraries path must be configured correctly
// 4. MMKV version should match original for data compatibility
