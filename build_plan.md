PROJECT NAME: F12 Browser — Mobile Developer Tools

PROJECT GOAL
Build a fully functional, Android-focused web browser with an integrated developer toolkit inspired by Chrome DevTools (F12). The entire application must be optimized for mobile phones, with a minimalist black-and-white theme.

The user should be able to browse any website and open the complete developer toolkit by tapping one F12 button positioned immediately beside the URL field.

This must be a real, working Android application, not a UI prototype or a collection of mock screens.

==================================================
1. PLATFORM AND TECHNOLOGY
==================================================

Platform: Native Android APK
Language: Java
UI: Android XML with a modern Material Design foundation
Browser engine: Android WebView
Build system: Gradle
IDE compatibility: Android Studio
Minimum Android version: Android 8.0, subject to dependency compatibility
Orientation: Portrait-first, with landscape support
Architecture: Modular, maintainable, testable
Network: Internet access with secure HTTPS support

Use Android WebView for browsing and implement an in-app developer toolkit using JavaScript instrumentation, Android APIs, and a modular debugging interface.

Enable WebView debugging in debug builds. Do not confuse desktop Chrome remote debugging with an in-app DevTools implementation.

The application must compile into an installable APK.

==================================================
2. CORE BROWSER INTERFACE
==================================================

Create a clean browser screen with the following top toolbar:

[ Home ] [ URL / Search Field ] [ F12 ] [ ⋮ ]

The F12 button must be immediately adjacent to the right side of the URL field.

The F12 button is the main feature of the application.

Browser functionality:
- Enter a URL or search query.
- Load HTTP and HTTPS websites.
- Navigate backward and forward.
- Reload and stop loading.
- Home page.
- Multiple tabs.
- Tab switching and closing.
- Bookmark management.
- Browsing history.
- Download handling.
- Find text on a page.
- Share the current URL.
- Desktop-site request where supported.
- Full-screen browsing where appropriate.
- Handle SSL errors securely.
- Display loading progress and connection errors.

Use a safe URL parser. Search queries should use a configurable search engine.

Never bypass certificate validation or automatically ignore SSL errors.

==================================================
3. F12 DEVELOPER TOOLKIT
==================================================

When the user taps F12, open a full-screen developer workspace inside the app.

The browser page should remain available, with a persistent developer-tools toolbar and a close button.

The developer workspace must use a compact mobile layout with horizontally scrollable tabs, searchable tool lists, readable code, and touch-friendly controls.

Main navigation:

Elements | Console | Network | Sources | Performance
Application | Security | Lighthouse | More

Include every tool listed below where technically possible.

Do not show fake results or controls that do nothing.

==================================================
4. ELEMENTS INSPECTOR
==================================================

Implement a working DOM inspector.

Requirements:
- Display the accessible DOM tree of the current page.
- Expand and collapse elements.
- Search for elements by tag, ID, or class.
- Select an element by tapping it.
- Highlight selected elements on the webpage.
- Display the selected element's HTML.
- Edit supported HTML attributes.
- Edit text content.
- Add, remove, and modify DOM elements.
- Inspect computed styles where accessible.
- Inspect CSS properties.
- Edit supported CSS properties live.
- Add CSS declarations.
- Toggle CSS declarations.
- Display element dimensions and box-model information.
- Inspect supported event listeners.
- Copy selected HTML or CSS.
- Provide a reset or reload option.

All edits should update the live page when technically supported.

Clearly distinguish temporary DOM changes from permanent website changes. Never imply that changes have been saved to the original website unless the website itself performs that operation.

==================================================
5. JAVASCRIPT CONSOLE
==================================================

Create a functional JavaScript console.

Requirements:
- Execute JavaScript in the current page's context.
- Display evaluation results.
- Capture console.log, info, warn, error, and debug messages.
- Capture uncaught JavaScript errors where possible.
- Capture unhandled promise rejections where possible.
- Display source information when available.
- Support multiline input.
- Maintain command history.
- Support clearing the console.
- Support filtering by log level.
- Copy output.
- Display objects, arrays, primitives, and errors in readable formats.
- Provide autocomplete for basic JavaScript syntax and inspected properties where feasible.
- Allow commands to inspect the current document and selected element.

