# RuneLite BRUHsailer Plugin — Design Notes

Living document. Captures decisions made during initial exploration so we don't lose context as we focus on individual workstreams.

## Goal

A RuneLite plugin that integrates the [BRUHsailer ironman guide](https://umkyzn.github.io/BRUHsailer/) into the game client.

## Features

### 1. Side panel with all tasks + current task
- Standard RuneLite `PluginPanel` (Swing).
- Shows the full ordered list of steps from the BRUHsailer guide.
- Tracks current step + completed steps; persisted via `ConfigManager`.
- Renders rich-text formatting from the guide (bold, color, font size). Red text in the guide carries semantic warnings — preserve at least that.

### 2. Quest Helper integration
- Quest Helper does not expose a clean public API for "start quest X".
- Approach: open the QH panel via its public `displayPanel()` method, deep-link by quest name. User clicks to actually start. Lowest friction, no reflection.
- Future escalation paths if UX demands it: reflection on private `startUpQuest`, or upstream a public API PR to Zoinkwiz/quest-helper.
- Treat QH as a soft dependency — detect via `PluginManager`, degrade gracefully if missing.

### 3. Bank filtering by current step
- Use the **Bank Tags** mechanism (the same one Quest Helper uses via `itemsToTag()` + `refreshBank()`). Not raw `BankSearch` (which has no public programmatic API).
- **Decision:** each step is the unit of filtering. One step → one filter set.
- **Decision:** our filter is triggered by our own button, mirroring QH's button. Both can coexist; the user picks which to apply. No conflict resolution needed.

## Data source

- BRUHsailer publishes structured JSON at `data/guide_data.json` in the [umkyzn/BRUHsailer](https://github.com/umkyzn/BRUHsailer) repo.
- 227 steps across 3 chapters / 9 sections.
- Schema: `chapter → section → step`; each step has `content[]` (formatted text fragments), `nestedContent[]`, `metadata { gp_stack, items_needed, total_time, skills_quests_met }`.
- `items_needed` is a comma-separated free-form string. Mostly clean OSRS item names with optional quantity prefix; ~5–10% are abstractions (`"melee gear"`, `"cash stack"`, `"food"`); 48 entries are `"tbd"`.
- Quest names appear in `content` prose, not metadata.
- **Decision:** ship the JSON bundled as a plugin resource, not fetched at runtime. Add an optional update-check command later.

## Step → structured-data mapping

The mapping problem is the linchpin for features 2 and 3. Decisions:

- **Approach B: hand-curated sidecar JSON** (`step_mappings.json`), shipped with the plugin.
- Generation pipeline: LLM-assisted (Haiku/Sonnet) per-step generation + follow-up verifier model + deterministic helper scripts (item-ID lookup, items_needed tokenizer, quest enum mapping).
- See `docs/sidecar-generation-design.md` for the pipeline design (separate doc).

### Step IDs
- Positional ID: `${chapterIdx}.${sectionIdx}.${stepIdx}` (e.g., `1.1.1`) as the lookup key.
- Content hash (SHA1 of concatenated step text) stored alongside as a "this step changed, re-verify mapping" signal.

### Quest mapping granularity
- Quest mapping is **per-step**, not section-level. Some steps span quest prep + execution; resolution should be at the step level for consistency with bank filtering.
- Reference Quest Helper's `QuestHelperQuest` enum for canonical IDs.

### Edge cases
- `"tbd"` → UI state "no items defined for this step", not an empty filter.
- Abstract items (`"melee gear"`, `"cash stack"`) → flagged as `abstractItems[]` with a label, not resolved to item IDs. Possibly expand later to tier-aware sets.
- Quantities preserved per item.

## Open questions / deferred

- How to render the guide's rich-text formatting in Swing — `JEditorPane` with HTML vs. custom layout. Decide during panel implementation.
- "Look-ahead window" in the bank filter (filter for next N steps) — deferred; per-step filtering is the v1 default.
- How to handle guide updates (new BRUHsailer JSON) — generator script will diff and only re-curate changed steps. Workflow detail TBD.
- Plugin name + Plugin Hub submission process — defer until the plugin actually works.

## Tech context

- Language: Java (RuneLite plugin standard).
- Build: Gradle (RuneLite example-plugin template).
- Soft deps: Quest Helper, Bank Tags (built-in to RuneLite).
- The sidecar generation pipeline lives in this repo too — likely under `tools/` or `scripts/`. Language for the generator: TBD (Node or Python both fine).
