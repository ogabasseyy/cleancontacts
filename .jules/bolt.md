# 2026-01-18 - Animation Layout Thrashing

**Learning:** Using `Modifier.offset` with generic `Dp` values inside an infinite animation loop causes a full layout pass on every frame, which is expensive.

**Action:** Use `Modifier.graphicsLayer { translationX = ...; translationY = ... }` for animations to skip the layout phase and run on the GPU.

```kotlin
// ❌ Avoid: Triggers layout on every frame
.offset(x = animatedOffset.value.dp, y = 0.dp)

// ✅ Prefer: GPU-accelerated, skips layout
.graphicsLayer {
    translationX = animatedOffset.value
    translationY = 0f
}
```

# 2026-01-18 - Regex Recompilation in Loops

**Learning:** Instantiating `Regex` objects inside a method called for every item in a list (like `detectJunk` for contacts) causes massive object allocation and compilation overhead (O(N)).

**Action:** Always move Regex patterns to a `private companion object` or top-level `val` to ensure they are compiled once and reused (O(1)). Also, prefer `string.filter` over `regex.replace` for simple character filtering.

```kotlin
// ❌ Avoid: Compiles regex for every item
fun process(input: String) {
    val regex = Regex("[^0-9]")
    val clean = input.replace(regex, "")
}

// ✅ Prefer: Compiles once, reuses
private companion object {
    private val DIGIT_REGEX = Regex("[^0-9]")
}
fun process(input: String) {
    // Or even better for this case: input.filter { it in '0'..'9' }
    val clean = DIGIT_REGEX.replace(input, "")
}
```

# 2026-02-04 - Regex to Character Loop Optimization

**Learning:** Even compiled regex patterns have overhead from the regex engine, NFA/DFA state machines, and object allocation for match results. For simple character validation, O(N) character loops are faster.

**Action:** Replace regex patterns with inline character checks when the pattern is simple enough to express as character conditions.

```kotlin
// ❌ Avoid: Regex engine overhead
private val INVALID_CHARS_REGEX = Regex("[^0-9+\\s()\\-]")
if (INVALID_CHARS_REGEX.containsMatchIn(number)) { ... }

// ✅ Prefer: O(N) character loop
private fun isValidNumberChar(c: Char): Boolean =
    c in '0'..'9' || c == '+' || c == '-' || c == ' ' || c == '(' || c == ')'
if (number.any { !isValidNumberChar(it) }) { ... }

// ❌ Avoid: Regex backreference for repetition detection
private val REPETITIVE_REGEX = Regex("(\\d)\\1{5,}")

// ✅ Prefer: Simple counting loop
private fun hasRepetitiveDigits(digits: String): Boolean {
    var count = 1
    for (i in 1 until digits.length) {
        if (digits[i] == digits[i - 1]) {
            if (++count >= 6) return true
        } else count = 1
    }
    return false
}
```

# 2026-02-18 - Single Pass Analysis Loop

**Learning:** When validating strings for multiple criteria (valid chars, digit count, repetition), performing multiple passes (e.g., `filter` + `length`, `any`, custom loop) creates unnecessary temporary objects and iterates the string multiple times.

**Action:** Consolidate multiple string analysis checks into a single character iteration loop that maintains state (counts, flags) to compute all metrics in one pass.

```kotlin
// ❌ Avoid: Multiple passes and allocation
val cleaned = input.filter { it.isDigit() } // Pass 1 + Allocation
if (input.any { !isValid(it) }) return // Pass 2
if (cleaned.length < 6) return
if (hasRepetition(cleaned)) return // Pass 3

// ✅ Prefer: Single pass state machine
var digitCount = 0
for (c in input) {
    if (!isValid(c)) return
    if (c.isDigit()) {
        digitCount++
        // track repetition here...
    }
}
```

# 2026-10-17 - Redundant DB Read in Scan Flow

**Learning:** In "Sync & Scan" workflows where data is fetched from a provider, processed, and inserted into a local DB, avoid immediately reading it back from the DB for subsequent analysis. The in-memory processed entities are the source of truth for that transaction.

**Action:** Always check if the data needed for post-processing is already available in scope before querying the database.

```kotlin
// ❌ Avoid: Insert then immediately fetch back
contactDao.replaceAllContacts(validatedEntities)
val allContacts = contactDao.getAllContacts().map { it.toDomain() } // Expensive O(N) DB + Deserialization

// ✅ Prefer: Reuse in-memory entities
contactDao.replaceAllContacts(validatedEntities)
val allContacts = validatedEntities.map { it.toDomain() } // Instant O(1) Access
```

