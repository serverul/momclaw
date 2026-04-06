# MomClaw Screenshots

This directory contains screenshots for store listings and documentation.

## Required Screenshots

### Google Play Store

- **Phone Screenshots (required):** 2-8 screenshots
  - Format: PNG or JPEG
  - Size: 1080x1920 (16:9) or similar
  - Location: `fastlane/metadata/android/en-US/images/phoneScreenshots/`

- **7-inch Tablet (optional):** 2-8 screenshots
  - Location: `fastlane/metadata/android/en-US/images/sevenInchScreenshots/`

- **10-inch Tablet (optional):** 2-8 screenshots
  - Location: `fastlane/metadata/android/en-US/images/tenInchScreenshots/`

### F-Droid

- F-Droid uses the same screenshots as Google Play
- Include at least 2-3 screenshots showing core functionality

## Screenshot Guidelines

1. **Chat Screen:** Show conversation with AI responses
2. **Models Screen:** Show model management interface
3. **Settings Screen:** Show configuration options
4. **Dark Theme:** Demonstrate Material You theming
5. **Features:** Highlight key capabilities (offline, privacy, etc.)

## Naming Convention

```
1_chat.png         - Main chat interface
2_models.png       - Model management
3_settings.png     - Settings screen
4_dark_theme.png   - Dark mode
5_features.png     - Feature highlights
```

## How to Take Screenshots

### Using ADB

```bash
# Take screenshot
adb shell screencap -p /sdcard/screenshot.png

# Pull to computer
adb pull /sdcard/screenshot.png ./phoneScreenshots/1_chat.png
```

### Using Android Studio

1. Run app on emulator/device
2. View → Tool Windows → Logcat
3. Click camera icon on left side
4. Save screenshot

### Using Emulator

1. Run app in emulator
2. Click camera icon in toolbar
3. Save screenshot

## Placeholder Screenshots

Until real screenshots are available, create placeholder images:

```bash
# Create placeholder images (requires ImageMagick)
convert -size 1080x1920 xc:gray \
  -gravity center -pointsize 72 -fill white \
  -annotate 0 "MomClaw\nChat Screen" \
  phoneScreenshots/1_chat.png
```

## Fastlane Integration

Screenshots in `fastlane/metadata/android/en-US/images/` are automatically uploaded to Google Play during deployment:

```bash
# Upload metadata including screenshots
fastlane update_metadata

# Or during full deploy
fastlane internal
```

## Current Status

- [ ] Chat screen screenshot
- [ ] Models screen screenshot  
- [ ] Settings screen screenshot
- [ ] Dark theme screenshot
- [ ] Feature highlights screenshot

---

*Add actual screenshots before first store submission.*
