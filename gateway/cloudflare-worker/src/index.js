const RATE_WINDOW_MS = 60 * 1000;
const RATE_LIMIT = 60;
const ipHits = new Map();

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders() });
    }

    if (request.method !== "POST") {
      return jsonResponse({ error: "Method not allowed" }, 405);
    }

    if (!rateLimit(request)) {
      return jsonResponse({ error: "Too many requests" }, 429);
    }

    const userApiKey = request.headers.get("X-API-Key");
    if (!env.APP_GATEWAY_API_KEY || userApiKey !== env.APP_GATEWAY_API_KEY) {
      return jsonResponse({ error: "Unauthorized" }, 401);
    }

    let body;
    try {
      body = await request.json();
    } catch {
      return jsonResponse({ error: "Invalid JSON body" }, 400);
    }

    const service = String(body.service || "").trim();
    const endpoint = String(body.endpoint || "").trim();
    const params = body.params || {};

    if (!service || !endpoint) {
      return jsonResponse({ error: "Missing service or endpoint" }, 400);
    }

    const prefix = service.toUpperCase();
    const serviceBaseUrl = env[`${prefix}_BASE_URL`];
    const serviceApiKey = env[`${prefix}_API_KEY`];
    const authMode = (env[`${prefix}_AUTH_MODE`] || "bearer").toLowerCase();
    const keyHeader = env[`${prefix}_API_KEY_HEADER`] || "Authorization";
    const keyParam = env[`${prefix}_API_KEY_PARAM`] || "apikey";

    if (!serviceBaseUrl || !serviceApiKey) {
      return jsonResponse(
        { error: `Service not configured: ${service}` },
        400
      );
    }

    const targetUrl = new URL(endpoint, serviceBaseUrl.endsWith("/") ? serviceBaseUrl : `${serviceBaseUrl}/`);
    const method = (body.method || "POST").toUpperCase();

    const headers = {
      "Content-Type": "application/json"
    };

    if (authMode === "bearer") {
      headers[keyHeader] = keyHeader.toLowerCase() === "authorization" ? `Bearer ${serviceApiKey}` : serviceApiKey;
    } else if (authMode === "header") {
      headers[keyHeader] = serviceApiKey;
    } else if (authMode === "query") {
      targetUrl.searchParams.set(keyParam, serviceApiKey);
    }

    let outboundBody = undefined;
    if (method === "GET") {
      for (const [k, v] of Object.entries(params)) {
        targetUrl.searchParams.set(k, String(v));
      }
    } else {
      outboundBody = JSON.stringify(params);
    }

    const upstream = await fetch(targetUrl.toString(), {
      method,
      headers,
      body: outboundBody
    });

    const text = await upstream.text();
    const contentType = upstream.headers.get("Content-Type") || "application/json";

    return new Response(text, {
      status: upstream.status,
      headers: {
        ...corsHeaders(),
        "Content-Type": contentType
      }
    });
  }
};

function rateLimit(request) {
  const ip = request.headers.get("CF-Connecting-IP") || "unknown";
  const now = Date.now();
  const bucket = ipHits.get(ip) || [];
  const recent = bucket.filter((ts) => now - ts < RATE_WINDOW_MS);
  if (recent.length >= RATE_LIMIT) {
    ipHits.set(ip, recent);
    return false;
  }
  recent.push(now);
  ipHits.set(ip, recent);
  return true;
}

function corsHeaders() {
  return {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "Content-Type, X-API-Key",
    "Access-Control-Allow-Methods": "POST, OPTIONS"
  };
}

function jsonResponse(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      ...corsHeaders(),
      "Content-Type": "application/json"
    }
  });
}
