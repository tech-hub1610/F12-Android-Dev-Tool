# F12 Browser — Mobile Developer Tools

**F12 Browser** is a native Android web browser built from the ground up in Java and Android XML Layouts, featuring a Chrome DevTools-inspired in-app developer toolkit accessible with a single tap on the **[F12]** button immediately adjacent to the URL field.

---

## Tech Stack & Compatibility
- **Platform**: Native Android (Phone & Tablet)
- **Programming Language**: Java 17
- **UI Architecture**: Android XML Layouts with Material Design foundation
- **Theme**: Minimalist Black & White monochrome (`#000000`, `#121212`, `#FFFFFF`)
- **Browser Engine**: Android WebView (Chromium core) with secure JavaScript instrumentation
- **Build System**: Gradle 8.4 + Android Gradle Plugin 8.2.2
- **Minimum Android Version**: Android 8.0 (API 26)
- **Target Android Version**: Android 14 (API 34)
- **Output Artifacts**: Versioned Android APKs (Debug & Signed Release)

---

## Key Features

- **App Icon**: Redesigned monochrome F12 browser window emblem with terminal prompt cursor.
- **Splash Screen**: Dedicated launch activity (`SplashActivity`) featuring the minimalist F12 vector logo, animated loading bar, and subtle version indicator.
- **Native Browser Dashboard**: Replaces traditional raw HTML start pages with a high-performance native Android XML dashboard featuring:
  - Header branding & quick search card that directly activates the URL bar.
  - Speed Dial grid: Google, GitHub, MDN Docs, StackOverflow, CanIUse, Localhost (:3000), Emulator (:8080), and Sandbox.
  - DevTools Quick Access chips: Instant one-tap navigation to Elements, Console, Network, Audits, Storage, or Responsive modes.
  - Dynamic Bookmarks and Browsing History cards with "View All" modal triggers.
- **Top Toolbar**: `[ Home ] [ URL / Search Field ] [ Active Tab Switcher ] [ F12 ] [ ⋮ ]`
- **Active Tab Switcher**: Displays live open tab count inside a chrome-style badge; tap to switch/close tabs, long-press to open new tab.
- **F12 Button**: Located immediately beside the active tab icon for instant inspection.
- **Full Navigation**: Back, Forward, Reload, Stop loading, Home page, and Download handling.
- **Multiple Tabs**: Dynamic tab creation, switching, and closing.
- **Bookmarks & History**: Local storage for bookmarks and timestamped browsing history.
- **Find in Page**: Asynchronous text search across live web pages.
- **Desktop Mode**: One-tap toggle to desktop Chrome User-Agent.
- **SSL Security**: Strict SSL certificate verification with interactive warning dialogs.

### 2. Integrated F12 Developer Toolkit
- **Elements Inspector**:
  - Live hierarchical DOM tree with collapsible/expandable nodes.
  - Search by tag, ID, or CSS class.
  - Interactive "Tap to Inspect" mode that highlights elements directly on the webpage.
  - Live editing of HTML and CSS styles.
  - Interactive CSS Box Model visualizer (Margin, Border, Padding, Content).
- **JavaScript Console**:
  - Evaluate JavaScript in the active page's context.
  - Capture `console.log`, `info`, `warn`, `error`, and uncaught runtime errors.
  - Command history navigation (Up / Down) and multiline support.
  - `$0` shortcut references the selected element in the Elements tab.
- **Network Monitor**:
  - Intercepts XHR, Fetch, documents, scripts, stylesheets, and images.
  - View status codes, request/response headers, timings, and formatted response body previews.
  - Export HAR/JSON diagnostic report.
- **Sources Viewer**:
  - Inspect HTML, inline scripts, and discovered external CSS/JS files.
  - Monospace code view with line numbers and keyword search.
- **Performance Monitor**:
  - Real W3C Navigation Timing: DNS, TCP, TTFB, DOMContentLoaded, Load.
  - Paint Timing (FP & FCP).
  - Live recording session tool.
- **Responsive Device Tester**:
  - Viewport presets: iPhone 14/15 Pro, Pixel 7/8, iPad, Desktop 1080p, and custom dimensions.
  - Orientation toggling (Portrait / Landscape).
  - Live media queries breakdown.
- **Application & Storage**:
  - Local Storage, Session Storage, Cookies, and IndexedDB inspector.
  - Add, edit, delete, and clear stored entries with confirmation dialogs.
- **Security Panel**:
  - TLS status and SSL certificate details (Subject, Issuer, Validity dates).
  - Automatic detection of insecure mixed content.
