# 2026-01-28 - Custom Clickable Feedback

**Learning:** When using `clickable(indication = null)` to remove the default ripple, the element becomes static and unresponsive to touch. This violates UX heuristics for feedback.

**Action:** Always implement an alternative feedback mechanism (e.g., scale animation using `interactionSource.collectIsPressedAsState()`) when disabling default indications on interactive elements.

## 2026-02-04 - Navigable List Item Pattern

**Learning:** Settings or list items that navigate to other screens must have both semantic (Role.Button) and visual (Chevron) indicators. Without these, users rely on trial-and-error to determine interactivity.

**Action:** For all navigable rows, force `Role.Button` in semantics and append `Icons.AutoMirrored.Filled.KeyboardArrowRight`.
