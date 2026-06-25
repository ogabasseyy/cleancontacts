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

## 2026-03-19 - Contextual Empty States

**Learning:** Empty states should match their context. In a cleaning app, finding "No Contacts" in a junk or duplicates category isn't just an empty state—it's a success state (the user has finished cleaning!). Displaying generic "No items found" misses an opportunity to provide positive reinforcement and can feel like an error.
**Action:** When designing empty states for action-driven lists (like error reports or cleanup tasks), always evaluate if "empty" means "success." If so, celebrate it with a distinct success icon, positive messaging ("All Clear!"), and brand colors to reward the user.

## 2026-03-16 - Avoid False Affordances on Reusable Cards

**Learning:** Using an empty lambda (`{ /* no-op */ }`) for an `onClick` parameter on a reusable card component applies a `clickable` modifier, creating a "false affordance." Screen readers will announce it as a button or tab, and it will respond to touch/clicks (with ripples), confusing users when no action occurs.
**Action:** Make `onClick` parameters nullable (`(() -> Unit)? = null`) for reusable cards that may act as informational displays. Conditionally apply the `clickable` modifier and any interactive semantic roles (`Role.Button`, `Role.Tab`) ONLY when `onClick` is not null.

## 2026-03-19 - Explicit Content Descriptions for Dashboard Summary

**Learning:** For dashboard summary cards containing multiple disjoint values (like total issues and accounts), simply using `semantics(mergeDescendants = true)` can result in screen readers reading the numbers and text incohesively.
**Action:** When creating complex summary cards, provide a clear, synthesized sentence as the `contentDescription` on the parent container to ensure the user gets a meaningful overview.

## 2026-03-19 - Screen Reader Cohesion for Non-Interactive List Rows

**Learning:** Even for non-interactive list items (like `FeatureRow` displaying checkmarks and feature descriptions in a Paywall), screen readers will announce each element (the icon, the text) individually. This leads to disjointed "Check... Unlimited cleanups" announcements that disrupt the user's flow and cognitive understanding.
**Action:** Apply `.semantics(mergeDescendants = true) {}` to the outer container (e.g., `Row`) of informational list items grouping descriptive icons and text labels to ensure screen readers announce them as a single cohesive unit.

## 2026-03-21 - Cohesive Screen Reader Announcements on Custom Compose Cards

**Learning:** Adding `semantics(mergeDescendants = true)` alone to a parent container such as `Surface` causes screen readers to concatenate child text nodes in source order, which produces clunky announcements on multi-part cards.
**Action:** For interactive cards that present related values like count, title, and supporting detail, synthesize a single parent `contentDescription` so TalkBack and VoiceOver announce one structured sentence instead of fragmented child content.

## 2026-03-29 - Avoid Redundant Grouping on Lists of Badges

**Learning:** When grouping multiple badges (e.g., `FeatureBadge`) inside a parent container (like a `Row`), applying `semantics(mergeDescendants = true)` to both the individual badges AND the parent container creates a redundant grouping. The parent's modifier will collapse all badges into a single block of text, which degrades the accessibility experience by preventing screen reader users from navigating the badges individually.
**Action:** For lists or rows of distinct tags/badges, apply `semantics(mergeDescendants = true) {}` ONLY to the individual badge components, not to their parent container.

## 2024-05-15 - Interactive Elements Require Explicit Focus Rings
**Learning:** In custom Tailwind-based designs (like `landing-page`), `<a>` and `<button>` elements overriding default browser styles lose native high-contrast focus indicators. For keyboard navigation to be effective, custom pseudo-classes (`focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand`) must be explicitly applied.
**Action:** Always verify keyboard focus state (`Tab` key) when introducing new interactive components or links in the `landing-page`. Do not rely on default styling if the component has custom CSS/Tailwind utility classes.

## 2024-05-24 - Screen Reader Cohesion for Radio Button List Items

**Learning:** When building custom list items with radio buttons (e.g., `AccountSelectionItem`), applying `semantics(mergeDescendants = true) {}` to a `Surface.selectable` creates a single focus target, but TalkBack will simply concatenate the inner text elements (e.g., "Google, test@google.com") without context.
**Action:** Always synthesize a clear, formatted `contentDescription` inside the `semantics` block of the `selectable` parent, interpolating the relevant text variables (e.g., `contentDescription = "${account.displayLabel} - ${account.accountName}"`) so screen readers announce a logical, cohesive description.

