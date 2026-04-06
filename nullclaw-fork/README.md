# NullClaw Binary Setup

This directory should contain the NullClaw agent binary for MOMCLAW.

## What is NullClaw?

NullClaw is the agent runtime that handles:
- Tool execution (shell commands, file operations)
- Memory management (SQLite)
- External channel integration (Telegram, Discord)
- OpenClaw synchronization

## Obtaining the Binary

### Option 1: Build from Source

```bash
# Clone NullClaw repository (when available)
git clone https://github.com/serverul/nullclaw.git
cd nullclaw

# Build for Android ARM64
zig build -Dtarget=aarch64-linux-android
zig build -Dtarget=arm-linux-android  # For 32-bit ARM

# Output will be in zig-out/bin/
```

### Option 2: Download Pre-built Binary

```bash
# Download from releases (when available)
wget https://github.com/serverul/nullclaw/releases/latest/download/nullclaw-aarch64-linux-android -O nullclaw

chmod +x nullclaw
```

## Installation

After obtaining the binary, place it in the Android app's assets:

```bash
# Copy to Android assets
cp nullclaw ../android/app/src/main/assets/

# Or for multiple architectures
cp nullclaw-arm64 ../android/app/src/main/assets/nullclaw-arm64
cp nullclaw-arm32 ../android/app/src/main/assets/nullclaw-arm32
cp nullclaw-x86_64 ../android/app/src/main/assets/nullclaw-x86_64
```

## Architecture Support

| Architecture | Android ABI | Device Examples |
|-------------|-------------|-----------------|
| ARM64 | arm64-v8a | Most modern phones |
| ARM32 | armeabi-v7a | Older devices |
| x86_64 | x86_64 | Emulators, tablets |

## Configuration

The NullClaw agent reads configuration from:
- Default: `agent_config.json` in app's files directory
- Custom: Via `ConfigGenerator.kt`

## Ports

- **Inference Bridge**: localhost:8080 (LiteRT)
- **Agent Service**: localhost:9090 (NullClaw)

## Testing

```bash
# Start agent manually for testing
adb shell
cd /data/data/com.loa.MOMCLAW/files
./nullclaw --config agent_config.json

# Check if running
curl http://localhost:9090/health
```

## Status

- [ ] NullClaw repository public
- [ ] Pre-built binaries available
- [ ] Build from source tested

## Alternative: Stub Mode

If NullClaw binary is not available, the app can still run in **LiteRT-only mode**:
- Direct inference via LiteRT Bridge
- No tool execution
- No external channels
- Basic chat functionality only

This is handled automatically by `NullClawBridge.kt` when the binary is not found.
