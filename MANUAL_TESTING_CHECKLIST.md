# MomClaw Manual Testing Checklist

**Date:** 2026-04-07  
**Version:** 1.0.0  
**Tester:** ________________  
**Device:** ________________  
**Android Version:** ________________

---

## 📋 Pre-Test Setup

### Environment Check
- [ ] Java 17 installed (`java -version`)
- [ ] ANDROID_HOME set correctly
- [ ] Android SDK API 35 available
- [ ] ADB connected to device/emulator (`adb devices`)
- [ ] Build successful (`./gradlew assembleDebug`)

### Install App
```bash
cd /home/userul/.openclaw/workspace/momclaw/android
./gradlew installDebug
```
- [ ] App installed successfully
- [ ] App launches without crash

---

## 🧪 Test Suite 1: Chat Screen

### 1.1 Initial State
- [ ] Chat screen shows on launch
- [ ] Empty state message displays
- [ ] Input field shows "Type a message..." placeholder
- [ ] Send button disabled when input empty
- [ ] TopAppBar shows "MOMCLAW" title
- [ ] Agent status shows (online/offline)

### 1.2 Message Sending
- [ ] Type message → send button enables
- [ ] Tap send → message appears in list
- [ ] User message right-aligned with primary color
- [ ] Auto-scroll to new message works
- [ ] Input field clears after send
- [ ] Assistant response starts streaming (or shows loading)

### 1.3 Streaming Messages
- [ ] Typing indicator shows (three dots)
- [ ] Streaming cursor blinks
- [ ] Text appears token-by-token
- [ ] Auto-scroll follows streaming
- [ ] Cancel button appears during streaming
- [ ] Tap cancel → streaming stops

### 1.4 Message History
- [ ] Send 5+ messages
- [ ] All messages display correctly
- [ ] Scrolling works smoothly
- [ ] User/assistant messages distinguishable
- [ ] Timestamps correct (if shown)

### 1.5 Chat Actions
- [ ] Tap clear conversation → all messages removed
- [ ] Confirmation appears (or immediate clear)
- [ ] Tap new conversation → fresh chat starts
- [ ] Previous conversation cleared
- [ ] Tap settings icon → navigates to settings
- [ ] Back from settings → chat state preserved

### 1.6 Error Handling
- [ ] Disable network → send message
- [ ] Error banner appears
- [ ] Retry button shows
- [ ] Tap retry → attempts again
- [ ] Dismiss error works

### 1.7 Edge Cases
- [ ] Send very long message (500+ chars)
- [ ] Message displays without truncation
- [ ] Send message with special characters
- [ ] Send emoji in message
- [ ] Rapid send 10 messages → no crashes

---

## 🧪 Test Suite 2: Models Screen

### 2.1 Initial State
- [ ] Navigate to Models screen
- [ ] Loading indicator shows briefly
- [ ] Models list displays (or empty state)
- [ ] TopAppBar shows "Models" title
- [ ] Refresh button visible

### 2.2 Empty State
- [ ] If no models: empty state shows
- [ ] "No models available" message displays
- [ ] Refresh button shows
- [ ] Icon/illustration displays

### 2.3 Model Cards
- [ ] Model cards display correctly
- [ ] Model name shows
- [ ] Model size shows
- [ ] Status icon shows (cloud/download/check)
- [ ] Appropriate action buttons show

### 2.4 Download Model
- [ ] Find undownloaded model
- [ ] Tap "Download" button
- [ ] Progress indicator appears
- [ ] Percentage shows (0%, 25%, 50%, etc.)
- [ ] Progress bar updates
- [ ] Download completes
- [ ] "Load" button appears

### 2.5 Load Model
- [ ] Find downloaded, unloaded model
- [ ] Tap "Load" button
- [ ] Loading indicator shows
- [ ] "Active" badge appears when loaded
- [ ] Status icon changes to check

### 2.6 Model Switching
- [ ] Load second model
- [ ] First model unloaded automatically
- [ ] New model shows "Active" badge
- [ ] Previous model badge removed

