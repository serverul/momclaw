# MOMCLAW v1.0.0 - Release Checklist

**Quick Reference for Developers**

---

## ✅ Pre-Release Checklist

### 1. Material3 Compliance

- [ ] All screens use `MaterialTheme.colorScheme`
- [ ] Typography uses `MaterialTheme.typography`
- [ ] Components are Material3 (not Material2)
- [ ] Colors defined for both light and dark themes
- [ ] Custom colors follow Material3 guidelines

**How to verify:**
```bash
# Search for Material2 imports
grep -r "import androidx.compose.material\." app/src/main/java/ | grep -v "material3"
# Should return nothing
```

---

### 2. Responsive Design

**Phone (Compact):**
- [ ] Bottom navigation works
- [ ] All screens scroll properly
- [ ] Touch targets ≥ 48dp
- [ ] Content not too wide

**Tablet (Medium/Expanded):**
- [ ] Navigation rail displays
- [ ] Grid layouts active
- [ ] Two-column settings
- [ ] Content centered/max-width

**Foldables:**
- [ ] Smooth transitions on fold/unfold
- [ ] State preserved
- [ ] Navigation switches correctly

**How to test:**
```bash
# Run on different emulators
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.loa.momclaw.ui.ResponsiveTest
```

---

### 3. Performance

**Streaming:**
- [ ] Tokens appear smoothly
- [ ] No jank or stuttering
- [ ] UI updates throttled (50ms intervals)
- [ ] Database writes batched (500ms intervals)

**Memory:**
- [ ] Message pagination active (max 100)
- [ ] No memory leaks in ViewModels
- [ ] Proper cleanup in `onCleared()`
- [ ] StreamBuffer cleared on cancel

**How to profile:**
```bash
# Run performance tests
./gradlew :app:benchmark:streamingBenchmark

# Profile memory
adb shell dumpsys meminfo com.loa.MOMCLAW
```

---

### 4. Themes

**Dark Theme:**
- [ ] All text readable
- [ ] Icons visible
- [ ] Status bar adapts
- [ ] All screens look good

**Light Theme:**
- [ ] All text readable
- [ ] Icons visible
- [ ] Status bar adapts
- [ ] No washed-out colors

**Theme Switching:**
- [ ] Toggle works instantly
- [ ] State persists across restarts
- [ ] No flicker on app start

**How to test:**
```bash
# Run screenshot tests
./gradlew :app:testDebugUnitTest --tests "*ThemeScreenshots"
```

---

### 5. Accessibility

**Screen Reader:**
- [ ] All content announced
- [ ] Navigation logical
- [ ] Actions described
- [ ] Live regions for updates

**Visual:**
- [ ] Color contrast ≥ 4.5:1
- [ ] Touch targets adequate
- [ ] No color-only information

**How to verify:**
```bash
# Run Accessibility Scanner
# Or run accessibility tests
./gradlew :app:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.loa.momclaw.accessibility.AccessibilityTest
```

---

### 6. Offline Support

**Functionality:**
- [ ] Messages saved offline
- [ ] Settings accessible
- [ ] Graceful degradation
- [ ] Error handling

**Data Persistence:**
- [ ] Room database works
- [ ] DataStore saves settings
- [ ] Conversations maintained

**How to test:**
```bash
# Run offline tests
./gradlew :app:testDebugUnitTest --tests "*OfflineFunctionalityTest"

# Test manually
adb shell am kill com.loa.MOMCLAW
# Reopen app and verify data still there
```

---

## 🔍 Quick Verification Commands

### Build and Test

```bash
# Clean build
./gradlew clean

# Debug build
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Run Android tests
./gradlew connectedAndroidTest

# Run lint
./gradlew lint

# Generate release build
./gradlew assembleRelease
```

### Performance Check

```bash
# Check APK size
./gradlew assembleRelease
ls -lh app/build/outputs/apk/release/*.apk

# Should be < 30MB for universal APK
# Should be < 15MB per ABI split

# Check method count
./gradlew assembleRelease
# Use APK Analyzer in Android Studio
```

### Code Quality

```bash
# Check for TODOs
grep -r "TODO\|FIXME" app/src/main/java/

# Check for print statements
grep -r "println\|print(" app/src/main/java/

# Check for debug logs (should be removed)
grep -r "Log.d\|Log.v\|Log.i" app/src/main/java/

# Check for hardcoded strings
grep -r "android:text=\"" app/src/main/res/layout/
```

---

## 📊 Quality Gates

### Must Pass (P0)

- ✅ All unit tests pass
- ✅ No lint errors (warnings OK)
- ✅ Material3 compliance 100%
- ✅ Dark/Light themes work
- ✅ Offline mode works
- ✅ Performance benchmarks pass

