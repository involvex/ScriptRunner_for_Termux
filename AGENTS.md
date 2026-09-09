# AGENTS.md — Script Runner for Termux

Agent instructions for contributing to **Script Runner for Termux**, an Android app that provides a secure bridge between third-party apps and Termux for script management and execution.

---

## Project Overview

Script Runner for Termux is an Android application built with Kotlin, Jetpack Compose, and Jetpack libraries. It allows users to write, manage, and execute scripts via Termux, with features including:

- Integrated script editor with multi-page support
- Background or interactive execution via Termux sessions
- Quick Settings tiles (up to 5) for fast script access
- Home screen shortcut creation
- Environment variable and runtime parameter management
- Automation chains and scheduling
- Webhook server for remote triggering
- Script monitoring with auto-restart ("resurrection")
- Encrypted storage using SQLCipher and Tink
- Import/export of script libraries (JSON DTOs)

**Key details:**
- **Package name:** `io.github.swiftstagrime.termuiRunner`
- **Min SDK:** 24 (Android 7.0) | **Target SDK:** 37 (Android 16) | **Compile SDK:** 37
- **Version:** 1.8.0 (versionCode 180)
- **Gradle:** Latest AGP (9.3.0), Kotlin 2.4.10
- **License:** MIT

---

## Technologies & Dependencies

### Core
| Category | Technology |
|----------|-----------|
| Language | Kotlin 2.4.10 (JVM target 21) |
| Build System | Gradle (Kotlin DSL) with version catalog (`gradle/libs.versions.toml`) |
| Architecture | Clean Architecture: `domain` → `data` → `ui` (unidirectional data flow) |
| DI | Hilt (Hilt Android + Hilt Navigation Compose + Hilt Work) |
| UI | Jetpack Compose (Material 3, Adaptive), Navigation 3 runtime & UI |
| Threading | Kotlin Coroutines + Flow, `DispatcherModule` (IO/Main/Main.immediate/Unconfined) |

### Data Layer
| Concern | Library |
|---------|---------|
| Database | Room (runtime 2.8.4, compiler via KSP) with SQLCipher encryption |
| Serialization | kotlinx.serialization (core + json) |
| Cryptography | Tink (Android) |
| Preferences | DataStore Preferences |
| File I/O | Coil (image loading + network), AndroidX ExifInterface |
| App Widgets | Glance (Material 3 + AppWidget) |
| Background Work | WorkManager (with Hilt WorkerFactory) |
| Webhook Server | NanoHTTPD |
| Notifications | AndroidX Core + Custom notification helpers |

### Tooling & Code Quality
| Tool | Configuration |
|------|--------------|
| **ktlint** | `android = true`, `ignoreFailures = true`, HTML reporter, excludes `**/generated/**` |
| **detekt** | `buildUponDefaultConfig = true`, `allRules = false`, HTML + TXT reports |
| **Android Lint** | Baseline in `lint-baseline.xml`, `abortOnError = false` |

### Testing
| Framework | Scope |
|----------|-------|
| JUnit 4 | Unit & instrumented tests |
| MockK | Mocking (unit + Android variant) |
| Robolectric | JVM-based Android tests (`@Config(sdk = [33])`) |
| Hilt Testing | Instrumentation tests with `HiltTestRunner` |
| Espresso | UI testing framework |
| AndroidX Benchmark | Baseline profile generation |

