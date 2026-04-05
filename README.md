# MOMCLAW 🐱

**Mobile Offline Model — AI Agent 100% offline pe Android**

> Inspirat de [NullClaw](https://github.com/nullclaw/nullclaw) + llama.cpp + [Private Edge Gallery](https://git.hartagis.ro/ghostul/gallery-no-tracker)
> Model: **Gemma 4E4B-it** (GGUF via llama.cpp)

## Ce face?

Un agent AI complet autonom care rulează pe telefon — zero cloud, zero tracking:
- 🧠 Conversații inteligente cu reasoning
- 🔧 Tool calls (shell, file ops, web search)
- 💾 Memorie persistentă (SQLite)
- 📱 Canale externe (Telegram, Discord) când e online
- 🔄 Sync cu instanță OpenClaw principală

## Arhitectură

```
┌─────────────────────────────────────────────┐
│            MOMCLAW Android App              │
├────────────────────────────────┬────────────┤
│ UI (Kotlin + Compose)         │ NULLCLAW   │
│ • Chat interface              │ (Zig)      │
│ • Model management            │ • Agent    │
│ • Settings & config           │ • Tools    │
├────────────────────────────────┼────────────┤
│ llama.cpp Android             │ HTTP API   │
│ • GGUF inference              │ localhost   │
│ • Gemma 4E4B optimized        │ :8080       │
│ • CPU + GPU delegate           │            │
├────────────────────────────────┴────────────┤
│ Android Foreground Service                  │
│ • Background agent                          │
│ • Telegram/Discord channels                 │
│ • OpenClaw sync                            │
└─────────────────────────────────────────────┘
```

## Tech Stack

| Componentă   | Tehnologie               |
|--------------|--------------------------|
| Android App  | Kotlin + Compose         |
| Inference    | llama.cpp (C++)          |
| Agent        | NullClaw (Zig)           |
| Model        | Gemma 4E4B-it (Q4_K_M)   |
| Memory       | SQLite                    |
| Build        | Gradle + CMake + Zig     |

## Quick Start (Development)

```bash
git clone https://github.com/serverul/momclaw.git
cd momclaw
# Android Studio → Open project → Build → Run
```

Vezi [MOMCLAW-PLAN.md](MOMCLAW-PLAN.md) pentru planul complet.

## License

Apache License 2.0 — vezi [LICENSE](LICENSE)

---
*Built with ❤️ by HartaGIS*