## 2026-04-06 - Explicit Focus States on Custom Navigation Buttons

**Learning:** When using `<button>` or `<a>` elements with custom styling for navigation links in headers and footers, they often lose default browser focus outlines. Relying solely on `hover:` classes makes them inaccessible to keyboard users, preventing them from seeing which element is currently focused.

**Action:** Always provide explicit, high-contrast keyboard focus indicators using the `focus-visible` pseudo-class (e.g., `focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand`) on any custom interactive element used for navigation.

## 2024-06-25 - Web Decorative SVGs Inside Links

**Learning:** Decorative SVG icons inside standard Web components like `<Link>` or `<a>` are often incorrectly read by screen readers if they lack `aria-hidden="true"`. Furthermore, they can become focusable targets if `focusable="false"` is missing, causing multiple tab stops for a single link.
**Action:** Always add `aria-hidden="true"` and `focusable="false"` to decorative `<svg>` elements inside interactive components to reduce screen reader clutter and prevent redundant focus tab stops.

## 2026-04-09 - Accessible Empty States for Action Screens

**Learning:** For actionable task screens like `SensitiveReviewScreen`, generic "No data found" messages are unrewarding and lack cohesive accessibility structure. Grouping visual empty state components (icons and multi-line text) without `mergeDescendants` forces screen readers to read fragments.
**Action:** When creating success/empty states for actionable lists, apply `semantics(mergeDescendants = true)` to the outer container and synthesize the message into a single clear `contentDescription` (e.g., "All Clear! No sensitive data found"). Nullify inner icon descriptions to avoid redundant audio.

## 2026-10-27 - Custom Screen Reader Readouts for Text Containers

**Learning:** In Jetpack Compose, when overriding the screen reader announcement for a parent container (like a `Column` containing multiple `Text` elements), using `semantics(mergeDescendants = true) { contentDescription = "..." }` can result in the screen reader announcing both the custom description and the text of the child nodes.
**Action:** Use `Modifier.clearAndSetSemantics { contentDescription = "..." }` on the parent container to completely replace the semantics of all descendants, ensuring a single, cohesive, non-repetitive read-out.

## 2024-05-30 - Accessible Loading States

**Learning:** Skeleton loading states made purely of `div`s with `animate-pulse` are invisible to screen readers, leaving users wondering what is happening.
**Action:** Always add `aria-busy="true"` and an `aria-label` (e.g., "Loading blog posts") to the parent container of skeleton loaders. Additionally, add `aria-hidden="true"` to the inner decorative pulse `div`s to prevent them from creating structural noise for screen readers.

## 2026-04-15 - Interactive Card Links Missing Focus Indicators

**Learning:** Reusable card components (`SupportCard`, `TermsCard`) in the React/Tailwind landing page that utilize interactive <a> tags heavily rely on hover states but often overlook keyboard focus indicators, making them invisible to keyboard navigation.
**Action:** When implementing custom interactive elements (`<a>` or `<button>`) that override standard browser styling, always explicitly provide high-contrast keyboard focus indicators using the `focus-visible` pseudo-class (e.g., `focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 focus-visible:ring-offset-dark`) to ensure WCAG compliance and keyboard accessibility.

## 2026-04-16 - Web Decorative SVGs Inside Links (Focusability)

**Learning:** When using decorative `<svg>` elements within interactive components (like `<a>` or `<button>`), `aria-hidden="true"` correctly prevents screen reader announcements, but older browsers or certain environments can still apply keyboard focus to the SVG itself, resulting in redundant tab stops for a single link.
**Action:** Always add both `aria-hidden="true"` and `focusable="false"` to decorative `<svg>` elements inside interactive components to reduce screen reader clutter and prevent redundant focus tab stops.

## 2024-05-18 - Decorative Animated Indicators