Important security requirements:
- Do not execute page commands in the Android application context.
- Do not expose Android Java objects to arbitrary webpages through unrestricted JavaScript bridges.
- Keep developer commands scoped to the active page.
- Warn users before executing potentially destructive commands.
- Make it clear that JavaScript execution can modify the current page.

Do not implement the console as a text box that merely prints simulated output.

==================================================
6. NETWORK INSPECTOR
==================================================

Implement network monitoring for requests that can be observed through supported Android WebView APIs and controlled instrumentation.

Requirements:
- List observed requests.
- Display URL, method, status, resource type, and timing where available.
- Show request and response headers where available.
- Show response previews for supported content types.
- Filter by Fetch/XHR, document, script, stylesheet, image, font, and media where data is available.
- Search requests by URL.
- Display loading and failure states.
- Record request timing.
- Provide clear and preserve-log controls.
- Display available request details.
- Export a HAR-like diagnostic report.

Explicit limitations:
Android WebView does not expose every browser-network detail through public APIs. Some requests, response bodies, service-worker traffic, WebSocket frames, and browser-internal requests may not be fully observable.

Use the strongest reliable implementation possible without claiming unsupported access.

Never intercept or export sensitive request data silently. Provide privacy warnings and controls before exporting headers, cookies, or other potentially sensitive information.

==================================================
7. SOURCES AND CODE VIEWER
==================================================

Implement:
- View the current document's available HTML.
- View accessible CSS and JavaScript resources.
- Search within available source text.
- Syntax highlighting.
- Line numbers.
- Copy source.
- Inspect available script errors and source locations.
- View resource URLs.
- Display source maps when they are accessible and can be resolved safely.
- Provide a read-only mode by default.
- Clearly label source code that is fetched separately from the live runtime.

Do not claim to reproduce Chrome's complete debugger or breakpoint engine unless it is genuinely implemented.

Where WebView limitations prevent a feature, explain the limitation in the UI.

==================================================
8. PERFORMANCE PANEL
==================================================

Implement a practical performance diagnostic panel.

Show available metrics such as:
- Page load milestones.
- Navigation timing.
- DOM content loaded.
- Resource timing.
- First paint and contentful paint when supported.
- Largest contentful paint when supported.
- Layout shift observations when supported.
- Long tasks when supported.
- JavaScript execution measurements for supported tests.
- Approximate DOM element count.
- Resource counts and transfer-size estimates when available.
- Page responsiveness indicators.

Include a Start Recording and Stop Recording workflow.

Provide a timeline of supported observations and a diagnostic summary.

Do not invent unavailable metrics. Distinguish browser-reported values from estimates.

==================================================
9. RESPONSIVE DEVICE TESTING
==================================================

Build a mobile-friendly responsive testing panel.

Requirements:
- Select common viewport presets.
- Enter custom viewport width and height.
- Change device scale assumptions where supported.
- Toggle portrait and landscape previews.
- Inspect viewport dimensions.
- Test CSS media-query breakpoints.
- Reload the page at the selected viewport.
- Display the current viewport dimensions.
- Support zoom controls.
- Allow the user to compare supported viewport configurations.

Important:
WebView cannot reproduce every hardware-specific feature of Chrome's desktop device emulator. Do not claim full hardware emulation of an iPhone, Android device, or desktop GPU.

Use an accurate CSS viewport simulation and label any simulated device settings.

==================================================
10. APPLICATION AND STORAGE PANEL
==================================================

Implement inspection of accessible website storage.

Include:
- Local Storage.
- Session Storage.
- IndexedDB metadata and supported records.
- Cookies where Android WebView APIs permit access.
- Cache Storage where accessible.
- Service-worker registrations and status where accessible.
- Supported Web SQL information for legacy pages.
- Origin and storage usage information where available.

Allow appropriate operations:
- View keys and values.
- Search stored entries.
- Add or edit supported entries.
- Delete selected entries.
- Clear supported storage for the current origin.
- Refresh storage information.
- Export selected non-sensitive diagnostic data.

Before destructive operations, display a confirmation.