- **Lighthouse-Style Audit Engine**:
  - Real audits for SEO, Accessibility, Best Practices, Performance, and Mobile Usability.
  - Category percentage scores and exportable audit reports.
- **More Tools**:
  - User-Agent switcher & Sensor Geolocation override.
  - Offline utilities: JSON Formatter, URL Encoder/Decoder, Base64 Codec, RegEx Tester.
  - Complete Diagnostic Bundle export.

---

## Directory Structure

```
/sdcard/prjcts/F12/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/f12/browser/
│   │   │   │   ├── MainActivity.java
│   │   │   │   ├── SplashActivity.java
│   │   │   │   ├── browser/
│   │   │   │   │   ├── BrowserTab.java
│   │   │   │   │   ├── TabManager.java
│   │   │   │   │   ├── BrowserWebViewClient.java
│   │   │   │   │   ├── BrowserWebChromeClient.java
│   │   │   │   │   ├── BookmarkManager.java
│   │   │   │   │   ├── HistoryManager.java
│   │   │   │   │   └── SettingsManager.java
│   │   │   │   ├── devtools/
│   │   │   │   │   ├── DevToolsBridge.java
│   │   │   │   │   ├── DevToolsController.java
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── ConsoleMessageItem.java
│   │   │   │   │   │   ├── NetworkRequestItem.java
│   │   │   │   │   │   ├── DomNode.java
│   │   │   │   │   │   ├── AuditItem.java
│   │   │   │   │   │   └── PerformanceMetrics.java
│   │   │   │   │   └── panels/
│   │   │   │   │       ├── DevToolsPanel.java
│   │   │   │   │       ├── ElementsPanel.java
│   │   │   │   │       ├── ConsolePanel.java
│   │   │   │   │       ├── NetworkPanel.java
│   │   │   │   │       ├── SourcesPanel.java
│   │   │   │   │       ├── PerformancePanel.java
│   │   │   │   │       ├── ResponsivePanel.java
│   │   │   │   │       ├── ApplicationPanel.java
│   │   │   │   │       ├── SecurityPanel.java
│   │   │   │   │       ├── LighthousePanel.java
│   │   │   │   │       └── MoreToolsPanel.java
│   │   │   │   └── util/
│   │   │   │       ├── UrlUtils.java
│   │   │   │       └── FormatUtils.java
│   │   │   ├── assets/
│   │   │   │   ├── devtools/injected.js
│   │   │   │   └── home/index.html
│   │   │   ├── res/
│   │   │   │   ├── drawable/
│   │   │   │   ├── layout/
│   │   │   │   │   ├── activity_main.xml
│   │   │   │   │   ├── activity_splash.xml
│   │   │   │   │   └── layout_browser_dashboard.xml
│   │   │   │   ├── values/
│   │   │   │   └── mipmap-anydpi-v26/
│   │   │   └── AndroidManifest.xml
│   │   └── test/java/com/f12/browser/
│   │       └── BrowserCoreUnitTest.java
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── release.keystore
├── output/
│   ├── F12-Browser-v1.0.0-debug.apk
│   ├── F12-Browser-v1.0.0-release.apk
│   ├── F12-Browser-v1.1.0-debug.apk
│   └── F12-Browser-v1.1.0-release.apk
├── build_apk.sh
├── build_plan.md
├── FEATURES.md
├── build.gradle
├── settings.gradle
└── gradle.properties
```

---

## Build & Run Instructions

### 1. Build Versioned APKs (Debug & Release)
To build both debug and release APKs:
```bash
gradle assemble
```
The generated APKs are automatically saved to `output/`:
- `output/F12-Browser-v1.0.0-debug.apk`
- `output/F12-Browser-v1.0.0-release.apk`

### 2. Building Versioned Upgrades
To build an upgraded version, run the automated `build_apk.sh` script:
```bash
./build_apk.sh <versionName> <versionCode>
# Example:
./build_apk.sh 1.1.0 2
```
This updates the version metadata in `app/build.gradle`, runs all unit tests, compiles the application, and produces:
- `output/F12-Browser-v1.1.0-debug.apk`
- `output/F12-Browser-v1.1.0-release.apk`

### 3. Running Automated Tests
```bash
gradle test
```

### 4. Installing the APK on a Device
```bash
adb install -r output/F12-Browser-v1.0.0-debug.apk
```
Or for release:
```bash
adb install -r output/F12-Browser-v1.0.0-release.apk
```
