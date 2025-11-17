# Auxillium Health Wound Tissue Classification SDK – Android

An Android SDK and sample app for capturing wound images, classifying tissue, and previewing previously captured wounds.

---

## Repository Structure

This repository contains:

* **`app/`** – Sample app demonstrating SDK integration
* **`woundtissueclassification/`** – Android library (SDK) providing UI flows, CameraX integration, and networking

---

## Requirements

* **Android Studio:** Giraffe (AGP 8+)
* **Android SDK:** `compileSdk 36`, `minSdk 26`, `targetSdk 36`
* **JDK:** 17+ (toolchain configured in Gradle)
* **Device Requirements:**

  * Camera (CameraX supported)
  * Internet access

---

## Repositories

Ensure your `repositories` block includes:

```kotlin
repositories {
    google()
    mavenCentral()
    // Add GitHub Packages or JitPack if using remote artifacts
}
```

---

## Installation

You can integrate the SDK in two ways:

### Option A: Include the Module (Used in This Repo)

1. Add the module in `settings.gradle.kts`:

   ```kotlin
   include(":woundtissueclassification")
   ```
2. Add dependency in your app module’s `build.gradle.kts`:

   ```kotlin
   dependencies {
       implementation(project(":woundtissueclassification"))
   }
   ```

### Option B: Use the Published Artifact

If using GitHub Packages:

```kotlin
dependencies {
    implementation("com.github.AuxilliumHealth:woundtissueclassification:1.0.2")
}
```

---

## Permissions

Add the following permissions to your app’s `AndroidManifest.xml`:

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
* **Wound List Preview Flow**

Example (`MainActivity.java` in the sample `app/`):

```java
public class MainActivity extends AppCompatActivity {
    private final ActivityResultLauncher<Intent> woundTissueLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            // TODO: Handle result if your flow returns data
            // Example: inspect result.getResultCode() and result.getData()
        });

    private void launchCapture() {
        com.auxilliumhealth.woundtissueclassification.woundtissueclassification
            .woundtissueclassificationWithLauncher(
                woundTissueLauncher,
                this,
                "user-Identity",   // your userId
                "token",           // Bearer token from https://console.woundtele.com
                "#2196F3"          // primary color (hex or theme key)
            );
    }

    private void launchPreview() {
        com.auxilliumhealth.woundtissueclassification.woundtissueclassification
            .launchPreviewWoundList(
                this,
                "user-Identity",
                "token",
                "#2196F3",
                false //riskScoreRequired
            );
    }
}
```

### Optional Calibration Reset

```java
// Reset calibration data if needed
// com.auxilliumhealth.woundtissueclassification.woundtissueclassification.resetCalibration(context);
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