Respect origin boundaries, WebView profiles, website permissions, and Android sandbox restrictions.

Never display passwords or authentication tokens unnecessarily. Mask sensitive values by default.

Do not claim access to Android system storage or other applications' private data.

==================================================
11. SECURITY PANEL
==================================================

Display:
- Current page URL.
- HTTPS status.
- Certificate information available through supported APIs.
- Mixed-content warnings where detectable.
- Security-related console errors.
- Relevant cookie security attributes where accessible.
- Permission and insecure-resource warnings.
- Available Content Security Policy information.
- Potentially unsafe page configuration warnings.

Provide clear explanations for detected issues.

Do not claim that a page is completely secure based on a limited inspection.

Do not bypass browser security protections.

==================================================
12. LIGHTHOUSE-STYLE AUDITS
==================================================

Create an in-app website audit tool.

Audit categories:
- Performance.
- Accessibility.
- Best practices.
- Basic SEO.
- Mobile usability.

Implement real checks, including:
- Missing image alternative text.
- Missing document title.
- Missing or invalid viewport metadata.
- Missing form labels.
- Low-contrast text where measurable.
- Missing document language.
- Broken or missing resource checks where possible.
- Image dimensions and loading hints.
- Basic heading structure.
- Link accessibility.
- Common performance problems.
- HTTPS and mixed-content checks.

Provide:
- Individual findings.
- Severity.
- Evidence.
- Suggested fixes.
- A summary score only when a documented scoring method is implemented.
- Exportable audit reports.

Do not falsely label this as Google's official Lighthouse implementation. Build an independent Lighthouse-style audit engine.

==================================================
13. ADDITIONAL DEVELOPER TOOLS
==================================================

Include a More Tools menu containing:

- Rendering diagnostics.
- WebSocket monitoring where technically supported.
- Sensor simulation where supported.
- JavaScript execution timing.
- Cookie viewer.
- Storage inspector.
- Page information.
- Resource viewer.
- Request and response diagnostics.
- Cache diagnostics.
- Permissions inspector.
- Accessibility tree inspection where feasible.
- Colour picker.
- CSS box-model viewer.
- JSON formatter.
- URL encoder and decoder.
- Base64 encoder and decoder.
- Regular-expression tester.
- User-agent viewer.
- Viewport inspector.
- Export diagnostics.
- Developer settings.

For each feature, implement its actual functionality or explicitly label it as unsupported.

Do not create decorative buttons for unavailable tools.

==================================================
14. USER INTERFACE DESIGN
==================================================

Theme: Minimalist black and white only.

Visual direction:
- Pure black or near-black backgrounds.
- White primary text.
- Neutral gray secondary text.
- Thin monochrome borders.
- Minimal shadows.
- Clean typography.
- Compact but readable code editor.
- Consistent monochrome icons.
- Subtle dividers.
- No gradients.
- No colourful dashboards.
- No unnecessary animations.
- No oversized cards.
- No orange, green, blue, or purple accent colours.
- Clear contrast for selected and disabled states.

Use a monochrome typography and icon system.

Browser toolbar:
[Home] [URL field] [F12] [Menu]

The F12 button must be visually prominent while matching the monochrome theme.

Developer tools layout:
- Top row: page information and close button.
- Second row: horizontally scrollable developer-tool tabs.
- Main area: selected tool.
- Bottom area: optional console drawer or action toolbar.

Use a mobile-first layout. Avoid desktop-style sidebars that make the content unreadable on narrow screens.

Support Android back navigation intelligently:
1. Close an open dialog.
2. Close the F12 workspace.
3. Navigate backward in browser history.
4. Ask before exiting when appropriate.

Preserve accessibility, touch targets, text scaling, and keyboard input support.

==================================================
15. SETTINGS
==================================================

Include:
- Light/dark preference, defaulting to monochrome dark.
- Default search engine.
- Homepage.
- JavaScript execution warnings.
- Network capture preferences.
- Sensitive-data masking.
- Storage inspection confirmations.
- Diagnostic export controls.
- Clear browsing data.
- Clear developer logs.
- Reset developer-tool settings.
- About and version information.

