## 2024-05-24 - Accessible Links and Banners
**Learning:** Applying `semantics(mergeDescendants = true) { role = Role.Button }` to interactive containers (like banners or inline link groups) is critical. Without it, screen readers may announce text and click actions separately, confusing users. Using `onClickLabel` inside `clickable` provides clear context about the action.
**Action:** Always wrap interactive groups in `semantics(mergeDescendants = true)` and provide descriptive `onClickLabel`s for `clickable` elements, especially for inline links and promotional banners.
