# 2026-01-28 - Custom Clickable Feedback

**Learning:** When using `clickable(indication = null)` to remove the default ripple, the element becomes static and unresponsive to touch. This violates UX heuristics for feedback.

**Action:** Always implement an alternative feedback mechanism (e.g., scale animation using `interactionSource.collectIsPressedAsState()`) when disabling default indications on interactive elements.

## 2026-02-04 - Navigable List Item Pattern

**Learning:** Settings or list items that navigate to other screens must have both semantic (Role.Button) and visual (Chevron) indicators. Without these, users rely on trial-and-error to determine interactivity.

**Action:** For all navigable rows, force `Role.Button` in semantics and append `Icons.AutoMirrored.Filled.KeyboardArrowRight`.

## 2026-05-20 - [Local Feedback for Settings Items]
**Learning:** Purely informational settings items (e.g., Version) create frustration when they appear interactive but do nothing. Adding micro-interactions (e.g., copy-to-clipboard with Snackbar) confirms system responsiveness and delights users.
**Action:** Always make static settings items interactive if they contain copyable data, providing immediate local feedback (e.g. Snackbar) instead of navigation.

## 2026-05-25 - [Clickable Card Padding]
**Learning:** Applying `clickable` after inner padding creates unresponsive edges on cards. Users expect the entire visual container to be interactive.
**Action:** For cards with inner padding, apply `clickable` *before* the padding modifier but after the visual shape/background (e.g. `glassy -> clickable -> padding`).

## 2026-06-03 - CountryCodePicker Accessibility
**Learning:** `CountryCodePicker` triggers often lack `Role.Button` and proper `contentDescription` because they are custom composables. Adding `Role.Button` and a merged description ("Change country...") is crucial for blind users.
**Action:** For custom pickers, always add `semantics(mergeDescendants = true)` with a descriptive label and `Role.Button`. Adding a visual dropdown arrow improves affordance for everyone.