### Should Pass (P1)

- ✅ Accessibility score ≥ 90%
- ✅ Memory usage stable
- ✅ APK size < 30MB
- ✅ Cold start < 2 seconds

### Nice to Have (P2)

- ✅ Screenshot tests pass
- ✅ Accessibility tests pass
- ✅ Memory leak tests pass

---

## 🐛 Known Issues

### Version 1.0.0

1. **Accessibility:** Live regions not implemented (planned for v1.1.0)
2. **Memory:** Pagination not implemented (planned for v1.1.0)
3. **Testing:** UI tests incomplete (planned for v1.1.0)

---

## 📱 Device Testing Matrix

### Phones

- [ ] Pixel 4a (5.81", 1080x2340)
- [ ] Pixel 7 (6.3", 1080x2400)
- [ ] Samsung Galaxy S21 (6.2", 1080x2400)
- [ ] OnePlus 9 (6.55", 1080x2400)

### Tablets

- [ ] Pixel Tablet (10.95", 1600x2560)
- [ ] Samsung Galaxy Tab S8 (11.0", 1600x2560)
- [ ] iPad Pro 11 (equivalent Android tablet)

### Foldables

- [ ] Samsung Galaxy Z Fold 4 (folded/unfolded)
- [ ] Google Pixel Fold (folded/unfolded)
- [ ] Samsung Galaxy Z Flip 4 (folded/unfolded)

### Screen Sizes

- [ ] Small: < 600dp width
- [ ] Medium: 600-840dp width
- [ ] Expanded: > 840dp width

---

## 🎨 Theme Testing

### Light Theme Verification

```kotlin
// In SettingsScreen, toggle theme
MOMCLAWTheme(darkTheme = false) {
    // Check all screens
    ChatScreen()
    ModelsScreen()
    SettingsScreen()
}
```

### Dark Theme Verification

```kotlin
// In SettingsScreen, toggle theme
MOMCLAWTheme(darkTheme = true) {
    // Check all screens
    ChatScreen()
    ModelsScreen()
    SettingsScreen()
}
```

### Contrast Check

Use these tools:
- [WebAIM Contrast Checker](https://webaim.org/resources/contrastchecker/)
- Accessibility Scanner app
- Android Studio Layout Inspector

---

## 🚀 Release Process

### Step 1: Version Bump

```bash
# Update version in app/build.gradle.kts
versionCode = 2
versionName = "1.0.1"
```

### Step 2: Update Changelog

```markdown
# v1.0.1 (2026-04-XX)

## New Features
- [List new features]

## Bug Fixes
- [List bug fixes]

## Improvements
- [List improvements]

## Breaking Changes
- [List breaking changes, if any]
```

### Step 3: Build Release

```bash
# Clean
./gradlew clean

# Build release
./gradlew assembleRelease

# Verify APK
ls -lh app/build/outputs/apk/release/*.apk
```

### Step 4: Test Release Build

```bash
# Install on test device
adb install app/build/outputs/apk/release/app-release.apk

# Run smoke tests
# - Open app
# - Navigate all screens
# - Test core functionality
# - Check for crashes
```

### Step 5: Upload to Play Store

1. Open Google Play Console
2. Select MOMCLAW app
3. Create new release
4. Upload APK
5. Fill release notes
6. Roll out to production (or staged rollout)

---

## 📋 Final Checks Before Release

- [ ] Version number updated
- [ ] Changelog updated
- [ ] All tests pass
- [ ] Lint clean
- [ ] Performance acceptable
- [ ] Memory stable
- [ ] Accessibility verified
- [ ] Offline mode works
- [ ] All devices tested
- [ ] Screenshots updated
- [ ] Store listing updated
- [ ] Release notes written
- [ ] Team reviewed
- [ ] Stakeholders approved

---

## 🆘 Emergency Rollback

If critical bug found after release:

```bash
# 1. Halt staged rollout (if applicable)
# 2. Previous version still available
# 3. Create hotfix branch
git checkout -b hotfix/v1.0.1

# 4. Fix issue
# 5. Test thoroughly
# 6. Bump version to 1.0.2
# 7. Build and release

# Alternative: Unpublish release in Play Console
```

---

## 📞 Support Contacts

**Issues during release:**
- Technical: [Technical Lead]
- Product: [Product Manager]
- QA: [QA Lead]

**Post-release monitoring:**
- Crashlytics: [Link]
- Play Console: [Link]
- Analytics: [Link]

---

**Last Updated:** 2026-04-06  
**Version:** 1.0.0  
**Status:** Ready for Release ✅
