# Android Assets

This directory should contain the NullClaw binary for the MomClaw agent.

## Required Files

- `nullclaw` - NullClaw agent binary (ARM64)

## Multiple Architectures (Optional)

For broader device support, include multiple binaries:
- `nullclaw-arm64` - ARM64 devices (most modern phones)
- `nullclaw-arm32` - ARM32 devices (older phones)
- `nullclaw-x86_64` - x86_64 devices (emulators, some tablets)

## How to Add

1. Build or download the NullClaw binary
2. Place it in this directory
3. The app will extract and run it on first launch

See `../../../nullclaw-fork/README.md` for build instructions.

## Fallback

If no binary is present, the app runs in LiteRT-only mode with basic chat functionality.