# 2026-02-24 - Local Number Optimization

**Learning:** Validating international formats on local numbers (starting with '0') is wasteful. Since E.164 country codes never start with 0, we can short-circuit these checks immediately.

**Action:** Always check the first digit of a number before attempting expensive parsing. If it's '0', return early.

```kotlin
// ❌ Avoid: Expensive parsing for local numbers
val cleaned = raw.filter { it.isDigit() }
val parsed = phoneUtil.parse("+$cleaned", "ZZ") // Throws/Fails for local numbers

// ✅ Prefer: Quick check for leading zero
val firstDigit = raw.firstOrNull { it.isDigit() }
if (firstDigit == '0') return null // Skip parsing
```

# 2026-06-15 - Double-Pass Pattern in "Optimized" Code

**Learning:** Code comments or documentation claiming "Single-pass O(N) optimization" can be misleading if subsequent refactors or initial implementations left external helper calls (like `TextAnalyzer.hasFancyFonts`) intact. These external calls often iterate the string again, negating the single-pass benefit.

**Action:** Verify "single-pass" claims by tracing all function calls within the loop. Inline simple character checks (like range checks for fancy fonts) directly into the main loop to ensure true O(N) complexity and avoid hidden traversals.

```kotlin
// ❌ Avoid: External call iterating string again
for (c in name) { ... } // Pass 1
if (helper.hasFancyFonts(name)) ... // Pass 2 (Hidden O(N))

// ✅ Prefer: Inline checks in main loop
var hasFancyFont = false
for (c in name) {
    if (isFancy(c)) hasFancyFont = true // True O(N)
    // ...
}
if (hasFancyFont) ...
```

# 2026-03-05 - Avoid Algorithms that Alter Duplicate Grouping
**Learning:** Implementing coarse string-matching (e.g., 'last 7 digits') before E.164 normalization for duplicate phone numbers causes false negatives (misses short numbers) and accidental group splitting. E.164 normalization provides completeness that coarse heuristics break.
**Action:** When optimizing duplicate detection, use memoization/caching to reduce identical heavy normalization calls (from O(N) to O(U)), preserving the algorithm's correctness while improving speed.

# 2026-03-05 - Avoid Intermediate Collections in High-Frequency List Operations
**Learning:** Using multi-pass functional chains like `groupBy { ... }.filter { ... }.map { ... }` in high-frequency string list iterations creates unnecessary intermediate memory allocations and iterates over the same data repeatedly.
**Action:** Replace multi-pass chains with single-pass loops using a `mutableMapOf` for grouping, combined with early checks (e.g., `isNullOrBlank()`) before performing any expensive manipulations like `trim().lowercase()`.

# 2026-03-10 - Two-Pass String Filtering

**Learning:** When looking for a character in a string and then extracting a subset of characters (like finding a specific digit then removing non-digits), doing `string.firstOrNull { ... }` followed by `string.filter { ... }` traverses the string twice and creates intermediate `List`/`String` objects.

**Action:** Combine conditions into a single O(N) loop with a pre-sized `StringBuilder` to eliminate intermediate allocations and multiple traversals.

```kotlin
// ❌ Avoid: Iterates string twice, creates temporary objects
val firstDigit = number.firstOrNull { it.isDigit() }
if (firstDigit == '0') return null
val cleaned = number.filter { it.isDigit() }

// ✅ Prefer: Single pass, no intermediate strings
var firstDigit: Char? = null
val sb = java.lang.StringBuilder(number.length)
for (i in number.indices) {
    val c = number[i]
    if (c.isDigit()) {
        if (firstDigit == null) {
            firstDigit = c
            if (c == '0') return null
        }
        sb.append(c)
    }
}
val cleaned = sb.toString()
```

# 2026-03-15 - Eliminate Intermediate List Allocation in Transformation Pipelines

**Learning:** Using multi-pass functional chains like `.filter { ... }.map { ... }.sortedBy { ... }` creates multiple intermediate `List` allocations, causing memory pressure and garbage collection overhead during large iterations.

**Action:** Replace `filter` and `map` chains with a single loop that iterates over the source collection, applies conditions, and populates a pre-sized `ArrayList`.

```kotlin
// ❌ Avoid: Creates two intermediate List allocations before sorting
val results = items
    .filter { isValid(it) }
    .map { transform(it) }
    .sortedBy { it.key }

// ✅ Prefer: Single-pass loop with one pre-sized collection
val results = ArrayList<ResultType>(items.size)
for (item in items) {
    if (isValid(item)) {
        results.add(transform(item))
    }
}
results.sortBy { it.key }
```