### 2.7 Delete Model
- [ ] Find downloaded model (not active)
- [ ] Tap delete icon
- [ ] Confirmation dialog appears
- [ ] Dialog shows model name
- [ ] Tap "Delete" → model removed
- [ ] Tap "Cancel" → dialog dismisses

### 2.8 Active Model Protection
- [ ] Try to delete active model
- [ ] Delete button disabled
- [ ] Or confirmation prevents deletion

### 2.9 Refresh
- [ ] Tap refresh icon
- [ ] Loading indicator shows
- [ ] Models list updates

### 2.10 Error Handling
- [ ] Simulate download failure
- [ ] Error message shows
- [ ] Retry option available
- [ ] Simulate load failure
- [ ] Error shows with details

### 2.11 Responsive Layout
**On Phone:**
- [ ] List layout (single column)
- [ ] Full-width cards
- [ ] Horizontal content layout

**On Tablet:**
- [ ] Grid layout (2 columns)
- [ ] Compact cards
- [ ] Centered content

---

## 🧪 Test Suite 3: Settings Screen

### 3.1 Initial State
- [ ] Navigate to Settings screen
- [ ] All sections display
- [ ] Current values load from storage
- [ ] Scroll works properly

### 3.2 Agent Configuration

#### System Prompt
- [ ] Current system prompt displays
- [ ] Tap to edit
- [ ] Multi-line input works
- [ ] Changes tracked

#### Temperature
- [ ] Current temperature displays
- [ ] Slider updates value
- [ ] Range: 0.0 to 2.0
- [ ] Supporting text shows
- [ ] Value displays in real-time

#### Max Tokens
- [ ] Current max tokens displays
- [ ] Slider updates value
- [ ] Range: 256 to 8192
- [ ] Value displays as integer
- [ ] Supporting text shows

#### Model Primary
- [ ] Current model ID displays
- [ ] Can edit model ID
- [ ] Changes tracked

#### Base URL
- [ ] Current URL displays
- [ ] Enter valid URL → no error
- [ ] Enter invalid URL → error shows
- [ ] Error message: "Invalid URL format"
- [ ] Error clears when valid
- [ ] Empty URL allowed

### 3.3 App Settings

#### Dark Theme
- [ ] Current state shows
- [ ] Toggle switch
- [ ] Theme changes immediately
- [ ] All screens update
- [ ] Status bar color adapts

#### Streaming Enabled
- [ ] Toggle works
- [ ] State saves

#### Notifications Enabled
- [ ] Toggle works
- [ ] State saves

#### Background Agent
- [ ] Toggle works
- [ ] State saves

### 3.4 Save Changes
- [ ] Make a change
- [ ] "Save" button appears in TopAppBar
- [ ] Tap save
- [ ] Changes persist
- [ ] Snackbar shows (or confirmation)
- [ ] Navigate away → return → changes kept

### 3.5 Reset to Defaults
- [ ] Tap "Reset to Defaults"
- [ ] Confirmation dialog appears
- [ ] Dialog warns about reset
- [ ] Tap "Reset" → all values revert
- [ ] Tap "Cancel" → dialog dismisses

### 3.6 Validation
- [ ] Set temperature to 0 → no error
- [ ] Set temperature to 2 → no error
- [ ] Set max tokens to 256 → no error
- [ ] Set max tokens to 8192 → no error
- [ ] Save button disabled with validation errors

### 3.7 Persistence
- [ ] Make changes → save
- [ ] Close app completely
- [ ] Relaunch app
- [ ] Navigate to settings
- [ ] All changes persisted

### 3.8 Responsive Layout
**On Phone:**
- [ ] Single column layout
- [ ] All sections vertical
- [ ] Save button at bottom
- [ ] Proper scrolling

**On Tablet:**
- [ ] Two column layout
- [ ] Left: Agent settings
- [ ] Right: App settings + About
- [ ] Save button in right column

---

## 🧪 Test Suite 4: Navigation

### 4.1 Bottom Navigation (Phone)
- [ ] Bottom bar shows
- [ ] Three items: Chat, Models, Settings
- [ ] Current screen highlighted
- [ ] Icons and labels visible
- [ ] Tap each item → navigates

