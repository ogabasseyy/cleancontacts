## 2026-06-12 - Tactile Feedback for Primary CTAs
**Learning:** When using Tailwind CSS, adding `active:scale-95 transition-all duration-200` to primary CTA buttons provides consistent, satisfying tactile feedback during click interactions that significantly improves perceived responsiveness and micro-UX, especially on mobile devices.
**Action:** Ensure any existing `transition-colors` or similar classes are replaced with `transition-all` when adding this scaling effect so both color changes and scaling animate smoothly.
## 2026-06-12 - TypeScript focusable="false" Error
**Learning:** Do not apply the `focusable="false"` attribute to standard HTML elements like `<div>` in the `landing-page` project. It is not supported by React's `DetailedHTMLProps` and will cause `pnpm run typecheck` to fail.
**Action:** When adding accessibility or micro-UX improvements to standard HTML elements (not SVGs), avoid using `focusable="false"` to prevent TypeScript compilation errors.
