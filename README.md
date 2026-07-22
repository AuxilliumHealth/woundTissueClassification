# Auxillium Health Wound Tissue Classification SDK – Android

An Android SDK and sample app for capturing wound images, classifying wound tissue, and previewing previously captured wounds.

## Repository Structure

This repository contains:

- **`app/`** – Sample app demonstrating SDK integration
- **`woundtissueclassification/`** – Android library (SDK) providing UI flows, CameraX integration, and networking

## Requirements

- **Android Studio:** Giraffe (AGP 8+)
- **Android SDK:** `compileSdk 36`, `minSdk 26`, `targetSdk 36`
- **JDK:** 17+ (toolchain configured in Gradle)

**Device Requirements:**

- Camera (CameraX supported)
- Internet access

## Repositories

### Groovy DSL (`settings.gradle`)

```groovy
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven { url 'https://jitpack.io' }
  }
}
```

### Kotlin DSL (`settings.gradle.kts`)

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

## Installation

### Option A: Include the Module

```kotlin
include(":woundtissueclassification")
```

```kotlin
dependencies {
    implementation(project(":woundtissueclassification"))
}
```

### Option B: Published Artifact

```kotlin
dependencies {
    implementation("com.github.AuxilliumHealth:woundtissueclassification:1.0.5")
}
```

## What's New in 1.0.5

- **Samsung Camera Fix**: Fixed Samsung multi-camera devices from locking/disabling the capture button.
- **Back Press Handling**: Enhanced back-press cancellation to properly exit back to the home page.
- **Stereo Camera Cleanup**: Completely removed legacy stereo camera code.

## Permissions

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```


> Note: Request **camera permission at runtime** on Android 6.0 (API 23) and above.

---

## Quick Start (Activity Result API)

The SDK provides two main entry points:

* **Wound Capture & Classification Flow**
  - `woundtissueclassificationWithLauncher()`: Launches the full wound assessment flow
    - `riskScoreRequired`: Boolean - If true, requires the user to complete a risk assessment
    - `bodySelectionRequired`: Boolean - If true, requires the user to select a body part
    - `calibrationRequired`: Boolean - If true, forces the calibration screen to appear (bypasses any existing calibration data)

* **Wound List Preview Flow**
  - `launchPreviewWoundList()`: Shows a list of previously captured wounds

### Flow Control Parameters

1. **riskScoreRequired** (Boolean)
   - `true`: User must complete a risk assessment before proceeding
   - `false`: Skips the risk assessment step

2. **bodySelectionRequired** (Boolean)
   - `true`: User must select a body part before capturing the wound
   - `false`: Skips the body part selection step

3. **calibrationRequired** (Boolean)
   - `true`: Always shows the calibration screen, even if calibration data exists
   - `false`: Only shows calibration if no calibration data exists

### Best Practices
- For first-time users, set `calibrationRequired = true` to ensure proper setup
- For returning users, you can set `calibrationRequired = false` to skip calibration if they've already completed it
- Use `riskScoreRequired` and `bodySelectionRequired` based on your clinical workflow requirements

Example (`MainActivity.java` in the sample `app/`):

```java
public class MainActivity extends AppCompatActivity {
    private final ActivityResultLauncher<Intent> woundTissueLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            // TODO: Handle result if your flow returns data
            // Example: inspect result.getResultCode() and result.getData()
        });
// Reset calibration data if needed
private void resetCalibration() {
    com.auxilliumhealth.woundtissueclassification.woundtissueclassification.resetCalibration(context);
}

    private void launchCapture() {
        com.auxilliumhealth.woundtissueclassification.woundtissueclassification
            .woundtissueclassificationWithLauncher(
                woundTissueLauncher,
                this,
                "user-Identity",   // your userId
                "wound_id",        // woundId (Required)
                "token",           // Bearer token from https://console.woundtele.com
                "#2196F3",         // primary color (hex or theme key)
                true,               // riskScoreRequired: if true, requires risk assessment
                true,              // bodySelectionRequired: if true, requires body part selection
                true               // calibrationRequired: if true, forces calibration screen
            );
    }

  
        private void launchPreview() {
            woundtissueclassification.launchPreviewWoundList(this, 
                    "user_id", // userId (Mandatory)
                    "wound_id",//woundId  (optional)
                    "token", // https://console.woundtele.com (Mandatory)
                    "#2CA6CC"); //primaryColor (Mandatory)
        }
    
}
```

### Optional Calibration Reset


Example (`MainActivity.kt` in the sample `app/`):

```kotlin
class MainActivity : AppCompatActivity() {

    private val woundTissueLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // TODO: Handle result if your flow returns data
            // Example: inspect result.resultCode and result.data
        }
    private fun resetCalibration() {
        com.auxilliumhealth.woundtissueclassification.woundtissueclassification
            .resetCalibration(context)
    }


    private fun launchCapture() {
        com.auxilliumhealth.woundtissueclassification.woundtissueclassification
            .woundtissueclassificationWithLauncher(
                woundTissueLauncher,
                this,
                "user-Identity",   // your userId
                "wound_id",        // woundId (Required)
                "token",           // Bearer token from https://console.woundtele.com
                "#2196F3",          // primary color (hex or theme key)
                true,               // riskScoreRequired: if true, requires risk assessment
                true,              // bodySelectionRequired: if true, requires body part selection
                true               // calibrationRequired: if true, forces calibration screen
            )
    }

    private fun launchPreview() {
        com.auxilliumhealth.woundtissueclassification.woundtissueclassification
            .launchPreviewWoundList(
                this,
                "user-Identity",// your userId
                "wound_id", // woundId (optional)
                "token", // Bearer token from https://console.woundtele.com
                "#2196F3" // primary color (hex or theme key)
             
            )
    }
}
```

### Optional Calibration Reset

```kotlin
// Reset calibration data if needed
 com.auxilliumhealth.woundtissueclassification.woundtissueclassification.resetCalibration(context)
