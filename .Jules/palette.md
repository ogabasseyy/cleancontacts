# Palette Learnings

## 2026-03-05 - Add semantics to SensitiveReviewScreen

**Learning:** Applying `semantics(mergeDescendants = true)` to a parent container (like `Surface` or `Card`) incorrectly merges the interactive area of child actions (like an `IconButton`), making them inaccessible to screen readers. Always apply it only to the specific nested container (e.g., a `Row`) holding the static content.

**Action:** When creating components with mixed static text and actionable buttons, apply `semantics(mergeDescendants = true)` selectively to the inner layout container (like a `Row` or `Column`) wrapping only the static content, leaving the action buttons outside the merged semantics group.
