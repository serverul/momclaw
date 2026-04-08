# GitHub Issues to Create

Use `gh issue create` or create manually via GitHub UI.

---

## Issue 1: [Cleanup] Root directory bloated with 136 agent-generated reports
**Labels:** cleanup, documentation, priority:high

### Problem
The root directory contained 136 .md files — temporary agent completion reports, integration test summaries, and status documents from multi-agent development sprints. Most were obsolete.

### What was done
- Moved 36 useful docs to `docs/`
- Archived 94 obsolete reports to `docs/archive/`
- Root reduced from 137 → 7 essential .md files

### Remaining
- [ ] Review archived reports — delete if truly obsolete
- [ ] Update README.md to reflect new doc structure
- [ ] Update DOCUMENTATION-INDEX.md (now in docs/)

---

## Issue 2: [Cleanup] 24 shell scripts with heavy overlap consolidated to 8
**Labels:** cleanup, ci/cd, priority:high

### Problem
scripts/ had 24 shell scripts with overlapping functionality:
- 4 build variants (build-fdroid, build-optimized, build-release, ci-build)
- 4 test runners (run-tests, run-integration-tests, test-integration, comprehensive-test-runner)
- 7 validation scripts with overlapping concerns

### What was done
- Archived 16 overlapping scripts to `scripts/archive/`
- Kept 8 essential scripts: setup, ci-build, run-tests, build-release, deploy, version-manager, download-model, generate-icons

### Remaining
- [ ] Verify CI still works with consolidated scripts
- [ ] Update scripts/README.md

---

## Issue 3: [Bug] Duplicate ModelsScreen — dead code in ui/models/
**Labels:** bug, duplicate-code, priority:high

### Problem
Two nearly identical ModelsScreen implementations:
- `ui/models/ModelsScreen.kt` (506 lines) — **NOT imported anywhere**
- `ui/screens/ModelsScreen.kt` (605 lines) — used by NavGraph

Same issue with ModelsViewModel:
- `ui/models/ModelsViewModel.kt` — dead code
- `ui/screens/ModelsScreenViewModel.kt` — used by NavGraph

### What was done
- Archived dead `ui/models/ModelsScreen.kt` and `ui/models/ModelsViewModel.kt`
- The active `ui/screens/` versions are untouched

---

## Issue 4: [Bug] Triple AgentConfig definition
**Labels:** bug, duplicate-code, priority:medium

### Problem
Three different AgentConfig definitions:
1. `agent/model/AgentConfig.kt` — **canonical implementation** ✅
2. `agent/AgentConfig.kt` — marked @Deprecated, typealias to #1
3. `domain/model/AgentConfig.kt` — typealias to #1

### What was done
- Archived deprecated `agent/AgentConfig.kt`
- Kept canonical `agent/model/AgentConfig.kt`
- Kept domain typealias (good pattern for app module access)

---

## Issue 5: [Refactor] Extract hardcoded URLs to NetworkConfig
**Labels:** refactor, priority:medium

### Problem
72 hardcoded URLs scattered across codebase:
- `http://localhost:8080` (multiple files)
- `http://localhost:9090` (AgentClient.kt)
- `https://huggingface.co/api/` (HuggingFaceApi.kt)

### Recommendation
Create a single `NetworkConfig.kt`:
```kotlin
object NetworkConfig {
    const val DEFAULT_BASE_URL = "http://localhost:8080"
    const val AGENT_URL = "http://localhost:9090"
    const val HUGGINGFACE_API = "https://huggingface.co/api/"
    const val HUGGINGFACE_FILES = "https://huggingface.co/"
}
```

---

## Issue 6: [Refactor] Move hardcoded UI strings to strings.xml
**Labels:** refactor, i18n, priority:medium

### Problem
130+ hardcoded user-facing strings in Kotlin files:
- "Initializing agent system"
- "Failed to load models"
- "Download complete"
- etc.

This breaks i18n and accessibility.

### Recommendation
Extract to `res/values/strings.xml` and use `context.getString(R.string.xxx)` or stringResource().

---

## Issue 7: [Cleanup] Android module report files archived
**Labels:** cleanup, priority:low

### What was done
- Archived 12 report .md files from `android/` subdirectories
- Kept only README.md in each module
- Bridge module: archived LITERT_IMPLEMENTATION.md

---

## Issue 8: [Security] Update .gitignore with missing patterns
**Labels:** security, priority:medium

### What was done
- Added `*.env` pattern (was missing)
- Added `google-services.json` (Firebase config with API keys)
- Deduplicated existing entries (.DS_Store, *.iml, *.jks appeared twice)
- Cleaned up redundant patterns

---

## Issue 9: [Investigate] Verify CI pipeline actually passes
**Labels:** ci/cd, priority:high

### Problem
Recent commits show repeated CI fix attempts:
- `fix(ci): replace CodeQL autobuild with manual Android build`
- `fix(ci): secrets not available in if expressions - use env vars`

### Recommendation
- Run full build locally: `cd android && ./gradlew assembleDebug`
- Run tests: `./gradlew test`
- Verify CI secrets are configured in GitHub repo settings

---

## Issue 10: [Refactor] Centralize dependency versions
**Labels:** refactor, build, priority:low

### Problem
Different `build.gradle.kts` files hardcode `kotlin("1.9.22")` independently.

### Recommendation
Use `gradle/libs.versions.toml` for centralized version management.
