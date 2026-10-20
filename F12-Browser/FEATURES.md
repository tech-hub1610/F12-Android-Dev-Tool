# F12 Browser — Feature Support & Limitations Matrix

This document provides a technical accounting of implemented, partially supported, and unsupported features in the F12 Browser application, reflecting the technical boundaries of the Android WebView runtime.

---

## 1. Core Browser Functionality

| Feature | Status | Notes |
| :--- | :---: | :--- |
| URL & Search Input | **Supported** | Auto-detects URLs, hostnames, ports, and queries with configurable search engine. |
| Navigation (Back/Forward) | **Supported** | Native WebView history stack navigation. |
| Reload & Stop Loading | **Supported** | Full page reload and lifecycle controls. |
| Custom Start / Home Page | **Supported** | Built-in offline monochrome developer dashboard with live testing sandbox. |
| Multi-Tab Management | **Supported** | Dynamic tab switching, creation, and closing with memory cleanup. |
| Bookmarks System | **Supported** | Add, list, open, and delete bookmarks. |
| Browsing History | **Supported** | Timestamped history with search and clear data options. |
| Downloads Handling | **Supported** | Integrates with Android `DownloadManager` for background file downloads. |
| Find in Page | **Supported** | High-performance asynchronous `findAllAsync` and `findNext` match traversal. |
| Share Page | **Supported** | Standard Android `Intent.ACTION_SEND` sharing. |
| Desktop Site Request | **Supported** | Toggles Chrome Desktop User-Agent with wide viewport simulation. |
| SSL / TLS Security | **Supported** | Strict SSL error dialog with certificate inspection; never blindly bypassed. |

---

## 2. F12 Developer Toolkit Panels

### Elements Inspector
- **DOM Hierarchy**: **Supported**. Traverses and serializes live document tree into collapsible/expandable nodes.
- **Search Elements**: **Supported**. Search by tag, `#id`, or `.class`.
- **Live Highlight**: **Supported**. Injects high-contrast bounding box overlay with dimension tooltips over the live element.
- **Live HTML Editing**: **Supported**. Live editing of `outerHTML` reflects immediately on the page.
- **Live CSS Editing**: **Supported**. Dynamic property/value mutation updates live element styles.
- **Box Model Diagram**: **Supported**. Computes and renders Margin, Border, Padding, and Content dimensions.
- **Attribute Inspector**: **Supported**. View, add, and update element attributes.
- **Tap to Inspect on Page**: **Supported**. Interactive touch inspector on the page selects and highlights the tapped element.

### JavaScript Console
- **Script Evaluation**: **Supported**. Evaluates JS in page context using `evaluateJavascript`.
- **Log Interception**: **Supported**. Captures `log`, `info`, `warn`, `error`, `debug`, `window.onerror`, and `unhandledrejection`.
- **Filtering & Search**: **Supported**. Filter by severity levels and text queries.
- **Multiline Input & History**: **Supported**. Command input with history traversal and clear/copy controls.
- **$0 Shortcut**: **Supported**. Inspects currently selected element in the Elements panel.

### Network Inspector
- **Fetch & XHR Interception**: **Supported**. Monkey-patched runtime hooks capture URL, method, status, request/response headers, duration, and response body previews.
- **Resource Subrequest Interception**: **Supported**. `WebViewClient.shouldInterceptRequest` captures documents, stylesheets, scripts, images, fonts, and media.
- **HAR Export**: **Supported**. Exports standard HAR/JSON diagnostic archive via Android share sheet.
- **Streaming / WebSocket Frames**: **Partially Supported**. Requests to WebSocket URLs are observed; individual frame-level binary packet decoding is limited by Android WebView APIs.

### Sources & Code Viewer
- **Document Source**: **Supported**. View full DOM/HTML source with line numbers.
- **External Scripts & Styles**: **Supported**. Discovers `<script src>` and `<link rel="stylesheet">` tags and displays source code.
- **Search in Source**: **Supported**. Real-time keyword highlighting.
- **Chrome Breakpoints / Debugger Step**: **Unsupported by Platform**. Android WebView does not expose Chrome DevTools Protocol (CDP) breakpoint step execution in-app without desktop Chrome USB remote debugging.

### Performance Monitor
- **Navigation Timing**: **Supported**. DNS, TCP connect, TTFB, DOMContentLoaded, and Page Load completion.
- **Paint Timing**: **Supported**. First Paint (FP) and First Contentful Paint (FCP).
- **DOM Composition**: **Supported**. Counts total DOM elements, scripts, styles, and images.
- **Live Recording**: **Supported**. Real-time recording workflow of page responsiveness.

### Responsive Device Testing
- **Device Presets**: **Supported**. iPhone 14/15 Pro, Pixel 7/8, iPad Mini/Air, Desktop 1080p.
- **Custom Viewports**: **Supported**. Width and height adjustment via dynamic viewport meta tag manipulation.
- **Orientation Toggle**: **Supported**. Switches between Portrait and Landscape configurations.
- **Active Media Queries**: **Supported**. Live evaluation of CSS media query breakpoints.

### Application & Storage Inspector
- **Local Storage**: **Supported**. View, search, add, edit, delete, and clear.
- **Session Storage**: **Supported**. View, search, add, edit, delete, and clear.
- **Cookies**: **Supported**. View, add, and delete cookies via `CookieManager` and `document.cookie`.
- **IndexedDB**: **Supported**. Lists database names and versions via `indexedDB.databases()`.

### Security Inspector
- **HTTPS Status**: **Supported**. Real-time TLS status.
- **Certificate Inspection**: **Supported**. Subject DN, Issuer DN, and Validity dates from Android `SslCertificate`.
- **Mixed Content Warnings**: **Supported**. Detects HTTP subresources loaded on HTTPS pages.
- **Security Headers**: **Supported**. Analyzes Content Security Policy (CSP) and security flags.

### Lighthouse-Style Audits
- **Auditing Engine**: **Supported**. Real checks for SEO, Accessibility, Best Practices, Performance, and Mobile Usability.
- **Scoring & Evidence**: **Supported**. 0-100% category scoring with evidence and recommendations.
- **Report Export**: **Supported**. Export audit findings as text/JSON.

### More Tools
- **User-Agent Switcher**: **Supported**. Switch between Android Mobile, Desktop Chrome, Safari iOS, Firefox, and Custom UA.
- **Sensor Override**: **Supported**. Injects mock Geolocation coordinates (`navigator.geolocation.getCurrentPosition`).
- **Developer Utilities**: **Supported**. Offline JSON Formatter/Validator, URL Encoder/Decoder, Base64 Codec, and RegEx Tester.
- **Full Diagnostic Export**: **Supported**. Bundles logs, network entries, DOM metrics, and audit results into one JSON file.
