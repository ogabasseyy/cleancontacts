# Palette Learnings

## 2024-05-24 - Interactive List Item Accessibility Semantics

**Learning:** In Jetpack Compose, when standard structural layouts like `Row` or `Box` are made interactive via `Modifier.clickable()`, they are not automatically announced as "Buttons" by screen readers like TalkBack or VoiceOver unless a role is explicitly provided. Furthermore, without `mergeDescendants = true`, the screen reader may force the user to focus on each individual internal text or icon node separately, creating a disjointed navigational experience.

**Action:** When creating custom interactive list items (e.g. `DuplicateGroupItem` or `ContactListItem`), configure the `clickable` modifier with `role = Role.Button` (or Role.Tab, etc. as appropriate). Use `.semantics(mergeDescendants = true) {}` only when the item has no separately actionable child controls; if it contains child actions like Edit/Delete `IconButton`s, keep the parent clickable but avoid merging descendants so those controls remain individually focusable. For example, `ContactListItem` can merge the avatar-and-text content into one accessible tap target while leaving trailing action buttons outside that merged semantics tree.
