# Palette Learnings

## 2024-05-24 - Interactive List Item Accessibility Semantics

**Learning:** In Jetpack Compose, when standard structural layouts like `Row` or `Box` are made interactive via `Modifier.clickable()`, they are not automatically announced as "Buttons" by screen readers like TalkBack or VoiceOver unless a role is explicitly provided. Furthermore, without `mergeDescendants = true`, the screen reader may force the user to focus on each individual internal text or icon node separately, creating a disjointed navigational experience.

**Action:** When creating custom interactive list items (e.g. `DuplicateGroupItem` or `ContactListItem`), configure the `clickable` modifier with `role = Role.Button` (or Role.Tab, etc. as appropriate). Use `.semantics(mergeDescendants = true) {}` only when the item has no separately actionable child controls; if it contains child actions like Edit/Delete `IconButton`s, keep the parent clickable but avoid merging descendants so those controls remain individually focusable. For example, `ContactListItem` can merge the avatar-and-text content into one accessible tap target while leaving trailing action buttons outside that merged semantics tree.
## 2026-03-09 - [Screen Reader Item Merging]
**Learning:** List item components in Jetpack Compose built out of `Surface` or `Card` and that are interactive natively (like `selectable`, `onClick`) still read children components individually on screen readers.
**Action:** Always add `.semantics(mergeDescendants = true) {}` explicitly to interactive list items composed of multiple elements.

## 2026-03-11 - Accessible Accordions

**Learning:** When building custom accordion or FAQ components, using a `<button>` isn't enough. Screen readers need to know the relationship between the trigger and the content it reveals.

**Action:** Always link accordion buttons to their content using `aria-controls="[content-id]"`. Additionally, wrap the revealed content in an element with `role="region"` and `aria-labelledby="[button-id]"` so it can be navigated to as a distinct landmark. Provide visual feedback for keyboard users (`focus-visible`).

## 2024-05-24 - Accessible Links and Banners

**Learning:** Applying `semantics(mergeDescendants = true) { role = Role.Button }` to interactive containers (like banners or inline link groups) is critical. Without it, screen readers may announce text and click actions separately, confusing users. Using `onClickLabel` inside `clickable` provides clear context about the action.

**Action:** Always wrap interactive groups in `semantics(mergeDescendants = true)` and provide descriptive `onClickLabel`s for `clickable` elements, especially for inline links and promotional banners.

## 2026-03-05 - Add semantics to SensitiveReviewScreen

**Learning:** Applying `semantics(mergeDescendants = true)` to a parent container (like `Surface` or `Card`) incorrectly merges the interactive area of child actions (like an `IconButton`), making them inaccessible to screen readers. Always apply it only to the specific nested container (e.g. a `Row`) holding the static content.

**Action:** When creating components with mixed static text and actionable buttons, apply `semantics(mergeDescendants = true)` selectively to the inner layout container (like a `Row` or `Column`) wrapping only the static content, leaving the action buttons outside the merged semantics group.

## 2026-03-12 - Accessible Cards with Actions

**Learning:** Applying `semantics(mergeDescendants = true)` to a parent container (like `Surface` or `Card`) incorrectly merges the interactive area of child buttons (like an `IconButton` for delete or clear actions), making them harder for screen readers to target directly or obscuring their `contentDescription`.

**Action:** For list items with multiple actions, apply `semantics(mergeDescendants = true)` only to the nested container (e.g. a `Row` or `Column`) holding the static primary content (avatar, title, subtitle). Keep actionable `IconButton` or secondary interactive components as sibling elements outside of the merged semantics block.

## 2026-03-20 - Explicit Content Descriptions for Merged Semantics

**Learning:** When grouping child components with `semantics(mergeDescendants = true)` (e.g., inside complex cards or list items), screen readers will automatically and clunkily concatenate the inner texts.
**Action:** Explicitly provide a properly formatted `contentDescription` on the parent modifier to ensure screen readers read a cohesive, structured sentence rather than disjointed inner text values.

## 2026-03-16 - Avoid False Affordances on Reusable Cards

**Learning:** Using an empty lambda (`{ /* no-op */ }`) for an `onClick` parameter on a reusable card component applies a `clickable` modifier, creating a "false affordance." Screen readers will announce it as a button or tab, and it will respond to touch/clicks (with ripples), confusing users when no action occurs.
**Action:** Make `onClick` parameters nullable (`(() -> Unit)? = null`) for reusable cards that may act as informational displays. Conditionally apply the `clickable` modifier and any interactive semantic roles (`Role.Button`, `Role.Tab`) ONLY when `onClick` is not null.

## 2026-03-19 - Explicit Content Descriptions for Dashboard Summary

**Learning:** For dashboard summary cards containing multiple disjoint values (like total issues and accounts), simply using `semantics(mergeDescendants = true)` can result in screen readers reading the numbers and text incohesively.
**Action:** When creating complex summary cards, provide a clear, synthesized sentence as the `contentDescription` on the parent container to ensure the user gets a meaningful overview.

## 2026-03-19 - Screen Reader Cohesion for Non-Interactive List Rows

**Learning:** Even for non-interactive list items (like `FeatureRow` displaying checkmarks and feature descriptions in a Paywall), screen readers will announce each element (the icon, the text) individually. This leads to disjointed "Check... Unlimited cleanups" announcements that disrupt the user's flow and cognitive understanding.
**Action:** Apply `.semantics(mergeDescendants = true) {}` to the outer container (e.g., `Row`) of informational list items grouping descriptive icons and text labels to ensure screen readers announce them as a single cohesive unit.
