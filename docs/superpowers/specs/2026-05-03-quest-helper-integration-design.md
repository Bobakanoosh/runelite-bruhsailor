# Quest Helper Integration — Design

**Date:** 2026-05-03
**Scope:** Feature 2 from `docs/plugin-overview.md` — render clickable chips in the current-step block for quests Quest Helper can open. Soft dependency: panel keeps working without QH installed.

## Goal

When a step has mapped quest IDs, render each as a small clickable chip below the rich-text content. Click → switches RuneLite's right panel to Quest Helper, deep-linked to that quest. If QH isn't installed or its API has shifted, the chips degrade gracefully — no panel crash, no error popup spam.

## Decisions Already Pinned (`plugin-overview.md`)

- Approach: open the QH panel via its public `displayPanel()` method, deep-link by quest name. The user clicks "Start" inside QH themselves.
- QH is a **soft dependency** — detect via `PluginManager`, degrade gracefully if missing.
- Per-step quest mapping (lookup by `StepId`).

## Decisions Made During Brainstorming

- **UI shape:** chips inline in the step content area (option C). Chips appear below the `StepRenderer` output, inside `currentStepHolder`. Each chip is its own clickable component (not embedded mid-paragraph in the `JTextPane`).
- **Filtering of `questIds[]`:** soft filter (option B). At startup, build the canonical `{enumName → displayName}` map from the bundled `quests.json` (266 entries scraped by the sidecar generator), then enrich with whatever the loaded QH plugin's `QuestHelperQuest` enum exposes via reflection. Any ID in `questIds[]` that resolves through this map gets a chip; others are skipped silently.
- **Item chips and `items[]` consumption are out of scope** for this feature; that's Feature 3.

## Bundled Resources

- `src/main/resources/quests.json` — copy of `tools/sidecar-gen/data/quests.json` (266 entries: `{enumName, displayName, aliases, shortAliases}`). Bundled so the chip set is deterministic without QH installed.
- `src/main/resources/step_mappings.json` — already present; not previously consumed by the plugin.

## Architecture

Four new units, plus targeted edits to two existing ones.

### 1. `QuestRegistry`

Owns the canonical set of quest entries (`{String enumName, String displayName}`). Public surface:

- `static QuestRegistry create(PluginManager pluginManager)` — loads bundled `quests.json`, then attempts runtime enrichment from QH (see Reflection paths below). Either step's failure is non-fatal.
- `Optional<QuestEntry> resolve(String enumName)` — returns the canonical entry or empty.
- `boolean enrichedFromRuntime()` — true if the QH-runtime enrichment succeeded; observable for tests + UI tooltips.

Immutable after construction. Internally a `Map<String, QuestEntry>` keyed by `enumName`.

### 2. `StepMappings`

Mirror of `GuideRepository` for sidecar data.

- `static StepMappings loadBundled()` — loads `step_mappings.json` from classpath, parses with Gson into a flat `Map<StepId, StepMapping>`. Throws on missing resource; the plugin catches and continues without chips.
- `Optional<StepMapping> findById(StepId)` — O(1) lookup.
- `StepMapping` is a plain data class: `List<String> questIds`, `List<Item> items` (for Feature 3, declared but unused in this spec), and `String contentHash` (for future change-detection; not used in this feature).

### 3. `QuestHelperBridge`

Soft façade for opening QH. All reflection lives here; consumers never see a `Class<?>` or `Method`.

- `boolean isInstalled()` — true if `pluginManager.getPlugins()` contains a plugin whose class name is `com.questhelper.QuestHelperPlugin` (or fall through if QH renamed it). Cached after first call.
- `boolean open(QuestEntry quest)` — show the QH panel and select `quest.displayName()`. Returns true on success, false on any reflection failure. Log warning once per failed reflection target; never throw.

