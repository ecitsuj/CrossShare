# CrossShare 📱💻

**CrossShare** is a modern, lightweight, and completely offline file-sharing Android application. It transforms your Android device into a local HTTP web server, allowing any nearby device with a web browser (such as a Mac, Windows PC, iOS device, or Linux machine) to seamlessly upload and download files over a local Wi-Fi hotspot or shared router—**without requiring an internet connection, cloud storage, or companion apps.**

---

## ✨ Key Features

* **Instant QR Code Connection:** Start the server on Android, scan the generated QR code with any device's camera, and instantly open the responsive web interface in your browser.
* **100% Offline & Private:** Operates purely on your local network. No external servers, cloud relays, or data tracking.
* **Browser-Based File Uploads:** Connected clients can stream file uploads directly into your Android device's Downloads directory using efficient memory-safe chunking via `MediaStore`.
* **Multi-File Sharing:** Select multiple local files using native Android system file pickers and stream them directly to browser clients via `ContentResolver`.
* **Reliable Background Execution:** Runs inside an Android Foreground Service with a persistent notification and wake-lock management to keep transfers stable even if the app goes to the background.

---

## 🛠️ Tech Stack & Libraries

* **Language:** [Kotlin](https://kotlinlang.org/) (Coroutines & StateFlows)
* **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) & Material 3
* **Embedded HTTP Server:** [Ktor Server](https://ktor.io/) (CIO Engine, listening on port `8080`)
* **QR Code Rendering:** [ZXing (Zebra Crossing)](https://github.com/zxing/zxing)
* **Android System APIs:** Foreground Services, `MediaStore`, and `ContentResolver`

---

## 🚀 Getting Started & Build Instructions

### Prerequisites

* [Android Studio](https://developer.android.com/studio) (Koala or newer recommended)
* Android SDK (Min SDK: 26, Target SDK: 35)
* JDK 17

### Installation & Local Build

1. **Clone the repository:**
```bash
git clone https://github.com/ecitsuj/CrossShare.git

```


2. **Open the project:**
* Launch Android Studio, select **Open**, and choose the cloned `CrossShare` folder.


3. **Sync Gradle:**
* Allow Android Studio to fetch project dependencies and sync files.


4. **Build and Run:**
* Connect an Android device via USB (with USB Debugging enabled) or start an emulator, then click the **Run ' ▶ '** button in Android Studio.



---

## 📄 License

This project is open-source and available under the [MIT License](https://www.google.com/search?q=LICENSE).