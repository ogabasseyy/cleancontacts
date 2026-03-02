import type { VercelRequest, VercelResponse } from "@vercel/node";

const RESEND_API_KEY = process.env.RESEND_API_KEY;
const FEEDBACK_EMAIL = "basseybjohn@gmail.com";

const VALID_CATEGORIES = ["Bug Report", "Feature Request", "General Feedback"];

function escapeHtml(str: string): string {
  return str
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/\n/g, "<br>");
}

export default async function handler(req: VercelRequest, res: VercelResponse) {
  // CORS headers for mobile app requests
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");

  if (req.method === "OPTIONS") {
    return res.status(200).end();
  }

  if (req.method !== "POST") {
    return res.status(405).json({ success: false, error: "Method not allowed" });
  }

  if (!RESEND_API_KEY) {
    return res.status(500).json({ success: false, error: "Email service not configured" });
  }

  const { category, message, email, deviceInfo } = req.body ?? {};

  if (!message || typeof message !== "string" || message.trim().length === 0) {
    return res.status(400).json({ success: false, error: "Message is required" });
  }

  if (message.length > 5000) {
    return res.status(400).json({ success: false, error: "Message too long" });
  }

  if (!category || !VALID_CATEGORIES.includes(category)) {
    return res.status(400).json({ success: false, error: "Invalid category" });
  }

  const sanitizedEmail =
    email && typeof email === "string" ? email.trim().slice(0, 254) : "Not provided";
  const sanitizedDevice =
    deviceInfo && typeof deviceInfo === "string" ? deviceInfo.trim().slice(0, 500) : "Not provided";

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
          <p>${escapeHtml(message)}</p>
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
