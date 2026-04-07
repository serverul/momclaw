# MOMCLAW FAQ - Frequently Asked Questions

**Complete FAQ for users and developers**  
**Version:** 1.0.0  
**Last Updated:** 2026-04-06

---

## 📱 General Questions

### What is MOMCLAW?

**MOMCLAW** (Mobile Offline Model Agent) is a privacy-first AI assistant that runs entirely on your Android device. It uses the Gemma 3 E4B-it language model to provide intelligent conversations without requiring an internet connection for core features.

### Is MOMCLAW free?

Yes! MOMCLAW is:
- **Free to use** - No subscription or payment required
- **Open source** - Licensed under Apache 2.0
- **No ads** - No advertising or tracking
- **No account needed** - Just install and use

### What makes MOMCLAW different from other AI assistants?

| Feature | MOMCLAW | Other AI Assistants |
|---------|---------|---------------------|
| **Offline** | ✅ Works without internet | ❌ Requires connection |
| **Privacy** | ✅ All data on device | ❌ Data sent to cloud |
| **Account** | ✅ No account required | ❌ Account needed |
| **Tracking** | ✅ No tracking | ❌ User tracking |
| **Cost** | ✅ Free forever | ❌ Subscription fees |
| **Open Source** | ✅ Fully open | ❌ Proprietary |

### What can I use MOMCLAW for?

- 💬 **General conversations** - Chat about any topic
- 📝 **Writing assistance** - Help with emails, documents, creative writing
- 💻 **Code help** - Debugging, code explanations, suggestions
- 📚 **Learning** - Educational content, explanations, tutoring
- 🧠 **Brainstorming** - Ideas, planning, problem-solving
- 🔧 **Tool execution** - Shell commands, file operations, web search (when online)

---

## 📲 Installation

### What are the system requirements?

**Minimum Requirements:**
- **Android 9.0+** (API 28+)
- **4GB RAM** (6GB+ recommended)
- **3GB free storage** (for app + model)
- **ARM64 or ARMv7 processor**

**Recommended:**
- **Android 11+**
- **6GB+ RAM**
- **4GB free storage**
- **ARM64 processor** (better performance)

### How do I install MOMCLAW?

**From Google Play Store:**
1. Open Google Play Store
2. Search for "MOMCLAW"
3. Tap "Install"
4. Wait for installation (~25MB)
5. Open app and download model (~2GB)

