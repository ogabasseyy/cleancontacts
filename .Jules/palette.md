## 2024-05-23 - Custom Button Accessibility
**Learning:** Custom clickable `Box` elements in Compose lack semantic roles and visual feedback by default, making them inaccessible and unresponsive.
**Action:** Always add `role = Role.Button` to `clickable` modifiers and implement `interactionSource.collectIsPressedAsState()` for visual feedback (e.g., scale animation) on custom buttons.
