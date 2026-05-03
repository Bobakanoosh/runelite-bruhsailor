import { mkdir } from "node:fs/promises";
import { dirname } from "node:path";
import type { QuestEntry, QuestTable } from "./types.ts";
import { QUEST_HELPER_ENUM_URL, QUEST_TABLE_PATH } from "./paths.ts";

async function ensureDir(path: string) {
  await mkdir(dirname(path), { recursive: true });
}

// QuestHelperQuest.java enum entries look like:
//   COOKS_ASSISTANT(new CooksAssistant(), Quest.COOKS_ASSISTANT, ...),
//   SHIELD_OF_ARRAV_PHOENIX_GANG(new ...(), Quest.SHIELD_OF_ARRAV.getId(), "Shield of Arrav - Phoenix Gang", ...),
// Most entries derive their display name from the referenced Quest enum at
// runtime, so a string literal is rare. We extract every enum name and
// optionally pick up an inline display string when present.
const ENTRY_RE = /^\s*([A-Z][A-Z0-9_]+)\s*\(\s*new\s+[A-Za-z0-9_]+\s*\(\s*\)\s*,([^\n]*)/gm;
const INLINE_STRING_RE = /"([^"]+)"/;
// Quest acronyms used by the BRUHsailer guide (and OSRS players generally).
// Case-sensitive whole-word match against step text, so "DT" won't match inside
// "DTII" or other letters.
const ACRONYMS: Record<string, string[]> = {
  DESERT_TREASURE: ["DT"],
  DESERT_TREASURE_II: ["DT2", "DTII"],
  DRAGON_SLAYER_I: ["DS", "DS1"],
  DRAGON_SLAYER_II: ["DS2", "DSII"],
  MONKEY_MADNESS_I: ["MM", "MM1"],
  MONKEY_MADNESS_II: ["MM2", "MMII"],
  ONE_SMALL_FAVOUR: ["OSF"],
  SONG_OF_THE_ELVES: ["SOTE"],
  MOURNINGS_END_PART_I: ["MEP", "MEP1"],
  MOURNINGS_END_PART_II: ["MEP2", "MEPII"],
  WHILE_GUTHIX_SLEEPS: ["WGS"],
  // RFD is a meta-quest; attach the bare "RFD" acronym to Start as a fallback.
  // Specific subquest variants are added via EXTRA_ALIASES below.
  RECIPE_FOR_DISASTER_START: ["RFD"],
};

// Long-form aliases for quests whose guide-truncated names don't substring-match
// the canonical "RFD - X" displayName. After separator normalization these
// match the guide's "RFD/Pirate", "RFD/Awowogei", etc.
const EXTRA_ALIASES: Record<string, string[]> = {
  RECIPE_FOR_DISASTER_MONKEY_AMBASSADOR: ["RFD Awowogei", "Awowogei"],
  RECIPE_FOR_DISASTER_PIRATE_PETE: ["RFD Pirate"],
  RECIPE_FOR_DISASTER_EVIL_DAVE: ["RFD Evil"],
  RECIPE_FOR_DISASTER_SIR_AMIK_VARZE: ["RFD Sir", "RFD Varze"],
  RECIPE_FOR_DISASTER_SKRACH_UGLOGWEE: ["RFD Skrach"],
  RECIPE_FOR_DISASTER_WARTFACE_AND_BENTNOZE: ["RFD Goblin", "RFD Goblins"],
  RECIPE_FOR_DISASTER_DWARF: ["RFD Dwarf"],
  RECIPE_FOR_DISASTER_LUMBRIDGE_GUIDE: ["RFD Lumbridge"],
};

const NON_QUEST_PREFIXES = [
  "BALLOON_TRANSPORT_",
  "FAUX_LEAGUES",
  "BIKE_SHEDDER",
  "SKILL_",
  "ACHIEVEMENT_DIARIES",
  "ACHIEVEMENT_DIARY",
  "MINIQUEST_",
];

