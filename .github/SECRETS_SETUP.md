# GitHub Secrets Setup Guide

This guide explains how to configure GitHub Secrets for MOMCLAW CI/CD workflows.

---

## Required Secrets

### For Release Builds

These secrets are required for building and signing release APKs/AABs.

#### 1. KEYSTORE_BASE64

Your signing keystore encoded as base64.

**Generate:**
```bash
# From MOMCLAW root directory
base64 -w 0 MOMCLAW-release-key.jks > keystore_base64.txt

# Copy the content
cat keystore_base64.txt
```

**Add to GitHub:**
1. Go to repository Settings → Secrets and variables → Actions
2. Click "New repository secret"
3. Name: `KEYSTORE_BASE64`
4. Value: Paste the base64-encoded keystore
5. Click "Add secret"

#### 2. STORE_PASSWORD

The password for your keystore file.

**Add to GitHub:**
1. Name: `STORE_PASSWORD`
2. Value: Your keystore password
3. Click "Add secret"

#### 3. KEY_PASSWORD

The password for your signing key (can be same as STORE_PASSWORD).

**Add to GitHub:**
1. Name: `KEY_PASSWORD`
2. Value: Your key password
3. Click "Add secret"

#### 4. KEY_ALIAS

The alias of your signing key.

**Add to GitHub:**
1. Name: `KEY_ALIAS`
2. Value: `MOMCLAW` (or your chosen alias)
3. Click "Add secret"

---

### For Google Play Deployment

These secrets are required for deploying to Google Play Store.

#### 5. GOOGLE_PLAY_SERVICE_ACCOUNT

JSON key for Google Play Console API access.

**Generate:**
1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Select or create a project
3. Navigate to IAM & Admin → Service Accounts
4. Click "Create Service Account"
5. Enter name: "MOMCLAW Play Console"
6. Add roles:
   - Service Account User
   - Android Management API User
7. Click "Create Key" → JSON
8. Download the JSON file

**Add to GitHub:**
1. Name: `GOOGLE_PLAY_SERVICE_ACCOUNT`
2. Value: Paste the entire JSON content
3. Click "Add secret"

**Configure Play Console:**
1. Go to [Google Play Console](https://play.google.com/console)
2. Select your app
3. Go to Setup → API access
4. Link your Cloud project
5. Grant access to the service account

---

### For F-Droid Builds

These secrets are required for building and signing F-Droid APKs.

#### 6. GPG_PRIVATE_KEY

Your GPG private key for signing F-Droid APKs.

**Generate:**
```bash
# Generate GPG key (if you don't have one)
gpg --full-generate-key
# Select: RSA and RSA, 4096 bits, key does not expire
# Enter your name and email

# Export private key
gpg --armor --export-secret-keys YOUR_EMAIL > gpg_private_key.asc

# Copy the content
cat gpg_private_key.asc
```

**Add to GitHub:**
1. Name: `GPG_PRIVATE_KEY`
2. Value: Paste the entire ASCII-armored key
3. Click "Add secret"

**Publish to keyserver:**
```bash
# Get your key ID
gpg --list-secret-keys --keyid-format LONG

# Publish to keyserver
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
```

---

### For Notifications (Optional)

#### 7. DISCORD_WEBHOOK_ID

Discord webhook ID for release notifications.

**Setup:**
1. In Discord, go to channel settings → Integrations → Webhooks
2. Create a new webhook
3. Copy the webhook URL (format: `https://discord.com/api/webhooks/ID/TOKEN`)
4. Extract the ID (first number in URL)

**Add to GitHub:**
1. Name: `DISCORD_WEBHOOK_ID`
2. Value: Your webhook ID
3. Click "Add secret"

#### 8. DISCORD_WEBHOOK_TOKEN

Discord webhook token.

**Setup:**
From the same webhook URL, extract the TOKEN (second part after ID).

**Add to GitHub:**
1. Name: `DISCORD_WEBHOOK_TOKEN`
2. Value: Your webhook token
3. Click "Add secret"

---

### For Advanced Security Scanning (Optional)

#### 9. GITLEAKS_LICENSE

License key for Gitleaks (if using licensed version).

**Add to GitHub:**
1. Name: `GITLEAKS_LICENSE`
2. Value: Your license key
3. Click "Add secret"

---

## How to Add Secrets

### Via GitHub Web UI

1. Go to your repository on GitHub
2. Click **Settings** tab
3. In left sidebar, click **Secrets and variables** → **Actions**
4. Click **New repository secret**
5. Enter the name and value
6. Click **Add secret**
7. Repeat for each secret

### Via GitHub CLI

```bash
# Install GitHub CLI if not already installed
# https://cli.github.com/

# Login to GitHub
gh auth login

# Add secrets
gh secret set KEYSTORE_BASE64 < keystore_base64.txt
gh secret set STORE_PASSWORD -b"YOUR_PASSWORD"
gh secret set KEY_PASSWORD -b"YOUR_PASSWORD"
gh secret set KEY_ALIAS -b"MOMCLAW"
gh secret set GOOGLE_PLAY_SERVICE_ACCOUNT < service-account.json
gh secret set GPG_PRIVATE_KEY < gpg_private_key.asc
```

---

## Verifying Secrets

### Check if secrets are set

Secrets are encrypted and cannot be viewed after being set. To verify:

1. Go to Settings → Secrets and variables → Actions
2. You'll see the list of secret names (values are hidden)
3. Check that all required secrets are present

### Test in workflow

Add a test step to verify secrets (values will be masked):

```yaml
- name: Verify secrets
  run: |
    if [ -n "${{ secrets.KEYSTORE_BASE64 }}" ]; then
      echo "✅ KEYSTORE_BASE64 is set"
    else
      echo "❌ KEYSTORE_BASE64 is missing"
      exit 1
    fi
```

---

## Security Best Practices

### ✅ DO

- Use strong, unique passwords for keystores
- Rotate secrets periodically (every 6-12 months)
- Limit repository access to trusted collaborators
- Use environment-specific secrets (e.g., `prod_`, `staging_`)
- Enable branch protection rules
- Review workflow runs for suspicious activity

### ❌ DON'T

- Never commit secrets to the repository
- Never share secrets via chat or email
- Never print secrets in workflow logs (they'll be masked, but still risky)
- Don't use the same keystore for multiple apps
- Don't skip secret rotation

---

## Troubleshooting

### "Keystore not found" error

1. Verify `KEYSTORE_BASE64` secret exists
2. Check that base64 encoding is correct:
   ```bash
   # Decode and verify
   echo $KEYSTORE_BASE64 | base64 -d | file -
   # Should output: "Java KeyStore"
   ```

### "Google Play deployment failed"

1. Verify `GOOGLE_PLAY_SERVICE_ACCOUNT` JSON is valid
2. Check service account has proper permissions
3. Ensure app is linked in Play Console
4. Verify track name (internal/alpha/beta/production)

### "GPG signing failed"

1. Verify `GPG_PRIVATE_KEY` is properly formatted
2. Check key is not expired
3. Verify key is published to keyserver
4. Ensure key has signing capability

---

## Need Help?

- GitHub Secrets Docs: https://docs.github.com/en/actions/security-guides/encrypted-secrets
- Google Play API Docs: https://developers.google.com/android-publisher
- GPG Manual: https://www.gnupg.org/documentation/manuals/gnupg/

---

**Last Updated**: 2026-04-06
