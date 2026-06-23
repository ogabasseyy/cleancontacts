## 2026-06-23 - Add tactile scaling feedback to CTA buttons
**Learning:** Users lack clear tactile feedback during Call-to-Action button click interactions. Providing a subtle scale down effect on click improves perceived responsiveness.
**Action:** Apply `active:scale-95 transition-all duration-200` to primary CTA buttons. Replace existing `transition-colors` with `transition-all` to ensure the scale animation happens smoothly alongside color changes.