function deriveDisplayName(enumName: string): string {
  // Snake → title case; small fixups for common patterns.
  const words = enumName
    .toLowerCase()
    .split("_")
    .filter(Boolean)
    .map(w => (w === "i" || w === "ii" || w === "iii" || w === "iv" || w === "v" ? w.toUpperCase() : w));
  let s = words.map(w => w[0]!.toUpperCase() + w.slice(1)).join(" ");
  // Common apostrophe/word fixups
  s = s
    .replace(/\bCooks\b/, "Cook's")
    .replace(/\bDorics\b/, "Doric's")
    .replace(/\bErnest The\b/, "Ernest the")
    .replace(/\bThe\b /g, "The ")
    .replace(/\bOf\b/g, "of")
    .replace(/\bThe\b/g, (m, off) => (off === 0 ? m : "the"))
    .replace(/\bAnd\b/g, "and")
    .replace(/\bA\b/g, (m, off) => (off === 0 ? m : "a"))
    .replace(/\bIn\b/g, (m, off) => (off === 0 ? m : "in"))
    .replace(/\bOn\b/g, (m, off) => (off === 0 ? m : "on"))
    .replace(/\bTo\b/g, (m, off) => (off === 0 ? m : "to"));
  // "ROMEO__JULIET" double underscore => &
  s = s.replace(/  +/g, " & ");
  return s;
}

async function fetchEnumSource(): Promise<string> {
  console.log(`[quests] fetching ${QUEST_HELPER_ENUM_URL}`);
  const res = await fetch(QUEST_HELPER_ENUM_URL);
  if (!res.ok) throw new Error(`quest enum fetch failed: ${res.status}`);
  return res.text();
}

function aliasesFor(displayName: string): string[] {
  const aliases = new Set<string>([displayName]);
  // common shorthand: drop punctuation
  aliases.add(displayName.replace(/['’`]/g, ""));
  // Roman numeral handling: "Dragon Slayer I" <-> "Dragon Slayer 1"
  if (/\b[IVX]+\b/.test(displayName)) {
    aliases.add(
      displayName.replace(
        /\b([IVX]+)\b/g,
        (_, r) => String(romanToInt(r)),
      ),
    );
  }
  return [...aliases];
}

function romanToInt(s: string): number {
  const m: Record<string, number> = { I: 1, V: 5, X: 10 };
  let total = 0;
  for (let i = 0; i < s.length; i++) {
    const cur = m[s[i]!] ?? 0;
    const next = m[s[i + 1]!] ?? 0;
    total += cur < next ? -cur : cur;
  }
  return total;
}

export async function buildQuestTable(): Promise<QuestTable> {
  const src = await fetchEnumSource();
  const seen = new Set<string>();
  const quests: QuestEntry[] = [];
  for (const m of src.matchAll(ENTRY_RE)) {
    const enumName = m[1]!;
    const rest = m[2] ?? "";
    if (seen.has(enumName)) continue;
    if (NON_QUEST_PREFIXES.some(p => enumName.startsWith(p))) continue;
    if (enumName === "BALLOON_TRANSPORT") continue;
    seen.add(enumName);
    const inline = INLINE_STRING_RE.exec(rest)?.[1];
    const displayName = inline ?? deriveDisplayName(enumName);
    const aliases = aliasesFor(displayName);
    // also include the derived form even when inline existed, for matching robustness
    if (inline) aliases.push(deriveDisplayName(enumName));
    for (const extra of EXTRA_ALIASES[enumName] ?? []) aliases.push(extra);
    const shortAliases = ACRONYMS[enumName] ?? [];
    quests.push({
      enumName,
      displayName,
      aliases: [...new Set(aliases)],
      shortAliases,
    });
  }
  const table: QuestTable = {
    fetchedOn: new Date().toISOString(),
    quests,
  };
  await ensureDir(QUEST_TABLE_PATH);
  await Bun.write(QUEST_TABLE_PATH, JSON.stringify(table, null, 2));
  console.log(`[quests] wrote ${quests.length} quests -> ${QUEST_TABLE_PATH}`);
  return table;
}
