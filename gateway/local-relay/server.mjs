import http from "node:http";

const PORT = Number(process.env.PORT || 8787);
const TARGET_BASE_URL = process.env.TARGET_BASE_URL || "https://alphaai-gateway.developer1-ad7.workers.dev";
const GATEWAY_KEY = process.env.GATEWAY_KEY || "";

const server = http.createServer(async (req, res) => {
  if (req.method === "OPTIONS") {
    setCors(res);
    res.writeHead(204);
    res.end();
    return;
  }

  if (req.method !== "POST" || req.url !== "/api") {
    setCors(res);
    res.writeHead(404, { "Content-Type": "application/json" });
    res.end(JSON.stringify({ error: "Not found" }));
    return;
  }

  let body = "";
  req.on("data", (chunk) => {
    body += chunk;
  });

  req.on("end", async () => {
    try {
      const upstream = await fetch(`${TARGET_BASE_URL.replace(/\/$/, "")}/api`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-API-Key": GATEWAY_KEY
        },
        body
      });

      const text = await upstream.text();
      setCors(res);
      res.writeHead(upstream.status, {
        "Content-Type": upstream.headers.get("content-type") || "application/json"
      });
      res.end(text);
    } catch (error) {
      setCors(res);
      res.writeHead(502, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ error: String(error?.message || error) }));
    }
  });
});

server.listen(PORT, "0.0.0.0", () => {
  console.log(`AlphaAI local relay listening on http://0.0.0.0:${PORT}`);
  console.log(`Forwarding to ${TARGET_BASE_URL}`);
});

function setCors(res) {
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type, X-API-Key");
  res.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
}
