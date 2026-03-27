
## 2024-03-05 - Contextual Content Descriptions in List Actions
**Learning:** Screen readers announce a generic "Undo" or "Skip" repeatedly when list item actions have static descriptions. To give proper context, we need to interpolate the specific list item's data (e.g., `contentDescription = "Undo ${snapshot.description}"`).
**Action:** Always inject specific item names or descriptions into the `contentDescription` of interactive elements inside lists.
