# Security Notes

## 2026-01-28 - Partial Path Traversal in FileService

**Vulnerability:** `FileServiceImpl` used `canonicalPath.startsWith(cacheCanonical)` for validation. This allows partial path traversal (e.g., `/cache_secret` matches `/cache`).

**Learning:** String-based path validation is error-prone and insecure against sibling directory attacks.

**Prevention:** Always use `java.nio.file.Path` API (`toPath().toAbsolutePath().normalize().startsWith(...)`) which respects path components.
