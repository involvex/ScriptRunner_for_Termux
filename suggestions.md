# Feature Suggestions — Script Runner for Termux

This document outlines features that could be implemented to extend the capabilities of
Script Runner for Termux. Each suggestion includes the rationale, estimated complexity,
and relevant existing code references.

---

## Summary of Existing Features

The app already provides:

- **Script editor** with multi-page support, syntax-aware editing, line wrapping toggle
- **Script execution** via Termux RUN_COMMAND intent (background/foreground, session reuse, heartbeat monitoring with TCP-based resurrection)
- **Quick Settings tiles** (up to 5) and **home screen shortcuts** (regular + monochrome styled)
- **Environment variables** and **runtime parameters** (args, prefix, presets)
- **Automations** with scheduling types (one-time, periodic, weekly, monthly, time-window, random-delay), event triggers (screen on/off, network, USB, boot), conditions (WiFi, charging, battery threshold), and **automation chains** with conditional execution (on success/failure/always) and env pass-through (`PREV_OUTPUT`, `PREV_EXIT_CODE`)
- **Webhook server** (NanoHTTPD) for remote script/automation triggering with bearer token auth and LAN access toggle
- **Script monitoring** with heartbeat broadcasts and TCP socket monitoring for auto-restart
- **Encrypted storage** via SQLCipher + Tink (KeyManager)
- **Import/export** of full script libraries as JSON DTOs, plus single-script import
- **Custom themes** with Material 3 dynamic color palette generation, light/dark modes
- **Script versions** (snapshot backup/restore with auto-backup-before-restore)
- **Built-in templates** (bash, python, node, powershell) with search
- **Execution history** with exit code, duration, stdout/stderr
- **Categories** for script organization, search (debounced), and multiple sort options
- **Glance widgets** (scripts, automations, automation logs) with on-click actions
- **Script execution timeout** (`executionTimeoutMs` field on `Script`/`ScriptEntity`, enforced via `timeout` shell command in `RunScriptUseCase`)
- **Notification actions** (`NotificationAction` list on `Script` allowing notification buttons that trigger other automations)
- **Automation trigger codes** (`automationCode` field on `Automation`/`AutomationEntity` for ADB and webhook triggering)
- **Single script import** from files (`ScriptRepository.importSingleScript`)
- **Webhook URL examples** with curl commands shown in `WebhookSettingsScreen`

---

## Bug Fixes

### B1: `executionTimeoutMs` Not Mapped in `ScriptEntity.toScriptDomain()`

**Category:** Bug Fix
**Impact:** High (silent data loss)
**Effort:** Trivial
**Confidence:** 100%

**Rationale:** The `executionTimeoutMs` field exists in `ScriptEntity`'s constructor, is included in `toScriptEntity()`, is included in `ScriptExportDto.toEntity()`, and is configured in the UI via `ScriptConfigState` and `ScriptConfigDialog`. However, `ScriptEntity.toScriptDomain()` (in `ScriptEntity.kt`, lines 59–88) **does not** pass `executionTimeoutMs` to the `Script` constructor. This means every time a script is loaded from the database, its execution timeout is silently lost (defaults to `null`).

**Current state:** `ScriptEntity.toScriptDomain()` builds `Script(...)` with all fields except `executionTimeoutMs`. The field is present in the entity but the mapping is incomplete.

**Suggested fix:**
1. Add `executionTimeoutMs = executionTimeoutMs` to the `Script(...)` constructor call inside `toScriptDomain()` in `ScriptEntity.kt`

**File to edit:** `app/src/main/java/io/github/swiftstagrime/termuxrunner/data/local/entity/ScriptEntity.kt`

### B2: `automationCode` UI Label Is Misleading

**Category:** Bug Fix (UX)
**Impact:** Low
**Effort:** Trivial
**Confidence:** 100%

**Rationale:** The `AutomationConfigDialog.kt` (line 216) labels the trigger code field with `R.string.adb_code` ("Trigger code (empty = cannot trigger externally)"). Since this code is used for **both** ADB triggering (`AdbScriptExecutionService`) and webhook triggering (`WebhookService`), the "adb_code" label is misleading.

**Suggested fix:**
1. Add a new string resource `R.string.label_automation_trigger_code` (e.g., "Trigger Code")
2. Replace `R.string.adb_code` with the new string in `AutomationConfigDialog.kt` line 216

**File to edit:** `app/src/main/java/io/github/swiftstagrime/termuxrunner/ui/features/automation/components/AutomationConfigDialog.kt`

---

## High Priority Suggestions

### H1: In-App Live Terminal / Output Viewer

