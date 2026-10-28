# Project Plan

Create a complete, offline, cross-platform file-sharing Android app named CrossShare in Kotlin using Jetpack Compose and Material 3 for phone form factors.

App Purpose & Architecture:
The Android device acts as a local HTTP web server. Users on iOS or other devices connect to the Android phone's Wi-Fi Hotspot (or shared local router), scan a QR code shown on the Android UI, and open a hosted web interface in their mobile browser to upload files to the Android device or download shared files.

Required Dependencies & Libraries:
- Ktor Server or NanoHTTPD for embedded HTTP server on port 8080.
- ZXing for generating QR codes in Compose.

Core Technical Requirements:
1. Hotspot-Compatible IP Detection using NetworkInterface.getNetworkInterfaces() searching for IPv4 site-local addresses (192.168.x.x, 10.x.x.x, 172.16-31.x.x) across wlan, ap, or rndis interfaces.
2. Foreground Service with persistent notification running the HTTP server.
3. Memory-Safe File Uploads serving an embedded HTML/CSS/JS web page at GET /, POST /upload streaming to Android Downloads folder via MediaStore/Storage.
4. File Sharing (Android to Browser): Pick multiple files via GetMultipleContents, stream via ContentResolver at GET /download/{id}.
5. Jetpack Compose UI: Start/Stop Server button, active local URL text, generated QR Code bitmap, Select Files to Share button, list of shared/received files, and notification permission requests.

## Project Brief

# Project Brief: CrossShare

## Features

1. **Local HTTP Server & QR Code Access**: Hosts an offline embedded web server within an Android Foreground Service, automatically detects the device's Wi-Fi or Hotspot IPv4 address, and displays an on-screen QR code for instant browser connection from connected client devices.
2. **Browser-Based File Uploads**: Serves a lightweight web interface to connected client browsers allowing users on iOS or other platforms to stream file uploads directly to the Android device's Downloads directory using `MediaStore`.
3. **Multi-File Sharing & Downloading**: Enables selecting multiple local files using Android system file pickers (`GetMultipleContents`) and streams them efficiently to browser clients via `ContentResolver`.
4. **Server Control & Transfer Dashboard**: A Jetpack Compose user interface providing controls to Start/Stop the HTTP server service, view active connection URLs, display generated QR codes, select files for sharing, and track shared/received file lists.

---

## High-Level Technical Stack

- **Language & Core Runtime**: Kotlin, Kotlin Coroutines, and Flow.
- **UI Framework**: Jetpack Compose with Material 3.
- **Navigation & Adaptive Strategy**: Jetpack Navigation 3 (state-driven navigation) and Compose Material Adaptive (`androidx.compose.material3.adaptive`).
- **Embedded Web Server**: Ktor Server (CIO engine) listening on port 8080 for handling GET and POST multipart file streaming requests.
- **QR Code Generation**: ZXing (`core` and `android-embedded` / Compose bitmap renderer).
- **System Integration**: Android Foreground Service with Persistent Notification, `NetworkInterface` IP detection, and `ContentResolver` / `MediaStore` APIs for memory-safe file operations.

## Implementation Steps
**Total Duration:** 2h 13m 30s

### Task_1_DependenciesAndCoreServerEngine: Configure Gradle dependencies (Ktor CIO, ZXing) and implement IP address detection, Ktor web server routes (GET /, POST /upload, GET /download/{id}), and MediaStore/ContentResolver streaming.
- **Status:** COMPLETED
- **Updates:** Successfully added Ktor server (CIO, core, cors), ZXing, and SLF4J dependencies. Implemented NetworkUtils for hotspot-compatible IPv4 detection, WebPageContent for HTML/CSS/JS frontend, memory-safe MediaStore file streaming for uploads at POST /upload, ContentResolver streaming at GET /download/{id}, and SharedFileManager state management. Unit tests and assembleDebug build succeeded.
- **Acceptance Criteria:**
  - Ktor and ZXing dependencies added
  - IP address detection utility finds local IPv4 address
  - Ktor web server routes handle web page, upload, and download streaming
- **Duration:** 17m 34s

### Task_2_ForegroundServiceAndQRGeneration: Implement Android Foreground Service running Ktor HTTP server with persistent notification, manifest permissions, and ZXing QR code bitmap generation.
- **Status:** COMPLETED
- **Updates:** Implemented FileSharingService with notification channel, persistent notification showing active URL and stop action, foregroundServiceType dataSync/connectedDevice, wifi lock management, service binder and StateFlows for UI binding. Updated AndroidManifest.xml with all required foreground service and network permissions. Enhanced QrCodeGenerator to return Compose ImageBitmap. Verified build and tests cleanly.
- **Acceptance Criteria:**
  - Foreground service manages Ktor HTTP server lifecycle with persistent notification
  - Manifest configured with foreground service and network permissions
  - ZXing QR generator produces Bitmap for connection URL
- **Duration:** 3m 24s

### Task_3_ComposeUIAndFilePicker: Develop Jetpack Compose Material 3 UI with server controls, active URL text, QR code viewer, file picker integration, and file list.
- **Status:** COMPLETED
- **Updates:** Implemented complete Jetpack Compose Material 3 UI in MainScreen.kt and MainActivity.kt. Added Server Control Card with Start/Stop toggles, active URL with copy/open actions, QR code card with animated loading/display, file selector with ActivityResultContracts.GetMultipleContents(), tabbed list for Shared and Received files with remove/open actions, notification permission handling for Android 13+, and FileUtils for metadata and file opening. Build and unit tests verified successfully.
- **Acceptance Criteria:**
  - Start/Stop server button and QR code rendered in Compose UI
  - File picker allows selecting files to serve at GET /download/{id}
  - Shared and received files listed cleanly in UI
  - Notification permission requested when required
- **Duration:** 35m 18s

### Task_4_RunAndVerify: Execute final build, test server lifecycle, verify web interface streaming, confirm UI alignment, and instruct critic_agent to verify app stability and report any issues.
- **Status:** COMPLETED
- **Updates:** Investigated and resolved server startup issue: added fallback port binding (8080-8089) to handle port-in-use gracefully, ensured non-blocking CIO server startup with try-catch error handling, improved FileSharingService coroutine dispatch and state updates on failure, and verified build and unit tests successfully.
- **Acceptance Criteria:**
  - build pass
  - app does not crash
  - make sure all existing tests pass
  - application stability verified and meets user requirements
- **Duration:** 1h 17m 14s