### 4.2 Navigation Rail (Tablet)
- [ ] Side rail shows
- [ ] Three items display
- [ ] Current screen highlighted
- [ ] Tap each item → navigates
- [ ] Chat shows back button

### 4.3 Navigation Flow
- [ ] Chat → Models → works
- [ ] Models → Settings → works
- [ ] Settings → Chat → works
- [ ] All transitions animated
- [ ] Animations smooth

### 4.4 Back Navigation
- [ ] Use system back gesture
- [ ] Proper back stack
- [ ] State preserved
- [ ] Chat is start destination

### 4.5 State Preservation
- [ ] Type message → navigate away → return
- [ ] Input text preserved
- [ ] Start model download → navigate → return
- [ ] Download continues
- [ ] Make setting change → navigate → return
- [ ] Change still there

### 4.6 Rapid Navigation
- [ ] Rapidly tap nav items
- [ ] No crashes
- [ ] No memory leaks
- [ ] Smooth performance

---

## 🧪 Test Suite 5: Responsive Design

### 5.1 Phone Portrait (< 600dp)
- [ ] Bottom navigation bar
- [ ] Single column layouts
- [ ] Content full width
- [ ] All text readable
- [ ] Touch targets adequate (48dp min)

### 5.2 Phone Landscape
- [ ] Layout adapts
- [ ] Bottom navigation still accessible
- [ ] Content scrolls properly
- [ ] No truncated text

### 5.3 Tablet Portrait (600-840dp)
- [ ] Bottom navigation or rail
- [ ] Slightly wider content
- [ ] Models grid shows
- [ ] Settings two-column ready

### 5.4 Tablet Landscape (> 840dp)
- [ ] Navigation rail on side
- [ ] Content centered with max width
- [ ] Chat: 800dp max
- [ ] Models: Grid layout (2 columns)
- [ ] Settings: Two columns
- [ ] All layouts look balanced

### 5.5 Very Large Screen (> 1200dp)
- [ ] Navigation rail
- [ ] Content centered
- [ ] Excessive whitespace handled
- [ ] Still usable

---

## 🧪 Test Suite 6: Theme & Appearance

### 6.1 Dark Theme
- [ ] Enable dark theme
- [ ] All screens dark
- [ ] Text readable
- [ ] Icons visible
- [ ] Colors appropriate
- [ ] No eye strain

### 6.2 Light Theme
- [ ] Enable light theme
- [ ] All screens light
- [ ] Text readable
- [ ] Icons visible
- [ ] Colors appropriate
- [ ] No glare issues

### 6.3 Theme Switching
- [ ] Toggle theme in settings
- [ ] Change applies immediately
- [ ] No restart needed
- [ ] All components update
- [ ] Status bar adapts

### 6.4 Typography
- [ ] Headings distinct from body
- [ ] Text sizes appropriate
- [ ] Line spacing readable
- [ ] No text truncation

### 6.5 Icons
- [ ] All icons display correctly
- [ ] Icons match actions
- [ ] Icon colors appropriate
- [ ] Icons visible in both themes

---

## 🧪 Test Suite 7: Accessibility

### 7.1 Screen Reader (TalkBack)
- [ ] Enable TalkBack
- [ ] Navigate through Chat screen
- [ ] All elements readable
- [ ] Content descriptions meaningful
- [ ] Navigation logical

### 7.2 Content Descriptions
- [ ] All icons have descriptions
- [ ] Buttons have descriptions
- [ ] Images have descriptions
- [ ] No "unlabeled" elements

### 7.3 Font Scaling
- [ ] Set font scale to 200%
- [ ] All text still readable
- [ ] No truncation
- [ ] Layouts adapt
- [ ] No overlapping text

### 7.4 Touch Targets
- [ ] All buttons ≥ 48dp
- [ ] Easy to tap
- [ ] No accidental taps
- [ ] Adequate spacing

### 7.5 Color Contrast
- [ ] High contrast mode
- [ ] All text readable
- [ ] No information by color alone
- [ ] Color blind friendly

---

## 🧪 Test Suite 8: Performance