Reflection plan, in order of preference:
1. Locate the QH plugin instance via `PluginManager.getPlugins()`.
2. Find a public method named `displayPanel(String)` or `displayPanel(QuestHelperQuest)` on it. Invoke with the matching argument shape.
3. If neither exists, fall back to: locate the QH `NavigationButton` from QH's internal toolbar entry (look for it in `ClientToolbar`'s registered buttons), trigger `onClick`. This opens QH's panel without selecting a quest — partial success.
4. If everything fails, return false. Caller renders a passive failure (chip stays clickable but no-op; tooltip explains).

### 4. `QuestChip`

Small clickable Swing component. Static factory:

- `JComponent QuestChip.create(QuestEntry quest, QuestHelperBridge bridge)`

Returns a `JLabel`-based pill:
- Text: `<displayName> ↗`.
- Font: `FontManager.getRunescapeFont().deriveFont(14f)`.
- Background: `ColorScheme.DARK_GRAY_HOVER_COLOR`. Foreground: `ColorScheme.LIGHT_GRAY_COLOR`.
- Border: 1px line `ColorScheme.DARKER_GRAY_HOVER_COLOR`, rounded inset via empty border padding `(4, 8, 4, 8)`.
- Cursor: `HAND_CURSOR` when QH installed; default cursor otherwise.
- Tooltip: `"Open in Quest Helper"` when installed; `"Quest Helper plugin not installed"` when not.
- Click: if `bridge.isInstalled()` is true, call `bridge.open(quest)` synchronously on the EDT (reflection is cheap; no I/O). On failure (returned false), update tooltip to `"Couldn't open Quest Helper — see logs"` and skip further attempts for this chip.

The chip is purely view-layer; it doesn't subscribe to events.

### 5. Edits to `BruhsailorPanel`

- Constructor signature gains `StepMappings stepMappings`, `QuestRegistry questRegistry`, `QuestHelperBridge questBridge`.
- New private field `JPanel chipsRow` (a `JPanel` with `FlowLayout(LEFT, 4, 4)`, transparent background, `LEFT_ALIGNMENT`). Added inside `currentStepHolder`'s `BorderLayout.SOUTH`.
- `refreshAll()` — after re-rendering the step body into `BorderLayout.CENTER`, rebuild `chipsRow`:
  1. Clear existing children.
  2. `stepMappings.findById(currentStepId)` → if absent, hide the row (`chipsRow.setVisible(false)`) and return.
  3. For each `String enumName` in `questIds`, `questRegistry.resolve(enumName)` → skip if empty.
  4. For each resolved `QuestEntry`, add `QuestChip.create(entry, questBridge)`.
  5. If no chips added, hide the row; otherwise `setVisible(true)`.
  6. `chipsRow.revalidate(); chipsRow.repaint();`
- The row's height is part of the step-block's 65/35 split allocation — it shrinks the rich-text vertical space when chips exist. That's acceptable; the rich-text viewport scrolls.

### 6. Edits to `BruhsailorPlugin`

`startUp()` becomes:

```java
GuideRepository repo = GuideRepository.loadBundled();
StepMappings mappings;
try { mappings = StepMappings.loadBundled(); }
catch (RuntimeException e) { log.error("Failed to load step_mappings", e); mappings = StepMappings.empty(); }
QuestRegistry registry = QuestRegistry.create(pluginManager);
QuestHelperBridge bridge = new QuestHelperBridge(pluginManager);
GuideStateService state = new GuideStateService(repo, configManager, eventBus);
panel = new BruhsailorPanel(repo, state, eventBus, mappings, registry, bridge);
```

Inject `PluginManager` — already available in RuneLite plugin DI.

`StepMappings.empty()` returns a no-op repository whose `findById` always returns `Optional.empty()`. Used so the panel's chip logic can stay branchless on the "couldn't load" path.

## Data Flow

```
quests.json (classpath) ─┐
                         ├──▶ QuestRegistry ──┐
QH plugin (runtime)      ┘                    │
                                              ▼
step_mappings.json (classpath) ─▶ StepMappings ──▶ BruhsailorPanel ──▶ QuestChip
                                                          │              │
                                                          │              ▼
                                                          │      QuestHelperBridge
                                                          │              │
                                                          │ click        │ reflection
                                                          ▼              ▼
                                                   user click     QH plugin instance
```

