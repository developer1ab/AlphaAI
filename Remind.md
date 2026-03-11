# Remind: Deployment And Integration Notes

This file records practical pitfalls found during Cloud Gateway development and rollout.

## 1. workers.dev Reachability

Symptom:
- App-side weather calls fail, while other internet features look normal.

Observed cause:
- Some networks can resolve `workers.dev` but fail on transport path to the resolved IP.

Recommendation:
- Prefer a custom domain bound to the Worker (for example `gateway.example.com`) for production.

Quick checks:
- `Test-NetConnection <gateway-domain> -Port 443`
- `curl -I https://<gateway-domain>/api`

## 2. Cloudflare Access Interaction

Symptom:
- Requests are blocked before entering Worker business logic.

Cause:
- Cloudflare Access is enabled, but the app only sends `X-API-Key`.

Recommendation:
- For current architecture, keep Access disabled.
- If Access must be enabled, add service token headers in client requests:
  - `CF-Access-Client-Id`
  - `CF-Access-Client-Secret`

## 3. Nameserver Migration Confusion

Symptom:
- Domain appears added in Cloudflare, but Worker custom domain does not activate.

Cause:
- NS records were edited in DNS-record view, not registrar-level nameserver settings.

Correct action:
- Update registrar nameservers to Cloudflare-assigned nameservers.
- Wait until zone status is `Active`.

## 4. Worker Domain Binding Visibility

What to expect after adding custom domain to Worker:
- DNS page shows a `Worker`-type record for your subdomain.
- Proxy status should be enabled.

## 5. API Gateway Request Shape

Current expected request payload:

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

Headers:
- `X-API-Key: <APP_GATEWAY_API_KEY>`
- `Content-Type: application/json`

## 6. Local Relay Is For Debugging Only

Local relay via `10.0.2.2` can help isolate emulator networking issues, but it should not be the final production path.

Production path:
- App -> custom HTTPS gateway domain -> Worker -> upstream API

## 7. Intent URL Parsing Regression (fixed)

A previous normalization step replaced `:` and broke URLs like `https://...`.

Rule of thumb:
- Never normalize away URL scheme delimiters in command parsing.

## 8. Suggested Release Checklist

1. Gateway custom domain is `Active`.
2. Access policy is intentionally configured (disabled for current app flow).
3. Host smoke test returns 200 for weather request.
4. App weather command returns 200 and readable output.
5. `local.properties` is local-only and not committed.
