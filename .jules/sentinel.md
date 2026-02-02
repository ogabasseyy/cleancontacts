# Security Notes

## 2026-01-28 - Partial Path Traversal in FileService

**Vulnerability:** `FileServiceImpl` used `canonicalPath.startsWith(cacheCanonical)` for validation. This allows partial path traversal (e.g., `/cache_secret` matches `/cache`).

**Learning:** String-based path validation is error-prone and insecure against sibling directory attacks.

**Prevention:** Always use `java.nio.file.Path` API (`toPath().toAbsolutePath().normalize().startsWith(...)`) which respects path components.

## 2026-01-29 - ReDoS Risk in Regex-heavy Detectors

**Vulnerability:** `SensitiveDataDetector` applied multiple complex Regex patterns on user-provided input without length limits, exposing the app to Regular Expression Denial of Service (ReDoS).

**Learning:** Even strict regexes can be computationally expensive on maliciously crafted long inputs.

**Prevention:** Enforce strict length limits (e.g., < 100 chars) on inputs before passing them to regex engines, especially for PII detection where valid inputs are inherently short.

## 2026-01-29 - Algorithmic DoS in Duplicate Detection
**Vulnerability:** `DuplicateDetector` performed O(N*M) Levenshtein distance calculations on potentially unbounded name strings, allowing DoS via massive inputs.
**Learning:** Reusing buffers for performance (optimization) does not prevent algorithmic complexity attacks if the inputs themselves are too large.
**Prevention:** Enforce strict length limits (e.g. 1000 chars) on inputs before passing them to computationally expensive algorithms (O(N^2) or worse).
