# Contributing to MomClaw

🎉 Welcome! We're excited that you want to contribute to MomClaw.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [How to Contribute](#how-to-contribute)
- [Coding Standards](#coding-standards)
- [Commit Guidelines](#commit-guidelines)
- [Pull Request Process](#pull-request-process)
- [Issue Guidelines](#issue-guidelines)

---

## Code of Conduct

This project follows the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code. Please report unacceptable behavior to [maintainers](mailto:contact@example.com).

---

## Getting Started

### Fork and Clone

```bash
# 1. Fork the repository on GitHub

# 2. Clone your fork
git clone https://github.com/YOUR_USERNAME/momclaw.git
cd momclaw

# 3. Add upstream remote
git remote add upstream https://github.com/serverul/momclaw.git

# 4. Fetch upstream changes
git fetch upstream
```

### Keep Your Fork Updated

```bash
# Sync with upstream
git checkout main
git pull upstream main
git push origin main
```

---

## Development Setup

### Prerequisites

- **JDK 17** (required)
- **Android Studio Hedgehog** or newer
- **Android SDK API 35**
- **Android NDK r25c+**
- **Git**

### Initial Setup

```bash
# 1. Open project in Android Studio
# File → Open → Select momclaw/android

# 2. Wait for Gradle sync (5-10 minutes first time)

# 3. Run debug build
./android/gradlew assembleDebug

# 4. Run tests
./scripts/run-tests.sh --unit
```

### IDE Setup

**Android Studio:**

1. **Kotlin Style Guide:**
   - Settings → Editor → Code Style → Kotlin
   - Set from Kotlin official style guide

2. **Line Length:**
   - Settings → Editor → Code Style → General
   - Hard wrap at: 120

3. **Import Order:**
   - Settings → Editor → Code Style → Kotlin → Imports
   - Use Kotlin official import order

**Recommended Plugins:**

- .gitignore
- Detekt
- Kotlin
- Rainbow Brackets

---

## How to Contribute

### Reporting Bugs

1. **Search existing issues** first
2. **Use bug report template** when creating new issue
3. **Include:**
   - Android version
   - Device model
   - Steps to reproduce
   - Expected vs actual behavior
   - Screenshots/logs if applicable

### Suggesting Features

1. **Search existing feature requests**
2. **Use feature request template**
3. **Describe:**
   - Use case
   - Proposed solution
   - Alternatives considered

### Code Contributions

#### Good First Issues

Look for issues labeled `good first issue` or `help wanted`.

#### Types of Contributions

- 🐛 Bug fixes
- ✨ New features
- 📝 Documentation improvements
- 🎨 UI/UX enhancements
- ⚡ Performance optimizations
- 🌐 Translations
- 🧪 Test coverage improvements

---

## Coding Standards

### Kotlin Style Guide

We follow the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).

**Key Points:**

- **Indentation:** 4 spaces (no tabs)
- **Line length:** Maximum 120 characters
- **Wildcard imports:** Not allowed
- **Naming:**
  - Classes: PascalCase (`ChatRoute`)
  - Functions: camelCase (`sendMessage`)
  - Constants: SCREAMING_SNAKE_CASE (`MAX_TOKENS`)

### Code Organization

```
com.loa.momclaw/
├── data/              # Data layer
│   ├── local/         # Room, DataStore
│   ├── remote/        # API clients
│   └── repository/    # Repository pattern
├── domain/            # Business logic
│   ├── model/         # Domain models
│   └── usecase/       # Use cases
├── ui/                # Presentation layer
│   ├── chat/          # Chat feature
│   ├── models/        # Models feature
│   └── settings/      # Settings feature
└── di/                # Dependency injection
```

### Best Practices

#### Compose

```kotlin
// ✅ Good
@Composable
fun ChatScreen(
    messages: List<Message>,
    onSendMessage: (String) -> Unit
) {
    // Implementation
}

// ❌ Bad - mutable state in parameters
@Composable
fun ChatScreen(
    messages: MutableList<Message>
) {
    // Don't do this
}
```

#### Coroutines

```kotlin
// ✅ Good - structured concurrency
viewModelScope.launch {
    repository.sendMessage(message)
        .catch { e -> _error.value = e.message }
        .collect { response ->
            _messages.value += response
        }
}

// ❌ Bad - GlobalScope
GlobalScope.launch {
    // Avoid GlobalScope
}
```

#### Dependency Injection

```kotlin
// ✅ Good - constructor injection
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val agentClient: AgentClient
) : ViewModel() {
    // Implementation
}

// ❌ Bad - field injection
@ViewModel
class ChatViewModel : ViewModel() {
    @Inject
    lateinit var repository: ChatRepository
}
```

### Run Detekt

```bash
# Run static analysis
./android/gradlew detekt

# Auto-correct issues
./android/gradlew detekt --auto-correct
```

---

## Commit Guidelines

### Commit Message Format

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation only
- `test`: Adding/updating tests
- `refactor`: Code refactoring
- `perf`: Performance improvement
- `chore`: Maintenance tasks
- `style`: Code style (formatting, semicolons, etc.)
- `ci`: CI/CD changes

### Examples

```bash
# Feature
feat(chat): add streaming response support

# Bug fix
fix(agent): resolve memory leak in tool execution

# Documentation
docs(api): update OpenAI-compatible endpoint docs

# Breaking change
feat(api)!: change authentication header format

BREAKING CHANGE: X-API-Key header replaced by Authorization Bearer
```

### Commit Best Practices

- **Write clear, concise messages**
- **Use imperative mood** ("add feature" not "added feature")
- **Reference issues** when applicable
- **Keep commits atomic** (one logical change per commit)

---

## Pull Request Process

### Before Submitting

1. **Sync with upstream:**
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```

2. **Run tests:**
   ```bash
   ./scripts/run-tests.sh --all
   ```

3. **Run validation:**
   ```bash
   ./scripts/validate-build.sh --full
   ```

4. **Check coverage:**
   - Maintain or improve test coverage
   - New code should have tests

### Creating a PR

1. **Push to your fork:**
   ```bash
   git push origin feature/my-feature
   ```

2. **Create PR on GitHub:**
   - Use PR template
   - Reference related issues
   - Describe changes clearly

3. **Fill out PR template:**
   - What does this PR do?
   - Why is it needed?
   - How was it tested?
   - Screenshots (if UI changes)

### PR Checklist

- [ ] Code compiles without errors
- [ ] All tests pass
- [ ] Lint checks pass
- [ ] Code follows style guidelines
- [ ] Documentation updated (if needed)
- [ ] Commit messages follow guidelines
- [ ] PR references related issues

### Review Process

1. **Automated checks:**
   - CI must pass (build, test, lint)
   - CodeQL security analysis

2. **Code review:**
   - At least 1 approval required
   - Address all review comments
   - Keep discussion constructive

3. **Merge:**
   - Squash and merge for feature branches
   - Maintainer will merge when approved

### After Merge

- Delete your feature branch
- Update your local main branch
- Celebrate! 🎉

---

## Issue Guidelines

### Bug Report Template

```markdown
**Description**
Clear description of the bug

**To Reproduce**
1. Go to '...'
2. Click on '....'
3. See error

**Expected Behavior**
What should happen

**Actual Behavior**
What actually happens

**Screenshots**
If applicable

**Environment**
- Device: [e.g., Pixel 6]
- OS: [e.g., Android 14]
- MomClaw version: [e.g., 1.0.0]

**Additional Context**
Any other relevant info
```

### Feature Request Template

```markdown
**Is your feature request related to a problem?**
Clear description of the problem

**Proposed Solution**
Describe the feature you'd like

**Alternatives Considered**
Other solutions you've thought about

**Additional Context**
Any other context, screenshots, etc.
```

---

## Questions?

- **GitHub Discussions:** [momclaw/discussions](https://github.com/serverul/momclaw/discussions)
- **Issues:** [momclaw/issues](https://github.com/serverul/momclaw/issues)
- **Email:** contact@example.com

---

## License

By contributing to MomClaw, you agree that your contributions will be licensed under the Apache License 2.0.

---

Thank you for contributing! 🙏