### Project Structure
```
.
├── app/                    # Main application module
│   ├── src/main/
│   │   ├── java/io/github/swiftstagrime/termuiRunner/
│   │   │   ├── data/       # DAOs, entities, DTOs, repos, services, receivers, workers
│   │   │   ├── di/         # Hilt modules (AppModule, DatabaseModule, DispatcherModule)
│   │   │   ├── domain/     # Models, repository interfaces, use cases, utils
│   │   │   └── ui/         # Compose UI, ViewModels, screens, navigation, theme
│   │   └── res/            # Resources (layouts, drawables, strings, values)
│   ├── src/test/           # Unit tests (Robolectric, MockK)
│   ├── src/androidTest/    # Instrumented tests (Hilt, Espresso)
│   └── schemas/            # Room schema exports (version-controlled)
├── baselineprofile/        # Baseline profile module
├── fastlane/               # App store metadata (Fastlane)
├── gradle/libs.versions.toml   # Version catalog (single source of truth for deps)
├── build.gradle.kts        # Root build script
└── settings.gradle.kts     # Module definitions (:app, :baselineprofile)
```

---

## Useful Commands

### Building
```bash
# Debug build (install on connected device)
./gradlew installDebug

# Release build (unsigned APK — set isMinifyEnabled/shrink in build.gradle.kts)
./gradlew assembleRelease

# Assemble all variants
./gradlew assemble

# Clean build
./gradlew clean assembleDebug
```

### Testing
```bash
# Run unit tests (fast, JVM-based with Robolectric)
./gradlew testDebugUnitTest

# Run instrumented tests (requires device/emulator)
./gradlew connectedDebugAndroidTest

# Run a specific test class
./gradlew testDebugUnitTest --tests "*.ScriptRepositoryTest"

# Run tests with coverage
./gradlew testDebugUnitTest --scan
```

### Code Quality
```bash
# Run ktlint (formatter + linter)
./gradlew ktlintCheck

# Run detekt (static analysis)
./gradlew detekt

# Run Android Lint
./gradlew lint

# Auto-format with ktlint
./gradlew ktlintFormat
```

### Baseline Profiles
```bash
# Generate baseline profiles (requires connected device)
./gradlew :baselineprofile:generateBaselineProfile

# Install benchmark build for profile generation
./gradlew :app:assembleBenchmark
```

### Gradle Helpers
```bash
# List all available tasks
./gradlew tasks --all

# Run with dry-run to see task order
./gradlew assembleDebug --dry-run

# Pass JVM args (useful for memory issues)
./gradle -Dorg.gradle.jvmargs="-Xmx4096m" assembleDebug
```

### Git & Release
```bash
# Tag a release (triggers CI build in .github/workflows/release.yml)
git tag v1.8.1
git push origin v1.8.1

# View recent commit history
git --no-pager log --oneline -15
```

---

## Architecture

### Clean Architecture Layers

The project follows a **domain-driven, layered architecture** with strict separation of concerns:

#### 1. Domain Layer (`domain/`)
- **Models:** Pure Kotlin data classes (e.g., `Script`, `Automation`, `Category`, `CustomTheme`)
- **Repository Interfaces:** Abstract contracts (e.g., `ScriptRepository`, `TermuxRepository`, `AutomationRepository`)
- **Use Cases:** Single-responsibility business logic classes (e.g., `RunScriptUseCase`, `DeleteScriptUseCase`, `ExecuteChainStepUseCase`)
- **Utilities:** Domain-level helpers (`AutomationTimeCalculator`, `BatteryUtils`, `MiuiUtils`, `LocaleManager`)

#### 2. Data Layer (`data/`)
- **Local Data Sources:** Room DAOs, Entities, DTOs, Converters (`AppDatabase.kt`)
- **Repository Implementations:** Concrete implementations of domain repository interfaces (e.g., `ScriptRepositoryImpl`, `TermuxRepositoryImpl`)
- **Services:** Android services (`AdbScriptExecutionService`, `HeartbeatService`, `WebhookService`, `BaseScriptTileService`)
- **Receivers:** Broadcast receivers (`DeviceBootReceiver`, `EventReceiver`, `TermuxResultReceiver`, `AutomationReceiver`)
- **Workers:** WorkManager workers (`AutomationWorker`)
- **DI Modules:** Hilt `@Module` classes providing database, repositories, and infrastructure

