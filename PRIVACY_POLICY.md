# MOMCLAW Privacy Policy

**Last Updated: April 6, 2026**

---

## Summary

**MOMCLAW is designed from the ground up for privacy.** All AI processing happens locally on your device. Your data never leaves your phone unless you explicitly choose to enable external features.

---

## 1. Data Collection

### What We DO NOT Collect

MOMCLAW does **NOT** collect, transmit, or store:

- ❌ Personal information
- ❌ Chat messages or conversations
- ❌ AI prompts or responses
- ❌ Usage analytics
- ❌ Device identifiers
- ❌ Location data
- ❌ Contact information
- ❌ Files or documents

### What Stays On Your Device

All of the following remain **100% on your device**:

- ✅ Chat history (stored in local SQLite database)
- ✅ AI model and inference data
- ✅ Settings and preferences
- ✅ Downloaded models
- ✅ Conversation context

---

## 2. AI Processing

### Local-First Architecture

MOMCLAW uses **LiteRT-LM** for on-device AI inference:

- AI model (Gemma 3 E4B-it) runs entirely on your device
- No cloud API calls for core features
- No data sent to external servers for processing
- Inference happens in your device's memory

### Model Data

- **Model Location:** `/sdcard/Android/data/com.loa.MOMCLAW/files/models/`
- **Model Size:** ~2.5GB (downloaded once)
- **Network:** No internet required for inference

---

## 3. Network Features (Optional)

MOMCLAW includes optional features that require network access:

### Telegram Integration

- **When Enabled:** Only if you configure Telegram bot token
- **Data Sent:** Messages you send through Telegram to the bot
- **Purpose:** Receive responses from your MOMCLAW instance
- **Controlled By:** You (disabled by default)

### Discord Integration

- **When Enabled:** Only if you configure Discord bot token
- **Data Sent:** Messages you send through Discord to the bot
- **Purpose:** Receive responses from your MOMCLAW instance
- **Controlled By:** You (disabled by default)

### OpenClaw Sync

- **When Enabled:** Only if you configure sync with external OpenClaw instance
- **Data Sent:** Conversation data you choose to sync
- **Purpose:** Synchronize conversations between devices
- **Controlled By:** You (disabled by default)

**These features are DISABLED by default.** You must explicitly enable and configure them.

---

## 4. Permissions

MOMCLAW requests the following Android permissions:

| Permission | Purpose | Required |
|-----------|---------|----------|
| `INTERNET` | Optional Telegram/Discord/OpenClaw features | No (for core features) |
| `FOREGROUND_SERVICE` | Background agent execution | Yes |
| `POST_NOTIFICATIONS` | Status notifications | No |
| `READ_EXTERNAL_STORAGE` | Load custom models | No |
| `WRITE_EXTERNAL_STORAGE` | Save conversation exports | No |

---

## 5. Data Storage

### Local Database

- **Technology:** Room Database (SQLite)
- **Location:** `/data/data/com.loa.MOMCLAW/databases/`
- **Encrypted:** No (stored in app-private storage)
- **Accessible:** Only by MOMCLAW app

### Data Retention

- **Conversations:** Stored until you delete them
- **Settings:** Stored until app uninstall
- **Models:** Stored in external storage until you delete them

### Data Deletion

You can delete your data at any time:

1. **Delete Conversations:** In app → Settings → Clear Chat History
2. **Delete All Data:** Android Settings → Apps → MOMCLAW → Clear Data
3. **Uninstall:** Removes all app data automatically

---

## 6. Third-Party Components

### AI Model

- **Name:** Gemma 3 E4B-it
- **Provider:** Google
- **License:** Apache 2.0
- **Privacy:** Runs entirely on-device
- **More Info:** https://ai.google.dev/gemma

### Open Source Libraries

MOMCLAW uses open-source libraries. See `LICENSE` and `NOTICE` files for details.

---

## 7. Security

### App Security

- **Code:** Open source and auditable
- **Signing:** Release builds are signed
- **Updates:** Distributed via Google Play, F-Droid, or GitHub Releases

### Recommendations

- Download MOMCLAW only from official sources
- Keep the app updated
- Review permissions periodically
- Use a secure screen lock

---

## 8. Children's Privacy

MOMCLAW does not knowingly collect any data from anyone, including children under 13. Since we don't collect any data at all, our service is suitable for all ages.

---

## 9. Changes to Privacy Policy

We may update this privacy policy from time to time. Changes will be:

- Posted on GitHub: https://github.com/serverul/MOMCLAW/blob/main/PRIVACY_POLICY.md
- Announced in release notes
- Dated at the top of this document

---

## 10. Contact

### Questions or Concerns?

- **GitHub Issues:** https://github.com/serverul/MOMCLAW/issues
- **GitHub Discussions:** https://github.com/serverul/MOMCLAW/discussions
- **Email:** privacy@example.com (TODO)

### Source Code

The complete source code is available at:
https://github.com/serverul/MOMCLAW

---

## 11. Open Source License

MOMCLAW is licensed under the Apache License 2.0. See [LICENSE](LICENSE) for details.

---

**Your privacy is our priority.** MOMCLAW is built to keep your AI conversations private and on your device.

---

*This privacy policy is effective as of April 6, 2026.*