**Category:** Feature (Execution)
**Impact:** High
**Effort:** Medium-High
**Confidence:** 95%

**Rationale:** Currently, scripts run in Termux. There is a `ScriptOutputViewerScreen` route for viewing stored output, but there is no way to see **live** stdout/stderr streaming from a running script within the app itself. Users who run scripts in the background lose visibility.

**Current state:** `ScriptOutputViewerRoute.kt` exists but only shows stored execution results (`ScriptExecutionRepository.getExecutionById`), not live streaming output.

**Suggested improvement:** Introduce a live terminal view that captures stdout/stderr from Termux sessions. This would require:
- A new repository method in `TermuxRepository` to capture live output streams
- A new Composable screen using `LazyColumn` to append output lines in real time
- A new route entry in `Route.kt` (e.g., `LiveTerminal(scriptId, executionId)`)

### H2: Script Execution Timeout (Hard Kill) ✅ IMPLEMENTED

**Category:** Feature (Automation)
**Impact:** High
**Effort:** Low
**Confidence:** 98%
**Status:** ✅ Already implemented — see **B1** for a critical bug in the DB mapping

**Rationale:** Scripts can run indefinitely. There is a heartbeat timeout for monitoring but no hard timeout that kills a script after a configurable duration. Long-running or hung scripts consume resources.

**Current state:** `executionTimeoutMs: Long? = null` has been added to `Script` and `ScriptEntity`, and is configured in `ScriptConfigState` and `ScriptConfigDialog`. `RunScriptUseCase` enforces the timeout via the `timeout` shell command. The `ScriptExportDto` includes `executionTimeoutMs`.

- ⚠️ **Bug:** See **B1** — `toScriptDomain()` in `ScriptEntity` does not include `executionTimeoutMs`, so the value is lost when loading from the database.

### H3: Webhook Request Body / POST Data Handling

**Category:** Feature (Webhook)
**Impact:** High
**Effort:** Medium
**Confidence:** 92%

**Rationale:** The webhook server (`WebhookService`) currently only supports triggering scripts/automations by ID or code. It does not accept a request body that could be passed to the script as stdin or a runtime argument. This limits integrations where a caller wants to send dynamic data.

**Current state:** `handleScriptTrigger` in `WebhookService.kt` (lines 225–271) ignores request body entirely.

**Suggested improvement:**
1. In `WebhookService.handleScriptTrigger`, read the POST body from the `IHTTPSession` using NanoHTTPD's body-reading API
2. Pass the body as a runtime argument or environment variable (e.g., `WEBHOOK_BODY`) to `RunScriptUseCase`
3. Add an option in `WebhookSettingsScreen` to enable/disable body passthrough
4. Document the `WEBHOOK_BODY` environment variable in the app's webhook documentation

### H4: Script Execution Confirmation Dialog

**Category:** Feature (UX)
**Impact:** Medium
**Effort:** Low
**Confidence:** 95%

**Rationale:** Accidental taps on Quick Settings tiles or widgets can trigger scripts unintentionally. An optional confirmation step before execution would prevent this.

**Current state:** `HomeViewModel.runScript()` (line 211) and tile services execute scripts immediately without prompting.

**Suggested improvement:**
1. Add `confirmBeforeRun: Boolean = false` to `Script` and `ScriptEntity`
2. Add a toggle in `ScriptConfigState` / `ScriptConfigDialog`
3. In `HomeViewModel.runScript()`, check this flag and emit a new UI event (e.g., `HomeUiEvent.ConfirmScriptExecution(script)`)
4. Add a simple confirmation Composable (Yes/No dialog with script name)
5. Also check the flag in `BaseScriptTileService.onClick()` and show a toast-based confirmation or launch a confirmation activity

### H5: Scheduled Auto-Backups

**Category:** Feature (Backup)
**Impact:** High
**Effort:** Medium
**Confidence:** 90%

**Rationale:** The app supports manual export (JSON) but does not auto-backup. Users may lose all scripts if the device is lost or the app data is corrupted. Scheduled backups to a user-chosen directory would prevent permanent data loss.

**Current state:** `SettingsViewModel.exportData()` is triggered manually only. `WorkManager` is used for `AutomationWorker` but not for backup scheduling. `ScriptRepository.exportScripts(uri)` already has the core export logic.

**Suggested improvement:**
1. Create a new `BackupWorker` (HiltWorker) that calls `ScriptRepository.exportScripts()`
2. Add user preferences for backup frequency (daily/weekly), backup location, encryption toggle
3. Schedule via `WorkManager` with `PeriodicWorkRequest` in `ScriptRunnerApp.onCreate()` or a dedicated initializer
4. Add UI in `SettingsScreen` for backup configuration

