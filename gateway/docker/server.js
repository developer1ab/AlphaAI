const express = require("express");

const app = express();
app.use(express.json({ limit: "1mb" }));

const RATE_WINDOW_MS = 60 * 1000;
const RATE_LIMIT = Number(process.env.RATE_LIMIT || "60");
const ipHits = new Map();

app.use((req, res, next) => {
  if (req.method === "OPTIONS") {
    res.setHeader("Access-Control-Allow-Origin", "*");
    res.setHeader("Access-Control-Allow-Headers", "Content-Type, X-API-Key");
    res.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
    return res.status(204).end();
  }

  const userApiKey = req.header("X-API-Key");
  if (!process.env.APP_GATEWAY_API_KEY || userApiKey !== process.env.APP_GATEWAY_API_KEY) {
    return res.status(401).json({ error: "Unauthorized" });
  }

  const now = Date.now();
  const ip = req.ip || "unknown";
  const bucket = ipHits.get(ip) || [];
  const recent = bucket.filter((ts) => now - ts < RATE_WINDOW_MS);
  if (recent.length >= RATE_LIMIT) {
    ipHits.set(ip, recent);
    return res.status(429).json({ error: "Too many requests" });
  }
  recent.push(now);
  ipHits.set(ip, recent);
  next();
});

app.post("/api", async (req, res) => {
  try {
    const { service, endpoint, params = {}, method = "POST" } = req.body || {};
    if (!service || !endpoint) {
      return res.status(400).json({ error: "Missing service or endpoint" });
    }

    const prefix = String(service).toUpperCase();
    const baseUrl = process.env[`${prefix}_BASE_URL`];
    const serviceApiKey = process.env[`${prefix}_API_KEY`];
    const authMode = (process.env[`${prefix}_AUTH_MODE`] || "bearer").toLowerCase();
    const apiKeyHeader = process.env[`${prefix}_API_KEY_HEADER`] || "Authorization";
    const apiKeyParam = process.env[`${prefix}_API_KEY_PARAM`] || "apikey";

    if (!baseUrl || !serviceApiKey) {
      return res.status(400).json({ error: `Service not configured: ${service}` });
    }

    const targetUrl = new URL(endpoint, baseUrl.endsWith("/") ? baseUrl : `${baseUrl}/`);
    const reqMethod = String(method).toUpperCase();

    const headers = { "Content-Type": "application/json" };
    if (authMode === "bearer") {
      headers[apiKeyHeader] = apiKeyHeader.toLowerCase() === "authorization" ? `Bearer ${serviceApiKey}` : serviceApiKey;
    } else if (authMode === "header") {
      headers[apiKeyHeader] = serviceApiKey;
    } else if (authMode === "query") {
      targetUrl.searchParams.set(apiKeyParam, serviceApiKey);
    }

    let body;
    if (reqMethod === "GET") {
      for (const [k, v] of Object.entries(params)) {
        targetUrl.searchParams.set(k, String(v));
      }
    } else {
      body = JSON.stringify(params);
    }

    const upstream = await fetch(targetUrl.toString(), {
      method: reqMethod,
      headers,
      body
    });

    const text = await upstream.text();
    const contentType = upstream.headers.get("content-type") || "application/json";
    res.status(upstream.status).setHeader("Content-Type", contentType).send(text);
  } catch (error) {
    res.status(500).json({ error: String(error.message || error) });
  }
});

const port = Number(process.env.PORT || "8080");
app.listen(port, () => {
  console.log(`AlphaAI gateway listening on :${port}`);
});
