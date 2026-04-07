# Google Play Store Graphics

Create the following files in this directory:

## Icon
- **File:** `icon.png`
- **Size:** 512x512 PNG (32-bit)
- **Requirements:**
  - No transparency
  - Rounded corners will be applied by Play Store
  - Max file size: 1024KB

## Feature Graphic
- **File:** `featureGraphic.png`
- **Size:** 1024x500 PNG or JPEG
- **Requirements:**
  - Will be displayed as a banner on Play Store
  - Keep text and important elements within safe area
  - Max file size: 1024KB

## Screenshots

### Phone Screenshots
- **Directory:** `phoneScreenshots/`
- **Size:** Minimum 320px on any side
- **Maximum:** 2 screenshots per language for short form
- **Recommended:** 16:9 or 9:16 ratio
- **File:** `phoneScreenshot1.png`, `phoneScreenshot2.png`, etc. (max 8)

### 7-inch Tablet Screenshots
- **Directory:** `images/phoneScreenshots/sevenInchScreenshots/`
- **Size:** Minimum 320px on any side
- **Recommended:** 1280x800 or 800x1280
- **File:** `sevenInchScreenshot1.png`, etc. (max 8)

### 10-inch Tablet Screenshots
- **Directory:** `images/phoneScreenshots/tenInchScreenshots/`
- **Size:** Minimum 320px on any side
- **Recommended:** 1920x1200 or 1200x1920
- **File:** `tenInchScreenshot1.png`, etc. (max 8)

## Promo Video (Optional)
- **File:** `video.txt`
- **Content:** YouTube video URL
- **Requirements:** 30 seconds to 2 minutes

---

## Quick Setup

1. Create `graphics/` directory structure:
```
graphics/
├── en-US/
│   ├── icon.png
│   ├── featureGraphic.png
│   ├── phoneScreenshots/
│   │   ├── phoneScreenshot1.png
│   │   ├── phoneScreenshot2.png
│   │   └── phoneScreenshot3.png
│   ├── sevenInchScreenshots/
│   └── tenInchScreenshots/
```

2. Run Fastlane to upload:
```bash
cd android
fastlane upload_metadata
fastlane deploy
```

## Asset Generation

Use the icon generation script:
```bash
./scripts/generate-icons.sh
```

## Tips

- Use consistent branding across all assets
- Keep text minimal in screenshots
- Test graphics on different screen sizes before uploading
- Use Play Store listing preview tools before publishing
