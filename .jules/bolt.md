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

# 2026-01-20 - Regex vs Character Loops in Hot Paths

**Learning:** Replacing `Regex` validation with manual character loops in high-frequency methods (like `JunkDetector.detectJunk`) significantly reduces CPU overhead and object allocation, especially when checking thousands of items.

**Action:** Prefer `string.all { isValid(it) }` or `string.any { !isValid(it) }` with simple char checks over `Regex.matches()` or `Regex.containsMatchIn()`.

```kotlin
// ❌ Avoid: Regex engine overhead per call
val regex = Regex("[^0-9]")
if (regex.containsMatchIn(input)) ...

// ✅ Prefer: O(N) primitive check
if (input.any { !it.isDigit() }) ...
```
