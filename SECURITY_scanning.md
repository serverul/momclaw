# MomClAW Security Scanning Configuration

Complete guide for security scanning and vulnerability management.

**Version**: 1.0.0
**Last Updated**: 2026-04-06

---

## Overview

MomClAW implements comprehensive security scanning as part of the development and deployment process.

### Security Scan Types

| Scan Type | Tool | Frequency | Purpose |
|----------|------|-----------|---------|
| **Dependency Check** | OWasp Dependency-Check | Every build | CVE vulnerabilities in dependencies |
| **CodeQL** | GitHub CodeQL | Every push + weekly | Code security vulnerabilities |
| **Secrets scan** | TruffleHog + Gitleaks | every push | accidentally committed secrets |
| **Security lint** | Android Lint | every build | Android security issues |
| **Dependency review** | GitHub Dependency Review | PRs | license compliance |

---

## Automated Scanning

### GitHub Actions Workflows

All security scans run automatically in CI/CD:

#### 1. Dependency Check
- **Workflow**: `.github/workflows/security.yml`
- **Trigger**: Push to main/develop + weekly schedule
- **Tool**: owasp Dependency-Check
- **Reports**: Uploaded to GitHub artifacts
- **Fail threshold**: CVSS 7+

#### 2. CodeQL Analysis
- **Workflow**: `.github/workflows/security.yml`
- **Trigger**: push to main/develop + weekly schedule
- **Languages**: Java/Kotlin
- **Queries**: security-extended
- **Reports**: GitHub Security tab

#### 3. Secrets scan
- **Workflow**: `.github/workflows/security.yml`
- **Trigger**: push to main/develop
- **Tools**: TruffleHog, Gitleaks
- **Reports**: GitHub Security tab

#### 4. Security Lint
- **Workflow**: `.github/workflows/security.yml`
- **Trigger**: push to main/develop
- **Tool**: Android Lint
- **Checks**: Security-focused

#### 5. Dependency Review
- **Workflow**: `.github/workflows/security.yml`
- **Trigger**: pull requests only
- **Tool**: GitHub Dependency Review
- **Fail threshold**: moderate severity

### Workflow Configuration

The security workflow is configured in `.github/workflows/security.yml`:

```yaml
name: Security

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]
  schedule:
    - cron: '0 0 * * 0'  # Weekly

jobs:
  dependency-check:
    # owasp dependency-check configuration
  
  codeql:
    # CodeQL analysis configuration
    
  secrets-scan:
    # TruffleHog + Gitleaks configuration
    
  lint-security:
    # Android security lint configuration
    
  dependency-review:
    # PR dependency review configuration
```

---

## Local Security Scans

### 1. Dependency Vulnerability Scan

Scan for known vulnerabilities in dependencies:

```bash
# Run dependency check
./android/gradlew dependencyCheckAnalyze

# View report
# Report location: android/app/build/reports/dependency-check/dependency-check-report.html

# Or using owasp dependency-check directly
dependency-check \
  --project MomClAW \
  --scan . \
  --format HTML \
  --out reports \
  --failOnCVSS 7
```

### 2. CodeQL Analysis

```bash
# Install CodeQL CLI
# See: https://github.com/github/codeql-cli-binaries

# Create database
codeql database create \
  --language=java-kotlin \
  --source-root=. \
  --overwrite \
  momclaw-codeql-db

# Run queries
codeql database analyze \
  momclaw-codeql-db \
  --format=csv \
  --output=codeql-results.csv \
  github/codeql/java/ql/src/experimental/Security/*
```

### 3. Secrets Scan

```bash
# TruffleHog
trufflehog git file://. --only-verified --fail

# Gitleaks
gitleaks detect \
  --source=. \
  --verbose \
  --redacted

# Or scan specific files
trufflehog filesystem ./android/app/src/main
```

### 4. Android Security Lint

```bash
# Run security-focused lint
./android/gradlew lint -Pandroid.lint.checks=Security

# View report
# Report: android/app/build/reports/lint-results.html

# Common security checks:
# - ExportedServiceBackupAgent
# - GrantRevokeOnPermissionRequest
# - UnprotectedServiceBroadcastReceiver
# - UsingHttps
# - SetJavaScriptEnabled
```

### 5. Dependency Review

```bash
# Check dependency licenses
./android/gradlew dependencyInsight

# View in Android Studio
# Analyze → Inspect Dependencies

# Check for outdated dependencies
./android/gradlew dependencyUpdates
```

---

