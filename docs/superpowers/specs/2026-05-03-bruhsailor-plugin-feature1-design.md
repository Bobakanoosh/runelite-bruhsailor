# BRUHsailer Plugin — Feature 1 Design

**Date:** 2026-05-03
**Scope:** Feature 1 from `docs/plugin-overview.md` — the side panel — end-to-end. No Quest Helper integration, no bank filtering.

## Goal

A working RuneLite plugin that loads the bundled BRUHsailer guide, displays the current step in rich text in a side panel, and lets the user navigate steps and track completion. Persisted across sessions.

## Decisions Already Pinned (from `plugin-overview.md`)

- Java + Gradle, structured after RuneLite's `example-plugin` template.
- `guide_data.json` is bundled as a classpath resource at `src/main/resources/guide_data.json` (already present).
- Persistence via `ConfigManager`.
- Rich-text rendering must preserve at minimum the red-warning semantics of the source.

## Decisions Made During Brainstorming

- **Rendering approach:** `JTextPane` with `StyledDocument`, programmatic `AttributeSet`s. Rejected `JEditorPane`/HTML to match RuneLite's native panel aesthetic via `FontManager` and `ColorScheme`.
- **Panel layout:** current-step-focus pattern. Top: header (chapter + section title), full rich-rendered current step, metadata row, navigation controls. Below: compact scrollable list of all 227 steps with the current row highlighted and completed rows struck through.
- **Step list row label:** `1.1.1  <first-line snippet of step content>` (truncated). Section and chapter titles appear as non-selectable separator rows.
- **Completion semantics:** explicit. `[Mark complete ☐]` toggle on the current step. Prev/Next move the pointer only; they do not change completion state. Completed set and current pointer are independent.
- **Package:** `com.bruhsailor.plugin`.
- **Plugin display name:** `BRUHsailer Guide`.
- **ConfigManager group:** `bruhsailor`.
  - Key `currentStepId` — string `"chapter.section.step"`, default = first step.
  - Key `completedStepIds` — comma-joined sorted string, default empty.

## Architecture

Six units, each independently testable.

### 1. `StepId`

Value type wrapping `chapter.section.step`. Parse from string, format to string, natural ordering (chapter, then section, then step). Throws on malformed input.

### 2. `GuideRepository`

Loads `guide_data.json` once at plugin startup via classpath. Parses with Gson. Exposes:

- `List<Step> steps()` — flat ordered list of all 227 steps.
- `Optional<Step> findById(StepId)` — O(1) lookup.
- `String chapterTitleFor(StepId)` and `String sectionTitleFor(StepId)`.
- `int indexOf(StepId)` and `Optional<StepId> idAt(int)` for prev/next.

Immutable after construction. No mutation API. Throws on parse failure; the plugin handles the failure (see Error Handling).

`Step` is a plain data class: `StepId id`, `List<ContentFragment> content`, `List<NestedBlock> nestedContent`, `Metadata metadata`. `NestedBlock` carries `int level` + `List<ContentFragment> content` (matches the JSON shape `{level, content: [...]}`). `ContentFragment` carries `text` + nullable `bold`, `fontSize`, `color`.

### 3. `GuideStateService`

Owns user state. Constructor injects `ConfigManager` and `EventBus`. Methods:

- `StepId getCurrent()`
- `void setCurrent(StepId)` — persists, fires `GuideStateChanged` event.
- `void next()` / `void prev()` — clamped at list ends; no-op at boundaries.
- `boolean isComplete(StepId)`
- `void setComplete(StepId, boolean)` — persists, fires event.

On construction, reads persisted state. If `currentStepId` is missing or unknown to the repository, defaults to the first step and logs a warning. Unknown IDs in `completedStepIds` are dropped silently with a single warning log.

`completedIds` is held in memory as `Set<StepId>`; written to `ConfigManager` as a sorted comma-joined string for stable diffs.

### 4. `StepRenderer`

Pure function `JComponent render(Step)`. Builds a non-editable opaque `JTextPane` with a `StyledDocument`:

- Walk `content[]`. For each fragment, compute an `AttributeSet`:
  - `bold` → `StyleConstants.Bold = true`.
  - `fontSize` numeric → bucketed:
    - `≤ 11` → small (base − 2)
    - `12` → base (RuneLite default)
    - `≥ 14` → large (base + 2)
  - `color` `{r,g,b}` floats in `[0,1]` → `new Color(r, g, b)`. No remapping. Default when absent → `ColorScheme.LIGHT_GRAY_COLOR`.
- Append the fragment's text with that attribute set.
- A fragment whose text is `"\n"` produces a paragraph break.
- After `content`, render each `NestedBlock` in `nestedContent[]` as an indented paragraph (left margin via `StyleConstants.LeftIndent`, scaled by `block.level`); the block's inner `content` fragments are styled by the same fragment rules.

If applying styling to a single fragment throws, fall back to plain-text append for just that fragment and continue. One bad fragment must not black out the step.

Background: `ColorScheme.DARKER_GRAY_COLOR`. Base font: `FontManager.getRunescapeFont()`.

### 5. `BruhsailorPanel extends PluginPanel`

Layout, top to bottom:

