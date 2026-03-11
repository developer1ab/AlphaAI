# AppHub (AlphaAI)

AppHub is an Android AI assistant prototype with a pluggable skill system, behavior learning, and a rule-based recommendation engine.

## Highlights

- Skill scheduling via `SimpleScheduler`
- Built-in skills:
  - Toast (`system.toast`)
  - Camera photo capture (`camera.take_photo`)
  - Location (`location.get`)
  - Notification (`notification.show`)
  - Intent execution (`intent.execute`)
- Intent-based commands:
  - Open apps
  - Open URLs
  - Dial numbers
  - Share text
  - Open map intents
- Local behavior learning with Room:
  - User action logs
  - Recommendation hit logs
  - Basic profile fields
- Rule-based recommendation engine (WorkManager + JSON rules)
- In-app debug trigger for immediate recommendation checks

## Project Structure

- `app`: UI, app lifecycle, runtime integration
- `core`: scheduler, resolver, core execution contracts
- `skills`: built-in skill implementations
- `data`: Room entities, DAO, database, profile generator
- `recommendation`: rule model, loader, matcher, worker, notifier
- `common`: shared Android library placeholder

## Quick Start

### Requirements

- Android Studio (Giraffe+ recommended)
- JDK 17
- Android SDK with API 33
- Emulator or Android device

### Build & Install

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

On Windows PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:installDebug
```

### Run

Launch the app from Android Studio, or with adb:

```bash
adb shell am start -n com.example.alphaai/com.yourname.alphaai.MainActivity
```

## Example Commands

### Core Skills

- `toast hello`
- `photo`
- `location`
- `notification`

### Intent Skills

- `open settings`
- `open chrome`
- `open https://www.github.com`
- `visit github.com`
- `dial 10086`
- `share hello to chrome`

## Recommendation Rules

Default rules are loaded from:

- `app/src/main/assets/recommendations.json`

Periodic recommendation checks are scheduled in:

- `AppHubApplication` via WorkManager

You can trigger recommendation evaluation immediately from the UI button:

- `立即触发推荐`

## Data & Privacy

- All learning data is stored locally in `apphub.db`
- You can clear learning data from the app UI (`清除学习数据`)

## Development Status

Current implementation includes:

- Core scheduling + system skills
- Learning data collection and history UI
- Recommendation engine v1 with notification output

Planned improvements:

- Feedback-based recommendation weight updates
- Better context providers (foreground app, richer location types)
- Dynamic/registry-based skill loading for open-source contributors

## Contributing

Please read:

- `CONTRIBUTING.md`

## License

This project is licensed under the MIT License.

- `LICENSE`
