# MomClaw Build Scripts

Automated scripts for build, test, and deployment.

## Structure

```
scripts/
├── ci-build.sh          # CI/CD entry point — build, test, deploy
├── build-release.sh     # Release APK + AAB builds
├── run-tests.sh         # Unit + instrumented test runner
├── deploy.sh            # Google Play / F-Droid / GitHub deployment
├── setup.sh             # Initial project setup
├── version-manager.sh   # Version bumping & tagging
├── download-model.sh    # Download LiteRT AI model
├── generate-icons.sh    # Generate app icons
└── archive/             # Deprecated/duplicate scripts
```

## Quick Start

```bash
# Initial setup
./scripts/setup.sh

# Build release
./scripts/build-release.sh 1.0.0

# Run tests
./scripts/run-tests.sh

# Deploy
./scripts/deploy.sh internal
```

## Prerequisites

- **JDK 17+**
- **Android SDK** (API 28+)
- **Git**
- **Fastlane** (for Play deployment): `gem install fastlane`

## CI/CD

The `ci-build.sh` script is used by GitHub Actions CI. See `.github/workflows/ci.yml`.

## Documentation

- [BUILD.md](../docs/BUILD.md) — Build instructions
- [DEPLOYMENT.md](../docs/DEPLOYMENT.md) — Deployment guide
- [DEVELOPMENT.md](../docs/DEVELOPMENT.md) — Developer guide
