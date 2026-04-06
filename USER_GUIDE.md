# MomClAW User Guide

**Version**: 1.0.0  
**Last Updated**: 2026-04-06

---

## 📖 Table of Contents

- [Introduction](#introduction)
- [Getting Started](#getting-started)
- [Core Features](#core-features)
- [Using the App](#using-the-app)
- [Settings & Configuration](#settings--configuration)
- [Managing Models](#managing-models)
- [Understanding Conversations](#understanding-conversations)
- [Memory & History](#memory--history)
- [Advanced Features](#advanced-features)
- [Troubleshooting](#troubleshooting)
- [Privacy & Security](#privacy--security)
- [Tips & Best Practices](#tips--best-practices)
- [FAQ](#faq)

---

## Introduction

### What is MomClAW?

MomClAW (Mobile Offline Model Agent) is an AI assistant that runs **entirely on your Android device**—no cloud, no tracking, 100% offline. It uses advanced language models to provide intelligent conversations, tool execution, and persistent memory.

### Key Features

- 🧠 **AI-Powered Conversations**: Natural language understanding with Gemma 3 E4B-it
- 🔧 **Tool Integration**: Execute shell commands, manage files, search the web
- 💾 **Persistent Memory**: All conversations saved locally in SQLite
- 🔒 **Privacy-First**: Your data never leaves your device
- ⚡ **Optimized Performance**: Mobile-tuned inference with LiteRT-LM
- 🌙 **Modern UI**: Material You design with dark theme support

### System Requirements

- **Android Version**: Android 9.0 (API 28) or higher
- **RAM**: 4GB or more recommended
- **Storage**: 
  - 3GB+ free for model download (~2.5GB)
  - Additional space for conversations and data
- **Architecture**: ARM64 or ARMv7 device

---

## Getting Started

### Installation

#### From Google Play Store (Recommended)
1. Open Google Play Store
2. Search for "MomClAW"
3. Tap "Install"
4. Wait for installation to complete

#### From GitHub Releases
1. Go to [MomClAW Releases](https://github.com/serverul/MOMCLAW/releases)
2. Download the latest APK for your device architecture:
   - `arm64-v8a` for modern 64-bit devices (most common)
   - `armeabi-v7a` for older 32-bit devices
3. Open the downloaded APK
4. If prompted, enable "Install from unknown sources"
5. Tap "Install"

### First-Time Setup

1. **Launch the App**
   - Tap the MomClAW icon
   - Grant requested permissions:
     - Storage (for model and conversation storage)
     - Optional: Notifications

2. **Download Model** (First Use)
   - You'll see a prompt to download the AI model
   - Tap "Download Model"
   - Wait for download (~2.5GB, use Wi-Fi)
   - Model is saved to: `/sdcard/Android/data/com.loa.MOMCLAW/files/models/`

3. **Initialize Model**
   - After download, tap "Load Model"
   - First load takes 30-60 seconds
   - Subsequent loads are faster (10-15 seconds)

4. **Start Chatting**
   - Type your first message
   - The AI will respond based on context

### Initial Configuration

**Recommended First Steps:**
1. Go to **Settings**
2. Adjust these settings:
   - **Temperature**: 0.7 (balanced creativity)
   - **Max Tokens**: 2048 (sufficient for most responses)
   - **System Prompt**: Customize if needed

3. Test basic conversation:
   - Ask: "Hello! What can you help me with?"
   - Try: "Explain how you work"

---

## Core Features

### 1. Conversational AI

**Basic Chat:**
- Type messages in natural language
- Receive context-aware responses
- Supports multi-turn conversations
- Maintains conversation history

**Example Interactions:**
```
You: "What's the capital of France?"
AI: "The capital of France is Paris. It's known for the Eiffel Tower..."

You: "Tell me more about its history"
AI: [Continues with context about Paris history]
```

### 2. Tool Execution

MomClAW can execute tools to help with tasks:

**Shell Commands:**
```
You: "Create a new directory called 'project'"
AI: [Executes: mkdir project] "I've created the directory."
```

**File Operations:**
```
You: "List files in the current directory"
AI: [Lists files with details]
```

**Web Search** (requires internet):
```
You: "Search for recent news about AI"
AI: [Searches and summarizes results]
```

### 3. Persistent Memory

All conversations are automatically saved:
- Browse chat history
- Search through past conversations
- Export conversations
- Clear specific chats or all data

---

## Using the App

### Main Chat Interface

**Top Bar:**
- **Conversation Title**: Tap to rename
- **Menu (⋮)**: 
  - New conversation
  - Export chat
  - Clear conversation
  - Settings

**Chat Area:**
- Your messages (right-aligned)
- AI responses (left-aligned)
- Timestamps for each message
- Long-press to copy text

**Input Area:**
- Text field for typing
- Send button
- Voice input (if enabled)

### Navigation

**Bottom Navigation:**
- **Chat**: Main conversation interface
- **Models**: Model management
- **History**: Past conversations
- **Settings**: App configuration

### Creating New Conversation

1. Tap **⋮** menu in top-right
2. Select "New conversation"
3. Type your first message
4. Conversation auto-saves

### Managing Conversations

**Rename Conversation:**
1. Tap conversation title
2. Enter new name
3. Press Enter

**Export Conversation:**
1. Tap **⋮** menu
2. Select "Export chat"
3. Choose format:
   - Plain text (.txt)
   - JSON (structured data)
4. Save location appears

**Delete Conversation:**
1. Go to **History** tab
2. Long-press conversation
3. Select "Delete"
4. Confirm deletion

---

## Settings & Configuration

### Accessing Settings

1. Tap **Settings** in bottom navigation
2. Browse categories:
   - AI Settings
   - Model Settings
   - Memory Settings
   - Appearance
   - Privacy
   - About

### AI Settings

**Temperature** (0.0 - 2.0)
- **Low (0.0-0.5)**: More focused, deterministic
- **Medium (0.6-0.9)**: Balanced creativity
- **High (1.0+)**: More creative, varied
- **Recommended**: 0.7

**Max Tokens** (100 - 4096)
- Controls maximum response length
- Higher = longer responses, more time
- **Recommended**: 2048

**Top P** (0.0 - 1.0)
- Nucleus sampling threshold
- Lower = more focused
- **Recommended**: 0.9

**System Prompt**
- Custom instructions for AI behavior
- Example: "You are a helpful coding assistant."
- Leave empty for default behavior

### Model Settings

**Current Model**
- Shows loaded model name
- Model size and version

**Model Path**
- Location: `/sdcard/Android/data/com.loa.MOMCLAW/files/models/`

**Load Model**
- Load downloaded model
- 10-60 seconds depending on device

**Unload Model**
- Free memory
- Model loads again on next use

### Memory Settings

**Conversation History**
- Enable/disable auto-save
- Set retention period (days)

**Memory Limit**
- Maximum conversations to keep
- Automatic cleanup when exceeded

### Appearance

**Theme**
- System default
- Light
- Dark

**Dynamic Colors** (Android 12+)
- Enable/disable Material You colors

**Font Size**
- Small
- Medium (default)
- Large

---

## Managing Models

### Model Overview

MomClAW uses the **Gemma 3 E4B-it** model:
- **Size**: ~2.5GB
- **Format**: LiteRT-LM (optimized for mobile)
- **Quantization**: Q4_K_M (balanced quality/speed)

### Downloading Model

**First Download:**
1. App prompts automatically
2. Tap "Download Model"
3. Use Wi-Fi (large file)
4. Wait 5-15 minutes depending on speed

**Manual Download:**
1. Go to **Models** tab
2. Tap "Download Model"
3. Select model version
4. Confirm download

### Model Locations

**Internal Storage:**
```
/sdcard/Android/data/com.loa.MOMCLAW/files/models/
  └── gemma-3-E4B-it.litertlm
```

**External Storage (if configured):**
```
[SD Card]/Android/data/com.loa.MOMCLAW/files/models/
```

### Loading Model

1. Go to **Models** tab
2. Tap "Load Model"
3. Wait for initialization (10-60s)
4. Status shows "Loaded"

### Updating Model

When new model versions are available:
1. Download new model
2. Delete old model if desired
3. Load new model

### Managing Storage

**Check Model Size:**
1. Go to **Settings** → **About**
2. View "Storage Used"

**Delete Old Models:**
1. Go to **Models** tab
2. Long-press old model
3. Select "Delete"

---

## Understanding Conversations

### How Context Works

MomClAW maintains context across the conversation:
- Remembers previous messages
- References earlier statements
- Tracks conversation flow

**Example:**
```
You: "My name is Alice"
AI: "Nice to meet you, Alice! How can I help?"

You: "What's my name?"
AI: "You told me your name is Alice."
```

### Conversation Limits

**Context Window:**
- Maximum tokens the model can process
- Varies by model (typically 8K-32K)
- Older messages may be truncated

**Managing Long Conversations:**
- Start new chat for different topics
- Export important conversations
- Clear old conversations periodically

### Tips for Better Conversations

**Be Clear and Specific:**
- ✅ "Write a Python function to sort a list"
- ❌ "Help with code"

**Provide Context:**
- ✅ "I'm learning Python. Explain list comprehensions."
- ❌ "What are comprehensions?"

**Break Complex Tasks:**
- ✅ "First, explain what an API is. Then give an example."
- ❌ "Explain APIs with examples for everything."

---

## Memory & History

### Conversation History

**Accessing History:**
1. Tap **History** in bottom nav
2. Browse by date or search
3. Tap to continue conversation

**Search History:**
1. Tap search icon
2. Enter keywords
3. Results show matching conversations

### Managing Memory

**Storage Location:**
```
/data/data/com.loa.MOMCLAW/databases/
  └── momclaw_database.db
```

**Export Conversations:**
1. In **History**, tap conversation
2. Tap **⋮** → **Export**
3. Choose format
4. Save to file

**Clear History:**
1. Go to **Settings** → **Memory**
2. Tap "Clear All Conversations"
3. Confirm action
4. **Warning**: This cannot be undone

### Memory Best Practices

**Organize Conversations:**
- Use clear titles
- Delete irrelevant chats
- Export important ones

**Manage Storage:**
- Monitor conversation count
- Enable auto-cleanup
- Export before clearing

---

## Advanced Features

### Custom System Prompts

**What are System Prompts?**
- Instructions that define AI behavior
- Set once per conversation
- Override default personality

**Examples:**
```
"You are a Python programming expert. Provide code examples and explanations."

"You are a creative writing assistant. Help with stories and character development."

"You are a concise assistant. Keep responses brief and to the point."
```

**Setting Custom Prompt:**
1. Go to **Settings** → **AI Settings**
2. Edit "System Prompt"
3. Save changes
4. New conversations use this prompt

### Tool Configuration

**Enable/Disable Tools:**
1. Go to **Settings** → **Tools**
2. Toggle tools:
   - Shell commands
   - File operations
   - Web search

**Tool Permissions:**
- Shell: Requires storage permission
- File: Requires storage permission
- Web: Requires internet permission

### API Access (Advanced)

MomClAW provides a local API:
- **LiteRT Bridge**: `http://localhost:8080/v1`
- **OpenAI-compatible** endpoints

**Use Cases:**
- Custom integrations
- Automation scripts
- External apps

**Documentation**: See [DOCUMENTATION.md](DOCUMENTATION.md#api-documentation)

---

## Troubleshooting

### Common Issues

#### App Won't Start

**Symptoms:**
- Crashes on launch
- Shows black screen

**Solutions:**
1. Clear app cache: Settings → Apps → MomClAW → Clear Cache
2. Restart device
3. Reinstall app
4. Check Android version (9.0+ required)

#### Model Won't Load

**Symptoms:**
- "Failed to load model" error
- Loading stuck forever

**Solutions:**
1. Check available RAM (close other apps)
2. Verify model file exists
3. Free storage space
4. Re-download model
5. Restart app

#### Slow Responses

**Symptoms:**
- Long wait times
- App freezes during response

**Solutions:**
1. Close background apps
2. Reduce max tokens in settings
3. Check device temperature
4. Restart app
5. Use smaller model (if available)

#### Out of Memory

**Symptoms:**
- App crashes
- "Out of memory" error

**Solutions:**
1. Unload model when not in use
2. Clear conversation history
3. Restart device
4. Reduce context size

#### Storage Full

**Symptoms:**
- Can't save conversations
- Download fails

**Solutions:**
1. Delete old models
2. Export and clear conversations
3. Move to SD card (if available)
4. Clear app data

### Error Messages

**"Model file not found"**
- Download model again
- Check file location

**"Insufficient permissions"**
- Grant storage permission
- Reinstall app

**"Failed to initialize model"**
- Restart app
- Check device compatibility
- Free RAM

**"Database error"**
- Clear app data
- Reinstall app

### Getting Help

**Support Channels:**
- **GitHub Issues**: [momclaw/issues](https://github.com/serverul/MOMCLAW/issues)
- **Discussions**: [momclaw/discussions](https://github.com/serverul/MOMCLAW/discussions)
- **Email**: support@momclaw.app

**When Reporting Issues:**
1. Include device model and Android version
2. Describe steps to reproduce
3. Attach error screenshots
4. Note any recent changes

---

## Privacy & Security

### Data Handling

**What Data is Stored:**
- Conversation history (locally)
- Model files (locally)
- App settings (locally)

**What Data is NOT Sent:**
- ❌ Conversations
- ❌ Personal information
- ❌ Usage analytics
- ❌ Crash reports (unless opted-in)

### Permissions

**Required Permissions:**
- `INTERNET`: Optional web search tool
- `FOREGROUND_SERVICE`: Background processing
- `READ/WRITE_EXTERNAL_STORAGE`: Model and file storage

**Why These Permissions:**
- **Storage**: Store models and conversation files
- **Internet**: Web search tool (offline use doesn't need this)
- **Foreground Service**: Keep AI running in background

### Security Best Practices

**Protect Your Data:**
1. Use device encryption
2. Set up screen lock
3. Don't share conversation exports
4. Clear sensitive conversations

**Manage Privacy:**
1. Review permissions regularly
2. Disable unused tools
3. Clear history periodically
4. Use private conversations

### Data Deletion

**Delete App Data:**
1. Settings → Apps → MomClAW → Clear Data
2. All conversations deleted
3. Models remain on storage

**Complete Removal:**
1. Uninstall app
2. Delete folder: `/sdcard/Android/data/com.loa.MOMCLAW/`
3. All data removed

---

## Tips & Best Practices

### Performance Optimization

**Improve Speed:**
1. Close other apps
2. Unload model when done
3. Use shorter max tokens
4. Restart app daily

**Save Battery:**
1. Lower temperature (less computation)
2. Reduce max tokens
3. Unload model when idle
4. Disable background service

### Conversation Quality

**Better Responses:**
1. Be specific in requests
2. Provide context
3. Break complex tasks into steps
4. Use system prompts for specialized tasks

**Context Management:**
1. Start new chat for different topics
2. Clear irrelevant context
3. Summarize long conversations
4. Export important ones

### Organization

**Keep Chats Organized:**
1. Use descriptive titles
2. Delete old conversations
3. Export important ones
4. Use tags or notes (if supported)

### Workflow Tips

**Daily Use:**
1. Load model at start of day
2. Keep conversations focused
3. Unload at end of day
4. Review and clean weekly

**Project Work:**
1. Create separate chat per project
2. Use system prompt for context
3. Export when complete
4. Archive old projects

---

## FAQ

### General

**Q: Is MomClAW really offline?**  
A: Yes! All AI processing happens on your device. Internet is only needed for:
- Initial model download
- Web search tool (optional)

**Q: How accurate is the AI?**  
A: MomClAW uses Gemma 3 E4B-it, a state-of-the-art model. Accuracy depends on:
- Question complexity
- Model training data
- Context provided

**Q: Can I use MomClAW without internet?**  
A: Yes! Once the model is downloaded, all features work offline except web search.

### Performance

**Q: Why is it slow?**  
A: Mobile AI requires significant computation. Factors affecting speed:
- Device RAM and CPU
- Model size
- Response length
- Background apps

**Q: How much battery does it use?**  
A: Similar to other AI apps. Tips to reduce:
- Unload model when idle
- Use shorter responses
- Reduce temperature

**Q: Can I use a smaller model?**  
A: Currently, MomClAW uses one model size. Future versions may offer options.

### Privacy

**Q: Is my data private?**  
A: Yes! All data stays on your device. Nothing is sent to servers.

**Q: Do you collect analytics?**  
A: No. MomClAW has no telemetry or analytics.

**Q: Can I delete my data?**  
A: Yes. Clear conversations in app, or uninstall to remove everything.

### Features

**Q: What languages are supported?**  
A: Primarily English and Romanian. The model understands many languages with varying quality.

**Q: Can I customize the AI personality?**  
A: Yes! Use system prompts in settings to define behavior.

**Q: Does it support voice input?**  
A: Yes, if enabled in settings. Requires Android voice input service.

**Q: Can I export conversations?**  
A: Yes! Export to text or JSON from the conversation menu.

### Technical

**Q: What devices are supported?**  
A: Android 9.0+ with ARM64 or ARMv7 architecture. 4GB+ RAM recommended.

**Q: How much storage do I need?**  
A: 3GB+ free space: ~2.5GB for model, plus space for conversations.

**Q: Can I move the model to SD card?**  
A: Yes, if your device supports adoptable storage or allows app data on SD.

**Q: Is the code open source?**  
A: Yes! See [GitHub](https://github.com/serverul/MOMCLAW). Apache 2.0 license.

---

## Getting Updates

### Checking for Updates

**Google Play:**
- Automatic updates (if enabled)
- Manual: Play Store → My apps → MomClAW → Update

**GitHub:**
- Check [Releases](https://github.com/serverul/MOMCLAW/releases)
- Download and install new APK

### What's New

**Version 1.0.0:**
- Initial release
- Core AI features
- Tool integration
- Persistent memory
- Material You design

**Future Versions:**
See [MOMCLAW-PLAN.md](MOMCLAW-PLAN.md) for roadmap.

---

## Additional Resources

### Documentation

- [README.md](README.md) - Project overview
- [DOCUMENTATION.md](DOCUMENTATION.md) - Technical docs
- [CONTRIBUTING.md](CONTRIBUTING.md) - Contribution guide

### Community

- **GitHub Discussions**: Questions and general discussion
- **GitHub Issues**: Bug reports and feature requests
- **Email**: support@momclaw.app

### Learning More

**About the Model:**
- [Gemma Documentation](https://ai.google.dev/gemma)
- [LiteRT-LM](https://ai.google.dev/edge/litert)

**About MomClAW:**
- [Architecture](DOCUMENTATION.md#arhitectură)
- [Development](DEVELOPMENT.md)

---

**Thank you for using MomClAW!** 🐾

Built with ❤️ by [LinuxOnAsteroids](https://github.com/serverul)

---

**Last Updated**: 2026-04-06  
**Version**: 1.0.0