### 8.1 App Launch
- [ ] Cold start < 3 seconds
- [ ] Warm start < 1 second
- [ ] No ANR dialogs
- [ ] Splash screen shows briefly

### 8.2 Message Performance
- [ ] Send 50+ messages
- [ ] Scroll smooth
- [ ] Memory stable
- [ ] No lag

### 8.3 Model Operations
- [ ] Download starts quickly
- [ ] Progress updates smooth
- [ ] Load operation responsive
- [ ] Delete completes fast

### 8.4 Navigation Performance
- [ ] Screen transitions smooth
- [ ] No frame drops
- [ ] Animations fluid
- [ ] Rapid navigation OK

### 8.5 Memory Usage
- [ ] Monitor with Android Profiler
- [ ] No memory leaks
- [ ] Memory stable over time
- [ ] GC not excessive

### 8.6 Battery Impact
- [ ] Run app for 1 hour
- [ ] Battery drain reasonable
- [ ] No excessive CPU usage
- [ ] Background usage minimal

---

## 🧪 Test Suite 9: Error Handling

### 9.1 Network Errors
- [ ] Disable network
- [ ] Send message → error shows
- [ ] Retry works
- [ ] Download model → error
- [ ] Load model → error

### 9.2 Storage Errors
- [ ] Fill storage
- [ ] Download model → error
- [ ] App handles gracefully

### 9.3 Service Errors
- [ ] Stop agent service
- [ ] App shows offline
- [ ] Error message clear
- [ ] Retry when service restarts

### 9.4 Model Errors
- [ ] Corrupt model file
- [ ] Load fails gracefully
- [ ] Error message shows
- [ ] Can try different model

### 9.5 Input Errors
- [ ] Invalid URL → validation
- [ ] Out of range values → validation
- [ ] Empty required fields → handled

---

## 🧪 Test Suite 10: Edge Cases

### 10.1 Empty States
- [ ] Fresh install → no messages
- [ ] Fresh install → no models
- [ ] Clear data → all empty
- [ ] Empty states helpful

### 10.2 Long Content
- [ ] Very long message (>1000 chars)
- [ ] Very long system prompt
- [ ] Long model names
- [ ] All display correctly

### 10.3 Special Characters
- [ ] Emoji in messages
- [ ] Unicode in settings
- [ ] Special chars in prompts
- [ ] All save/display correctly

### 10.4 Concurrent Operations
- [ ] Download 2 models simultaneously
- [ ] Send message while loading
- [ ] Change settings while streaming
- [ ] All handle correctly

### 10.5 Interruptions
- [ ] Receive call during operation
- [ ] Background app during download
- [ ] Kill app during streaming
- [ ] All recover correctly

### 10.6 Configuration Changes
- [ ] Rotate during send
- [ ] Rotate during download
- [ ] Resize window (if supported)
- [ ] All preserve state

---

## 🐛 Issues Found

### Critical Issues
| ID | Description | Steps to Reproduce | Expected | Actual | Status |
|----|-------------|-------------------|----------|--------|--------|
| | | | | | |

### Major Issues
| ID | Description | Steps to Reproduce | Expected | Actual | Status |
|----|-------------|-------------------|----------|--------|--------|
| | | | | | |

### Minor Issues
| ID | Description | Steps to Reproduce | Expected | Actual | Status |
|----|-------------|-------------------|----------|--------|--------|
| | | | | | |

---

## 📊 Test Results Summary

**Total Tests:** ______  
**Passed:** ______  
**Failed:** ______  
**Skipped:** ______

**Pass Rate:** ______%

### Critical Blockers: ______
### Major Issues: ______
### Minor Issues: ______

---

## ✅ Sign-Off

**App Ready for Release:** [ ] YES [ ] NO  
**Tester Signature:** ________________  
**Date:** ________________  
**Notes:**

---

## 📸 Screenshots

Attach screenshots for:
- [ ] Empty states (all three screens)
- [ ] Error states
- [ ] Loading states
- [ ] Dark theme
- [ ] Light theme
- [ ] Phone layout
- [ ] Tablet layout
- [ ] Any issues found

---

**Test Completed:** [ ]  
**Duration:** ______ hours  
**Devices Tested:** ______
