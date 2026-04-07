# GitHub Secrets Setup pentru MomClAW

Acest document descrie toate secretele necesare pentru funcționarea corectă a workflow-urilor GitHub Actions.

## 🔑 Secrete Necesare

### 1. **GITHUB_TOKEN** (Automat)
- **Scope necesar:** Automat furnizat de GitHub
- **Utilizare:** Toate workflow-urile
- **Note:** Nu necesită configurare manuală

### 2. **KEYSTORE_BASE64** (Obligatoriu pentru Release)
- **Descriere:** Keystore-ul de semnare în format Base64
- **Scope:** `release.yml`
- **Cum să obții:**
  ```bash
  # Dacă ai deja keystore-ul:
  base64 -i MOMCLAW-release-key.jks | pbcopy  # macOS
  base64 -i MOMCLAW-release-key.jks | xclip -selection clipboard  # Linux
  
  # Sau pentru a crea unul nou:
  keytool -genkeypair -v -keystore MOMCLAW-release-key.jks \
    -alias momclaw -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass YOUR_STORE_PASSWORD -keypass YOUR_KEY_PASSWORD
  ```
- **Note:** Fără acest secret, release-urile vor fi nesemnate

### 3. **STORE_PASSWORD** (Obligatoriu pentru Release)
- **Descriere:** Parola pentru keystore
- **Scope:** `release.yml`
- **Note:** Aceeași parolă folosită la crearea keystore-ului

### 4. **KEY_PASSWORD** (Obligatoriu pentru Release)
- **Descriere:** Parola pentru cheia din keystore
- **Scope:** `release.yml`
- **Note:** Poate fi aceeași cu STORE_PASSWORD

### 5. **KEY_ALIAS** (Obligatoriu pentru Release)
- **Descriere:** Alias-ul cheii din keystore
- **Scope:** `release.yml`
- **Valoare implicită:** `momclaw`
- **Note:** Trebuie să corespundă alias-ului din keystore

### 6. **PLAY_STORE_SERVICE_ACCOUNT_JSON** (Opțional - Google Play)
- **Descriere:** JSON-ul contului de serviciu pentru Google Play Console
- **Scope:** `release.yml`
- **Cum să obții:**
  1. Mergi la [Google Play Console](https://play.google.com/console)
  2. Setup → API access → Service accounts
  3. Creează un service account nou
  4. Descarcă JSON-ul
  5. Convertește în Base64:
     ```bash
     base64 -i service-account.json | pbcopy  # macOS
     base64 -i service-account.json | xclip -selection clipboard  # Linux
     ```
- **Note:** Fără acest secret, deploy-ul pe Google Play va fi omis

## 📋 Checklist Configurare

- [ ] **GITHUB_TOKEN** - Automat (verifică că workflow-urile au permisiuni)
- [ ] **KEYSTORE_BASE64** - Creează/obține keystore și convertește în Base64
- [ ] **STORE_PASSWORD** - Setează parola keystore-ului
- [ ] **KEY_PASSWORD** - Setează parola cheii
- [ ] **KEY_ALIAS** - Setează alias-ul (default: `momclaw`)
- [ ] **PLAY_STORE_SERVICE_ACCOUNT_JSON** - Opțional pentru Google Play deploy

## 🔧 Configurare în GitHub

1. Mergi la repository: `https://github.com/serverul/momclaw`
2. Settings → Secrets and variables → Actions
3. Click "New repository secret"
4. Adaugă fiecare secret cu numele exact (case-sensitive)

## ⚠️ Securitate

- **NICIODATĂ** nu commite secretele în cod
- Folosește GitHub Secrets pentru toate informațiile sensibile
- Keystore-ul trebuie păstrat într-un loc sigur (backup offline)
- Service account-ul Google Play trebuie să aibă doar permisiunile necesare

## 🧪 Validare

După configurare, poți valida workflow-urile:

1. **CI Workflow** - Se declanșează automat la push/PR
2. **Android Build** - Build complet cu toate artifactele
3. **Security Scan** - Rulează zilnic și la push
4. **Release** - Se declanșează la tag-uri `v*`

Pentru testare:
```bash
# Creează un tag de test
git tag v1.0.1-test
git push origin v1.0.1-test

# Verifică în GitHub Actions că release workflow-ul rulează
```

## 📝 Workflow-uri și Permisiuni

### ci.yml
- **Permisiuni:** `contents: read`
- **Secrete:** `GITHUB_TOKEN`

### android-build.yml
- **Permisiuni:** `contents: read`
- **Secrete:** `GITHUB_TOKEN`

### release.yml
- **Permisiuni:** `contents: write`
- **Secrete:** 
  - `GITHUB_TOKEN`
  - `KEYSTORE_BASE64` (opțional dar recomandat)
  - `STORE_PASSWORD`
  - `KEY_PASSWORD`
  - `KEY_ALIAS`
  - `PLAY_STORE_SERVICE_ACCOUNT_JSON` (opțional)

### security.yml
- **Permisiuni:** 
  - `contents: read`
  - `security-events: write`
- **Secrete:** `GITHUB_TOKEN`

### dependabot-auto-merge.yml
- **Permisiuni:**
  - `contents: write`
  - `pull-requests: write`
- **Secrete:** `GITHUB_TOKEN`

## 🚀 Release Automation

Pentru a face un release:

1. **Pregătire:**
   ```bash
   # Asigură-te că ești pe main și ai totul actualizat
   git checkout main
   git pull origin main
   
   # Verifică că nu ai modificări necommit-uite
   git status
   ```

2. **Update CHANGELOG:**
   ```bash
   # Editează CHANGELOG.md cu schimbările pentru versiunea nouă
   # Mută ce e sub [Unreleased] la secțiunea nouă [vX.Y.Z]
   ```

3. **Creează tag-ul:**
   ```bash
   # Pentru release stabil
   git tag -a v1.0.1 -m "Release v1.0.1: Bug fixes and improvements"
   git push origin v1.0.1
   
   # Pentru pre-release
   git tag -a v1.1.0-beta.1 -m "Beta release v1.1.0-beta.1"
   git push origin v1.1.0-beta.1
   ```

4. **Monitorizează:**
   - GitHub Actions → release workflow
   - Verifică că artifactele sunt generate
   - Verifică GitHub Releases

## 📚 Referințe

- [GitHub Actions Secrets](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [Android App Signing](https://developer.android.com/studio/publish/app-signing)
- [Google Play Console API](https://developers.google.com/android-publisher)
