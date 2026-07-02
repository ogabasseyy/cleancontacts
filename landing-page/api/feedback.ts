const RESEND_API_KEY = process.env.RESEND_API_KEY;
const FEEDBACK_EMAIL = process.env.FEEDBACK_EMAIL;

const VALID_CATEGORIES = ["Bug Report", "Feature Request", "General Feedback"];

type FeedbackRequestBody = {
  category?: string;
  message?: string;
  email?: string;
  deviceInfo?: string;
};

type FeedbackRequest = {
  method?: string;
  body?: FeedbackRequestBody;
  headers?: Record<string, string | string[] | undefined>;
};

type FeedbackResponse = {
  setHeader(name: string, value: string): void;
  status(code: number): {
    json(payload: unknown): unknown;
    end(): unknown;
  };
};

function escapeHtml(str: string): string {
  return str
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;")
    .replace(/\n/g, "<br>");
}

// 2026 Security Fix: Implement rate limiting to prevent email spam/DoS
// Simple in-memory rate limiter for serverless environment
// Note: In a highly distributed serverless environment, this is per-instance,
// but it's sufficient for basic protection against burst attacks.
const rateLimitMap = new Map<string, { count: number; timestamp: number }>();
const RATE_LIMIT_WINDOW_MS = 15 * 60 * 1000; // 15 minutes
const MAX_REQUESTS_PER_WINDOW = 5;

// Clean up old entries periodically to prevent memory leaks
function cleanupRateLimitMap() {
  const now = Date.now();
  for (const [ip, data] of rateLimitMap.entries()) {
    if (now - data.timestamp > RATE_LIMIT_WINDOW_MS) {
      rateLimitMap.delete(ip);
    }
  }
}

export default async function handler(req: FeedbackRequest, res: FeedbackResponse) {
  // Restrict CORS to specific origins
  const origin = req.headers?.origin || req.headers?.Origin;
  const originStr = Array.isArray(origin) ? origin[0] : origin;

  const allowedOrigins = [
    "https://contactscleaner.tech",
    "http://localhost:5173", // For local web development
    "http://localhost", // Common for Android capacitor/webview
    "capacitor://localhost", // Common for iOS capacitor
    "ionic://localhost" // Common for Ionic
  ];

  const safeOrigin = (originStr && allowedOrigins.includes(originStr)) ? originStr : "https://contactscleaner.tech";

  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
  res.setHeader("Access-Control-Allow-Origin", safeOrigin);
  res.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");

  if (req.method === "OPTIONS") {
    return res.status(200).end();
  }

  if (req.method !== "POST") {
    return res.status(405).json({ success: false, error: "Method not allowed" });
  }

  // 2026 Security Fix: Apply rate limiting based on client IP
  // Use trusted proxy headers first to prevent IP spoofing
  const realIp = req.headers?.["x-real-ip"] || req.headers?.["x-vercel-forwarded-for"];
  const forwardedFor = req.headers?.["x-forwarded-for"];

  const extractedRealIp = Array.isArray(realIp) ? realIp[0] : realIp;
  const extractedForwardedFor = Array.isArray(forwardedFor)
    ? forwardedFor[0]
    : forwardedFor?.split(',')[0]?.trim();

  const clientIp = extractedRealIp || extractedForwardedFor || "unknown-ip";

  cleanupRateLimitMap();

  const now = Date.now();
  const limitData = rateLimitMap.get(clientIp);

  if (limitData) {
    if (now - limitData.timestamp < RATE_LIMIT_WINDOW_MS) {
      if (limitData.count >= MAX_REQUESTS_PER_WINDOW) {
        console.warn(`Rate limit exceeded for IP: ${clientIp}`);
        return res.status(429).json({ success: false, error: "Too many requests. Please try again later." });
      }
      limitData.count++;
    } else {
      rateLimitMap.set(clientIp, { count: 1, timestamp: now });
    }
  } else {
    rateLimitMap.set(clientIp, { count: 1, timestamp: now });
  }

  if (!RESEND_API_KEY || !FEEDBACK_EMAIL) {
    return res.status(500).json({ success: false, error: "Email service not configured" });
  }

  const { category, message, email, deviceInfo } = req.body ?? {};

  // 2026 Security Fix: Enforce early length limits to prevent ReDoS / CPU exhaustion
  // before string allocations or manipulations (like .trim()) occur.
  if (message && typeof message === "string" && message.length > 5000) {
    return res.status(400).json({ success: false, error: "Message too long" });
  }
  if (email && typeof email === "string" && email.length > 254) {
    return res.status(400).json({ success: false, error: "Email too long" });
  }
  if (deviceInfo && typeof deviceInfo === "string" && deviceInfo.length > 500) {
    return res.status(400).json({ success: false, error: "Device info too long" });
  }

  if (!message || typeof message !== "string" || message.trim().length === 0) {
    return res.status(400).json({ success: false, error: "Message is required" });
  }

  const trimmedMessage = message.trim();

  if (!category || !VALID_CATEGORIES.includes(category)) {
    return res.status(400).json({ success: false, error: "Invalid category" });
  }

  const trimmedEmail = email && typeof email === "string" ? email.trim() : "";
  if (trimmedEmail) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(trimmedEmail)) {
      return res.status(400).json({ success: false, error: "Invalid email format" });
    }
  }

  const sanitizedEmail = trimmedEmail || "Not provided";
  const sanitizedDevice =
    deviceInfo && typeof deviceInfo === "string" ? deviceInfo.trim() : "Not provided";

  try {
    const response = await fetch("https://api.resend.com/emails", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${RESEND_API_KEY}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        from: "Contacts Cleaner <onboarding@resend.dev>",
        to: FEEDBACK_EMAIL,
        subject: `[${category}] App Feedback`,
        html: `
          <h2>${escapeHtml(category)}</h2>
          <p><strong>Message:</strong></p>
          <p>${escapeHtml(trimmedMessage)}</p>
          <hr>
          <p><strong>Email:</strong> ${escapeHtml(sanitizedEmail)}</p>
          <p><strong>Device:</strong> ${escapeHtml(sanitizedDevice)}</p>
        `,
      }),
    });

    if (!response.ok) {
      let errorDetails: unknown;
      try {
        errorDetails = await response.json();
      } catch {
        errorDetails = await response.text();
      }
      console.error("Resend API error:", response.status, errorDetails);
      return res.status(502).json({ success: false, error: "Failed to send email" });
    }

    return res.status(200).json({ success: true });
  } catch (error) {
    console.error("Feedback submission error:", error);
    return res.status(500).json({ success: false, error: "Internal server error" });
  }
}
