# Contributing to AppHub

Thanks for your interest in contributing.

## Ground Rules

- Keep changes focused and reviewable.
- Prefer small pull requests with clear scope.
- Preserve existing behavior unless intentionally changed.
- Add tests or manual verification notes for non-trivial changes.

## Development Setup

1. Fork and clone the repository.
2. Open in Android Studio.
3. Sync Gradle.
4. Build and run:

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

Windows PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:installDebug
```

## Branch & Commit

- Branch naming examples:
  - `feat/recommendation-feedback`
  - `fix/intent-parser`
  - `docs/readme-update`
- Commit message style:
  - `feat: add recommendation feedback logging`
  - `fix: handle missing location permission`
  - `docs: improve setup section`

## Pull Request Checklist

- Build passes locally.
- Changed behavior is explained in PR description.
- UI changes include screenshots or short notes.
- If data schema changed, migration is provided.
- If permissions changed, reason is documented.

## Adding a New Skill

1. Implement a skill class in `skills` (or a dedicated skill module).
2. Implement `Skill` contract compatibility.
3. Register routing logic in scheduler if needed.
4. Add usage examples in README or docs.
5. Verify manually on emulator/device.

Minimum expectation for new skills:

- Graceful error handling
- Permission checks where required
- Clear output in result map

## Recommendation Rules Contribution

- Update `app/src/main/assets/recommendations.json`.
- Keep rule IDs stable and unique.
- Ensure conditions are realistic and testable.
- Avoid spammy/overlapping suggestions.

## Reporting Issues

Please include:

- Device/emulator info
- Android version/API level
- Reproduction steps
- Actual vs expected behavior
- Logs/screenshots when possible

## Code Style

- Follow Kotlin official style.
- Keep comments concise and meaningful.
- Avoid unnecessary refactors in unrelated files.

## Security & Privacy

- Do not add telemetry without explicit discussion.
- Keep user behavior data local unless explicitly approved.
- Be careful with permissions; request minimum required scope.

Thanks again for contributing.