# 2026-10-18 - Unnecessary Allocations with trim()

**Learning:** Calling `trim()` on whitespace-only strings creates new empty-string allocations. When a string is already trimmed, `trim()` returns the original value, so the optimization mainly benefits whitespace-only inputs while also skipping the follow-up work in high-frequency loops like contact scanning.

**Action:** Always evaluate `isBlank()` (or `isNullOrBlank()` if nullable) before invoking allocation-heavy string manipulations like `trim()` or `lowercase()` inside loops to prevent wasting memory and CPU cycles on empty strings.

```kotlin
// ❌ Avoid: Unconditionally creates a new object
val cleanValue = value.trim()

// ✅ Prefer: Avoids allocation for blank strings
if (value.isBlank()) return null
val cleanValue = value.trim()
```

# 2026-03-20 - Eliminate String Allocations from Chained Replace Calls

**Learning:** Using chained `.replace("-", "").replace(" ", "")` calls creates a new `String` object for every call in the chain. In high-frequency parsing paths like PII detection, this wastes CPU cycles and generates unnecessary garbage.

**Action:** Replace chained `.replace()` calls with a single-pass `StringBuilder` loop that filters out unwanted characters in one go, drastically reducing intermediate allocations and improving performance.

# 2026-03-16 - The `forEach` Placebo Optimization

**Learning:** In Kotlin, `Iterable<T>.forEach { ... }` is an `inline` function that generates the same underlying `for (item in list)` bytecode during compilation. Replacing it with a manual `for` loop provides absolutely zero performance benefit and is a placebo optimization.
**Action:** Do not refactor `forEach` to `for` loops under the guise of performance. Focus on actual algorithmic improvements or avoiding allocations.

# 2026-03-16 - Eager Size Checks to Avoid Collection Allocation

**Learning:** Operations like `distinctBy { it.id }` internally allocate a new `HashSet` and `ArrayList`. Applying this operation to every group in a collection (e.g., when identifying duplicates) results in massive allocation overhead, especially since the vast majority of groups will only have a single item (size = 1).
**Action:** When finding duplicates or processing groups, always check `if (group.size > 1)` *before* applying expensive functional transformations like `distinctBy` to eliminate unnecessary set and list allocations for single-item groups.

# 2026-10-18 - Replacing `take(1)` and `startsWith` with primitive `Char` checks

**Learning:** Extracting a single character prefix using `String.take(1)` creates an unnecessary `String` object allocation. In a tight inner loop processing thousands of elements (like checking duplicate names), these allocations accumulate, causing memory pressure and garbage collection overhead. Additionally, comparing that string prefix using `String.startsWith(String)` involves method calls and boundary checks that are heavier than a direct `Char` comparison.

**Action:** When you only need to compare the first character of a string, and it is guaranteed not to be empty, extract it as a primitive `Char` using `string[0]` and perform direct character comparison (`!=` or `==`). This is an O(1) operation that completely eliminates the `String` allocation.

```kotlin
// ❌ Avoid: Allocates a String per iteration and uses a heavy string method
val prefix = cleanNameA.take(1)
for (item in items) {
    if (!item.name.startsWith(prefix)) break
}

// ✅ Prefer: Zero allocation primitive Char extraction and comparison
val firstChar = cleanNameA[0] // Assuming length > 0
for (item in items) {
    if (item.name[0] != firstChar) break
}
```

## 2026-10-18 - Avoid String Methods for Single-Character Prefix Checking

**Learning:** When evaluating a string prefix against multiple single-character possibilities (e.g., checking if it starts with `=`, `@`, `\t`, `\r`, `+`, or `-`), making repeated calls to `String.startsWith(String)` introduces unnecessary method overhead and String allocations. This is particularly wasteful in high-frequency operations like CSV exporting where these evaluations occur thousands of times.

**Action:** Replace multiple `startsWith` calls with a single primitive `Char` extraction (`val firstChar = value[0]`) and perform direct character comparisons. Always guard the extraction with an `isEmpty()` check to prevent `StringIndexOutOfBoundsException`.

```kotlin
// ❌ Avoid: Multiple method calls and string allocations
val needsEscape = value.startsWith("=") || value.startsWith("@") || value.startsWith("+")

// ✅ Prefer: Single primitive char extraction and comparison
if (value.isEmpty()) return value
val firstChar = value[0]
val needsEscape = firstChar == '=' || firstChar == '@' || firstChar == '+'
```
