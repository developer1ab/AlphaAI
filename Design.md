# Design Notes

## Cloud Gateway Provider Mapping

Current default third-party providers in the Cloud Gateway template:

- weather: OpenWeatherMap
- translate: DeepL (`api-free.deepl.com`)
- news: NewsAPI (`newsapi.org`)
- stock: Alpha Vantage (`alphavantage.co`)

Configuration source:

- Cloudflare Worker vars: `gateway/cloudflare-worker/wrangler.toml`
- Docker env template: `gateway/docker/.env.example`

## Request Routing Model

App sends one unified request format to the gateway:

```json
{
  "service": "weather",
  "endpoint": "weather",
  "method": "GET",
  "params": {
    "q": "london",
    "units": "metric"
  }
}
```

Gateway behavior:

1. Verify app key from `X-API-Key`.
2. Resolve provider config by `service`.
3. Apply auth strategy (`bearer`, `header`, or `query`).
4. Forward request to upstream API.
5. Return upstream response to app.

## Extensibility

To switch to another provider, update only service config values:

- `<SERVICE>_BASE_URL`
- `<SERVICE>_API_KEY`
- `<SERVICE>_AUTH_MODE`
- `<SERVICE>_API_KEY_HEADER` or `<SERVICE>_API_KEY_PARAM`

No major scheduler or app-side protocol changes are required.

## Task 2: Screen Simulator Prototype

This high-risk capability is implemented as an optional Accessibility-based module.

### Safety Model

- Build-time toggle: `ACCESSIBILITY_SIMULATION_ENABLED` (default off).
- Runtime risk confirmation: every `access ...` command requires explicit user confirmation in UI.
- Local-only operation: no screen content is sent to network by this module.

### Components

- Accessibility service: `app/src/main/java/com/yourname/alphaai/accessibility/AutoClickService.kt`
- Service config: `app/src/main/res/xml/accessibility_service_config.xml`
- Skill adapter: `app/src/main/java/com/yourname/alphaai/accessibility/AccessibilitySkill.kt`
- Settings entry activity: `app/src/main/java/com/yourname/alphaai/settings/AccessibilitySettingsActivity.kt`

### Command Contract

- `access status`
- `access settings`
- `access click text <text>`
- `access click coord <x> <y>`

### Enable For Local Prototype

Set in local-only config:

```properties
ACCESSIBILITY_SIMULATION_ENABLED=true
```

Then in Android accessibility settings, enable the `AutoClickService` service for the app.