```
---

## Tokens and Backend

* The SDK communicates with Auxillium Health APIs via **Retrofit**.
* **Base URLs:**

    * API: `https://api.woundtele.com`
    * Calibration: `https://calibration.woundtele.com/`

You must pass a valid **Bearer token** obtained from the [Auxillium Health Console](https://console.woundtele.com).

---

## ProGuard / R8 Rules

If you enable code shrinking, keep these classes to avoid runtime issues:

```
# Auxillium SDK
-keep class com.auxilliumhealth.woundtissueclassification.** { *; }

# Retrofit / Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
```

Adjust based on your app’s build configuration.

---

## CameraX and NDK Notes

* The SDK uses the following CameraX dependencies:

    * `camera-core`
    * `camera-camera2`
    * `camera-lifecycle`
    * `camera-view`
    * `camera-extensions`
* Ensure your project matches the SDK’s `compileSdk` version.
* The sample app specifies:

  ```kotlin
  ndkVersion = "27.1.12297006"
  ```

  Match this version to your local NDK installation, or remove if unnecessary.

---

## Troubleshooting

| Issue                             | Possible Cause                         | Solution                                                                         |
| --------------------------------- | -------------------------------------- | -------------------------------------------------------------------------------- |
| Build fails on missing repository | Missing `mavenCentral()` or `google()` | Add required repositories in your Gradle config                                  |
| 401 / 403 API error               | Invalid or expired Bearer token        | Generate a new token from [console.woundtele.com](https://console.woundtele.com) |
| Camera not working                | Missing permissions or emulator usage  | Test on a physical device and verify runtime permission handling                 |

---
# 📦 Wound Data Storage Structure

This document explains how wound-related data is organized in a clear hierarchical format:

**User → Wound → Imaging Session**

This structure ensures clean organization, easy querying, and efficient tracking of wound healing progress over time.

---

## 📘 Hierarchical Data Model

```
User (userId)
    ├── Wound (woundId_1)
    │       ├── ImagingSession (sessionId_1)
    │       ├── ImagingSession (sessionId_2)
    │       └── ImagingSession (sessionId_3)
    │
    ├── Wound (woundId_2)
    │       ├── ImagingSession (sessionId_4)
    │       └── ImagingSession (sessionId_5)
    │
    └── Wound (woundId_3)
            └── ImagingSession (sessionId_6)
```

---

## 🧭 Explanation

### **1️⃣ User**
- Identified by **userId**
- Represents a patient in the system
- A user can have **multiple wounds**

### **2️⃣ Wound**
- Identified by **woundId**
- Belongs to a specific **userId**
- **A particular wound can have multiple imaging sessions for tracking the healing progress over time**
- Allows long-term comparison and monitoring

### **3️⃣ Imaging Session**
- Identified by **sessionId**
- Represents a single wound imaging event
- Stores:
    - Raw image(s)
    - Processed output
    - Pixel analysis
    - Measurements
    - Metadata
    - Timestamps
- Multiple sessions help visualize improvement or deterioration of a wound over days/weeks

---

## 📄 Example JSON Structure

```json
{
  "userId": "USER_001",
  "wounds": [
    {
      "woundId": "WOUND_01",
      "sessions": [
        { "sessionId": "SESSION_01" },
        { "sessionId": "SESSION_02" },
        { "sessionId": "SESSION_03" }
      ]
    },
    {
      "woundId": "WOUND_02",
      "sessions": [
        { "sessionId": "SESSION_04" },
        { "sessionId": "SESSION_05" }
      ]
    }
  ]
}
```

---

## 🗂 Relationship Summary

| Entity             | Identifier | Relationship                                    |
|-------------------|------------|--------------------------------------------------|
| **User**          | userId     | Can have multiple wounds                         |
| **Wound**         | woundId    | Belongs to a user; has multiple imaging sessions |
| **ImagingSession**| sessionId  | Belongs to a wound                               |

---


## Tissue Classification Reference

### Peri-Wound Tissue Type Detection (`#E1E1E1`)

| Tissue Type | Color Code |
|-------------| ---------- |
| Maceration  | `#B81901`  |
| Erythema    | `#FF644D`  |
| Callus      | `#59AF57`  |
| Other       | `#FFE8C7`  |


### Wound Tissue Type Detection (`#FF5454`)

| Tissue Type        | Color Code |
|--------------------| ---------- |
| Granulation Region | `#FE7F0F`  |
| Slough Region      | `#F4F472`  |
| Eschar Region      | `#207BB4`  |
| Other Region       | `#FFE8C7`  |

---

## License

**Proprietary. © Auxillium Health. All rights reserved.**

---

## Support

* 📧 Email: [software@auxilliumhealth.ai](mailto:software@auxilliumhealth.ai)
* 🌐 Console: [https://console.woundtele.com](https://console.woundtele.com)
