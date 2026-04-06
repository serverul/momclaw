# Security Policy

**MomClaw** - Privacy-first offline AI agent

---

## Supported Versions

We release patches for security vulnerabilities for the following versions:

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | ✅ Active support  |
| < 1.0   | ❌ Pre-release     |

---

## Reporting a Vulnerability

We take security seriously. If you discover a security vulnerability, please follow responsible disclosure.

### How to Report

**DO NOT** open a public issue for security vulnerabilities.

Instead, please:

1. **Email:** security@example.com (TODO: Set up security email)
   
2. **GitHub Security Advisory (Preferred):**
   - Go to https://github.com/serverul/momclaw/security/advisories
   - Click "Report a vulnerability"
   - Fill in the details

### What to Include

- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Any possible mitigations (optional)
- Your contact information for follow-up

### Response Timeline

- **Initial Response:** Within 48 hours
- **Status Update:** Within 7 days
- **Fix Timeline:** Depends on severity
  - Critical: 7 days
  - High: 14 days
  - Medium: 30 days
  - Low: 60 days

### Disclosure Policy

- We follow **Coordinated Vulnerability Disclosure**
- We'll work with you to understand and fix the issue
- We'll credit you in the security advisory (unless you prefer to remain anonymous)
- Please don't disclose publicly until a fix is released

---

## Security Best Practices

### For Users

1. **Download from Official Sources Only**
   - Google Play Store
   - F-Droid
   - GitHub Releases
   - Avoid third-party APK sites

2. **Keep Updated**
   - Install updates promptly
   - Enable automatic updates

3. **Model Safety**
   - Only download models from trusted sources
   - Verify model checksums when available

4. **Permissions**
   - Review requested permissions
   - Only grant what's necessary

5. **Network Features**
   - Telegram/Discord integration are disabled by default
   - Only enable if you trust your network
   - Use secure channels for sensitive conversations

### For Developers

1. **Code Review**
   - All changes require PR review
   - Security-sensitive code flagged for extra scrutiny

2. **Dependencies**
   - Regular dependency updates via Dependabot
   - Security audits for new dependencies

3. **Secrets Management**
   - Never commit secrets to repository
   - Use GitHub Secrets for CI/CD
   - Follow `.gitignore` rules

4. **Static Analysis**
   - Detekt for Kotlin analysis
   - Android Lint for security issues
   - CodeQL for vulnerability scanning

5. **Build Security**
   - Release builds are signed
   - Reproducible builds for verification
   - ProGuard/R8 obfuscation enabled

---

## Known Security Considerations

### 1. Local Data Storage

- **Risk:** Chat history stored in local SQLite database
- **Mitigation:** Database is in app-private storage (not accessible without root)
- **Recommendation:** Use device encryption; avoid storing sensitive info

### 2. Model Downloads

- **Risk:** Downloading models from untrusted sources
- **Mitigation:** In-app downloader uses HTTPS with certificate pinning
- **Recommendation:** Verify model source before download

### 3. Network Features

- **Risk:** Telegram/Discord integration sends messages over network
- **Mitigation:** Features are opt-in, disabled by default
- **Recommendation:** Only enable if you understand the implications

### 4. Agent Execution

- **Risk:** Agent can execute shell commands and file operations
- **Mitigation:** Runs in app sandbox with limited permissions
- **Recommendation:** Review tool permissions in settings

---

## Security Features

MomClaw includes several security features:

### Privacy-First Design

- ✅ All AI processing happens on-device
- ✅ No telemetry or analytics
- ✅ No cloud connections for core features
- ✅ Data never leaves device without explicit action

### App Security

- ✅ Signed release builds
- ✅ ProGuard/R8 code shrinking and obfuscation
- ✅ Network Security Configuration (HTTPS only)
- ✅ Backup disabled by default

### CI/CD Security

- ✅ Dependabot for dependency updates
- ✅ CodeQL for vulnerability scanning
- ✅ Trufflehog for secrets detection
- ✅ Dependency review on PRs

---

## Security Audits

### Automated Scanning

- **CodeQL:** Runs on every PR
- **Dependabot:** Weekly dependency checks
- **Trufflehog:** Secret scanning
- **Android Lint:** Security lint checks

### Manual Audits

No formal third-party security audit has been conducted yet.

If you'd like to conduct a security audit, please contact us.

---

## Security Changelog

| Date | Issue | Severity | Status |
|------|-------|----------|--------|
| - | No security issues reported | - | - |

---

## Contact

- **Security Issues:** security@example.com (TODO)
- **General Issues:** https://github.com/serverul/momclaw/issues
- **GitHub Security:** https://github.com/serverul/momclaw/security

---

## Credits

We appreciate responsible disclosure from security researchers. Contributors will be acknowledged here (with permission).

---

**Last Updated:** 2026-04-06

*This security policy will be updated as the project evolves.*