**From GitHub Releases:**
1. Go to [GitHub Releases](https://github.com/serverul/MOMCLAW/releases)
2. Download the latest APK
3. Enable "Install from unknown sources" in Settings
4. Open downloaded APK and tap "Install"
5. Open app and download model

**From F-Droid:**
1. Open F-Droid
2. Search for "MOMCLAW"
3. Tap "Install"
4. Open app and download model

### The app says "App not installed". What should I do?

**Solutions:**

1. **Uninstall previous version:**
   ```bash
   adb uninstall com.loa.MOMCLAW
   ```

2. **Enable unknown sources:**
   - Settings → Security → Unknown sources → Enable

3. **Check storage:**
   - Ensure 3GB+ free space

4. **Verify architecture:**
   ```bash
   adb shell getprop ro.product.cpu.abi
   # Use matching APK (arm64-v8a, armeabi-v7a, etc.)
   ```

### Can I install MOMCLAW on an emulator?

Yes, but with limitations:

**Working:**
- Android Studio Emulator (x86_64)
- Performance is slower than physical device

**Not Working:**
- Some low-end emulators
- ARM emulators on x86 hosts (very slow)

**Recommended:**
- Use physical Android device for best experience
- If using emulator, allocate 4GB+ RAM

---

## 🧠 Model & Performance

### How do I download the model?

**Automatic (Recommended):**
1. Open MOMCLAW
2. Go to Settings → Models
3. Tap "Download Model"
4. Wait for download (~2GB)

**Manual:**
```bash
# On computer
./scripts/download-model.sh ./models

# Push to device
adb push models/gemma-4-E4B-it.litertlm \
    /sdcard/Android/data/com.loa.MOMCLAW/files/models/
```

### Why is the model so large?

The Gemma 3 E4B-it model is ~2.5GB because it contains:
- **4 billion parameters** - For intelligent responses
- **Optimized for mobile** - Quantized for efficiency
- **Multilingual support** - Multiple languages
- **General knowledge** - Broad understanding

**Note:** This is actually smaller than comparable models (GPT-3.5 has 175B parameters).

### Can I use a different model?

Currently, MOMCLAW supports:
- **Gemma 3 E4B-it** (default, recommended)
- **Gemma 2 2B-it** (smaller, faster)

**To use a different model:**
1. Download supported LiteRT-LM model
2. Place in `/sdcard/Android/data/com.loa.MOMCLAW/files/models/`
3. Select in Settings → Models

**Future:** Custom model support is planned.

### How fast is MOMCLAW?

**Typical performance:**
- **Loading model:** 10-30 seconds (first time)
- **Response generation:** 10-30 tokens/second
- **Total response time:** Varies by length

**Factors affecting speed:**
- Device CPU/GPU
- Available RAM
- Model size
- Response length

**Benchmarks:**

| Device | Speed (tokens/sec) | Load Time |
|--------|-------------------|-----------|
| Pixel 8 Pro | 25-35 | 10-15s |
| Pixel 6 | 20-28 | 15-20s |
| Galaxy S23 | 22-30 | 12-18s |
| Mid-range device | 10-18 | 20-30s |
| Low-end device | 5-12 | 30-45s |

### Why is MOMCLAW slow on my device?

**Common causes:**

1. **Low RAM:** Close background apps
2. **Slow storage:** Move app to internal storage
3. **Thermal throttling:** Let device cool down
4. **Old device:** Hardware limitations

**Solutions:**
- Close other apps
- Reduce max_tokens in settings
- Use smaller model (Gemma 2 2B-it)
- Restart device to clear memory

---

## 🔒 Privacy & Security

### Is MOMCLAW really offline?

**Yes!** Core features work completely offline:
- ✅ Chat conversations
- ✅ AI responses
- ✅ Local memory
- ✅ Settings

**Requires internet:**
- Initial model download
- Web search tool (when enabled)
- External channels (Telegram/Discord)

### What data does MOMCLAW collect?

**Nothing!** MOMCLAW collects:
- ❌ No personal data
- ❌ No usage analytics
- ❌ No crash reports (unless you opt-in)
- ❌ No device information
- ❌ No location data

**All data stays on your device:**
- Conversations stored locally
- Settings stored locally
- Model stored locally

### Where is my conversation data stored?

**Location:**
```
/sdcard/Android/data/com.loa.MOMCLAW/files/
├── database/         # Conversations
├── models/           # AI model
└── preferences/      # Settings
```

**You control your data:**
- Export conversations anytime
- Delete conversations
- Clear all data (Settings → Storage → Clear Data)

### Can I encrypt my conversations?

**Future feature:** End-to-end encryption for conversations is planned.

**Current workaround:**
- Use Android's built-in storage encryption
- Enable device encryption in Settings → Security

### Is MOMCLAW safe to use?

**Yes!** MOMCLAW is:
- **Open source:** Code is auditable
- **No network calls:** For core features
- **No tracking:** No analytics
- **Regular security audits:** Via CI/CD pipeline

**Security measures:**
- ProGuard obfuscation
- Secure coding practices
- Dependency scanning
- Regular security updates

---

## 💬 Features & Usage

### How do I start a conversation?

1. Open MOMCLAW
2. Ensure model is loaded (green indicator)
3. Type your message
4. Press send
5. Watch AI respond in real-time

### Can MOMCLAW remember previous conversations?

**Yes!** MOMCLAW has persistent memory:
- Conversations saved automatically
- View history in "History" tab
- Search through past conversations
- Resume conversations anytime

### What are "tools" in MOMCLAW?

Tools extend MOMCLAW's capabilities:

| Tool | What it does | Requires Internet |
|------|--------------|-------------------|
| **Shell** | Execute commands | No |
| **Files** | Read/write files | No |
| **Web Search** | Search the web | Yes |

**Enable/Disable:**
- Settings → Tools → Toggle tools

### Can I customize MOMCLAW's behavior?

**Yes!** Adjust settings:
- **Temperature:** Creativity (0.0-2.0)
- **Max tokens:** Response length
- **Top-k:** Vocabulary diversity
- **Top-p:** Nucleus sampling

**Settings → AI Parameters**

### Does MOMCLAW support multiple languages?

**Yes!** Gemma 3 E4B-it supports:
- English (primary)
- Romanian
- French
- German
- Spanish
- Italian
- And more...

**Note:** English works best, other languages may have lower quality.

### Can I use MOMCLAW with voice?

**Not yet.** Voice input/output is planned for a future release.

**Current workaround:**
- Use Android's voice-to-text for input
- Copy responses to TTS apps

---

## 🐛 Troubleshooting

### MOMCLAW crashes on launch

**Solutions:**

1. **Clear app cache:**
   ```bash
   adb shell pm clear com.loa.MOMCLAW
   ```

2. **Check compatibility:**
   ```bash
   adb shell getprop ro.build.version.sdk  # Must be 28+
   adb shell getprop ro.product.cpu.abi    # Must be arm64 or armeabi
   ```

3. **Reinstall:**
   ```bash
   adb uninstall com.loa.MOMCLAW
   adb install app-release.apk
   ```

4. **Check logs:**
   ```bash
   adb logcat -s MOMCLAW:E | tail -50
   ```

### "Model file not found" error

**Solution:**

```bash
# Check if model exists
adb shell ls -la /sdcard/Android/data/com.loa.MOMCLAW/files/models/

# If missing, download it
# Option 1: Via app (Settings → Models → Download)
# Option 2: Manual
./scripts/download-model.sh ./models
adb push models/gemma-4-E4B-it.litertlm \
    /sdcard/Android/data/com.loa.MOMCLAW/files/models/
```

### MOMCLAW says "Out of memory"

**Solutions:**

1. **Unload model when not in use:**
   - Settings → Models → Unload Model

2. **Close background apps**

3. **Restart device**

4. **Reduce model usage:**
   - Lower max_tokens
   - Clear old conversations

### Responses are cut off

**Cause:** max_tokens too low

**Solution:**
- Settings → AI Parameters → Max Tokens
- Increase to 2048 or higher

### MOMCLAW uses too much battery

**Solutions:**

1. **Unload model when not in use**
2. **Reduce generation frequency**
3. **Use dark theme** (saves battery on OLED)
4. **Disable background processing**

### I found a bug. How do I report it?

**Report bugs at:** [GitHub Issues](https://github.com/serverul/MOMCLAW/issues)

**Include:**
- Device model
- Android version
- MOMCLAW version
- Steps to reproduce
- Logs (if possible)

```bash
# Collect logs
adb logcat -d > momclaw_logs.txt
```

---

## 🔧 Advanced Usage

### Can I run MOMCLAW as a service?

**Yes!** MOMCLAW can run in the background:
- Settings → Background Service → Enable
- Allows Telegram/Discord integration
- Uses more battery

### How do I export my conversations?

1. Open MOMCLAW
2. Go to History
3. Long-press conversation
4. Tap "Export"
5. Choose format (JSON, TXT)

### Can I sync conversations across devices?

**Not yet.** Sync feature is planned.

**Current workaround:**
- Export conversations
- Transfer manually
- Import on other device

### Can I use MOMCLAW with Tasker/Automation apps?

**Future feature:** Intent-based automation is planned.

### How do I contribute to MOMCLAW?

**Contributions welcome!**

1. Fork repository
2. Create feature branch
3. Make changes
4. Submit pull request

See [CONTRIBUTING.md](CONTRIBUTING.md) for details.

---

## 🚀 Development

### Can I build MOMCLAW from source?

**Yes!**

```bash
# Clone repository
git clone https://github.com/serverul/MOMCLAW.git
cd MOMCLAW

# Build debug APK
./android/gradlew assembleDebug

# Build release APK (requires keystore)
./android/gradlew assembleRelease
```

See [BUILD.md](BUILD.md) for detailed instructions.

### What technologies does MOMCLAW use?

| Component | Technology |
|-----------|------------|
| **Language** | Kotlin 2.0.21 |
| **UI** | Jetpack Compose |
| **Architecture** | MVVM + Clean Architecture |
| **DI** | Hilt (Dagger) |
| **Database** | Room (SQLite) |
| **Inference** | LiteRT-LM (Google AI Edge) |
| **Model** | Gemma 3 E4B-it |
| **Server** | Ktor (Netty) |
| **Agent** | NullClaw (Zig) |

### Can I use MOMCLAW's engine in my app?

**Yes!** MOMCLAW is open source (Apache 2.0).

**Options:**
1. Fork and modify
2. Use as library (bridge module)
3. Integrate LiteRT-LM directly

### How can I help improve MOMCLAW?

**Ways to contribute:**
- 🐛 Report bugs
- 💡 Suggest features
- 📖 Improve documentation
- 🌐 Translate to other languages
- 💻 Submit pull requests
- ⭐ Star on GitHub
- 📢 Spread the word

---

## 📞 Support

### Where can I get help?

| Channel | Purpose | Response Time |
|---------|---------|---------------|
| [GitHub Issues](https://github.com/serverul/MOMCLAW/issues) | Bug reports | 1-3 days |
| [GitHub Discussions](https://github.com/serverul/MOMCLAW/discussions) | Questions | 1-2 days |
| Email: support@momclaw.app | Private inquiries | 2-5 days |

### How do I stay updated?

- ⭐ Star [GitHub repository](https://github.com/serverul/MOMCLAW)
- 👀 Watch releases
- 📧 Subscribe to newsletter (coming soon)

### Is there a community?

**Join the community:**
- GitHub Discussions
- Discord server (coming soon)
- Telegram group (coming soon)

---

## 🎯 Feature Requests

### Will MOMCLAW support [feature]?

Check [MOMCLAW-PLAN.md](MOMCLAW-PLAN.md) for roadmap.

**Planned features:**
- Voice input/output
- Multi-device sync
- Custom models
- Themes
- Plugins system
- And more!

**Request features:**
- [GitHub Discussions](https://github.com/serverul/MOMCLAW/discussions) → Ideas

---

## 📄 Legal

### What license is MOMCLAW under?

**Apache License 2.0**

- ✅ Free to use
- ✅ Free to modify
- ✅ Free to distribute
- ✅ Commercial use allowed
- ✅ Must preserve copyright notice

### Can I use MOMCLAW commercially?

**Yes!** Apache 2.0 allows commercial use.

**Requirements:**
- Include original license
- State changes made
- Don't use trademark without permission

### Who created MOMCLAW?

**LinuxOnAsteroids** (Vlad)

- GitHub: [@serverul](https://github.com/serverul)
- Built with ❤️ and open source technologies

---

## 🙏 Acknowledgments

MOMCLAW wouldn't be possible without:

- **Google AI Edge** - LiteRT-LM inference engine
- **Gemma Team** - Amazing language model
- **NullClaw** - Agent framework
- **llama.cpp** - Inference optimizations
- **Open Source Community** - Libraries and tools

---

**Still have questions?**

- 📖 Check [USER_GUIDE.md](USER_GUIDE.md)
- 🐛 Check [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
- 💬 Ask on [GitHub Discussions](https://github.com/serverul/MOMCLAW/discussions)

---

**Last Updated:** 2026-04-06  
**Version:** 1.0.0
