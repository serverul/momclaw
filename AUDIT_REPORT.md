# MomClaw Code Audit Report
**Date:** 2026-04-08
**Auditor:** Hermes Agent
**Repo:** github.com/serverul/momclaw

---

## Executive Summary

MomClaw is an Android AI chat app using LiteRT for on-device inference. The app has a solid foundation but suffers from **repo bloat**, **code duplication**, and **technical debt** accumulated from multi-agent development iterations.

### Key Metrics
| Metric | Value | Verdict |
|--------|-------|---------|
| Kotlin code | ~30K lines | ✅ Reasonable |
| Markdown docs | ~64K lines | 🔴 **2x more docs than code** |
| Root .md files | **136** | 🔴 Unmanageable |
| Shell scripts | **24** (17 build/validate) | 🟡 Heavy overlap |
| Kotlin source files | 133 | ✅ |
| Duplicated modules | 3 major | 🔴 |
| Hardcoded URLs | 72 | 🟡 |
| Hardcoded UI strings | 130+ | 🟡 i18n impossible |
| Empty catch blocks | Not found | ✅ |
| Security issues | Low | ✅ |

### Severity Rating: **6/10** — Functional but needs cleanup before production

---

## Issues Found

### 🔴 CRITICAL — Must Fix

#### 1. Root Directory Bloat (136 .md files)
The root directory is flooded with temporary agent completion reports, integration test summaries, and status documents from multi-agent development sprints. Most are obsolete.

**Agent-generated reports (safe to archive/delete):**
- `AGENT1_*`, `AGENT2_*`, `AGENT3_*`, `AGENT4_*` — 26 files
- `*_COMPLETION_REPORT.md`, `*_FINAL_REPORT.md` — 30+ files
- `*_INTEGRATION_TEST*.md` — 15 files
- `*_VERIFICATION*.md` — 5 files
- `*_STATUS_REPORT*.md` — 5 files

**Recommendation:** Keep only: `README.md`, `SPEC.md`, `CHANGELOG.md`, `LICENSE`, `CONTRIBUTING.md`, `SECURITY.md`, `API_DOCUMENTATION.md`, `BUILD.md`, `DEPLOYMENT.md`, `TROUBLESHOOTING.md`, `USER_GUIDE.md`, `FAQ.md`, `PRIVACY_POLICY.md`, `QUICKSTART.md`, `DEVELOPMENT.md`, `README.md` (scripts/). Move everything else to `docs/archive/`.

#### 2. Duplicate ModelsScreen (2 implementations)
Two nearly identical screen implementations exist:
- `ui/models/ModelsScreen.kt` — 506 lines
- `ui/screens/ModelsScreen.kt` — 605 lines

**Fix:** Consolidate into one file, delete the other.

#### 3. Duplicate AgentService (2 implementations)
- `service/AgentService.kt` — 197 lines (thinner wrapper)
- `agent/AgentService.kt` — 395 lines (full implementation)

**Fix:** Keep `agent/AgentService.kt` as primary, update all imports.

#### 4. Triple AgentConfig (3 definitions)
- `domain/model/AgentConfig.kt` — typealias (good pattern)
- `agent/AgentConfig.kt` — DEPRECATED (marked but still exists)
- `agent/model/AgentConfig.kt` — actual implementation

**Fix:** Remove deprecated `agent/AgentConfig.kt` after verifying no imports use it.

---

### 🟡 WARNING — Should Fix

#### 5. Hardcoded URLs (72 occurrences)
Multiple `localhost:8080`, `localhost:9090`, and HuggingFace URLs scattered across code. Should be centralized in a config/constants file.

#### 6. Hardcoded UI Strings (130+)
User-facing strings like "Initializing agent system", "Failed to load models" are hardcoded in Kotlin files instead of `strings.xml`. This breaks i18n and accessibility.

#### 7. Script Overlap (24 scripts, heavy duplication)
Groups of overlapping scripts:
- **Build:** `build-fdroid.sh`, `build-optimized.sh`, `build-release.sh`, `ci-build.sh` (4 scripts)
- **Test:** `run-tests.sh`, `run-integration-tests.sh`, `test-integration.sh`, `comprehensive-test-runner.sh` (4 scripts)
- **Validate:** 7 validation scripts with overlapping concerns

**Recommendation:** Consolidate to ~6 scripts: `build.sh`, `test.sh`, `validate.sh`, `deploy.sh`, `setup.sh`, `version.sh`

#### 8. Missing .gitignore entries
- `*.env` — not ignored (risk of committing secrets)
- `google-services.json` — not ignored (Firebase config with API keys)

#### 9. Duplicate ModelsViewModel
- `ui/models/ModelsViewModel.kt` (in models/ package)
- `ui/screens/ModelsScreenViewModel.kt` (in screens/ package)

Same duplication pattern as ModelsScreen.

---

### ℹ️ INFO — Best Practice

#### 10. No CI/CD Verification of Build
CI workflow exists but recent commits show repeated build fix attempts (`fix(ci): replace CodeQL autobuild...`, `fix(ci): secrets not available...`). Suggest verifying CI actually passes.

#### 11. Test Coverage Unknown
30+ test files exist (impressive!) but no coverage reports. Consider adding JaCoCo.

#### 12. Kotlin Version Mismatch Risk
Different `build.gradle.kts` files show `1.9.22` hardcoded. Should use `libs.versions.toml` for centralized version management.

---

## Recommended Action Plan

### Phase 1: Repo Cleanup (immediate)
1. Create `docs/archive/` and move 100+ obsolete .md files there
2. Consolidate scripts from 24 → ~6
3. Update `.gitignore` with missing patterns
4. Clean up scripts/README.md to reflect new structure

### Phase 2: Code Dedup (high priority)
1. Merge duplicate ModelsScreen + ModelsViewModel into `ui/models/`
2. Consolidate AgentService — keep `agent/` version
3. Remove deprecated AgentConfig typealias file
4. Verify no broken imports after cleanup

### Phase 3: Code Quality (medium priority)
1. Extract hardcoded URLs to `NetworkConfig.kt`
2. Move UI strings to `strings.xml`
3. Add `libs.versions.toml` for dependency management
4. Run detekt with updated config

### Phase 4: Verification
1. Run full build: `./gradlew assembleDebug`
2. Run tests: `./gradlew test`
3. Run detekt: `./gradlew detekt`
4. Verify CI pipeline passes

---

## Files to Keep at Root (Essential)
```
README.md, SPEC.md, CHANGELOG.md, LICENSE, CONTRIBUTING.md,
SECURITY.md, PRIVACY_POLICY.md, .gitignore, .gitleaks.toml,
detekt.yml
```

## Files to Move to docs/
```
API_DOCUMENTATION.md, BUILD.md, BUILD-DEPLOYMENT-GUIDE.md,
DEPLOYMENT.md, DEVELOPMENT.md, FAQ.md, QUICKSTART.md,
TESTING.md, TROUBLESHOOTING.md, USER_GUIDE.md,
PRODUCTION-READINESS.md, RELEASE_NOTES.md,
GOOGLE_PLAY_STORE.md, GITHUB_SECRETS_SETUP.md
```

## Files to Archive (docs/archive/)
All `AGENT*`, `*_REPORT.md`, `*_STATUS.md`, `*_SUMMARY.md`,
`*_COMPLETION.md`, `*_FINAL.md`, `*_VERIFICATION.md`,
`*_INTEGRATION_TEST*.md`, `*_CHECKLIST.md` files.
