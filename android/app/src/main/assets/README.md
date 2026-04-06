# NullClaw Binary Integration

This directory should contain the NullClaw agent binaries for different Android ABIs.

## Required Files

Place the following files here before building:

```
assets/
├── nullclaw-arm64     # ARM64 (most modern devices)
├── nullclaw-arm32     # ARM32 (older 32-bit devices)
├── nullclaw-x86_64    # x86_64 (emulators, some tablets)
└── nullclaw-x86       # x86 (older emulators)
```

## Building NullClaw Binary

NullClaw is written in Zig. To build:

```bash
# Clone NullClaw repository (when available)
git clone https://github.com/yourorg/nullclaw
cd nullclaw

# Build for Android ARM64
zig build -Dtarget=aarch64-linux-android -Doptimize=ReleaseSmall

# Build for other ABIs
zig build -Dtarget=arm-linux-android -Doptimize=ReleaseSmall
zig build -Dtarget=x86_64-linux-android -Doptimize=ReleaseSmall
zig build -Dtarget=x86-linux-android -Doptimize=ReleaseSmall
```

## Binary Requirements

The NullClaw binary must:
1. Be statically linked (no external dependencies)
2. Support these command-line flags:
   - `--config <path>`: Configuration file path
   - `gateway`: Run gateway mode
   - `--mode local`: Local mode (no network exposure)
   - `--bind loopback`: Bind to loopback interface only
   - `--port 9090`: Gateway port

3. Expose these HTTP endpoints:
   - `GET /health`: Health check (returns 200 OK if healthy)
   - `GET /status`: Detailed status (JSON)
   - `POST /chat`: Chat completions endpoint

4. Read configuration from JSON file at startup

## Configuration Format

NullClaw expects a JSON configuration file:

```json
{
  "agents": {
    "defaults": {
      "model": {
        "primary": "litert-bridge/gemma-4e4b"
      },
      "system_prompt": "You are a helpful assistant."
    }
  },
  "models": {
    "providers": {
      "litert-bridge": {
        "type": "custom",
        "base_url": "http://localhost:8080",
        "api_format": "openai"
      }
    }
  },
  "gateway": {
    "mode": "local",
    "bind": "loopback",
    "port": 9090
  }
}
```

## Testing Without Binary

For development/testing, the bridge will create a stub script if no binary is found:
- The stub logs messages but provides no actual agent functionality
- This allows testing the LiteRT bridge independently
- Full functionality requires the actual NullClaw binary

## Download Pre-built Binaries

When available, pre-built binaries can be downloaded from:
- GitHub Releases: https://github.com/yourorg/nullclaw/releases

## Binary Size Targets

Optimized builds should target these sizes:
- ARM64: ~5-10 MB
- ARM32: ~4-8 MB
- x86_64: ~6-12 MB

## Security Considerations

1. Binary is extracted to app-private storage (not accessible to other apps)
2. Binary runs with app's UID (sandboxed)
3. Network binding is loopback-only (no external access)
4. Binary is made executable only for the app user