The requested default visual style must remain black and white.

==================================================
16. ARCHITECTURE
==================================================

Separate the application into maintainable modules:

Browser:
- BrowserActivity
- BrowserViewModel
- BrowserTabManager
- BrowserWebViewClient
- BrowserChromeClient
- NavigationController

Developer Tools:
- DevToolsController
- ElementsInspector
- JavaScriptConsole
- NetworkInspector
- SourcesViewer
- PerformanceMonitor
- ResponsiveTester
- ApplicationInspector
- SecurityInspector
- WebsiteAuditEngine
- DeveloperToolsSettings

UI:
- Browser toolbar.
- F12 workspace.
- Elements panel.
- Console panel.
- Network panel.
- Sources panel.
- Performance panel.
- Application panel.
- Security panel.
- Audit panel.
- Settings screens.

Testing:
- Unit tests.
- Instrumentation tests.
- WebView integration tests.
- UI navigation tests.
- Security tests.
- APK build verification.

Use a secure message bridge with origin checks, validated message schemas, and strict command allowlists. Avoid unrestricted JavaScript-to-Java bridges on arbitrary webpages.

==================================================
17. TESTING REQUIREMENTS
==================================================

Test the application using:
- Simple static HTML pages.
- JavaScript-heavy websites.
- Responsive websites.
- Pages with CSS errors.
- Pages with JavaScript errors.
- Pages making Fetch/XHR requests.
- Pages with Local Storage and Session Storage.
- Pages using IndexedDB.
- Pages with cookies.
- Pages using service workers.
- Pages with WebSockets.
- Slow and failed network connections.
- HTTPS websites.
- Websites with restrictive Content Security Policies.
- Websites that use cross-origin resources.

Verify that the browser still works when the developer workspace is closed.

Verify that every implemented tool produces real results.

Test the app on different Android screen sizes.

Check for memory leaks, WebView lifecycle errors, crashes, excessive battery usage, and unsafe JavaScript interfaces.

==================================================
18. IMPLEMENTATION RULES
==================================================

1. Build the real application, not just a visual demonstration.
2. Do not substitute mock data for real inspection results.
3. Do not leave major buttons unimplemented.
4. Do not claim unsupported desktop DevTools capabilities.
5. Maintain a feature matrix documenting implemented, partially supported, and unsupported functionality.
6. Explain platform limitations in the relevant panel.
7. Keep the browser and developer tools integrated into one APK.
8. Preserve the selected minimalist black-and-white design.
9. Use modular code and avoid a single oversized Activity.
10. Include all required source files, resources, Gradle configuration, and build instructions.
11. Run tests and fix build errors before declaring completion.
12. Produce a debug APK for testing and document how to build a release APK.
13. Do not embed API keys, passwords, or private credentials.
14. Do not silently upload browsing history, page contents, cookies, or diagnostic data.
15. Keep the application usable without requiring a paid service or cloud account.

==================================================
19. ACCEPTANCE CRITERIA
==================================================

The project is complete only when:

- The Android APK installs and launches.
- The browser can load websites.
- The URL field works.
- The F12 button is positioned immediately beside the URL field.
- Tapping F12 opens the developer workspace.
- The user can switch between developer tools.
- The Elements inspector can inspect the live DOM.
- The Console executes supported JavaScript and displays actual output.
- The Network panel shows genuine observable requests.
- The Sources panel displays available source code.
- The Performance panel reports genuine supported measurements.
- The responsive tester changes the page's CSS viewport.
- The Application panel inspects supported website storage.
- The Security panel reports supported security findings.
- The audit engine performs actual checks.
- Storage modifications and destructive operations require suitable confirmation.
- The interface follows the monochrome design.
- Unsupported functionality is documented rather than simulated.
- The application passes its build and core integration tests.

FINAL DELIVERABLES

1. Complete Android Studio project.
2. Java source code.
3. XML layouts and resources.
4. Gradle build configuration.
5. Automated tests.
6. Feature support and limitations document.
7. README with setup and build instructions.
8. Installable debug APK.
9. Release-build instructions.

Prioritize a secure, functional core implementation first, then expand the remaining tools without sacrificing reliability.