### H6: Script Output Logging to File

**Category:** Feature (Output)
**Impact:** Medium
**Effort:** Medium
**Confidence:** 88%

**Rationale:** Scripts that produce extensive output may benefit from having stdout/stderr saved to a file. Currently, output is stored in memory and shown in the `ScriptOutputViewerScreen` but not persisted to a file.

**Current state:** `ScriptExecution` has `stdout` and `stderr` fields stored in the Room DB. `ScriptOutputViewerViewModel` retrieves and displays them.

**Suggested improvement:**
1. Add `logToFile: Boolean = false` and `logFilePath: String? = null` to `Script` and `ScriptEntity`
2. In `RunScriptUseCase`, append output to a file when enabled
3. Add a config option in `ScriptConfigState`
4. Add a UI to view/download the log file from `ScriptOutputViewerScreen`

---

## Quick Wins (High-Priority)

The following are high-value, low-effort items that can be implemented quickly based on existing infrastructure.

### Q1: Fix `executionTimeoutMs` DB Mapping (B1)

**Category:** Bug Fix
**Impact:** High
**Effort:** Trivial — one line
**Confidence:** 100%

Add `executionTimeoutMs = executionTimeoutMs` to the `Script(...)` constructor call in `ScriptEntity.toScriptDomain()`. No new fields, no migrations, no UI changes.

### Q2: Single Script Share via `Intent.ACTION_SEND`

**Category:** Feature (Data Portability)
**Impact:** Medium
**Effort:** Low
**Confidence:** 95%

**Rationale:** The infrastructure already exists — `Script.toExportDto()` and `ScriptExportDto.toEntity()` handle serialization. Users just need a way to share one script.

**Current state:** `HomeScreen`'s 3-dot menu (`ScriptContextMenu`, lines 670–781) has options for config, shortcut, history, and delete — but no share/export option.

**Suggested improvement:**
1. Add a `shareScript(script: Script): Result<String>` method to `ScriptRepository` that serializes a single script to JSON and returns the JSON string
2. Add a "Share" menu item in `ScriptContextMenu` (and a corresponding callback in `HomeActions`)
3. In `HomeViewModel`, create an `Intent.ACTION_SEND` with the JSON and send it via a `UiEvent`

### Q3: Webhook — Accept All HTTP Methods

**Category:** Feature (Webhook)
**Impact:** Medium
**Effort:** Trivial
**Confidence:** 98%

**Rationale:** The webhook server currently rejects anything that isn't GET or POST with a 405. Allowing PUT, DELETE, PATCH is a one-line change.

**Current state:** `WebhookHttpServer.serve()` (line 109) checks `session.method != Method.POST && session.method != Method.GET` and returns 405.

**Suggested fix:** Remove or expand the method check to allow all standard HTTP methods.

### Q4: Webhook — Read POST Body as `WEBHOOK_BODY` Env Var

**Category:** Feature (Webhook)
**Impact:** Medium
**Effort:** Low
**Confidence:** 95%

**Rationale:** NanoHTTPD provides `session.inputStream` or `session.readBody()` to read the request body. Passing it as a `WEBHOOK_BODY` environment variable requires no model changes — just modify `handleScriptTrigger` to read and forward the body.

**Suggested improvement:**
1. In `WebhookService.handleScriptTrigger`, read the POST body from `session.inputStream`
2. Pass it to `runScriptUseCase` via the `runtimeEnv` parameter as `WEBHOOK_BODY`
3. Add a toggle in `WebhookSettingsScreen` to enable/disable this behavior

### Q5: Webhook — In-Memory Rate Limiting

**Category:** Feature (Webhook / Security)
**Impact:** Medium
**Effort:** Medium
**Confidence:** 90%

**Rationale:** A simple in-memory rate limiter using `ConcurrentHashMap<String, MutableList<Long>>` can protect against abuse without external dependencies.

**Suggested improvement:**
1. Add a `requestTimestamps` map in `WebhookService` keyed by IP address
2. On each request, prune timestamps older than the window (e.g., 60 seconds)
3. If the count exceeds the limit (e.g., 30 requests), return HTTP 429
4. Add a `MAX_REQUESTS_PER_MINUTE` constant
5. Expose rate limit config in `WebhookSettingsScreen`

### Q6: Webhook — IP Allowlist

**Category:** Feature (Webhook / Security)
**Impact:** Medium
**Effort:** Low
**Confidence:** 92%

**Rationale:** A comma-separated IP allowlist stored in `WebhookConfig` provides an additional security layer.

