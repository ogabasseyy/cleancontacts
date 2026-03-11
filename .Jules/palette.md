## 2026-03-11 - Accessible Accordions

**Learning:** When building custom accordion or FAQ components, using a `<button>` isn't enough. Screen readers need to know the relationship between the trigger and the content it reveals.
**Action:** Always link accordion buttons to their content using `aria-controls="[content-id]"`. Additionally, wrap the revealed content in an element with `role="region"` and `aria-labelledby="[button-id]"` so it can be navigated to as a distinct landmark. Provide visual feedback for keyboard users (`focus-visible`).
