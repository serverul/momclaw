# MOMCLAW — Mobile Offline Model 🐱

> Agent AI complet autonom, 100% offline, pe Android.
> Inspirat de NullClaw + Private Edge Gallery.
> Model: Gemma 4E4B (GGUF via llama.cpp).

---

## 🎯 Scop

Un agent AI care rulează nativ pe telefonul Android — zero cloud, zero API keys, zero tracking. Poate:
- Conversații inteligente cu reasoning
- Tool calls (shell, file ops, web search)
- Memorie persistentă (SQLite)
- Canale externe (Telegram, Discord) când e online
- Sync opțional cu o instanță OpenClaw principală

## 🏗️ Arhitectură

```
┌─────────────────────────────────────────────┐
│            MOMCLAW Android App              │
├──────────────────────────────────────┬──────┤
│ UI (Kotlin + Compose)              │ AGENT│
│ • Chat interface                   │      │
│ • Model management                 │ Null │
│ • Settings & config                │ Claw │
│ • Prompt Lab                       │ (Zig)│
├────────────────────────────────────┼──────┤
│ llama.cpp                          │ Null │
│ • GGUF inference                   │ Claw │
│ • Gemma 4E4B optimized             │ HTTP │
│ • NNCPU + GPU delegate             │ API  │
├────────────────────────────────────┼──────┤
│ Model Downloader                   │      │
│ • HF Hub integration               │ Mem  │
│ • GGUF format only                 │ SQLite│
├─────────────────────────────────────────────┤
│ Android Services                            │
│ • Background agent (foreground svc)         │
│ • Network channels (Telegram/Discord)       │
│ • Sync with main OpenClaw instance          │
└─────────────────────────────────────────────┘
```

## 📦 Componente

### 1. llama.cpp Android Library
- Build nativ pentru ARM64 + x86_64
- Gemma 4E4B GGUF optimizat (4-bit sau 8-bit)
- NPU delegate (Hexagon NNAPI) dacă există
- ~2-3 tok/sec pe CPU mid-range, ~8-15 tok/sec pe flagship

### 2. NullClaw Agent Bridge
- NullClaw compilat ca binary static ARM64
- Comunicație cu llama.cpp prin HTTP API local
- Tool calls, memory, channels, cron jobs
- ~1MB RAM overhead

### 3. Android UI
- Inspirat din Private Edge Gallery
- Chat interface cu streaming
- Model manager (download/load/switch)
- Prompt Lab pentru testing
- Settings pentru agent config

### 4. Background Agent
- Foreground service pentru autonomie
- Răspunde la mesaje chiar când app e închis
- Canale: Telegram (primary), Discord (opțional)
- Cron jobs pentru task-uri automate

## 🗂️ Structura GitHub Repo

```
momclaw/
├── android/           # App Kotlin + Compose
│   ├── app/
│   ├── core/          # llama.cpp JNI wrapper
│   ├── agent/         # NullClaw bridge
│   └── ui/
├── nullclaw-fork/     # Fork cu modificări MOMCLAW
│   ├── src/providers/llamacpp.zig  # Provider nou
│   └── config.json.example
├── native/
│   └── llama.cpp/     # Submodule + build scripts
├── models/
│   └── download/      # Scripts pre-GGUF
├── docs/
│   ├── ARCHITECTURE.md
│   ├── BUILD.md
│   └── DEVELOPMENT.md
├── .github/
│   ├── workflows/
│   └── ISSUE_TEMPLATES/
├── README.md
├── LICENSE (Apache 2.0)
└── momclaw-logo.png
```

## 🚀 Plan de Dezvoltare

### Phase 1: Foundation (Săptămâna 1-2)
- [ ] Repo GitHub public + setup CI/CD
- [ ] Build llama.cpp pe Android (ARM64 + x86_64)
- [ ] Testare cu model GGUF mic (2B params)
- [ ] NullClaw fork + provider llama.cpp
- [ ] Config.json de bază

### Phase 2: Agent Core (Săptămâna 3-4)
- [ ] Chat streaming funcțional
- [ ] Tool calls (shell, file ops)
- [ ] Memory SQLite
- [ ] Background service

### Phase 3: Android App (Săptămâna 5-6)
- [ ] UI Chat (inspirat Edge Gallery)
- [ ] Model Manager (HF Hub download)
- [ ] Prompt Lab
- [ ] Settings

### Phase 4: Channels & Sync (Săptămâna 7-8)
- [ ] Telegram bot integration
- [ ] Discord webhook
- [ ] Sync cu OpenClaw principal
- [ ] Cron jobs mobile

### Phase 5: Polish & Release (Săptămâna 9-10)
- [ ] Testing pe dispozitive reale
- [ ] Optimizare performanță
- [ ] APK Release
- [ ] Documentation

## 🛠️ Tech Stack

| Componentă        | Tehnologie                   |
|-------------------|------------------------------|
| Android App       | Kotlin + Jetpack Compose     |
| Inference         | llama.cpp (C++)              |
| Agent Framework   | NullClaw (Zig)               |
| Model             | Gemma 4E4B-it (GGUF Q4_K_M)  |
| Memory            | SQLite                       |
| Build System      | Gradle + CMake + Zig build   |
| CI/CD             | GitHub Actions               |
| Package Name      | `ro.hartagis.momclaw`        |

## 💡 Decizii Tehnice

### De ce llama.cpp în loc de LiteRT?
- Suport GGUF nativ → modele HuggingFace direct
- Cross-platform → același cod pe Android/iOS/Linux
- Comunitate mare → update-uri rapide
- Deja optimizat pentru ARM + NPU

### De ce NullClaw în loc de OpenClaw?
- 678KB binary vs ~28MB
- ~1MB RAM vs >1GB
- Arhitectură vtable extensibilă
- 50+ provideri deja
- 5,300+ teste

### De ce Gemma 4E4B?
- 4B params = ~2.5GB GGUF Q4_K_M
- Apache 2.0 → comercial ok
- Performanță bună pentru 4B
- Suport thinking mode

## 📋 Primii Pași

1. **Create GitHub repo** → `hartagis/momclaw`
2. **Setup basic structure** → README + LICENSE + .gitignore
3. **llama.cpp submodule** + Android CMake config
4. **NullClaw fork** + `llamacpp.zig` provider
5. **Test chain** → llama.cpp → NullClaw → Chat response

---

*Plan inițial — 5 Apr 2026*