1. **Header** — `JLabel` "Chapter N: …" above `JLabel` "N.M: …".
2. **Current step block** — scrollable container around `StepRenderer`'s output, then a metadata row (`gp_stack · total_time · skills_quests_met`), then a control row with `[Prev]`, `[Mark complete ☐]` toggle button, `[Next]`. `Prev` disabled at first step; `Next` disabled at last step.
3. **Step list** — `JList<StepListRow>` with a custom `ListCellRenderer`. Rows are either:
   - `SectionHeader` — chapter or section title, non-selectable, distinct background.
   - `StepRow` — `1.1.1  <snippet>`, with current row highlighted, completed rows struck through.
   Clicking a `StepRow` calls `state.setCurrent(id)`. Snippet is the first ~50 chars of plain-text-stripped `content[]`.

Subscribes to `GuideStateChanged` on the `EventBus`. On event:
- If current changed: rebuild header + current-step block, repaint list (cell renderer reads current via the service).
- If completion changed: repaint affected list rows + sync the toggle button.

Initial population happens once in the constructor.

### 6. `BruhsailorPlugin extends Plugin`

Wiring only. Annotated `@PluginDescriptor(name = "BRUHsailer Guide", description = "BRUHsailer ironman guide in a side panel", tags = {"ironman", "guide", "bruhsailer"})`.

`startUp()`:
- Construct `GuideRepository` (fails fast on bad JSON).
- Construct `GuideStateService` (reads persisted state).
- Construct `BruhsailorPanel`.
- Register a `NavigationButton` with the `ClientToolbar`. Icon: a small bundled PNG (placeholder for v1; iconography is not feature-critical).

`shutDown()`: deregister the navigation button, unsubscribe panel from event bus.

If `GuideRepository` construction fails, log the error and replace the panel content with a single error label so the user has visible feedback rather than a phantom plugin.

## Data Flow

```
guide_data.json (classpath)
        │ once at startUp
        ▼
GuideRepository ────reads──▶ BruhsailorPanel
                                  ▲
                                  │ GuideStateChanged events
                                  │
ConfigManager ◀──persists── GuideStateService ◀── user clicks
```

User action → service mutates → service persists → service fires event → panel re-renders. The panel never writes to `ConfigManager` directly.

## Persistence Format

ConfigManager group: `bruhsailor`.

| Key                  | Type   | Example                  | Default       |
|----------------------|--------|--------------------------|---------------|
| `currentStepId`      | String | `"1.1.1"`                | first step ID |
| `completedStepIds`   | String | `"1.1.1,1.1.2,1.2.5"`    | `""`          |

Recovery rules: unknown stored IDs are discarded with a single warning log; current falls back to first step.

## Error Handling

| Condition                                       | Behavior                                                        |
|-------------------------------------------------|-----------------------------------------------------------------|
| `guide_data.json` missing or malformed          | Plugin logs error; panel shows a single error label             |
| `currentStepId` references unknown step         | Fall back to first step, log warning                            |
| Entry in `completedStepIds` references unknown step | Drop entry, single warning log for the batch                |
| Render exception on a single content fragment   | Append fragment as plain text, continue rendering the step      |

No silent partial state. No retries.

## Testing

- **`StepIdTest`** — parse / format roundtrip, ordering across boundaries (`1.9.99` < `2.1.1`), rejection of malformed input.
- **`GuideRepositoryTest`** — load the real bundled `guide_data.json`, assert exactly 227 steps, IDs unique and parseable, every step has ≥1 content fragment, lookup-by-id roundtrip works for every step.
- **`GuideStateServiceTest`** — fake `ConfigManager` and `EventBus`. Cover: initial load with empty config, initial load with valid persisted state, initial load with unknown `currentStepId` (falls back), initial load with mixed valid/invalid completed IDs (drops invalid), prev at first step is no-op, next at last step is no-op, set-complete persists and fires event, toggle off removes from set.
- **`StepRendererTest`** — build a synthetic `Step` with bold, colored, sized, and nested fragments. Assert the produced `StyledDocument` has the expected attribute runs at the expected character offsets. No screen rendering required.
- **Panel** — manual smoke via the example-plugin runner. Not unit tested in v1.

## Project Layout

```
build.gradle              # adapted from runelite/example-plugin
settings.gradle
gradle.properties         # runeLiteVersion
src/main/java/com/bruhsailor/plugin/
    BruhsailorPlugin.java
    BruhsailorPanel.java
    GuideRepository.java
    GuideStateService.java
    StepRenderer.java
    StepId.java
    model/
        Step.java
        ContentFragment.java
        Metadata.java
src/main/resources/
    guide_data.json       # already exists
    step_mappings.json    # already exists, unused in feature 1
    icon.png              # navigation button icon (placeholder)
src/test/java/com/bruhsailor/plugin/
    StepIdTest.java
    GuideRepositoryTest.java
    GuideStateServiceTest.java
    StepRendererTest.java
```

## Out of Scope

- Quest Helper integration (Feature 2).
- Bank filtering via Bank Tags (Feature 3).
- Consumption of `step_mappings.json` (sidecar).
- Guide-update detection / hot-reload of `guide_data.json`.
- Look-ahead window in step list.
- Hyperlinks inside rendered step text.
- Plugin Hub submission.