State changes still flow through `GuideStateService` and `GuideStateChanged` events; chips are rebuilt during `refreshAll()` like the rest of the step block.

## Persistence

No new persisted state. ConfigManager group `bruhsailor` is unchanged.

## Reflection Discipline

- All reflection lives inside `QuestHelperBridge`.
- Each reflection target (find plugin instance, find method, invoke method) is wrapped in its own `try/catch (Throwable)` with a `LOG.warn` on first failure.
- A `volatile boolean reflectionDisabled` short-circuits subsequent attempts after a failure mode is hit, preventing log spam.
- No `--add-opens` or `setAccessible(true)` — we only call public methods. If QH's targets aren't public, reflection fails fast and we degrade.

## Error Handling

| Condition | Behavior |
|---|---|
| `quests.json` missing or malformed | Log error. `QuestRegistry` constructed with empty bundled set; runtime enrichment may still populate. If both fail → no chips ever render (graceful). |
| `step_mappings.json` missing or malformed | Log error. Use `StepMappings.empty()` so the panel never crashes. Chips never render. |
| QH not installed | Chips render but disabled (gray, no hover, tooltip explains). |
| QH installed but reflection target moved/renamed | `bridge.open()` returns false. First failure logs a warning; subsequent attempts short-circuit. Chip's tooltip updates to "Couldn't open Quest Helper — see logs". |
| `questIds[]` contains an ID with no canonical match | Skip silently — this is the documented filter behavior. |

## Testing

- **`QuestRegistryTest`** — load bundled list, assert >= 200 entries (real number is 266; allow growth). `resolve("COOKS_ASSISTANT")` returns "Cook's Assistant". `resolve("AGILITY")` returns empty (skill markers are not in the canonical list). Reflection path tested with a `PluginManager` mock returning no plugins → `enrichedFromRuntime()` returns false, but bundled set still works.
- **`StepMappingsTest`** — load real bundled JSON, assert all 227 step IDs are present, every entry's `questIds` is a non-null list.
- **`QuestHelperBridgeTest`** — `PluginManager` mock returning no plugins → `isInstalled()` false, `open()` returns false. With a mock plugin whose class name matches but has no `displayPanel` → `open()` returns false, no throw.
- **`QuestChipTest`** — render with `bridge.isInstalled() == false`, assert tooltip + cursor + foreground. Render with installed bridge, simulate click, assert `bridge.open(entry)` was invoked.
- **Panel integration** — manual smoke. Verify a step with `COOKS_ASSISTANT` shows a "Cook's Assistant ↗" chip and clicking it opens QH (when installed).

## Project Layout Additions

```
src/main/java/com/bruhsailor/plugin/
    QuestRegistry.java                       (new)
    QuestEntry.java                          (new — small value type)
    StepMappings.java                        (new)
    QuestHelperBridge.java                   (new)
    QuestChip.java                           (new)
    model/StepMapping.java                   (new — pojo)
    model/Item.java                          (new — pojo, unused this feature)
    BruhsailorPanel.java                     (modified)
    BruhsailorPlugin.java                    (modified)
src/main/resources/
    quests.json                              (new — copied from tools/sidecar-gen/data/)
    step_mappings.json                       (already present)
src/test/java/com/bruhsailor/plugin/
    QuestRegistryTest.java                   (new)
    StepMappingsTest.java                    (new)
    QuestHelperBridgeTest.java               (new)
    QuestChipTest.java                       (new)
```

## Out of Scope

- Item chips / Bank Tags integration (Feature 3).
- Auto-starting a quest in QH (we deep-link only; the user still clicks Start).
- Live "this step's quest is in progress" status surfacing.
- Caching the runtime-enriched registry to disk.
- Diary helpers and skill markers as chips (`ARDOUGNE_HARD`, `AGILITY`) — only canonical QH quests render.
- Translating quest acronyms in chip labels (sidecar already canonicalises during generation).