**Suggested improvement:**
1. Add `ipAllowlist: String` property to `WebhookConfig`
2. In `WebhookService.authenticateRequest()`, check if the request IP is in the allowlist when non-empty
3. Return HTTP 403 if the IP is not allowed
4. Add an input field in `WebhookSettingsScreen`

### Q7: Fix Automation Trigger Code Label (B2)

**Category:** Bug Fix (UX)
**Impact:** Low
**Effort:** Trivial
**Confidence:** 100%

Change `R.string.adb_code` to a new string resource `R.string.label_automation_trigger_code` in `AutomationConfigDialog.kt` line 216.

### Q8: Copy Script Code to Clipboard

**Category:** Feature (UX)
**Impact:** Low
**Effort:** Trivial
**Confidence:** 95%

**Rationale:** Users may want to copy script content without editing. A "Copy Code" item in the 3-dot menu is trivial to add.

**Suggested improvement:**
1. Add a "Copy Code" `DropdownMenuItem` in `ScriptContextMenu` (HomeScreen.kt)
2. Add a corresponding `onCopyCodeClick` callback in `HomeActions`
3. Implement in `HomeViewModel` using `ClipboardManager`

### Q9: Execution History — Delete for Specific Script

**Category:** Feature (UX)
**Impact:** Low
**Effort:** Trivial
**Confidence:** 95%

**Rationale:** `ExecutionHistoryViewModel.deleteForScript(scriptId: Int)` already exists (line 70) and `ScriptExecutionRepository.deleteForScript()` already exists. The UI just doesn't expose it.

**Suggested improvement:**
1. Add a "Clear History for This Script" option to the ExecutionHistoryScreen's 3-dot menu when a `scriptId` is provided
2. Call `viewModel.deleteForScript(scriptId)`

### Q10: Widget Transparency & Layout Customization

**Category:** Feature (Widgets)
**Impact:** Low
**Effort:** Low
**Confidence:** 90%

**Rationale:** Glance widgets support `GlanceModifier.alpha()` for transparency. A user preference for widget alpha is straightforward.

**Suggested improvement:**
1. Add `widgetTransparency` preference to `UserPreferencesRepository`
2. Pass alpha values to `ScriptWidget` and `AutomationWidget` Glance composables
3. Add a slider in `SettingsScreen` for widget transparency

### Q11: Quick Tile for Toggling All Automations

**Category:** Feature (Quick Settings)
**Impact:** Low
**Effort:** Low
**Confidence:** 90%

**Rationale:** A global automation toggle tile would be useful for temporary disables (meetings, presentations).

**Suggested improvement:**
1. Add `setAllAutomationsEnabled(enabled: Boolean)` to `AutomationRepository` / `AutomationDao`
2. Add `areAllAutomationsEnabled: Flow<Boolean>` to `AutomationRepository`
3. Create a new `MasterAutomationTileService : TileService`
4. Register in `AndroidManifest.xml`

---

## Medium Priority Suggestions

### M1: Automation Execution Limits

**Category:** Feature (Automation)
**Impact:** Medium
**Effort:** Medium
**Confidence:** 93%

**Rationale:** There is no way to limit the number of times an automation runs. For example, a periodic automation that checks a service should stop after N failures. Currently, failed automations continue to run indefinitely.

**Current state:** `Automation` model has `lastRunTimestamp`, `nextRunTimestamp`, `lastExitCode` but no failure counter or max-runs limit.

**Suggested improvement:**
1. Add `maxRuns: Int? = null` and `maxFailures: Int? = null` to `Automation` and `AutomationEntity`
2. Track `runCount` and `failureCount` in the automation state
3. In `AutomationWorker.doWork()`, check limits before executing and disable the automation if exceeded
4. Add UI fields in `AutomationConfigState` for these limits
5. Show counts in the automation list item

### M2: Webhook Rate Limiting & IP Allowlist

**Category:** Feature (Webhook) / Security
**Impact:** Medium
**Effort:** Medium
**Confidence:** 88%

**Note:** This is now partially covered by **Q5** (rate limiting) and **Q6** (IP allowlist) quick wins. If those are implemented, this suggestion is complete.

### M3: Script Tagging & Enhanced Filtering

**Category:** Feature (Organization)
**Impact:** Medium
**Effort:** Medium
**Confidence:** 90%

**Rationale:** Scripts are organized only by categories. Tags would allow multi-dimensional filtering (e.g., "backup + python + weekly"). Search currently filters by name and code only.

**Current state:** `Script` has `categoryId` but no `tags` field. `HomeViewModel` search filters on name and code content (lines 164–169).

