## 2026-01-28 - Custom Clickable Feedback
**Learning:** When using `clickable(indication = null)` to remove the default ripple, the element becomes static and unresponsive to touch. This violates UX heuristics for feedback.
**Action:** Always implement an alternative feedback mechanism (e.g., scale animation using `interactionSource.collectIsPressedAsState()`) when disabling default indications on interactive elements.
