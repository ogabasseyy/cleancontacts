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

## 2026-05-18 - Prevent DoS via Pagination Validation
**Vulnerability:** The `getContacts` API endpoint lacked validation for `limit` and `offset` parameters, allowing potential Denial of Service (DoS) and memory exhaustion by requesting excessively large batch sizes or invalid offsets.
**Learning:** Even internal API wrappers must validate input limits before forwarding requests to the server to fail fast and prevent client-side memory exhaustion or server-side overload.
**Prevention:** Always validate API pagination and batch parameters (e.g., `limit`, `offset`) against strict maximum thresholds (like `MAX_BATCH_SIZE`) before making network requests or executing queries.

## 2026-10-24 - PII Leak in WebSocket API Models
**Vulnerability:** The `WebSocketMessage` and `WebSocketEvent` data classes in `WhatsAppDetectorApi.kt` both contained a `phoneNumber` field but lacked a custom `toString()` method, which could lead to accidental logging of PII (CWE-532).
**Learning:** All data classes containing PII, even internal or nested ones used in API models, must explicitly override `toString()` to redact sensitive fields.
**Prevention:** Override `toString()` in all data classes containing sensitive information to explicitly redact PII.
## 2026-10-24 - Vulnerable Dependency: picomatch
**Vulnerability:** The `picomatch` dependency was flagged by `osv-scanner` for high severity vulnerabilities (GHSA-3v7f-55p6-f55p, GHSA-c2c7-rcm5-vvqj).
**Learning:** Development dependencies used in build scripts can introduce severe risks like ReDoS and path injection into the CI/CD pipeline or developer environments.
**Prevention:** Regularly audit and update dependencies, particularly glob-matching libraries which are historically prone to ReDoS. Keep node modules up to date using `npm audit`.
## 2026-04-04 - Restrict Permissive API CORS Policy
**Vulnerability:** The API endpoint `feedback.ts` used an overly permissive CORS policy (`Access-Control-Allow-Origin: *`).
**Learning:** This wildcard allowed any site to make requests to the endpoint, which is a potential risk for API abuse (e.g., spamming the email service).
**Prevention:** Use an explicit allowlist of trusted origins, validating `req.headers.origin` and defaulting to a safe origin (the app domain).
## 2026-04-19 - Rate Limiting API Endpoints
**Vulnerability:** The public-facing `feedback.ts` API endpoint lacked rate limiting, allowing unlimited POST requests which could lead to DoS or exhaustion of third-party API quotas (Resend).
**Learning:** Serverless functions must implement rudimentary application-layer rate limiting by default, especially when bridging to paid third-party APIs.
**Prevention:** Apply rate limiting logic (e.g., in-memory map tracking IPs via `x-forwarded-for`) on all unauthenticated endpoints that trigger external actions.

## 2026-10-24 - [HIGH] Defense in Depth against XSS with DOMPurify
**Vulnerability:** `landing-page/components/BlogPost.tsx` used `dangerouslySetInnerHTML={{ __html: html }}` with dynamically fetched content. While the HTML was compiled from our own markdown files, this violated the 'defense in depth' principle. If a vulnerability ever occurred in the markdown parser or the source changed to include user inputs, XSS would be immediately possible.
**Learning:** Even statically generated or seemingly 'trusted' dynamically fetched HTML should be sanitized at runtime when using `dangerouslySetInnerHTML` to adhere to 'Trust nothing, verify everything'.
**Prevention:** Always use a runtime sanitization library (like `DOMPurify`) for `dangerouslySetInnerHTML` regardless of the assumed source of the HTML.

## 2026-10-24 - DoS via Unbounded String Manipulation
**Vulnerability:** The API endpoint `feedback.ts` processed unbounded user inputs (`message`, `email`, `deviceInfo`) by calling string manipulation methods like `.trim()` before validating their length. This exposed the serverless function to CPU exhaustion and ReDoS attacks.
**Learning:** String manipulation operations (like trimming or regex replacements) on massive payloads can spike CPU usage and crash instances before length checks are reached.
**Prevention:** Always check the raw length of inputs (`input.length`) *before* executing any string manipulation or processing functions, especially in serverless environments.

## 2026-10-24 - Transitive PII Leak via Default toString() in API Responses
**Vulnerability:** The `WhatsAppContactsResponse` data class contained a `userId` field, which was being exposed when the object was logged, as it lacked a custom `toString()` method.
**Learning:** Default data class `toString()` methods automatically expose all fields, leading to accidental PII or sensitive data leaks in logs (CWE-532).
**Prevention:** Always explicitly override `toString()` to redact sensitive fields (like `userId`, `phoneNumber`, `code`) in any data class representing API requests, responses, or domain models.

## 2026-10-24 - Transitive PII Leak via Default toString() in Snapshot
**Vulnerability:** The `Snapshot` data class in `BackupRepository.kt` lacked a custom `toString()` override, which could lead to accidental logging of the entire list of user contacts (PII) if the object were ever logged or stringified (CWE-532).
**Learning:** Default data class `toString()` methods automatically expose all fields, leading to accidental PII or sensitive data leaks in logs. Even data classes that are primarily used internally for persistence logic must be secured.
**Prevention:** Always explicitly override `toString()` to redact sensitive fields (like `contacts`) in any data class that contains Personally Identifiable Information, regardless of where it is used in the architecture.