**Suggested improvement:**
1. Add a `tags: List<String>` column to `ScriptEntity` (stored as a comma-separated or JSON list via TypeConverter)
2. Add a `tags` field to `Script` model
3. Add a tag input UI in `ScriptConfigState` (chip-based input)
4. Extend search to include tags in `HomeViewModel.homeUiState`
5. Add a tag filter dropdown in `HomeScreen`

### M4: Execution Statistics Dashboard

**Category:** Feature (Analytics)
**Impact:** Medium
**Effort:** Medium
**Confidence:** 85%

**Rationale:** The app records execution history (`ScriptExecution` with exit code, duration, stdout/stderr) but does not aggregate or visualize this data. Users would benefit from seeing success/failure rates, average execution time, and trends.

**Current state:** `ScriptExecutionDao` has `getFailureCount()` and `getTotalCount()` as Flow queries, but no per-script or per-day aggregation. `ExecutionHistoryViewModel` shows a list but no statistics. No `StatisticsScreen` or `Route.Statistics` exists.

**Suggested improvement:**
1. Add aggregation queries to `ScriptExecutionDao` (e.g., `getSuccessRatePerScript()`, `getExecutionsPerDay()`)
2. Create a new `StatisticsViewModel` and `StatisticsScreen`
3. Display charts using MPAndroidChart or a Compose chart library
4. Show per-script success rate, average duration, last 7/30 days trend
5. Add a `Route.Statistics` entry in `Route.kt`

### M5: Script Sharing (Single Script)

**Category:** Feature (Data Portability)
**Impact:** Medium
**Effort:** Low
**Confidence:** 95%

**Note:** This is now **covered by Q2** (Single Script Share via `Intent.ACTION_SEND`). If Q2 is implemented, this suggestion is complete.

### M6: More Webhook HTTP Methods & Response Handling

**Category:** Feature (Webhook)
**Impact:** Medium
**Effort:** Medium
**Confidence:** 87%

**Note:** Q3 (Accept All HTTP Methods) is now a trivial quick win. The remaining parts of this suggestion (response headers, custom response body, configurable status code) still require medium effort.

**Suggested improvement (remaining scope):**
1. ✅ Accept all HTTP methods — covered by Q3
2. Read and store request headers for use in script execution (e.g., `WEBHOOK_HEADER_<name>` env vars)
3. Allow the script to determine the response (e.g., capture a file the script writes as the response body)
4. Add a configurable default response (status code, body) in `WebhookSettings`

### M7: Batch Script Operations

**Category:** Feature (UX)
**Impact:** Medium
**Effort:** Medium
**Confidence:** 90%

**Rationale:** Scripts can only be managed one at a time. Deleting multiple scripts or moving them to a category requires individual actions. This is tedious for users with many scripts.

**Current state:** `HomeViewModel` has `deleteScript()` for single scripts (line 265). No multi-select or batch mode exists.

**Suggested improvement:**
1. Add a `selectionMode` state to `HomeViewModel` (Multi-selection with checkboxes)
2. Add batch actions: Delete, Move to Category, Export Selection
3. Add a new repository method: `exportScripts(scripts: List<Script>, uri: Uri)`
4. Update `HomeScreen` with a selection toolbar (floating context bar)

### M8: Automation Trigger Code — Already Implemented (Needs Cleanup)

**Category:** Feature (Webhook / Refactor)
**Impact:** Low
**Effort:** Low (mostly UI labeling)
**Confidence:** 95%
**Status:** ✅ Core field already exists — see **B2** for the UI labeling fix

**Rationale:** The `automationCode` field already exists on `Automation`, `AutomationEntity`, `AutomationConfigState`, `AutomationConfigDialog`, and `AutomationExportDto`. Both `AdbScriptExecutionService` and `WebhookService` already use `getAutomationByAdbCode(code)` to look up automations. The remaining issues are:

1. The UI label says `adb_code` instead of "trigger code" — see B2
2. The method name `getAutomationByAdbCode` is misleading since it's used for webhooks too
3. The webhook trigger URL is shown as an example curl command but could be displayed more prominently

**Suggested improvement:**
1. ✅ Fix the label — covered by B2
2. Rename `getAutomationByAdbCode` to `getAutomationByTriggerCode` (or add an alias) across `AutomationDao`, `AutomationRepository`, `ScriptRepository` (for `getScriptByAdbCode`), and all callers
3. Add a dedicated webhook trigger URL display section in `WebhookSettingsScreen` showing the full URL (e.g., `POST http://<ip>:<port>/trigger/automation/<code>`)
4. Add QR code generation for the webhook trigger URL

---

## Low Priority Suggestions

### L1: App Icon State Badge

