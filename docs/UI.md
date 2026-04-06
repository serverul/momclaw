# MOMCLAW UI Documentation

**Version:** 1.0.0  
**Last Updated:** 2026-04-06  
**Tech Stack:** Jetpack Compose, Material3, Hilt, Navigation Compose

---

## Architecture

```
UI Layer (Compose Screens)
    ↓
ViewModels (StateFlow, Hilt)
    ↓
Repository (ChatRepository)
    ↓
Data Sources: Room DB, AgentClient (SSE), DataStore Preferences
```

All screens follow **unidirectional data flow (UDF)**:
- **ViewModel** exposes `StateFlow<UiState>`
- **Screen** subscribes via `collectAsStateWithLifecycle()`
- **Events** are callbacks passed from NavGraph → Screen → ViewModel

---

## Screens

### 1. ChatScreen

**File:** `ui/chat/ChatScreen.kt`  
**ViewModel:** `ChatViewModel`

#### Features
- Real-time streaming response display
- Pulsing dot animation (3 dots with staggered delays)
- Blinking cursor for active streaming
- Message bubbles with user (right, primary color) / assistant (left, surface_variant) layout
- Auto-scroll to latest message with debouncing
- Error banner with retry
- Input area with send/stop toggle
- Content width capping (280dp phone, 600dp tablet)
- Backpressure handling via `StreamBuffer` (50ms / 5-token batches)

#### UiState Properties
| Property | Type | Description |
|---|---|---|
| `messages` | `List<ChatMessage>` | Conversation messages |
| `inputText` | `String` | Current input field text |
| `isLoading` | `Boolean` | Waiting for first token |
| `isStreaming` | `Boolean` | Currently receiving tokens |
| `currentStreamingMessage` | `ChatMessage?` | Message being streamed |
| `error` | `String?` | Error message if any |
| `isAgentAvailable` | `Boolean` | Agent health check result |
| `config` | `AgentConfig?` | Current agent configuration |
| `tokenCount` | `Int` | Tokens received in current stream |
| `lastUpdateTime` | `Long` | Timestamp of last UI update |

#### Composable Components
- `MessageBubble` — Adaptive container (routes to User/Assistant)
- `UserMessageBubble` — Right-aligned, primary color
- `AssistantMessageBubble` — Left-aligned, surface_variant, streaming cursor
- `PulsingDot` — 3 loading dots with staggered animation
- `BlinkingCursor` — Single blinking bar for streaming
- `MessageInput` — TextField + Send/Stop button row

---

### 2. ModelsScreen

**File:** `ui/models/ModelsScreen.kt`  
**ViewModel:** `ModelsViewModel`

#### Features
- Model list with status indicators (downloaded, loaded, available)
- Download progress indicator (circular + linear)
- Grid layout on tablets (2 columns), list on phones
- Actions per model: Download → Load → Delete
- Animated rotating icon during loading/downloading
- Empty state with refresh CTA
- Error banner with retry

#### UiState Properties
| Property | Type | Description |
|---|---|---|
| `models` | `List<ModelItem>` | Available models |
| `isLoading` | `Boolean` | Loading models list |
| `isDownloading` | `Boolean` | Active download in progress |
| `downloadingModelId` | `String?` | ID of model being downloaded |
| `downloadProgress` | `Float` | 0.0–1.0 progress |
| `loadingModelId` | `String?` | ID of model being loaded |
| `error` | `String?` | Error message |

#### ModelItem Properties
| Property | Type | Description |
|---|---|---|
| `id` | `String` | Model identifier |
| `name` | `String` | Display name |
| `size` | `String` | Human-readable size |
| `downloaded` | `Boolean` | Locally cached |
| `loaded` | `Boolean` | Currently loaded in memory |

#### Composable Components
- `DownloadProgressIndicator` — Banner with dual progress indicators
- `EmptyModelsState` — Placeholder with icon and refresh button
- `ModelCard` — Status icon + info + action buttons (compact/full variants)
- `ModelStatusIcon` — Circle icon (check/done/cloud/rotate)
- `ModelActions` — Download/Load/Delete buttons with loading spinners

---

### 3. SettingsScreen

**File:** `ui/settings/SettingsScreen.kt`  
**ViewModel:** `SettingsViewModel`

#### Features
- Agent configuration: system prompt, temperature, max tokens, model, base URL
- App settings: dark theme, streaming, notifications, background agent
- About section with app info and reset button
- Two-column layout on tablets, single-column on phones
- Save button appears only when changes are detected (`hasChanges`)
- Animated save button entrance/exit

