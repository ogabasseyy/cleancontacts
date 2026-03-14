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

## 2026-02-04 - Broken CSV Quote Escaping

**Vulnerability:** `ContactImportParser` manually implemented CSV parsing logic that failed to correctly handle escaped double quotes (`""`) inside quoted fields, leading to data corruption and potential injection of field delimiters.
**Learning:** Naive state-based parsing (toggling a boolean on every quote) fails for escaped quotes because it requires lookahead or context awareness to distinguish an escaped quote from a closing quote.
**Prevention:** Use a robust state machine with lookahead for manual CSV parsing, or rely on established libraries where possible. Ensure all edge cases (escaped quotes, empty fields, end of line) are covered by specific unit tests.

## 2026-02-04 - Transitive PII Leak via Nested toString()
**Vulnerability:** `Contact.toString()` leaked phone numbers because it relied on the default `toString()` of a nested data class (`FormatIssue`) which contained sensitive fields (`normalizedNumber`).
**Learning:** Redacting PII in a parent object is insufficient if it contains child objects that expose PII in their `toString()` implementation.
**Prevention:** Override `toString()` in ALL data classes containing sensitive information, even if they are internal or nested, to explicitly redact PII.

## 2026-03-07 - ShareLauncher Path Traversal
**Vulnerability:** `writeToTempFile` in Android and iOS ShareLauncher accepted a caller-provided `fileName` and used it directly when building the output path, which could allow path traversal or unsafe hidden-file names.
**Learning:** Export and temp-file helpers need filename sanitization and a final path-boundary check even when they only write into cache or temp directories.
**Prevention:** Sanitize filenames by replacing non-safe characters, strip leading dots/underscores that can produce hidden or ambiguous paths, default empty results to `export.csv`, and verify the resolved path still lives under the intended export directory before writing.

## 2026-03-10 - Path Traversal Sibling Matching
**Vulnerability:** Android and iOS cache/export helpers used string prefix checks without forcing a trailing directory separator, so sibling paths like `/cache_secret` could still match `/cache`.
**Learning:** Canonicalization alone is not enough when you compare paths as strings; the directory boundary has to be explicit for the prefix check to be safe.
**Prevention:** When `Path.startsWith(...)` is not available or not compatible, append the platform separator to the canonical base directory before calling `startsWith`, and standardize iOS paths before comparing them.
## 2026-10-24 - [CRITICAL] Prevent Hardcoded Server/External API Keys Exposure
**Vulnerability:** A critical, hardcoded API Key for the WhatsApp Detector Service (`WHATSAPP_DETECTOR_API_KEY`) was accidentally committed and exposed as a fallback value in `WhatsAppDetectorConfig.ios.kt` (`"e59ec0ca77c64b123d56e683c92e7009c0cbaf0d393dc916b1856ead3b063332"`).
**Learning:** Hardcoded external service API keys represent critical vulnerabilities. However, there is a distinction between public SDK keys (like RevenueCat `appl_...`) and sensitive service keys. The iOS source set was originally configured independently, bypassing the `Secrets` generated object available to Android.
**Prevention:** Remove hardcoded sensitive keys immediately. Configure KMP build scripts to expose securely generated `Secrets` across all targets by adding `kotlin.srcDir(secretsDir)` to the `commonMain` configuration block in `build.gradle.kts`. This ensures all platforms access securely injected properties.