**Category:** Feature (UI)
**Impact:** Low
**Effort:** Low
**Confidence:** 90%

**Rationale:** A dynamic app icon that shows a badge (e.g., number of running scripts or failures) would give users at-a-glance status. Android 12+ supports adaptive icon badges via `ShortcutInfo`.

**Current state:** No dynamic icon or badge support. The app uses a static launcher icon.

**Suggested improvement:**
1. Create a method in `ShortcutRepository` or `IconRepository` to update the app icon badge
2. Update `HeartbeatService` or `ProcessTermuxResultUseCase` to set badge count based on active/failed scripts
3. Use `ShortcutManager` APIs or a third-party library for badge support across OEMs

### L2: Script Diff View Between Versions

**Category:** Feature (UX)
**Impact:** Medium
**Effort:** Medium
**Confidence:** 85%

**Rationale:** `ScriptVersionsViewModel` allows restoring versions but there is no visual diff between versions. Users cannot easily see what changed.

**Current state:** `ScriptVersionsScreen` shows a list of versions with timestamps. Selecting a version and restoring it is the only action. No comparison view exists.

**Suggested improvement:**
1. Add a "Compare" action to each version in `ScriptVersionsScreen`
2. Use a diff library (e.g., `java-diff-utils`)
3. Display side-by-side or unified diff in a new Composable
4. Add a `Route.ScriptVersionDiff(versionId1, versionId2)` route

### L3: Import Scripts from GitHub/GitLab URL

**Category:** Feature (Data Portability)
**Impact:** Low
**Effort:** Medium
**Confidence:** 80%

**Rationale:** Users may want to import scripts from public repositories. Currently, only local file import (URI-based) and full JSON library import are supported.

**Current state:** `ScriptRepository.importSingleScript(uri)` reads from a `Uri`. `ScriptRepositoryImpl` handles JSON parsing of `ScriptExportDto` and has an `INTERPRETER_MAP` for shebang detection.

**Suggested improvement:**
1. Add a new repository method: `importFromUrl(url: String): Result<Script>`
2. Fetch the raw script content via HTTP
3. Create a `Script` from the fetched content (infer name from URL)
4. Add an "Import from URL" option in `SettingsScreen` or `HomeScreen`

### L4: Quick Tile for Toggling All Automations

**Category:** Feature (Quick Settings)
**Impact:** Low
**Effort:** Low
**Confidence:** 90%

**Note:** This is now **covered by Q11** in the Quick Wins section.

### L5: Widget Transparency & Layout Style Customization

**Category:** Feature (Widgets)
**Impact:** Low
**Effort:** Low
**Confidence:** 85%

**Note:** Transparency is now **partially covered by Q10**. Layout style customization (list vs grid) remains as a low-priority enhancement.

**Suggested improvement (remaining scope):**
1. Add user preference: widget layout style (list/grid/compact)
2. Add this as a Glance parameter in widget configuration
3. Update `WidgetConfigurationActivity` to include this option

### L6: Terminal Widget

**Category:** Feature (Widgets)
**Impact:** Low
**Effort:** High
**Confidence:** 70%

**Rationale:** A Glance widget that embeds a mini-terminal for quick script execution would be convenient, but Glance widgets have limited interactivity and cannot embed an actual terminal emulator.

**Current state:** No terminal widget exists. Script widgets only show script names/icons and trigger execution on click.

**Suggested improvement:**
1. Create a new Glance widget that shows the last execution result (exit code + timestamp)
2. Add a "Run" action button
3. Consider using `GlanceActionParameters` to pass the result back for display
4. Note: A true interactive terminal inside a widget is not feasible with Glance; this would be limited to status display

### L7: Tasker Plugin Integration

**Category:** Feature (Integration)
**Impact:** Low
**Effort:** Medium
**Confidence:** 80%

**Rationale:** Tasker is a popular automation app on Android. Many users would benefit from being able to trigger Script Runner scripts/ automations from Tasker.

**Current state:** The app integrates with Termux via intent and with ADB via intent (`AdbScriptExecutionService`), but no Tasker plugin API exists.

**Suggested improvement:**
1. Implement a `Broadcast Receiver` that accepts Tasker plugin intents
2. Or implement the Tasker `Plugin` interface (two-way: trigger + result)
3. Allow selecting a script/automation from within Tasker's plugin configuration
4. Pass runtime args/env from Tasker variables into the script execution

### L8: Enhanced Editor — Syntax Highlighting & Auto-Formatting

**Category:** Feature (Editor)
**Impact:** Low
**Effort:** Medium
**Confidence:** 85%

**Rationale:** The editor uses a plain `BasicTextField`. Syntax highlighting and auto-formatting would improve the developer experience.

