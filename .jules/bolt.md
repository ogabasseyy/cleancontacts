## 2026-10-18 - Animation Layout Thrashing
**Learning:** Using `Modifier.offset` with generic `Dp` values inside an infinite animation loop causes a full layout pass on every frame, which is expensive.
**Action:** Use `Modifier.graphicsLayer { translationX = ...; translationY = ... }` for animations to skip the layout phase and run on the GPU.
