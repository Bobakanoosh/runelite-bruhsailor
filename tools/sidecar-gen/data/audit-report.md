# BRUHsailer Sidecar Audit Report
Generated: 2026-05-03T04:56:04.784Z
Auditor: 227 parallel Haiku subagents

## Summary

- **Total steps audited:** 227
- **Accurate:** 116 (51.1%)
- **Inaccurate:** 111 (48.9%)

## Confidence Distribution

- **Mean:** 0.815
- **Median:** 0.870
- **High (≥0.90):** 95 (41.9%)
- **Mid (0.70-0.90):** 98 (43.2%)
- **Low (<0.70):** 34 (15.0%)

## Issue Pattern Counts

- **other:** 125
- **missing-item:** 88
- **spurious-item:** 75
- **qty-issue:** 26
- **wrong-quest:** 23
- **unresolved-resolvable:** 21
- **tbd-step:** 7
- **abstract-misuse:** 4
- **tier-mismatch:** 3

## Lowest Confidence Steps (review priority)

| Step | Conf | Accurate | Issues | Notes (truncated) |
|------|------|----------|--------|---------------------|
| 1.1.18 | 0.35 | false | 2 | Quests and abstracts are correct; abstractions properly capture warm clothing, cash stacks, and food. The core problem i |
| 1.4.18 | 0.35 | false | 6 | Step involves Brimhaven jungle activities (gout tuber chopping, spider combat, cooking) and farming. Mapping lacks criti |
| 1.4.22 | 0.35 | false | 5 | Mapping has significant errors in item inclusion, quest scope, and GP estimation. The abstract 'gp' label is correct, bu |
| 1.4.8 | 0.40 | false | 3 | Core non-food items (Chronicle, spade, leather, needle, thread, runes) accurately mapped. Quest ID (THE_GRAND_TREE) corr |
| 1.2.8 | 0.42 | false | 5 | Mapping has multiple category violations: extraneous item (Firemaking Cape), unresolved specific items (insect repellant |
| 1.1.15 | 0.45 | false | 2 | Significant omission of 18 planks needed at step start. Quest mapping for WITCHS_POTION is correct. Items, abstracts, an |
| 1.2.3 | 0.45 | false | 5 | Quest mapping is accurate (THE_QUEEN_OF_THIEVES active). GP abstracts correct. However, 4 required items remain unresolv |
| 1.3.7 | 0.45 | false | 1 | Elemental bar is a specific, resolvable item with clear quantity from 'Make 3 elemental bars'. All other mappings accura |
| 1.3.27 | 0.45 | false | 3 | Major omission of FAIRYTALE_I quest which is explicitly progressed this step. Vinegar should be resolved to item ID 2022 |
| 2.1.3 | 0.45 | false | 6 | Core items (knife, rune axe, saw, hammer, forestry kit, cash stack) correctly identified but primary resource (oak plank |
| 2.1.41 | 0.45 | false | 8 | Only 3/10 items correct (berserker helm, pet rock, ancient staff). Excessive false positives from items_needed field; ne |
| 2.2.1 | 0.45 | false | 4 | itemsNeeded='tbd' allows confidence floor of 0.85 per rules, but mapping contains clear hallucinations (Ruby, Soul rune) |
| 1.1.11 | 0.62 | false | 4 | Quest mapping is correct (7 active quests). Most items properly resolved, but 4 entries incorrectly placed in unresolved |
| 1.1.17 | 0.62 | false | 2 | Quests and abstracts are correct. Item capture has a critical tier/variant mismatch (arrow shafts vs headless arrows) an |
| 1.4.28 | 0.62 | false | 6 | Quest mapping has false positive (RAG_AND_BONE_MAN_II). Item mapping leaves 6 items unresolved that should resolve or be |
| 2.1.45 | 0.62 | false | 8 | Quest mapping is correct (3/3 actively progressed). Core items from itemsNeeded are mostly mapped, but several items in  |
| 2.2.28 | 0.62 | false | 4 | Two critical items (Rock cake, Absorption) are clearly wrong and should be removed. Prayer potion(4), Dragon defender, B |
| 3.1.7 | 0.62 | false | 6 | Step is primarily about slayer training consumables and combat gear. Abstract items correctly identify rune/potion categ |
| 3.2.13 | 0.62 | false | 5 | Step text describes mahogany training + build/upgrade furniture, but item list contains many unrelated consumables and u |
| 1.1.1 | 0.65 | false | 3 | Quest and abstract items correct, but item mapping has a critical variant issue. The Jug needs to be filled with water p |
| 1.2.2 | 0.65 | false | 2 | Core items and quests mostly correct, but includes 3 unnecessary arrow variants and 1 context-only quest. Rune quantitie |
| 1.2.6 | 0.65 | false | 5 | Multiple quantity errors and missing items (bucket packs, runes). Quest mapping is correct. GP abstraction is proper. Pr |
| 1.2.14 | 0.65 | false | 4 | Fishing Contest quest is correctly included. GP and raw fish abstractions are appropriate. Primary issues: wrong fish it |
| 1.3.8 | 0.65 | false | 3 | Core item mappings (fire staff, runes, chisel, Level 3 certificate) are correct and properly sourced. However, extraneou |
| 1.3.10 | 0.65 | false | 2 | Core quest progression captured correctly. Two item mapping errors: extraneous adamant pickaxe and unresolved cut sapphi |
| 1.3.14 | 0.65 | false | 3 | Three critical mapping errors: wrong shortsword variant, and two unresolved items that are actually resolvable from item |
| 1.4.11 | 0.65 | false | 6 | Core items correct (rune axe, forestry kit, necklace of passage) but mapping polluted with optional/unmentioned items. I |
| 2.1.12 | 0.65 | false | 3 | Quest mapping incomplete (3/4 required quests missing BKF). Initiate armor pieces need specific ID resolution rather tha |
| 3.1.6 | 0.65 | false | 3 | Step text focuses on: sand purchasing, superglass make, glass blowing to 70 crafting, Pyramid Plunder sceptre collection |
| 1.4.9 | 0.68 | false | 7 | Mapping partially complete but hampered by unresolved items that should resolve (wind runes, face mask) and unclear ques |

## Highest Confidence Steps (sanity check)

| Step | Conf | Issues |
|------|------|--------|
| 1.2.7 | 1.00 | 0 |
| 2.1.46 | 1.00 | 0 |
| 2.2.10 | 1.00 | 0 |
| 1.1.12 | 0.99 | 0 |
| 1.2.15 | 0.98 | 0 |
| 1.3.4 | 0.98 | 0 |
| 1.3.12 | 0.98 | 0 |
| 1.3.15 | 0.98 | 0 |
| 1.3.19 | 0.98 | 0 |
| 1.3.20 | 0.98 | 0 |

## All Verdicts

| Step | Conf | Accurate | #Issues |
|------|------|----------|---------|
| 1.1.1 | 0.65 | false | 3 |
| 1.1.2 | 0.95 | true | 0 |
| 1.1.3 | 0.95 | true | 0 |
| 1.1.4 | 0.75 | false | 1 |
| 1.1.5 | 0.92 | false | 1 |
| 1.1.6 | 0.92 | false | 1 |
| 1.1.7 | 0.88 | false | 1 |
| 1.1.8 | 0.95 | true | 0 |
| 1.1.9 | 0.92 | true | 1 |
| 1.1.10 | 0.72 | false | 2 |
| 1.1.11 | 0.62 | false | 4 |
| 1.1.12 | 0.99 | true | 0 |
| 1.1.13 | 0.75 | false | 2 |
| 1.1.14 | 0.75 | false | 1 |
| 1.1.15 | 0.45 | false | 2 |
| 1.1.16 | 0.75 | false | 1 |
| 1.1.17 | 0.62 | false | 2 |
| 1.1.18 | 0.35 | false | 2 |
| 1.2.1 | 0.75 | false | 2 |
| 1.2.2 | 0.65 | false | 2 |
| 1.2.3 | 0.45 | false | 5 |
| 1.2.4 | 0.78 | false | 1 |
| 1.2.5 | 0.75 | false | 1 |
| 1.2.6 | 0.65 | false | 5 |
| 1.2.7 | 1.00 | true | 0 |
| 1.2.8 | 0.42 | false | 5 |
| 1.2.9 | 0.72 | false | 2 |
| 1.2.10 | 0.72 | false | 1 |
| 1.2.11 | 0.80 | true | 1 |
| 1.2.12 | 0.72 | false | 3 |
| 1.2.13 | 0.72 | false | 2 |
| 1.2.14 | 0.65 | false | 4 |
| 1.2.15 | 0.98 | true | 0 |
| 1.3.1 | 0.95 | true | 1 |
| 1.3.2 | 0.78 | false | 1 |
| 1.3.3 | 0.88 | true | 1 |
| 1.3.4 | 0.98 | true | 0 |
| 1.3.5 | 0.92 | false | 1 |
| 1.3.6 | 0.72 | false | 2 |
| 1.3.7 | 0.45 | false | 1 |
| 1.3.8 | 0.65 | false | 3 |
| 1.3.9 | 0.72 | false | 4 |
| 1.3.10 | 0.65 | false | 2 |
| 1.3.11 | 0.75 | false | 1 |
| 1.3.12 | 0.98 | true | 0 |
| 1.3.13 | 0.72 | false | 3 |
| 1.3.14 | 0.65 | false | 3 |
| 1.3.15 | 0.98 | true | 0 |
| 1.3.16 | 0.95 | true | 0 |
| 1.3.17 | 0.72 | false | 3 |
| 1.3.18 | 0.72 | false | 2 |
| 1.3.19 | 0.98 | true | 0 |
| 1.3.20 | 0.98 | true | 0 |
| 1.3.21 | 0.75 | false | 1 |
| 1.3.22 | 0.75 | false | 1 |
| 1.3.23 | 0.95 | true | 1 |
| 1.3.24 | 0.78 | false | 1 |
| 1.3.25 | 0.95 | true | 0 |
| 1.3.26 | 0.95 | true | 0 |
| 1.3.27 | 0.45 | false | 3 |
| 1.3.28 | 0.72 | false | 4 |
| 1.3.29 | 0.95 | true | 0 |
| 1.3.30 | 0.75 | false | 2 |
| 1.3.31 | 0.80 | true | 1 |
| 1.4.1 | 0.95 | true | 0 |
| 1.4.2 | 0.82 | true | 1 |
| 1.4.3 | 0.98 | true | 0 |
| 1.4.4 | 0.72 | false | 3 |
| 1.4.5 | 0.85 | true | 2 |
| 1.4.6 | 0.82 | false | 1 |
| 1.4.7 | 0.72 | false | 3 |
| 1.4.8 | 0.40 | false | 3 |
| 1.4.9 | 0.68 | false | 7 |
| 1.4.10 | 0.98 | true | 0 |
| 1.4.11 | 0.65 | false | 6 |
| 1.4.12 | 0.95 | true | 0 |
| 1.4.13 | 0.72 | false | 1 |
| 1.4.14 | 0.72 | false | 1 |
| 1.4.15 | 0.95 | true | 0 |
| 1.4.16 | 0.68 | false | 3 |
| 1.4.17 | 0.98 | true | 0 |
| 1.4.18 | 0.35 | false | 6 |
| 1.4.19 | 0.87 | true | 2 |
| 1.4.20 | 0.78 | false | 3 |
| 1.4.21 | 0.72 | false | 2 |
| 1.4.22 | 0.35 | false | 5 |
| 1.4.23 | 0.95 | true | 0 |
| 1.4.24 | 0.75 | false | 1 |
| 1.4.25 | 0.72 | false | 3 |
| 1.4.26 | 0.82 | true | 2 |
| 1.4.27 | 0.92 | true | 1 |
| 1.4.28 | 0.62 | false | 6 |
| 1.4.29 | 0.95 | true | 0 |
| 1.4.30 | 0.72 | false | 3 |
| 1.4.31 | 0.72 | false | 5 |
| 1.4.32 | 0.72 | false | 3 |
| 1.4.33 | 0.68 | false | 4 |
| 1.4.34 | 0.95 | true | 0 |
| 2.1.1 | 0.72 | false | 3 |
| 2.1.2 | 0.78 | false | 1 |
| 2.1.3 | 0.45 | false | 6 |
| 2.1.4 | 0.80 | false | 1 |
| 2.1.5 | 0.78 | false | 2 |
| 2.1.6 | 0.75 | false | 1 |
| 2.1.7 | 0.95 | true | 0 |
| 2.1.8 | 0.98 | true | 0 |
| 2.1.9 | 0.75 | false | 2 |
| 2.1.10 | 0.72 | false | 4 |
| 2.1.11 | 0.98 | true | 0 |
| 2.1.12 | 0.65 | false | 3 |
| 2.1.13 | 0.95 | true | 0 |
| 2.1.14 | 0.72 | false | 2 |
| 2.1.15 | 0.98 | true | 0 |
| 2.1.16 | 0.95 | true | 0 |
| 2.1.17 | 0.92 | true | 1 |
| 2.1.18 | 0.68 | false | 7 |
| 2.1.19 | 0.93 | true | 0 |
| 2.1.20 | 0.92 | true | 1 |
| 2.1.21 | 0.92 | true | 0 |
| 2.1.22 | 0.95 | true | 0 |
| 2.1.23 | 0.95 | true | 0 |
| 2.1.24 | 0.88 | true | 1 |
| 2.1.25 | 0.95 | true | 0 |
| 2.1.26 | 0.96 | true | 0 |
| 2.1.27 | 0.68 | false | 4 |
| 2.1.28 | 0.95 | true | 0 |
| 2.1.29 | 0.75 | false | 2 |
| 2.1.30 | 0.78 | false | 1 |
| 2.1.31 | 0.98 | true | 0 |
| 2.1.32 | 0.98 | true | 0 |
| 2.1.33 | 0.75 | false | 3 |
| 2.1.34 | 0.88 | true | 0 |
| 2.1.35 | 0.72 | false | 1 |
| 2.1.36 | 0.88 | true | 0 |
| 2.1.37 | 0.90 | true | 0 |
| 2.1.38 | 0.87 | true | 1 |
| 2.1.39 | 0.87 | true | 0 |
| 2.1.40 | 0.72 | false | 2 |
| 2.1.41 | 0.45 | false | 8 |
| 2.1.42 | 0.87 | true | 2 |
| 2.1.43 | 0.88 | true | 2 |
| 2.1.44 | 0.95 | true | 0 |
| 2.1.45 | 0.62 | false | 8 |
| 2.1.46 | 1.00 | true | 0 |
| 2.2.1 | 0.45 | false | 4 |
| 2.2.2 | 0.72 | false | 4 |
| 2.2.3 | 0.72 | false | 2 |
| 2.2.4 | 0.90 | false | 2 |
| 2.2.5 | 0.92 | true | 1 |
| 2.2.6 | 0.82 | false | 2 |
| 2.2.7 | 0.95 | true | 0 |
| 2.2.8 | 0.78 | false | 2 |
| 2.2.9 | 0.72 | false | 4 |
| 2.2.10 | 1.00 | true | 0 |
| 2.2.11 | 0.95 | true | 0 |
| 2.2.12 | 0.95 | true | 0 |
| 2.2.13 | 0.94 | true | 0 |
| 2.2.14 | 0.92 | true | 2 |
| 2.2.15 | 0.95 | true | 0 |
| 2.2.16 | 0.92 | true | 1 |
| 2.2.17 | 0.95 | true | 0 |
| 2.2.18 | 0.92 | true | 1 |
| 2.2.19 | 0.92 | true | 0 |
| 2.2.20 | 0.92 | true | 0 |
| 2.2.21 | 0.95 | true | 0 |
| 2.2.22 | 0.92 | true | 0 |
| 2.2.23 | 0.72 | false | 1 |
| 2.2.24 | 0.72 | false | 3 |
| 2.2.25 | 0.88 | true | 2 |
| 2.2.26 | 0.92 | true | 0 |
| 2.2.27 | 0.92 | true | 2 |
| 2.2.28 | 0.62 | false | 4 |
| 2.2.29 | 0.95 | true | 0 |
| 2.2.30 | 0.90 | true | 0 |
| 2.2.31 | 0.88 | true | 1 |
| 2.2.32 | 0.87 | true | 2 |
| 2.2.33 | 0.87 | true | 2 |
| 2.2.34 | 0.82 | false | 5 |
| 2.2.35 | 0.92 | true | 0 |
| 2.2.36 | 0.72 | false | 4 |
| 2.2.37 | 0.78 | false | 7 |
| 2.2.38 | 0.72 | false | 5 |
| 2.2.39 | 0.92 | true | 0 |
| 2.3.1 | 0.95 | true | 0 |
| 2.3.2 | 0.90 | true | 2 |
| 2.3.3 | 0.95 | true | 0 |
| 2.3.4 | 0.85 | true | 3 |
| 2.3.5 | 0.95 | true | 0 |
| 2.3.6 | 0.95 | true | 0 |
| 2.3.7 | 0.95 | true | 0 |
| 2.3.8 | 0.95 | true | 0 |
| 2.3.9 | 0.87 | true | 0 |
| 2.3.10 | 0.95 | true | 0 |
| 2.3.11 | 0.92 | true | 0 |
| 2.3.12 | 0.95 | true | 0 |
| 2.3.13 | 0.95 | true | 0 |
| 2.3.14 | 0.72 | false | 4 |
| 2.3.15 | 0.95 | true | 0 |
| 3.1.1 | 0.75 | false | 4 |
| 3.1.2 | 0.95 | true | 0 |
| 3.1.3 | 0.95 | true | 0 |
| 3.1.4 | 0.72 | false | 2 |
| 3.1.5 | 0.92 | true | 4 |
| 3.1.6 | 0.65 | false | 3 |
| 3.1.7 | 0.62 | false | 6 |
| 3.1.8 | 0.82 | true | 3 |
| 3.1.9 | 0.95 | true | 0 |
| 3.1.10 | 0.72 | false | 4 |
| 3.1.11 | 0.88 | true | 0 |
| 3.1.12 | 0.88 | true | 1 |
| 3.1.13 | 0.72 | false | 3 |
| 3.1.14 | 0.72 | false | 4 |
| 3.1.15 | 0.72 | false | 2 |
| 3.1.16 | 0.95 | true | 0 |
| 3.2.1 | 0.72 | false | 4 |
| 3.2.2 | 0.88 | false | 2 |
| 3.2.3 | 0.95 | true | 0 |
| 3.2.4 | 0.72 | false | 2 |
| 3.2.5 | 0.90 | true | 0 |
| 3.2.6 | 0.92 | true | 0 |
| 3.2.7 | 0.82 | true | 1 |
| 3.2.8 | 0.70 | false | 2 |
| 3.2.9 | 0.95 | true | 0 |
| 3.2.10 | 0.78 | false | 2 |
| 3.2.11 | 0.88 | true | 0 |
| 3.2.12 | 0.92 | true | 0 |
| 3.2.13 | 0.62 | false | 5 |