**Learning:** Purely decorative animated elements, such as a pulsating dot (`<span className="animate-pulse"></span>`) used for visual emphasis (like a "live" or "new" status), can be interpreted as structural noise by screen readers if left without ARIA attributes.
**Action:** Always apply `aria-hidden="true"` to purely decorative, content-less animated elements to ensure screen readers ignore them and focus on the meaningful sibling text.

## 2026-04-27 - Valid HTML Headings for Accordion Buttons

**Learning:** When building accordions or similar interactive UI elements, placing a block-level heading (`<h3>`) inside an inline `<button>` is invalid HTML and breaks the document outline for screen readers. This creates a confusing experience and degrades accessibility.
**Action:** Always wrap the interactive element with the heading tag (e.g., `<h3><button>...</button></h3>`), rather than placing the heading inside the button. Additionally, ensure decorative SVGs within these buttons have `focusable="false"` to prevent double-focus bugs.
## 2024-04-29 - Accessible Error States and Skeleton Loaders
**Learning:** React error boundaries (`BlogErrorBoundary.tsx`) and conditional empty states (`BlogPost.tsx` "not found") need explicit `role="alert"` for screen readers to immediately announce the failure to the user. Additionally, skeleton loaders must wrap their structural decorative elements in `aria-hidden="true"` while providing an `aria-busy="true"` container with an accessible label to prevent noisy, confusing readouts.
**Action:** Always add `role="alert"` to fallback UI containers rendered during errors or 404s. For skeleton loaders, use the `role="status"`, `aria-busy="true"`, `aria-label="..."` pattern on the parent, and `aria-hidden="true"` on the pulsating children.

## 2024-05-14 - Interactive Feature Cards Hover States

**Learning:** Static feature cards, while informative, can feel dead and unresponsive to user interaction. Even if they don't navigate anywhere, providing visual feedback on hover improves the micro-UX and makes the application feel more polished and alive.
**Action:** When implementing grids of feature cards or informative panels, always apply subtle hover effects (e.g., a background color change and a slight negative Y translation like `hover:bg-white/10 hover:-translate-y-1 transition-all duration-300`) to provide users with immediate, pleasant visual feedback.

## 2026-05-18 - Semantic HTML Lists vs Generic Divs
**Learning:** Using generic `<div>` containers with spacing classes (e.g., `space-y-3`) to create visual lists is an anti-pattern that strips inherent structure from screen readers. Screen readers rely on semantic `<ol>` and `<ul>` tags to announce the number of items and provide list navigation shortcuts. Additionally, purely visual decorative markers (like "01", "WA") inside these lists create redundant and confusing audio clutter if not hidden.
**Action:** Always prefer native `<ol>` and `<ul>` tags for lists, using utility classes like `list-none p-0 m-0` to reset default browser styling if a custom visual design is required. Always add `aria-hidden="true"` to purely visual decorative markers within these lists.

## 2026-05-18 - Consistent Visual Affordance for Actions
**Learning:** Inconsistent visual affordances (such as lacking an email icon on a `mailto:` link in one card while having it in others) degrades the micro-UX and user trust. Users rely on consistent iconography to quickly identify the nature of an action.
**Action:** Ensure that recurring action types (like `mailto:` links, external links) utilize a consistent set of iconography across all similar components (e.g., all informational cards) to establish a predictable interaction pattern.

## 2026-05-24 - [Hide Collapsed FAQ Answers from Screen Readers]
**Learning:** CSS-hidden accordions (using max-height and opacity) remain accessible in the accessibility tree, causing screen readers to read collapsed answers. This creates a confusing experience.
**Action:** Always use `aria-hidden={true/false}` on the content region of CSS-hidden accordions to properly sync the accessibility tree with the visual state.

## 2026-06-15 - Tactile Feedback for Primary CTAs
**Learning:** Call-to-action buttons (like 'Get App' or 'Contact Support') that only provide visual hover states can feel disconnected during actual click interactions, especially in a heavily styled UI. Adding a subtle scale-down effect on the active state provides tactile-like feedback, confirming the user's interaction instantly.
**Action:** In the landing page design system, apply `active:scale-95 transition-all duration-200` to primary Call-to-Action buttons (alongside hover states) to provide consistent tactile feedback during click interactions. Ensure any existing `transition-colors` classes are replaced with `transition-all` so the scaling animates smoothly.
