# MomClAW User Guide

**Version 1.0.0** | Complete guide for MomClAW users

---

## 📖 Table of Contents

- [What is MomClAW?](#what-is-momclaw)
- [Getting Started](#getting-started)
- [Using MomClAW](#using-momclaw)
- [Features Guide](#features-guide)
- [Settings & Configuration](#settings--configuration)
- [Model Management](#model-management)
- [Tips & Best Practices](#tips--best-practices)
- [Troubleshooting](#troubleshooting)
- [FAQ](#faq)
- [Privacy & Security](#privacy--security)

---

## What is MomClAW?

MomClAW is a **privacy-first AI assistant** that runs **100% offline** on your Android device. Unlike cloud-based AI assistants, MomClAW:

- ✅ Keeps all your conversations private on your device
- ✅ Works without internet connection (after model download)
- ✅ Requires no account or sign-up
- ✅ Has no tracking, telemetry, or data collection
- ✅ Gives you full control over the AI model

### Powered by Gemma 3

MomClAW uses the **Gemma 3 E4B-it** model from Google, optimized for mobile devices with:
- Natural, intelligent conversations
- Context-aware responses
- Multi-turn dialogue support
- Tool execution capabilities

---

## Getting Started

### Installation

#### From APK (Direct Install)

1. Download the APK from [GitHub Releases](https://github.com/serverul/MOMCLAW/releases)
2. Enable "Install from Unknown Sources" in your device settings
3. Open the APK file and tap "Install"
4. Launch MomClAW from your app drawer

#### From Google Play Store

1. Search for "MomClAW" in Google Play
2. Tap "Install"
3. Launch the app

### First-Time Setup

When you first open MomClAW, you'll need to:

1. **Grant Storage Permission** - Required to save the AI model
2. **Download the Model** (~2.5GB)
   - Tap "Download Model" button
   - Wait for download to complete (5-15 min depending on connection)
   - Model is saved to app's private storage

3. **Load the Model**
   - Tap "Load Model" in the Models screen
   - First load takes 10-30 seconds
   - Model stays loaded in memory while app is active

### System Requirements

| Requirement | Minimum | Recommended |
|------------|---------|-------------|
| Android Version | 9.0 (API 28) | 12.0+ (API 31+) |
| RAM | 4GB | 6GB+ |
| Storage | 3GB free | 4GB+ free |
| CPU | ARM64-v8a | Modern 8-core |

---

## Using MomClAW

### Main Interface

MomClAW has a clean, intuitive interface with three main screens:

#### 1. Chat Screen
- **Message Input**: Type your message at the bottom
- **Conversation History**: Scroll through past messages
- **Streaming Responses**: Watch AI generate responses in real-time
- **Copy/Share**: Long-press messages to copy or share

#### 2. Models Screen
- **Model List**: See available models
- **Download Models**: Get new AI models
- **Load/Unload**: Manage active models
- **Storage Info**: See model sizes

#### 3. Settings Screen
- **AI Parameters**: Customize behavior
- **Appearance**: Dark theme, colors
- **Privacy**: Clear data, export/import
- **About**: Version, license info

### Starting a Conversation

1. Ensure a model is loaded (check Models screen)
2. Go to Chat screen
3. Type your message in the input field
4. Tap Send (➤) or press Enter
5. Watch the AI generate its response

### Conversation Features

- **Multi-turn Dialogue**: The AI remembers context from previous messages
- **Streaming**: See responses appear word-by-word
- **Code Blocks**: Formatted code with syntax highlighting
- **Markdown Support**: Bold, italic, lists, headers

---

## Features Guide

### 🧠 Intelligent Conversations

MomClAW provides context-aware responses:
- Remembers previous messages in conversation
- Understands follow-up questions
- Maintains coherent multi-turn dialogues

**Example:**
```
You: What is machine learning?
AI: [Explains machine learning]

You: Can you give me an example?
AI: Sure! A common example is... [Uses context from first question]
```

### 🔧 Tool Execution

MomClAW can execute tools on your device:

#### Shell Commands
```
You: List files in my Downloads folder
AI: I'll check that for you.
[Executes: ls ~/Downloads]
Result: file1.pdf, file2.jpg, ...
```

#### File Operations
```
You: Read the file /sdcard/notes.txt
AI: [Reads and displays file contents]
```

#### Web Search (requires internet)
```
You: Search for latest news about Kotlin
AI: [Searches and summarizes results]
```

### 💾 Persistent Memory

- All conversations are saved automatically
- Search through past conversations
- Export conversations to JSON/Markdown
- Import conversations from backups

### 🌙 Dark Theme

MomClAW supports:
- **Light Mode**: Bright, clean interface
- **Dark Mode**: Easy on the eyes, battery-saving
- **System Default**: Follows your device settings

### 🎨 Material You Design

On Android 12+ devices:
- Dynamic color theming based on wallpaper
- Consistent with system design
- Smooth animations and transitions

---

## Settings & Configuration

### AI Parameters

Customize AI behavior in Settings → AI Parameters:

| Parameter | Range | Description |
|-----------|-------|-------------|
| **Temperature** | 0.0 - 2.0 | Creativity (0.0 = deterministic, 1.0 = balanced, 2.0 = creative) |
| **Max Tokens** | 100 - 4096 | Maximum response length |
| **Top P** | 0.0 - 1.0 | Nucleus sampling (lower = more focused) |
| **Top K** | 1 - 100 | Consider top K token choices |
| **Repeat Penalty** | 1.0 - 2.0 | Penalize repetition (1.0 = off) |

#### Recommended Presets

**Balanced (Default)**
- Temperature: 0.7
- Max Tokens: 2048
- Top P: 0.9
- Top K: 40
- Repeat Penalty: 1.1

**Creative**
- Temperature: 1.2
- Max Tokens: 4096
- Top P: 0.95
- Top K: 60
- Repeat Penalty: 1.15

**Precise**
- Temperature: 0.3
- Max Tokens: 1024
- Top P: 0.8
- Top K: 20
- Repeat Penalty: 1.05

### Appearance Settings

- **Theme**: Light, Dark, System Default
- **Dynamic Colors**: Enable/disable Material You (Android 12+)
- **Font Size**: Small, Normal, Large, Extra Large
- **Message Bubbles**: Rounded, Squared

### Privacy Settings

- **Clear Conversation History**: Delete all chats
- **Clear Model Cache**: Remove downloaded models
- **Export Data**: Backup conversations and settings
- **Import Data**: Restore from backup

---

## Model Management

### Available Models

MomClAW currently supports:

| Model | Size | RAM Required | Best For |
|-------|------|--------------|----------|
| **Gemma 3 E4B-it** | 2.5GB | 4GB+ | General conversations, tasks |
| Future models... | - | - | Coming soon |

### Downloading Models

1. Go to **Models** screen
2. Tap **Download** next to desired model
3. Wait for download to complete
4. Model is saved in app's private storage

**Note**: Downloads require internet connection. After download, the app works 100% offline.

### Loading/Unloading Models

**Load Model**
- Tap "Load" next to downloaded model
- First load: 10-30 seconds
- Subsequent loads: 1-5 seconds (cached)
- Model stays loaded while app is active

**Unload Model**
- Frees up RAM
- Does NOT delete the model file
- Useful on devices with limited RAM

### Model Updates

- Check for updates in Models screen
- New versions may improve quality or performance
- Old models can be safely deleted after update

---

## Tips & Best Practices

### Performance Tips

1. **Keep Model Loaded**: Avoid frequent load/unload cycles
2. **Close Background Apps**: Free up RAM for better performance
3. **Use Dark Mode**: Saves battery on OLED screens
4. **Clear Old Conversations**: Reduces database size

### Conversation Tips

1. **Be Specific**: Clear questions get better answers
2. **Provide Context**: Help AI understand your needs
3. **Use Follow-ups**: Build on previous messages
4. **Experiment with Temperature**: Find your preferred style

### Battery Tips

1. **Unload Model When Done**: Saves RAM and battery
2. **Lower Max Tokens**: Shorter responses = less computation
3. **Close App When Not in Use**: Fully exit, don't just minimize

---

## Troubleshooting

### App Crashes on Launch

**Possible causes:**
1. **Insufficient RAM** - Close background apps, try on device with 4GB+ RAM
2. **Corrupted Model** - Delete and re-download model
3. **Outdated Android** - Update to Android 9.0+

**Solution:**
```bash
# Clear app data
Settings → Apps → MomClAW → Storage → Clear Data

# Re-download model
```

### Model Won't Load

**Possible causes:**
1. **Not Enough RAM** - Unload other models, close apps
2. **Corrupted Download** - Re-download model
3. **Storage Permission Denied** - Grant permission in Settings

**Solution:**
1. Check available RAM (Settings → Memory)
2. Delete model and download again
3. Verify storage permission is granted

### Slow Responses

**Possible causes:**
1. **Low-End Device** - Normal on devices with <4GB RAM
2. **High Temperature** - CPU throttling due to heat
3. **Background Apps** - Other apps consuming CPU

**Solutions:**
- Lower Max Tokens setting
- Close background apps
- Take breaks to let device cool

### App Uses Too Much Storage

**Model Storage Location:**
```
/sdcard/Android/data/com.loa.MOMCLAW/files/models/
```

**To Free Space:**
1. Go to Models screen
2. Delete unused models
3. Clear conversation history

### Responses Are Repetitive

**Adjust Settings:**
1. Increase **Repeat Penalty** (try 1.15 - 1.3)
2. Increase **Temperature** (try 0.8 - 1.0)
3. Lower **Top K** (try 30 - 40)

### Responses Are Incoherent

**Adjust Settings:**
1. Lower **Temperature** (try 0.5 - 0.7)
2. Increase **Top P** (try 0.9 - 0.95)
3. Lower **Max Tokens** for more focused responses

---

## FAQ

### General Questions

**Q: Does MomClAW require internet?**
A: Only for initial model download. After that, it works 100% offline.

**Q: Is my data sent to the cloud?**
A: No. All conversations stay on your device. Zero data collection.

**Q: Do I need an account?**
A: No account required. Just install and use.

**Q: Can I use multiple models?**
A: Yes, but only one can be loaded at a time due to RAM constraints.

**Q: What languages does MomClAW support?**
A: Gemma 3 supports English primarily, with some capability in other languages.

### Technical Questions

**Q: Why does it require 4GB+ RAM?**
A: The AI model needs significant memory to run efficiently.

**Q: Can I move the model to SD card?**
A: No, models must be in internal storage for performance reasons.

**Q: Why is the first response slow?**
A: The first generation after loading requires "warming up" the model. Subsequent responses are faster.

**Q: Can I use MomClAW on an emulator?**
A: Yes, but you need an x86_64 system image and 6GB+ RAM allocated.

### Privacy Questions

**Q: Does MomClAW collect any data?**
A: No. Zero telemetry, zero analytics, zero tracking.

**Q: Where are my conversations stored?**
A: In a local SQLite database in the app's private directory.

**Q: Can I export my conversations?**
A: Yes! Go to Settings → Privacy → Export Data.

**Q: How do I delete all my data?**
A: Settings → Privacy → Clear Conversation History, or uninstall the app.

---

## Privacy & Security

### Data Collection

**MomClAW collects ZERO data:**

- ❌ No personal information
- ❌ No usage analytics
- ❌ No crash reports sent externally
- ❌ No advertising IDs
- ❌ No device fingerprints

### Data Storage

All data is stored locally:

```
/sdcard/Android/data/com.loa.MOMCLAW/
├── files/
│   ├── models/         # AI models
│   ├── database/       # Conversation history
│   └── settings/       # App settings
└── cache/              # Temporary files
```

### Data Deletion

To completely remove all data:

1. **In-App**: Settings → Privacy → Clear All Data
2. **System**: Settings → Apps → MomClAW → Storage → Clear Data
3. **Uninstall**: Removes everything

### Permissions

MomClAW requests minimal permissions:

| Permission | Purpose | Required? |
|-----------|---------|-----------|
| **Storage** | Save AI models | Yes |
| **Network** | Download models | Optional (only for downloads) |

### Security Best Practices

1. **Keep App Updated**: New versions may include security fixes
2. **Download Models from Trusted Sources**: Only from official sources
3. **Export Backups**: Regularly backup important conversations
4. **Use Screen Lock**: Protect your device and conversations

---

## Support & Community

### Get Help

- **GitHub Issues**: [MOMCLAW/issues](https://github.com/serverul/MOMCLAW/issues)
- **GitHub Discussions**: [MOMCLAW/discussions](https://github.com/serverul/MOMCLAW/discussions)
- **Email**: support@MOMCLAW.app

### Contributing

Want to improve MomClAW? See [CONTRIBUTING.md](CONTRIBUTING.md)

### License

MomClAW is open-source software licensed under the Apache License 2.0.

---

**Thank you for using MomClAW! 🐾**

Built with ❤️ by [LinuxOnAsteroids](https://github.com/serverul)

---

**Last Updated**: 2026-04-06  
**Guide Version**: 1.0
