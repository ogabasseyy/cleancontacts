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