**Current state:** `CodeEditor.kt` uses `BasicTextField` with no syntax highlighting. `SearchVisualTransformation` exists for search highlighting only.

**Suggested improvement:**
1. Use a library like `Jetpack Compose Text` with `Brush` for syntax coloring, or integrate a lightweight syntax highlighter
2. Add auto-formatting on save (e.g., `shfmt` via Termux, or a Kotlin-based formatter)
3. Add bracket matching and auto-closing
4. Add line numbers with click-to-jump

### L9: Cross-Device Sync via Local Network

**Category:** Feature (Sync)
**Impact:** Low
**Effort:** High
**Confidence:** 75%

**Rationale:** Users with multiple devices may want to sync their scripts and automations. Cloud sync (Google Drive) requires account permissions, but local network sync would be privacy-friendly.

**Current state:** No sync mechanism exists. Import/export is manual (JSON file). `WebhookService` provides local HTTP server but only for triggering, not data sync.

**Suggested improvement:**
1. Extend the webhook server (or create a new sync server) with endpoints:
   - `GET /sync/scripts` — returns all scripts as JSON
   - `POST /sync/scripts` — accepts script JSON and imports
   - `GET /sync/automations` — returns all automations
2. Add a "Sync" screen showing connected devices
3. Use mDNS discovery to find other devices on the network
4. Support conflict resolution (latest wins or user picks)

### L10: Script Execution Queue & Concurrency Limits

**Category:** Feature (Execution)
**Impact:** Low
**Effort:** Medium
**Confidence:** 88%

**Rationale:** There is no limit on how many scripts can execute simultaneously. Running many scripts at once can overwhelm the device.

**Current state:** `RunScriptUseCase` executes immediately via `TermuxRepository.runCommand()`. No queuing mechanism exists.

**Suggested improvement:**
1. Add a configurable max concurrent executions per script (e.g., `maxConcurrent: Int = 1`)
2. Queue excess requests using a `Mutex` or `Semaphore` in `RunScriptUseCase`
3. Show queued status in notifications
4. Add a global max concurrent limit in `Settings`

### L11: More Automation Event Triggers

**Category:** Feature (Automation)
**Impact:** Low
**Effort:** Medium
**Confidence:** 82%

**Rationale:** The existing event triggers cover screen, network, and USB states. Additional triggers like battery level thresholds, Bluetooth connect/disconnect, and timezone changes would expand automation possibilities.

**Current state:** `AutomationType` includes `SCREEN_ON`, `SCREEN_OFF`, `NETWORK_CONNECTED`, `NETWORK_DISCONNECTED`, `USB_CONNECTED`, `USB_DISCONNECTED`, and `BOOT`. Additional triggers are not supported.

**Suggested improvement:**
1. Add new `AutomationType` values:
   - `BATTERY_LOW`, `BATTERY_CHARGING`, `BATTERY_FULL`
   - `BLUETOOTH_CONNECTED`, `BLUETOOTH_DISCONNECTED`
   - `TIMEZONE_CHANGED`
   - `LOCALE_CHANGED`
2. Register corresponding `BroadcastReceiver` actions in `EventReceiver`
3. Extend `AutomationTimeCalculator` to handle event-based types (return `null` for next run, like existing events)
4. Update `AutomationConfigDialog` to show the new types in the event source dropdown

---

## Feature Suggestions Table