#### 3. UI Layer (`ui/`)
- **Navigation:** Jetpack Navigation 3 with `@Serializable` route classes (`Route.kt`)
- **Screens:** Per-feature composables (e.g., `HomeScreen`, `EditorScreen`, `AutomationScreen`, `SettingsScreen`)
- **View Models:** `ViewModel` classes with `SavedStateHandle` where needed (e.g., `HomeViewModel`, `EditorViewModel`, `AutomationViewModel`)
- **Components:** Reusable Compose UI elements (`ScriptPickerDialog`, `ScriptRuntimePromptDialog`, `CategorySpinner`, `DayOfWeekPicker`, `LanguageSelector`)
- **Theme:** Material 3 with dynamic color, custom accent colors, light/dark mode (`Theme.kt`, `Color.kt`, `Type.kt`, `AppTheme.kt`)
- **Preview helpers:** `SampleData.kt`, `DevicePreviews.kt`, `WidgetMockData.kt`

### Data Flow
```
UI (Compose) → ViewModel → Use Case → Repository Interface → Repository Impl → DAO/External API
                                                                   ↓
                                                           AppDatabase (Room + SQLCipher)
```

### Key Patterns
- **Repository Pattern:** Domain interfaces are implemented in the data layer; ViewModels depend on interfaces, never implementations
- **Hilt DI:** All repositories, DAOs, and infrastructure are injected via Hilt `@Provides` / `@Singleton`
- **Navigation 3:** Uses `NavKey` with `@Serializable` route classes and `EntryProvider`/`NavDisplay`
- **State Management:** ViewModels expose `StateFlow` / `SharedFlow` for UI state and one-shot events
- **Coroutines:** All async operations use `viewModelScope` / `CoroutineScope` with proper dispatchers from `DispatcherModule`

---

## Best Practices & Guidelines

### Kotlin & Android
- Use **Kotlin Coroutines** with `StateFlow` and `SharedFlow` for reactive state management
- Always pass the correct `CoroutineDispatcher` from `DispatcherModule` — never use `Dispatchers.IO` or `Dispatchers.Main` directly
- Use `viewModelScope` in ViewModels and `lifecycleScope` in Activities/Fragments
- Prefer **`runCatching`** or sealed result types for error handling instead of try/catch where appropriate
- Use **`@Parcelize`** for data classes that need to cross process boundaries (e.g., `Script`, `Automation`)
- Use **`@Serializable`** for models that are persisted to JSON (DTOs, Script data pages)
- Always use `SavedStateHandle` in ViewModels when the screen receives navigation arguments
- Cancel coroutines and close resources in `onStopListening()` or appropriate lifecycle callbacks (e.g., `BaseScriptTileService`, `TcpMonitor`)
- Use `rememberSaveable` in Compose for state that needs to survive process death
- Handle configuration changes gracefully — avoid `remember` for large data that should be in ViewModels

