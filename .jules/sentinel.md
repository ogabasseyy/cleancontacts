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

## 2026-01-30 - Unvalidated URL Schemes in UrlOpener

**Vulnerability:** `UrlOpener` allowed opening any URI scheme (e.g., `file://`, `javascript:`, `custom-scheme://`) passed to it via `Intent.ACTION_VIEW` or `UIApplication.openURL`, potentially enabling open redirects or malicious intent triggers.
**Learning:** Generic "open URL" utilities must strictly validate schemes to prevent intended web-only functions from being abused for system-level actions.
**Prevention:** Whitelist allowed schemes (e.g., `http`, `https`) before passing URIs to system launchers.

## 2026-02-04 - CSV Injection (Formula Injection) in Exports
**Vulnerability:** `ExportUtils` sanitized quotes but failed to escape formula triggers (`=`, `@`), allowing malicious contact names to execute code in spreadsheet software.
**Learning:** Standard CSV escaping (RFC 4180) only handles structural integrity (quotes/commas) but not payload safety for spreadsheet viewers (Formula Injection). Note: `+` and `-` must NOT be escaped in contact exports because phone numbers commonly start with `+`.
**Prevention:** Prefix values starting with `=` or `@` with a single quote `'` to force text interpretation. Exclude `+` and `-` to preserve phone number data integrity.

## 2026-02-04 - Refined CSV Injection Protection (Context-Aware Escaping)
**Vulnerability:** `ExportUtils` broadly exempted `+` and `-` from escaping to support phone numbers, but failed to distinguish between safe numeric strings and potential command payloads (e.g., `+cmd|...`) in non-numeric fields like names.
**Learning:** Broad exemptions for specific characters can reintroduce the vulnerability they were meant to avoid if the context (numeric vs text) is ignored.
**Prevention:** Refine exemptions to only allow `+` and `-` if the remaining string contains no letters (A-Z), effectively blocking command execution while preserving valid phone numbers.