| ID | Title | Category | Impact | Effort | Confidence | Status |
| --- | --- | --- | --- | --- | --- | --- |
| B1 | Fix `executionTimeoutMs` DB mapping | Bug Fix | High | Trivial | 100% | 🔴 Not started |
| B2 | Fix automation trigger code label | Bug Fix (UX) | Low | Trivial | 100% | 🔴 Not started |
| H1 | In-App Live Terminal/Output Viewer | Execution | High | Medium-High | 95% | 🔴 Not started |
| H2 | Script Execution Timeout (Hard Kill) | Automation | High | Low | 98% | ✅ Implemented (bug: B1) |
| H3 | Webhook Request Body / POST Data Handling | Webhook | High | Medium | 92% | 🔴 Not started |
| H4 | Script Execution Confirmation Dialog | UX | Medium | Low | 95% | 🔴 Not started |
| H5 | Scheduled Auto-Backups | Backup | High | Medium | 90% | 🔴 Not started |
| H6 | Script Output Logging to File | Output | Medium | Medium | 88% | 🔴 Not started |
| Q1 | Fix `executionTimeoutMs` DB mapping | Bug Fix | High | Trivial | 100% | 🔴 Not started |
| Q2 | Single Script Share via ACTION_SEND | Data Portability | Medium | Low | 95% | 🔴 Not started |
| Q3 | Webhook — Accept All HTTP Methods | Webhook | Medium | Trivial | 98% | 🔴 Not started |
| Q4 | Webhook — Read POST Body as env var | Webhook | Medium | Low | 95% | 🔴 Not started |
| Q5 | Webhook — In-Memory Rate Limiting | Webhook/Security | Medium | Medium | 90% | 🔴 Not started |
| Q6 | Webhook — IP Allowlist | Webhook/Security | Medium | Low | 92% | 🔴 Not started |
| Q7 | Fix Automation Trigger Code Label | Bug Fix (UX) | Low | Trivial | 100% | 🔴 Not started |
| Q8 | Copy Script Code to Clipboard | UX | Low | Trivial | 95% | 🔴 Not started |
| Q9 | Delete Execution History per-Script | UX | Low | Trivial | 95% | 🔴 Not started |
| Q10 | Widget Transparency & Layout Customization | Widgets | Low | Low | 90% | 🔴 Not started |
| Q11 | Quick Tile for Toggling All Automations | Quick Settings | Low | Low | 90% | 🔴 Not started |
| M1 | Automation Execution Limits | Automation | Medium | Medium | 93% | 🔴 Not started |
| M2 | Webhook Rate Limiting & IP Allowlist | Webhook/Security | Medium | Medium | 88% | 🟡 Partially via Q5+Q6 |
| M3 | Script Tagging & Enhanced Filtering | Organization | Medium | Medium | 90% | 🔴 Not started |
| M4 | Execution Statistics Dashboard | Analytics | Medium | Medium | 85% | 🔴 Not started |
| M5 | Script Sharing (Single Script) | Data Portability | Medium | Low | 95% | 🟡 Covered by Q2 |
| M6 | More Webhook HTTP Methods & Responses | Webhook | Medium | Medium | 87% | 🟡 Partially via Q3 |
| M7 | Batch Script Operations | UX | Medium | Medium | 90% | 🔴 Not started |
| M8 | Automation Trigger Code | Webhook/Refactor | Low | Low | 95% | ✅ Field exists (clean up: B2) |
| L1 | App Icon State Badge | UI | Low | Low | 90% | 🔴 Not started |
| L2 | Script Diff View Between Versions | UX | Medium | Medium | 85% | 🔴 Not started |
| L3 | Import Scripts from GitHub/GitLab URL | Data Portability | Low | Medium | 80% | 🔴 Not started |
| L4 | Quick Tile for Toggling All Automations | Quick Settings | Low | Low | 90% | 🟡 Covered by Q11 |
| L5 | Widget Transparency & Layout Customization | Widgets | Low | Low | 85% | 🟡 Partially via Q10 |
| L6 | Terminal Widget | Widgets | Low | High | 70% | 🔴 Not started |
| L7 | Tasker Plugin Integration | Integration | Low | Medium | 80% | 🔴 Not started |
| L8 | Enhanced Editor (Syntax Highlighting, Auto-Format) | Editor | Low | Medium | 85% | 🔴 Not started |
| L9 | Cross-Device Sync via Local Network | Sync | Low | High | 75% | 🔴 Not started |
| L10 | Script Execution Queue & Concurrency Limits | Execution | Low | Medium | 88% | 🔴 Not started |
| L11 | More Automation Event Triggers | Automation | Low | Medium | 82% | 🔴 Not started |

---

## Implementation Notes

- All new `Script` model fields should follow the existing pattern: add to `Script` data class, add to `ScriptEntity`, update the `toScriptDomain()` / `toScriptEntity()` mapping functions, and add to `ScriptConfigState` for UI configuration.
- New routes should be added to `Route.kt` as `@Serializable` data objects, and registered in `ScriptRunnerEntryProvider.kt`.
- New `AutomationType` values require updates in `AutomationType.kt`, `EventReceiver.kt`, and `AutomationConfigDialog.kt`.
- New background work should use `WorkManager` with `HiltWorker` (like `AutomationWorker`).
- All new ViewModels should use `@HiltViewModel` and inject `@IoDispatcher` / `@DefaultDispatcher` instead of `Dispatchers.IO` / `Dispatchers.Main` directly.
- UI state should be exposed via `StateFlow` using `stateIn` with `SharingStarted.WhileSubscribed(5000)`.
- New strings should follow the kebab/breadcrumb naming convention used in `strings.xml`.
- When adding new `ScriptEntity` columns, always update **both** `toScriptDomain()` and `toScriptEntity()` mapping functions — the B1 bug demonstrates what happens when only one is updated.