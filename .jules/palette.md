## 2023-10-27 - Tactile Feedback Consistency
**Learning:** Adding subtle `active:scale-95` to primary call-to-actions significantly improves the perceived responsiveness of the UI, especially on mobile devices where tap highlighting is often disabled.
**Action:** Always ensure that if an element has a hover state (like `hover:bg-gray-100`), it also has an active state to provide tactile feedback during interaction. When adding `scale` transforms, ensure `transition-colors` is updated to `transition-all` so the scaling animates smoothly.