#### UiState Properties
| Property | Type | Description |
|---|---|---|
| `systemPrompt` | `String` | AI instructions |
| `temperature` | `Float` | 0.0–2.0, randomness control |
| `maxTokens` | `Int` | 256–8192, response length |
| `modelPrimary` | `String` | Active model ID |
| `baseUrl` | `String` | Agent endpoint |
| `darkTheme` | `Boolean` | Theme toggle |
| `streamingEnabled` | `Boolean` | Stream responses toggle |
| `notificationsEnabled` | `Boolean` | Agent alerts toggle |
| `backgroundAgentEnabled` | `Boolean` | Foreground service toggle |
| `hasChanges` | `Boolean` | Unsaved changes indicator |

#### Composable Components
- `SettingsSection` — Reusable section header with icon
- `AgentSettingsSection` — Agent config fields
- `AppSettingsSection` — App toggles
- `AboutSection` — Info + reset
- `SettingsSlider` — Labeled Slider with value display + support text
- `SettingsSwitch` — ListItem with trailing Switch

---

## Navigation

**File:** `ui/navigation/NavGraph.kt`

### Destinations
| Route | Screen | Start |
|---|---|---|
| `chat` | ChatScreen | ✅ |
| `models` | ModelsScreen | |
| `settings` | SettingsScreen | |

### Responsive Behavior
- **Phone (COMPACT):** Bottom `NavigationBar` with 3 tabs
- **Tablet (MEDIUM/EXPANDED):** Side `NavigationRail` with icon+label

### Transitions
- Horizontal slide with spring animation (medium bounce, low stiffness)
- Cross-fade + slide
- State save/restore on navigation

---

## Theme

### Color Scheme
Based on Material3 with custom MOMCLAW brand colors:
- **Primary:** `#1A73E8` (Google blue)
- **Secondary:** `#03DAC6` (teal)
- Dark theme is **default**
- Dynamic color (Material You) supported on Android 12+

### Typography
Standard Material3 typography with default font family. All sizes follow M3 spec.

---

## Screenshots / Previews

Composable previews are defined for **all screens** in multiple states:

| File | Previews |
|---|---|
| `ChatScreenPreview.kt` | Empty, Messages, Streaming, Error, Dark, Tablet, MessageBubbles |
| `ModelsScreenPreview.kt` | Empty, Loading, List, Downloading, Error, Tablet, Dark, ModelCard States |
| `SettingsScreenPreview.kt` | Default, Modified, Loading, Tablet, Light, Settings Components |

View previews in **Android Studio → Design/Preview** or run:
```bash
./gradlew app:compileDebugKotlin
```

---

## Testing

### Unit Tests (JUnit + MockK)
- `ChatViewModelTest` — 11 test cases covering state, input, streaming, errors

### Instrumentation Tests (Compose Test)
- `ChatScreenTest` — 8 tests: empty state, messages, streaming, error, input, button states, toolbar
- `ModelsScreenTest` — 6 tests: empty, loading, list, download, error, tablet grid
- `SettingsScreenTest` — 8 tests: all sections, save button, tablet layout, supporting texts
- `NavGraphTest` — 4 tests: bottom nav, start destination, navigation, tablet rail

Run tests:
```bash
# Unit tests
./gradlew app:testDebugUnitTest

# Instrumented tests
./gradlew app:connectedDebugAndroidTest
```

---

## Performance Considerations

1. **StreamBuffer** — Batches token updates (50ms or 5 tokens) to reduce recomposition
2. **StateFlow + collectAsStateWithLifecycle** — Lifecycle-aware collection prevents leaks
3. **`remember` for derived values** — Minimizes recomposition scope
4. **`key` in LazyColumn items** — Efficient list diffing
5. **Stable composable classes** — UiState data classes are stable by default
6. **Batched DB writes** — Repository only writes to Room every 500ms during streaming

---

## Responsive Design Matrix

| Size Class | Width | Navigation | Chat Content Max | Models Layout | Settings Layout |
|---|---|---|---|---|---|
| COMPACT | <600dp | Bottom bar | 600dp | List (1 col) | Single column |
| MEDIUM | 600–840dp | NavigationRail | 800dp | Grid (2 col) | Two columns |
| EXPANDED | >840dp | NavigationRail | 800dp | Grid (2 col) | Two columns |