### Security
- **SQLCipher:** All database access goes through `SQLCipher` (loaded via `System.loadLibrary("sqlcipher")` in `ScriptRunnerApp`)
- **Tink:** Encryption keys for script data are managed via `KeyManager` (using Tink's `Aead`)
- **Termux Permission:** The app requires `com.termux.permission.RUN_COMMAND` — this is requested during onboarding
- **ADB Code:** ADB trigger scripts require a user-configured code for remote execution
- **Battery Exemption:** Request `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` for monitoring services to work reliably on OEMs
- **MIUI/HyperOS:** Use `MiuiUtils` to handle Xiaomi-specific permission quirks (shortcut permissions, background start)
- Never log sensitive data (env vars, script contents, encryption keys)
- Use `android:exported="false"` for internal services and receivers; only export what's needed for ADB/widget integration

### Performance
- Use **Baseline Profiles** for critical user journeys (already configured in `baselineprofile/` module)
- Enable **`android.nonTransitiveRClass`** (set in `gradle.properties`) to reduce R class size
- Use **`isMinifyEnabled = true`** + **`isShrinkResources = true`** in release builds (proguard-rules.pro)
- Profile with **Android Studio Profiler** or `adb shell am profile` for CPU/memory issues
- Be mindful of `BasicTextField` performance — large files (>170K chars) may lag on mid-range hardware; this is an accepted tradeoff for stylus support
- Use `LazyColumn` / `LazyRow` for lists — never use `Column` with many children in a scrollable container
- Cache expensive computations (e.g., regex patterns in `RunScriptUseCase`) rather than recompiling per call
- Use `Flow` operators like `distinctUntilChanged()` and `conflate()` to avoid redundant UI updates
- For Room queries returning `Flow`, use `Flow.distinctUntilChanged()` when the data shape is stable

### Dependency Injection (Hilt)
- All `@Provides` methods in `AppModule` should be `@Singleton` unless scoped otherwise (e.g., `@ViewModelInject`)
- DAOs are provided via `@Provides` in `DatabaseModule` — don't inject `AppDatabase` directly
- `WorkerFactory` is injected via `AppModule` and set in `ScriptRunnerApp.workManagerConfiguration`
- Use `@ApplicationContext` qualifier for context-dependent providers (never inject `Activity` context)
- For Compose `ViewModel` injection, use `hiltViewModel()` from `androidx.hilt.lifecycle.viewmodel.compose`

### Compose UI
- Use `collectAsStateWithLifecycle()` for collecting flows in composables (never plain `collectAsState()`)
- Annotate composable functions with `@OptIn(ExperimentalMaterial3Api::class)` only when necessary
- Use `Modifier.semantics { testTagsAsResourceId = true }` on root surfaces for testing
- Follow **Material 3** guidelines — prefer `MaterialTheme.colorScheme` over hardcoded colors
- Use `ScriptRunnerForTermuxTheme` wrapper (supports custom accent colors, dark mode, dynamic color)
- Preview composables with `@Preview` and sample data from `SampleData.kt`
- Use `DisposableEffect` + `LifecycleEventObserver` for lifecycle-aware side effects
- Use `rememberLauncherForActivityResult` for permission and intent requests

### Git & Commit Conventions
- Branch from `dev` for features; `main`/`master` is for releases
- Commit messages should be descriptive — multiple changes can be listed with ` - ` separators
- Tag releases with `v*` pattern (e.g., `v1.8.0`) to trigger CI
- Include changelog entries in `fastlane/metadata/android/en-US/changelogs/` for each release
- Squash-and-merge is preferred for feature branches

---

## Code Style

### Kotlin Style
- **Code style:** Official Kotlin coding conventions (configured in `gradle.properties`: `kotlin.code.style=official`)
- **Indentation:** 4 spaces (enforced by ktlint)
- **Line length:** No hard limit enforced by ktlint, but aim for 120 chars where practical
- **Imports:** Alphabetically sorted within groups, no wildcard imports (`.*` except for test assertions)
- **Naming:**
  - Classes/functions: `PascalCase` / `camelCase`
  - Constants: `UPPER_SNAKE_CASE` (e.g., `EXTRA_COMPONENT_NAME`)
  - Test methods: backtick style with descriptive names (e.g., `` `envMapConverters - correctly handles a valid map` ``)
- **Null safety:** Always prefer nullable types (`String?`) over platform types; use `?:` for defaults

### ktlint Rules
- `ktlint_function_naming_ignore_when_annotated_with = Composable` (set in `.editorconfig`) — allows `@Composable` functions to use snake_case if needed
- `ignoreFailures = true` — ktlint violations won't fail the build (but should be fixed)
- Run `./gradlew ktlintFormat` to auto-format before committing

### File Organization
- One top-level declaration per file (class, interface, object, etc.)
- `di/` package contains all Hilt modules and qualifiers
- `domain/model/` contains data classes — keep them pure (no Android dependencies unless Parcelable is needed)
- `data/local/` contains Room-related code (DAOs, entities, DTOs, converters, database)
- UI feature directories follow pattern: `features/<feature_name>/` containing Route, Screen, ViewModel, and optional `components/` subfolder
- Theme files in `ui/theme/` (Color, Type, Theme, AppTheme)

### `.editorconfig`
```
root = true
[*]
ktlint_function_naming_ignore_when_annotated_with = Composable
```

---

## Testing

### Test Organization
```
app/src/test/java/                 # Unit tests (JVM with Robolectric)
app/src/androidTest/java/          # Instrumented tests (on device/emulator)
```

### Test Naming & Conventions
- Test files are named `*Test.kt` (e.g., `ScriptRepositoryTest.kt`, `AutomationTimeCalculatorTest.kt`)
- Test classes use JUnit 4 (`@Test`, `@Before`, `@RunWith`)
- Unit tests use **MockK** for mocking (`mockk()`, `coEvery`, `coVerify`, `slot`, `mockkStatic`)
- JVM tests use **Robolectric** with `@Config(sdk = [33])` and `@RunWith(RobolectricTestRunner::class)`
- Instrumented tests use **Hilt** with `@HiltAndroidTest` and custom `TestApplication`
- Test method names use backtick string style for readability

### Test Setup
- `TestApplication.kt` — minimal Hilt application for JVM tests
- `HiltTestRunner.kt` — custom test runner for instrumented tests (in `androidTest/`)
- `Robots.kt` — Page Object pattern helpers for UI testing
- MockK Android variant: `mockk-android` for instrumented tests

### Running Tests
```bash
# Unit tests
./gradlew testDebugUnitTest

# Specific test class
./gradlew testDebugUnitTest --tests "io.github.swiftstagrime.termuiRunner.ScriptRepositoryTest"

# Instrumented tests
./gradlew connectedDebugAndroidTest

# All tests
./gradlew testDebugUnitTest connectedDebugAndroidTest
```

### Test Coverage Areas
- `AutomationSchedulerTest` — automation scheduling logic
- `AutomationTimeCalculatorTest` — time calculation edge cases
- `ConvertersTest` — Room TypeConverter serialization
- `DataLayerMappingTest` — entity ↔ domain model mapping
- `DeviceBootReceiverTest` — boot-time automation restoration
- `ExecutionHistoryViewModelTest` — ViewModel state flow
- `ProcessTermuxResultUseCaseTest` — Termux result processing
- `RunScriptShellValidityTest` / `RunScriptUseCaseTest` — script execution and shell injection prevention
- `ScriptExecutionMappingTest` — execution entity mapping
- `ScriptRepositoryTest` — repository CRUD + backup/restore
- `ScriptTileMappingTest` — Quick Settings tile mapping
- `TermuxRepositoryTest` — Termux integration
- `MigrationTest` — Room database migration verification
- `DaoTests` — DAO query tests
- `BackupImportExportTest` — full backup/restore round-trip

---

## Security

### Encryption
- **SQLCipher:** All persisted script data, configurations, and metadata are stored in an encrypted Room database. The native library is loaded in `ScriptRunnerApp.onCreate()`:
  ```kotlin
  System.loadLibrary("sqlcipher")
  ```
- The database key is derived and managed via `KeyManager` (using **Tink**'s `Aead` API)
- Schema locations are exported to `app/src/main/schemas/` for migration tracking

### Termux Integration
- Requires the `com.termux.permission.RUN_COMMAND` permission (granted during onboarding)
- Scripts are executed via Termux's `RUN_COMMAND` intent — the app never directly invokes shell commands
- **ADB remote execution** (`AdbTriggerActivity`) accepts a user-defined code via intent extra `io.github.swiftstagrime.termuiRunner.adb_code`

### Shell Injection Prevention
- `RunScriptUseCase` enforces an **interpreter allowlist** (only approved interpreters like `bash`, `sh` are accepted)
- All interpreter and argument values are **fully escaped** using bash-safe escaping
- User-provided runtime parameters are passed as intent extras, never interpolated into shell commands

### Permissions
| Permission | Purpose |
|-----------|---------|
| `com.termux.permission.RUN_COMMAND` | Send commands to Termux |
| `android.permission.POST_NOTIFICATIONS` | Show execution/monitoring notifications |
| `android.permission.FOREGROUND_SERVICE` | Foreground services for monitoring |
| `android.permission.RECEIVE_BOOT_COMPLETED` | Restore automations after reboot |
| `android.permission.SCHEDULE_EXACT_ALARM` | Precise automation scheduling |
| `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Disable battery optimization for monitoring |
| `android.permission.WRITE_EXTERNAL_STORAGE` | (API ≤ 28) Export/import files |
| `android.permission.INTERNET` | Webhook server |
| `INSTALL_SHORTCUT` (custom) | Create home screen shortcuts |

### OEM-Specific Handling
- `MiuiUtils` detects MIUI/HyperOS and handles:
  - Shortcut permission grant flow
  - Background process restrictions
- Other OEMs: users may need to manually enable "Background Start" / "Shortcut" permissions in system settings

---

## CI/CD

### GitHub Actions
- **Workflow:** `.github/workflows/release.yml`
- **Trigger:** On tag push matching `v*` or manual dispatch
- **Steps:** Checkout → Setup JDK 21 → Grant gradlew permissions → Build unsigned release APK (`assembleRelease`) → Upload artifact
- Lint baselines continue on errors (`-Dlint.baselines.continue=true`)

### Fastlane / Store Listing
- Metadata in `fastlane/metadata/android/en-US/`:
  - `title.txt`, `short_description.txt`, `full_description.txt`
  - Changelogs per versionCode in `changelogs/`
  - Screenshots and promotional graphics in `images/`
- Release tags follow pattern `v<versionCode>` (e.g., `v180`, `v182`)

### Release Process
1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`
2. Add changelog entry in `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`
3. Commit changes
4. Tag: `git tag v<versionName>` (e.g., `v1.8.0`)
5. Push: `git push origin v<versionName>`
6. GitHub Actions builds the unsigned APK and uploads it as an artifact

---

## Development Environment Setup

### Prerequisites
- **JDK:** 21 (Temurin recommended)
- **Android Studio:** Latest stable (Meerkat or newer) with Compose and Hilt plugins
- **Android SDK:** API 37 (compile), API 24+ (minimum)
- **Device/Emulator:** USB debugging enabled; for instrumented tests, a connected device or emulator with API 24+
- **Termux:** Installed on the target device for script execution testing

### First-Time Setup
```bash
# Clone and build
git clone <repo-url>
cd ScriptRunner_for_Termux
./gradlew assembleDebug       # builds the app
./gradlew testDebugUnitTest   # run unit tests
```

### Common Gotchas
- **`com.termux.permission.RUN_COMMAND`:** This permission must be granted via the system settings (search for "RUN_COMMAND" in the permission manager). The onboarding flow guides users through this.
- **SQLCipher native libs:** If you see `java.lang.UnsatisfiedLinkError` for `sqlcipher`, ensure `System.loadLibrary("sqlcipher")` is called before any database access (it's in `ScriptRunnerApp.onCreate()`).
- **Room schema exports:** Don't delete files in `app/src/main/schemas/` — they are version-controlled and used for migration verification.
- **Hilt code generation:** Always trigger a clean build if Hilt or Room annotations change (`./gradlew clean assembleDebug`).
- **Baseline profiles:** Do not run `generateBaselineProfile` on low-end devices; use a flagship or high-end emulator.
- **KSP vs KAPT:** This project uses KSP for Room and Hilt code generation. Don't add KAPT dependencies for these.
