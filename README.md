# AlphaAI

AlphaAI is an Android AI assistant prototype with a pluggable skill system, behavior learning, and a rule-based recommendation engine.

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
- Self-hosted cloud API gateway integration via CloudApiSkill (`cloud.api`)

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

### Cloud API Skills

- `weather london`
- `translate hello world to fr`
- `news ai`
- `stock AAPL`
- `cloud weather current city=tokyo`

## Cloud Gateway Deployment

AlphaAI provides a lightweight API gateway prototype so users can self-host and connect third-party cloud services securely.

### Option A: Cloudflare Workers (recommended)

Gateway template path:

- `gateway/cloudflare-worker`

Steps:

1. Install Node.js 20+ and Wrangler.
2. Edit `gateway/cloudflare-worker/wrangler.toml`:
  - Set `APP_GATEWAY_API_KEY`.
  - Configure service credentials (`WEATHER_*`, `TRANSLATE_*`, `NEWS_*`, `STOCK_*`).
3. Deploy:

```bash
cd gateway/cloudflare-worker
npm install
npm run deploy
```

4. Copy your Worker URL, for example:

```text
https://alphaai-gateway.<your-subdomain>.workers.dev
```

### Option B: Docker Compose (VPS or local server)

Gateway template path:

- `gateway/docker`
- `docker-compose.gateway.yml`

Steps:

1. Create env file:

```bash
cp gateway/docker/.env.example gateway/docker/.env
```

2. Fill in API keys in `gateway/docker/.env`.
3. Start gateway:

```bash
docker compose -f docker-compose.gateway.yml up -d --build
```

4. Gateway endpoint (default):

```text
http://<your-host>:8080/api
```

## AlphaAI App Configuration for Cloud Gateway

Set gateway values in `app/build.gradle`:

- `CLOUD_GATEWAY_BASE_URL`
- `CLOUD_GATEWAY_API_KEY`

Example:

```gradle
buildConfigField "String", "CLOUD_GATEWAY_BASE_URL", '"https://alphaai-gateway.example.com"'
buildConfigField "String", "CLOUD_GATEWAY_API_KEY", '"your-shared-gateway-key"'
```

Rebuild app after editing these fields.

For production hardening notes and deployment pitfalls, see `Remind.md`.

## Recommendation Rules

Default rules are loaded from:

- `app/src/main/assets/recommendations.json`

Periodic recommendation checks are scheduled in:

- `AlphaAIApplication` via WorkManager

You can trigger recommendation evaluation immediately from the UI button:

- `Trigger recommendation now`

## Data & Privacy

- All learning data is stored locally in `alphaai.db`
- You can clear learning data from the app UI (`Clear learning data`)

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
