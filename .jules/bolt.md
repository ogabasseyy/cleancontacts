<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 7b18673 (fix: address review findings in PR #364)
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

**Learning:** When evaluating a string prefix against multiple single-character possibilities (e.g., checking if it starts with `=`, `@`, `\t`, `\r`, `+`, or `-`), repeated calls to `startsWith` add method-call overhead and boundary checks. In hotspots such as CSV exporting, it is cheaper to guard with `isEmpty()` and compare the first character directly via `value[0]` when you only need a single-character prefix test.

**Action:** Replace multiple `startsWith` calls with a single primitive `Char` extraction (`val firstChar = value[0]`) and perform direct character comparisons. Always guard the extraction with an `isEmpty()` check to prevent `StringIndexOutOfBoundsException`.

```kotlin
// ❌ Avoid: Multiple method calls and boundary checks
val needsEscape = value.startsWith("=") || value.startsWith("@") || value.startsWith("+")

// ✅ Prefer: Single primitive char extraction and comparison
if (value.isEmpty()) return value
val firstChar = value[0]
val needsEscape = firstChar == '=' || firstChar == '@' || firstChar == '+'
```

## 2026-03-05 - [Regex to StringBuilder File Sanitization]

**Learning:** For filename sanitization logic (e.g., in `IosFileService`, `FileServiceImpl`), replacing repeated `Regex` compilation and multiple string passes with a single-pass `StringBuilder` loop minimizes allocation and CPU overhead.
**Action:** Use single-pass StringBuilder loops with direct character validation for high-frequency text filtering and sanitization to reduce garbage collection overhead and improve execution speed.



## 2026-10-18 - Missing Filter in Aggregate Subqueries

**Learning:** When performing complex aggregate queries (like `getScanStats` calculating a `crossAccountCount`), failing to filter out rows you don't care about before the `GROUP BY` and `HAVING COUNT(DISTINCT...)` operations creates a massive performance bottleneck. In this case, synced contacts (WhatsApp, Telegram) were being unnecessarily processed in the grouping phase. This not only wastes CPU and memory but also leads to inconsistent data if the subquery logic doesn't match the detailed view queries.

**Action:** Always verify that complex `SELECT COUNT(*) FROM (SELECT ... GROUP BY ... HAVING ...)` subqueries use the same restrictive `WHERE` filters as their corresponding individual detailed queries to avoid processing irrelevant rows in heavy grouping operations.

## 2026-10-18 - Replacing functional character filtering with primitive extraction
**Learning:** Using functional chains like `filter { it.isDigit() }` or `filter { it.isDigit() || it == '+' }` creates intermediate `List` allocations and unnecessary object creation. In high-frequency paths like phone number formatting and parsing, this accumulates significant garbage collection overhead.
**Action:** Replace functional character filtering chains with specialized extension functions (like `extractDigits()` or `extractDigitsAndPlus()`) that use a single-pass `StringBuilder` loop to extract characters into a new string directly.

## 2026-10-18 - Replacing functional string splitting with direct parsing
**Learning:** Extracting data from delimited strings using chained operations like `split(",").filter { it.isNotBlank() }` creates intermediate `List` allocations and temporary string objects. In high-frequency paths like database parsing for contact records, this puts unnecessary pressure on the Garbage Collector.
**Action:** Replace `split().filter()` chains with custom, allocation-free string parsing extension functions that use a single-pass `while` loop to find non-blank segments directly and construct the final collection or find the first valid element.

## 2026-10-18 - Avoid grouped multi-pass transformations in WhatsApp Detector Repository
**Learning:** In `WhatsAppDetectorRepositoryImpl.kt`, processing network responses using chained functional operations like `response.contacts.map { ... }` combined with separate passes like `response.contacts.count { ... }` creates multiple intermediate collection allocations and traverses the list multiple times.
**Action:** Replaced multi-pass transformations with a single `ArrayList` iteration that executes all formatting operations (`map` and `count`) concurrently to eliminate intermediate objects and reduce GC pressure.

## 2026-10-18 - Avoid Hidden O(N^2) in List Intersections

**Learning:** When determining which items to retain or remove based on another list, chaining operations like `existingContacts.filterNot { it.id in missingDbIds }` where `missingDbIds` is a `List` creates a hidden `O(N * M)` operation. In paths like `IosContactRepository.refreshContacts`, this leads to significant lag when processing large contact lists.

**Action:** Always convert the secondary lookup collection (e.g., `missingDbIds`) into a `HashSet` before performing intersection checks like `it.id in missingDbIds`, ensuring O(1) lookups and an overall `O(N)` time complexity.
## 2026-10-18 - Replacing multi-pass List Mapping with Indexed Loops
**Learning:** Using chained `.map { ... }` transformations to process large lists (such as contact duplicates mapping domain and local entities) creates intermediate `ArrayList` allocations and uses iterator overhead internally. In pathways processing tens of thousands of items, this creates significant GC pauses.
**Action:** Replace `.map {}` calls with pre-allocated `ArrayList` instances and indexed `for` loops (`for (i in list.indices) { results.add(transform(list[i])) }`) to eliminate multiple allocations and iterator overhead.

## 2026-04-25 - Prevent Multi-Pass Grouping Overheads in Kotlin Collections
**Learning:** Using functional chains like `.groupBy { ... }.filter { ... }.mapNotNull { ... }` creates significant intermediate allocations (multiple HashMaps and ArrayLists) and iterates the data multiple times, causing garbage collection overhead in memory-sensitive environments.
**Action:** When aggregating or grouping collections, use a single imperative pass with an explicit `LinkedHashMap` (for insertion order) or `HashMap` to construct the final grouped result without intermediate data structures.

## 2026-10-18 - Faster Aggregate Queries for Cross-Account Contacts
**Learning:** The `crossAccountCount` queries in `ContactDao.kt` were grouping and evaluating thousands of synced WhatsApp and Telegram contacts unnecessarily, wasting CPU and memory.
**Action:** Appended `AND is_whatsapp = 0 AND is_telegram = 0` to filter out synced contacts before the expensive `GROUP BY` execution, aligning with `getScanStats` optimization guidelines.

## 2026-10-18 - Avoid Repeated Filter and Map Passes in Selective Extraction
**Learning:** Building one list with `filter { ... }.map { ... }` and then scanning the source again to derive a second related list creates avoidable allocations and repeated work.
**Action:** When a loop is already deciding which items to keep or remove, accumulate every needed derived value in that same pass instead of rebuilding related collections afterward.

## 2026-10-18 - Avoid O(N*M) Lookup in Collection Filtering
**Learning:** Using `.filterNot { item -> collection.any { it.id == item.id } }` creates an O(N*M) bottleneck because the inner collection is scanned for every outer item.
**Action:** Build an O(1) lookup structure such as a `HashSet` before filtering, and combine that with a pre-sized `ArrayList` loop to cut both CPU cost and intermediate allocations.
