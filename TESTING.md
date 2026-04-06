# MomClAW Testing Guide

Complete testing strategy for MomClAW integration, validation, and verification.

---

## 📋 Table of Contents

- [Testing Strategy](#testing-strategy)
- [Automated Tests](#automated-tests)
- [Manual Testing Checklist](#manual-testing-checklist)
- [Integration Testing](#integration-testing)
- [Offline Testing](#offline-testing)
- [Performance Testing](#performance-testing)
- [Test Scripts](#test-scripts)

---

## 🎯 Testing Strategy

MomClAW uses a **multi-layer testing approach**:

```
┌─────────────────────────────────────────────┐
│           Manual Testing (UI/UX)             │
├─────────────────────────────────────────────┤
│      Integration Tests (Components)          │
│      • Service lifecycle                     │
│      • Database integration                  │
│      • Network communication                 │
├─────────────────────────────────────────────┤
│        Unit Tests (Individual)               │
│        • ViewModels                          │
│        • Repositories                        │
│        • Business logic                      │
├─────────────────────────────────────────────┤
│      Static Analysis (Code Quality)          │
│      • Lint                                  │
│      • Detekt                                │
│      • Security scans                        │
└─────────────────────────────────────────────┘
```

---

## 🤖 Automated Tests

### Running All Tests

```bash
# From project root
cd /home/userul/.openclaw/workspace/momclaw

# Run all automated tests
./scripts/run-tests.sh

# With coverage report
./scripts/run-tests.sh --coverage
```

### Unit Tests

```bash
# All unit tests
./android/gradlew testDebugUnitTest

# Specific test class
./android/gradlew test --tests "com.loa.momclaw.ui.chat.ChatViewModelTest"

# With detailed output
./android/gradlew testDebugUnitTest --info
```

### Integration Tests

```bash
# Requires connected device or emulator
./android/gradlew connectedAndroidTest

# Specific integration test
./android/gradlew connectedAndroidTest \
  --tests "com.loa.momclaw.integration.ServiceLifecycleIntegrationTest"
```

### Static Analysis

```bash
# Android Lint
./android/gradlew lintDebug

# Detekt (Kotlin static analysis)
./android/gradlew detekt

# Security checks
./scripts/validate-build.sh --security
```

---

## ✅ Manual Testing Checklist

### Pre-Requisites

- [ ] Android device (API 28+) or emulator configured
- [ ] Model file downloaded (~2.5GB)
- [ ] ADB debugging enabled
- [ ] Sufficient storage (5GB+ free)

### 1. Installation Testing

**Test: Fresh Install**

- [ ] Build debug APK: `./android/gradlew assembleDebug`
- [ ] Install on device: `adb install android/app/build/outputs/apk/debug/app-debug.apk`
- [ ] App launches without crash
- [ ] No permission dialogs on first launch (unless necessary)
- [ ] Default configuration loaded correctly
- [ ] First-run experience works

**Test: Upgrade from Previous Version**

- [ ] Install previous version
- [ ] Configure settings, create conversations
- [ ] Install new version (without uninstalling)
- [ ] All data preserved
- [ ] Settings migrated correctly
- [ ] Conversations accessible

**Test: Clean Uninstall**

- [ ] Uninstall app: `adb uninstall com.loa.momclaw`
- [ ] Verify all data removed
- [ ] Reinstall and verify fresh state

---

### 2. Service Startup Testing

**Test: Normal Startup Sequence**

- [ ] Launch app
- [ ] Observe notification "Starting LiteRT Bridge..."
- [ ] Wait ~5-10 seconds
- [ ] Notification updates to "Running on localhost:8080"
- [ ] Observe notification "Starting NullClaw agent..."
- [ ] Wait ~2-5 seconds
- [ ] Notification updates to "Agent running (PID: XXX)"
- [ ] Both services showing in foreground notifications
- [ ] Chat interface becomes available

**Test: Service Restart on Crash**

- [ ] Force kill NullClaw process: `adb shell ps | grep nullclaw` then `adb shell kill <PID>`
- [ ] Wait 5 seconds
- [ ] Agent should auto-restart (exponential backoff)
- [ ] Notification shows "Restarting agent in Xs (1/3)..."
- [ ] Agent restarts successfully
- [ ] Chat still functional

**Test: App Backgrounding**

- [ ] Start services
- [ ] Press Home button (app goes to background)
- [ ] Wait 5 minutes
- [ ] Return to app
- [ ] Services still running
- [ ] Chat history preserved

**Test: Memory Pressure**

- [ ] Start app and services
- [ ] Open multiple other apps
- [ ] Use `adb shell am kill-all` to simulate memory pressure
- [ ] Return to MomClAW
- [ ] Services restart if killed
- [ ] No data loss

---

### 3. Chat Functionality Testing

**Test: Basic Chat Flow**

- [ ] Type message in input field
- [ ] Send button enabled when text present
- [ ] Press send
- [ ] User message appears in chat
- [ ] "Typing..." indicator shows
- [ ] Assistant response streams token-by-token
- [ ] Response completes and saves
- [ ] Scroll to bottom works

**Test: Multi-turn Conversation**

- [ ] Send first message: "Hello"
- [ ] Receive response
- [ ] Send follow-up: "What's my name?"
- [ ] Verify context maintained (should know previous conversation)
- [ ] Continue conversation 5+ turns
- [ ] All context preserved

**Test: Long Messages**

- [ ] Send very long message (1000+ characters)
- [ ] Message displays correctly
- [ ] Response generates successfully
- [ ] No truncation issues

**Test: Special Characters**

- [ ] Send message with emojis: "Hello 👋 How are you? 😊"
- [ ] Send message with code: "```kotlin\nfun test() { }\n```"
- [ ] Send message with markdown: "# Heading\n**bold** _italic_"
- [ ] All display correctly

**Test: Streaming Cancellation**

- [ ] Send message
- [ ] While response is streaming, press "Stop" button (if available)
- [ ] Streaming stops immediately
- [ ] Partial response saved
- [ ] Can send new message

**Test: Error Recovery**

- [ ] Disable services (force stop)
- [ ] Try to send message
- [ ] Error message appears
- [ ] Retry button available
- [ ] Press retry
- [ ] Services restart
- [ ] Message sends successfully

---

### 4. Model Management Testing

**Test: Model Download**

- [ ] Go to Models screen
- [ ] Available models listed
- [ ] Download button visible for undownloaded models
- [ ] Tap download
- [ ] Progress bar shows
- [ ] Download completes
- [ ] Model appears as "Downloaded"

**Test: Model Switching**

- [ ] Multiple models downloaded
- [ ] Tap different model
- [ ] Services restart with new model
- [ ] Previous conversation still accessible
- [ ] New model responds correctly

**Test: Model Deletion**

- [ ] Long-press downloaded model
- [ ] Delete option appears
- [ ] Tap delete
- [ ] Confirmation dialog
- [ ] Confirm
- [ ] Model removed from list
- [ ] Storage freed

**Test: Insufficient Storage**

- [ ] Fill device storage to <500MB free
- [ ] Try to download large model
- [ ] Error message: "Insufficient storage"
- [ ] Download fails gracefully

---

### 5. Settings Testing

**Test: Temperature Adjustment**

- [ ] Go to Settings
- [ ] Adjust temperature slider (0.0 - 2.0)
- [ ] New value saved
- [ ] Send message
- [ ] Response creativity reflects new temperature

**Test: Max Tokens**

- [ ] Set max tokens to low value (100)
- [ ] Send message expecting long response
- [ ] Response truncated at ~100 tokens
- [ ] Increase to 2048
- [ ] Full response generated

**Test: System Prompt**

- [ ] Edit system prompt
- [ ] Save
- [ ] Start new conversation
- [ ] Agent behavior reflects new prompt

**Test: Offline Mode Toggle**

- [ ] Enable offline mode
- [ ] External channels disabled
- [ ] Only local inference works
- [ ] Disable offline mode
- [ ] Channels re-enabled

---

### 6. Offline Functionality Testing

**Test: Complete Offline Operation**

- [ ] Enable airplane mode
- [ ] Launch app
- [ ] Services start successfully
- [ ] Chat works normally
- [ ] All features accessible
- [ ] No network errors

**Test: Data Persistence Offline**

- [ ] Create conversations offline
- [ ] Close app
- [ ] Reopen app
- [ ] All conversations preserved
- [ ] Settings preserved
- [ ] Model still loaded

**Test: Offline to Online Transition**

- [ ] Create conversations offline
- [ ] Disable airplane mode
- [ ] App detects network
- [ ] No duplicate messages
- [ ] Sync works (if implemented)

---

### 7. Error Handling Testing

**Test: Model File Missing**

- [ ] Delete model file from device storage
- [ ] Launch app
- [ ] Error notification: "Model not found"
- [ ] Prompt to download model
- [ ] App doesn't crash

**Test: Corrupted Model**

- [ ] Corrupt model file (edit bytes)
- [ ] Try to load model
- [ ] Error message: "Model corrupted"
- [ ] Prompt to re-download

**Test: Service Crash Recovery**

- [ ] Force stop InferenceService
- [ ] Try to send message
- [ ] Error appears
- [ ] Auto-restart attempted
- [ ] Service recovers or shows clear error

**Test: Database Corruption**

- [ ] Corrupt SQLite database
- [ ] Launch app
- [ ] Database recovery attempted
- [ ] If unrecoverable, clear data and start fresh
- [ ] App doesn't crash

---

### 8. Performance Testing

**Test: Cold Start Time**

- [ ] Kill app completely
- [ ] Launch app
- [ ] Measure time to interactive chat
- [ ] Target: <15 seconds (including model load)
- [ ] Record actual time

**Test: Warm Start Time**

- [ ] Send app to background
- [ ] Wait 30 seconds
- [ ] Return to app
- [ ] Measure time to interactive
- [ ] Target: <2 seconds

**Test: Message Generation Speed**

- [ ] Send message
- [ ] Measure tokens per second
- [ ] Target: >5 tok/sec on mid-range device
- [   Record actual speed

**Test: Memory Usage**

- [ ] Launch app
- [ ] Use `adb shell dumpsys meminfo com.loa.momclaw`
- [ ] Initial memory: <500MB
- [ ] After 10 messages: <800MB
- [   No memory leaks detected

**Test: Battery Usage**

- [ ] Fully charge device
- [ ] Use app for 1 hour
- [ ] Check battery drain
- [   Target: <15% per hour

---

### 9. UI/UX Testing

**Test: Dark/Light Theme**

- [ ] Switch system theme
- [ ] App follows system theme
- [ ] All UI elements visible in both modes
- [ ] Text readable

**Test: Screen Rotation**

- [ ] Rotate device to landscape
- [ ] UI adapts correctly
- [ ] Chat history preserved
- [ ] Input field works
- [ ] Rotate back to portrait
- [ ] No data loss

**Test: Accessibility**

- [ ] Enable TalkBack
- [ ] Navigate app
- [ ] All elements described
- [ ] Actions accessible via gestures

**Test: Font Scaling**

- [ ] Set system font to largest
- [ ] All text visible
- [   No overlap issues

---

### 10. Edge Cases Testing

**Test: Empty Input**

- [ ] Leave input empty
- [ ] Send button disabled
- [ ] Cannot send empty message

**Test: Rapid Messages**

- [ ] Send message
- [ ] Immediately send another before response
- [ ] Second message queued
- [ ] No race conditions

**Test: Very Long Conversation**

- [ ] Create conversation with 100+ messages
- [ ] Scroll through history
- [   Performance acceptable
- [ ] New messages still generate correctly

**Test: Concurrent Operations**

- [ ] Download model
- [ ] While downloading, send message (if different model)
- [ ] Both operations handled correctly

---

## 🔗 Integration Testing

### Service Integration Matrix

| Test Case | InferenceService | AgentService | StartupManager | Expected Result |
|-----------|------------------|--------------|----------------|-----------------|
| Normal startup | START | WAIT → START | Monitors | ✅ Both running |
| Inference crash | RESTART | AUTO-RESTART | Notifies | ✅ Recovery |
| Agent crash | RUNNING | RESTART | Notifies | ✅ Recovery |
| Both crash | RESTART | WAIT → RESTART | Full restart | ✅ Full recovery |
| Manual stop | STOP | STOP | Stopped | ✅ Clean shutdown |

### Data Flow Integration

```kotlin
// Test: UI → ViewModel → Repository → AgentClient → Agent → LiteRT
@Test
fun testCompleteDataFlow() = runTest {
    // 1. User input
    viewModel.updateInputText("Test")
    
    // 2. Send message
    viewModel.sendMessage()
    
    // 3. Verify state changes
    assertTrue(viewModel.uiState.value.isLoading)
    
    // 4. Wait for response
    advanceUntilIdle()
    
    // 5. Verify complete
    assertFalse(viewModel.uiState.value.isLoading)
    assertTrue(viewModel.uiState.value.messages.isNotEmpty())
}
```

---

## 📴 Offline Testing

### Network Scenarios

1. **Airplane Mode** - Complete offline
2. **Weak Signal** - Intermittent connectivity
3. **No Internet** - WiFi connected but no route
4. **Metered Connection** - Mobile data with limits

### Offline Test Matrix

| Feature | Offline | Weak | Metered | Notes |
|---------|---------|------|---------|-------|
| Chat | ✅ | ✅ | ✅ | Core functionality |
| Model download | ❌ | ⚠️ | ⚠️ | Requires stable connection |
| Settings | ✅ | ✅ | ✅ | Local changes |
| Model switch | ✅ | ✅ | ✅ | If downloaded |
| External channels | ❌ | ⚠️ | ⚠️ | Optional feature |

---

## ⚡ Performance Testing

### Benchmark Suite

```bash
# Run performance benchmarks
./scripts/run-benchmarks.sh

# Output includes:
# - Cold start time
# - Message generation speed
# - Memory usage
# - Battery consumption
```

### Performance Targets

| Metric | Target | Minimum |
|--------|--------|---------|
| Cold start | <15s | <20s |
| Warm start | <2s | <5s |
| Inference speed | >5 tok/s | >3 tok/s |
| Memory (idle) | <500MB | <800MB |
| Memory (active) | <1GB | <1.5GB |
| Battery drain | <15%/hr | <25%/hr |
| Storage (app) | <500MB | <1GB |
| Storage (model) | ~3.5GB | ~4GB |

---

## 🛠️ Test Scripts

### Automated Test Runner

```bash
#!/bin/bash
# comprehensive-test.sh

echo "=== MomClAW Comprehensive Test Suite ==="

# Unit tests
echo "1. Running unit tests..."
./android/gradlew testDebugUnitTest

# Integration tests
echo "2. Running integration tests..."
./android/gradlew connectedAndroidTest

# Static analysis
echo "3. Running static analysis..."
./android/gradlew lint detekt

# Coverage
echo "4. Generating coverage report..."
./android/gradlew testDebugUnitTestCoverage

# Performance benchmarks
echo "5. Running performance benchmarks..."
./scripts/run-benchmarks.sh

echo "=== Test Suite Complete ==="
```

### Manual Test Logger

```bash
#!/bin/bash
# manual-test-logger.sh

LOG_FILE="manual-test-$(date +%Y%m%d_%H%M%S).log"

echo "Manual Test Session: $(date)" | tee -a "$LOG_FILE"
echo "Device: $(adb devices | grep device | head -1)" | tee -a "$LOG_FILE"
echo "---" | tee -a "$LOG_FILE"

echo "1. Testing service startup..."
read -p "Did services start correctly? (y/n): " result
echo "Service startup: $result" | tee -a "$LOG_FILE"

echo "2. Testing chat functionality..."
read -p "Did chat work correctly? (y/n): " result
echo "Chat functionality: $result" | tee -a "$LOG_FILE"

# ... more prompts

echo "Test session complete. Log saved to: $LOG_FILE"
```

---

## 📊 Test Reporting

### Coverage Reports

Location: `android/app/build/reports/coverage/test/debug/index.html`

```bash
# Generate coverage
./android/gradlew testDebugUnitTestCoverage

# Open report
open android/app/build/reports/coverage/test/debug/index.html
```

### Test Results

- Unit tests: `android/app/build/test-results/testDebugUnitTest/`
- Integration tests: `android/app/build/outputs/androidTest-results/connected/`
- Lint: `android/app/build/reports/lint-results-debug.html`

---

## 🚀 CI/CD Integration

### GitHub Actions Workflow

```yaml
name: Test Suite

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Run unit tests
        run: ./android/gradlew testDebugUnitTest
      
      - name: Run static analysis
        run: ./android/gradlew lint detekt
      
      - name: Upload coverage
        uses: codecov/codecov-action@v3
```

---

## 📝 Test Documentation

### Test Case Template

```markdown
## Test: [Test Name]

**Category:** [Unit/Integration/Manual]
**Priority:** [High/Medium/Low]
**Automated:** [Yes/No/Partial]

### Preconditions
- [ ] Condition 1
- [ ] Condition 2

### Steps
1. Step 1
2. Step 2
3. Step 3

### Expected Result
[What should happen]

### Actual Result
[What actually happened]

### Status
[Pass/Fail/Blocked]

### Notes
[Any additional information]
```

---

## ✅ Test Completion Checklist

Before release, ensure:

- [ ] All automated tests passing
- [ ] Manual test checklist 100% complete
- [ ] Performance targets met
- [ ] No critical bugs
- [ ] Coverage >70%
- [ ] Static analysis clean
- [ ] Security scan clean
- [ ] Documentation updated
- [ ] Release notes prepared

---

*Last updated: 2026-04-06*
*Version: 1.0*
