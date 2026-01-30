# Security Notes

## 2026-01-28 - Partial Path Traversal in FileService

**Vulnerability:** `FileServiceImpl` used `canonicalPath.startsWith(cacheCanonical)` for validation. This allows partial path traversal (e.g., `/cache_secret` matches `/cache`).

**Learning:** String-based path validation is error-prone and insecure against sibling directory attacks.

**Prevention:** Always use `java.nio.file.Path` API (`toPath().toAbsolutePath().normalize().startsWith(...)`) which respects path components.

## 2026-01-28 - Unbounded Regex Processing in Contact Detectors

**Vulnerability:** `SensitiveDataDetector` processed contact fields of arbitrary length with complex Regex (SSN, Credit Card), exposing the app to ReDoS and CPU exhaustion.

**Learning:** Contact data (e.g., vCard imports) is user-controlled and can contain excessively large strings designed to slow down processing.

**Prevention:** Enforce strict input length limits (e.g., `MAX_INPUT_LENGTH = 100`) on all detector functions *before* regex execution.