## Vulnerability Management

### Severity Levels

| Severity | CVSS | Action |
|----------|------|--------|
| **Critical** | 9.0-10.0 | Block release, immediate fix |
| **High** | 7.0-8.9 | Fix before next release |
| **Medium** | 4.0-6.9 | Fix in upcoming release |
| **Low** | 0.0-3.9 | fix as time permits |

### Response Process

#### Critical Vulnerability

```bash
1. Assess impact
2. Check if vulnerable code is reachable
3. If reachable:
   - Check for mitigations
   - Patch or update dependency
   - Release hotfix
4. Communicate to users
 needed
```

#### High Severity

```bash
1. Assess impact
2. Schedule fix for next release
3. Document in CHANGELOG
4. Notify security team
```

#### Medium/Low Severity

```bash
1. Add to backlog
2. Fix in routine update cycle
3. Document in commit message
```

### Vulnerability Tracking

All vulnerabilities are tracked in:

1. **GitHub Security** - Automated CodeQL alerts
2. **GitHub Issues** - Manual vulnerability reports
3. **Security advisories** - From dependency maintainers
4. **CVE database** - Continuous monitoring

---

## Security Best Practices

### 1. Dependency Management

```kotlin
// build.gradle.kts

dependencies {
    // Use dependency constraints
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    
    // Prefer stable versions
    implementation("androidx.core:core-ktx:1.13.1")
    
    // Avoid alpha/beta dependencies in production
}
```

### 2. ProGuard Configuration

```pro
# proguard-rules.pro

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}

# Keep sensitive data
-keep class com.loa.momclaw.BuildConfig {
    public static *** API_KEY;
}

# Obfuscate internal classes
-repackageclasses 'a'
```

### 3. Network Security

```kotlin
// Use HTTPS for all connections
<application
    android:usesCleartextTraffic="true"
    tools:targetApiLevel="31">
    
    <!-- Network security config -->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">api.example.com</domain>
    </domain-config>
</application>
```

### 4. Data Storage

```kotlin
// Use EncryptedSharedPreferences for sensitive data
val masterKey = MasterKey.Builder(application)
    .setKeyScheme(MasterKeyScheme.AES256_GCM)
    .setKeyAlias("momclaw_master_key")
    .build()

val sharedPreferences = EncryptedSharedPreferences.create(
    context,
    "secret_shared_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV
)
```

### 5. Certificate Pinning

```kotlin
// Use Certificate Pinning for API calls
val certificatePinner = CertificatePinner.Builder()
    .add("api.example.com", "sha256/AbCdEf...")
    .build()

val okHttpClient = OkHttpClient.Builder()
    .certificatePinner(certificatePinner)
    .build()
```

---

## Security Checklist

### Pre-release

- [ ] All dependencies up to date
- [ ] No critical vulnerabilities
- [ ] no high-severity vulnerabilities
- [ ] ProGuard rules tested
 - [ ] Secrets scanned
 - [ ] Security lint clean
- [ ] HTTPS used for all connections
- [ ] Sensitive data encrypted
- [ ] Certificate pinning configured
 - [ ] Logging disabled in release

### Regular audits

 - [ ] Monthly dependency vulnerability scan
- [ ] weekly security lint
- [ ] quarterly CodeQL scan
- [ ] continuous monitoring of security advisories
- [ ] review and update ProGuard rules
- [ ] audit third-party libraries

---

## Security Contacts

### Reporting vulnerabilities

If you discover a security vulnerability, please report it responsibly:

**Email**: security@momclaw.app

**Include**:
- Vulnerability description
- Steps to reproduce
- Affected versions
- Potential impact
- Suggested fix (if any)

### Response time

- **Critical**: 24 hours
- **High**: 7 days
- **Medium**: 14 days
- **Low**: 30 days

---

## Additional Resources

### Security documentation

- [SECURITY.md](SECURITY.md) - Security policy
- [PRivacy_policy.md](privacy_policy.md) - Privacy policy
- [proguard-rules.pro](android/app/proguard-rules.pro) - ProGuard configuration

### External links

- [owasp Dependency-Check](https://owasp.org/www-project-dependency-check/)
- [CodeQL](https://codeql.github.com/)
- [TruffleHog](https://github.com/trufflesecurity/trufflehog)
- [gitleaks](https://github.com/gitleaks/gitleaks)
- [Android Security](https://developer.android.com/topic/security)

---

**This document is maintained alongside the codebase. Last updated: 2026-04-06**
