import type {
  ItemTable,
  PreprocessedStep,
  QuestTable,
  ResolvedItem,
  ResolvedStep,
} from "./types.ts";
import { PREPROCESSED_PATH, RESOLVED_PATH, ITEM_TABLE_PATH, QUEST_TABLE_PATH } from "./paths.ts";

// Tokens that aren't items at all — we surface them as `abstractItems` so
// the UI can show them without trying to bank-filter on them.
const ABSTRACT_TOKENS = new Set<string>([
  "none", "tbd",
  "gp", "cash stack", "coins",
  "melee gear", "ranged gear", "mage gear",
  "food", "warm clothing",
  "all your feathers",
  "rune pouch", // technically an item, but tokens often imply "rune pouch with X" — handle in LLM stage
]);

// Known shorthand → canonical item name (extend as you find misses).
// Lowercase keys, lowercase values.
const ALIASES: Record<string, string> = {
  "logs": "logs",
  "knife": "knife",
  "tinderbox": "tinderbox",
  "spade": "spade",
  "rope": "rope",
  "hammer": "hammer",
  "saw": "saw",
  "chisel": "chisel",
  "bucket": "bucket",
  "jug": "jug (empty)",
  "jug of water": "jug of water",
  "fishing net": "small fishing net",
  "fly fishing rod": "fly fishing rod",
  "fishing rod": "fishing rod",
  "feathers": "feather",
  "shears": "shears",
  "ghostspeak amulet": "ghostspeak amulet",
  "catspeak amulet": "catspeak amulet",
  "monkeyspeak amulet": "monkeyspeak amulet",
  "chronicle": "chronicle",
  "games necklace": "games necklace(8)",
  "necklace of passage": "necklace of passage(5)",
  "ring of dueling": "ring of dueling(8)",
  "skills necklace": "skills necklace(6)",
  "ring of wealth": "ring of wealth (5)",
  "amulet of glory": "amulet of glory(6)",
  "poh tab": "teleport to house",
  "falador tab": "falador teleport",
  "varrock tab": "varrock teleport",
  "lumbridge tab": "lumbridge teleport",
  "camelot tab": "camelot teleport",
  "soft clay": "soft clay",
  "air runes": "air rune",
  "water runes": "water rune",
  "earth runes": "earth rune",
  "fire runes": "fire rune",
  "mind runes": "mind rune",
  "chaos runes": "chaos rune",
  "death runes": "death rune",
  "law runes": "law rune",
  "nature runes": "nature rune",
  "soul runes": "soul rune",
  "blood runes": "blood rune",
  "cosmic runes": "cosmic rune",
  "body runes": "body rune",
  "air staff": "staff of air",
  "water staff": "staff of water",
  "earth staff": "staff of earth",
  "fire staff": "staff of fire",
  "wind staff": "staff of air",
  "dramen staff": "dramen staff",
  "rune sword": "rune sword",
  "rune scimitar": "rune scimitar",
  "rune pickaxe": "rune pickaxe",
  "rune axe": "rune axe",
  "steel axe": "steel axe",
  "bronze axe": "bronze axe",
  "bronze pickaxe": "bronze pickaxe",
  "leather gloves": "leather gloves",
  "leather boots": "leather boots",
  "swordfish": "swordfish",
  "shark": "shark",
  "lobster": "lobster",
  "trout": "trout",
  "salmon": "salmon",
  "cabbage": "cabbage",
  "onion": "onion",
  "redberries": "redberries",
  "doogle leaves": "doogle leaves",
  "raw bear meat": "raw bear meat",
  "raw beef": "raw beef",
  "raw chicken": "raw chicken",
  "rat meat": "rat meat",
  "burnt meat": "burnt meat",
  "eye of newt": "eye of newt",
  "molten glass": "molten glass",
  "soda ash": "soda ash",
  "buckets of sand": "bucket of sand",
  "bucket of sand": "bucket of sand",
  "balls of wool": "ball of wool",
  "ball of wool": "ball of wool",
  "bolts of cloth": "bolt of cloth",
  "bolt of cloth": "bolt of cloth",
  "noted planks": "plank",
  "planks": "plank",
  "oak logs": "oak logs",
  "wax logs": "redwood logs", // best guess; LLM stage will correct
  "steel nails": "steel nail",
  "iron bars": "iron bar",
  "iron bar": "iron bar",
  "bronze bar": "bronze bar",
  "stew": "stew",
  "energy potions": "energy potion(4)",
  "prayer potions": "prayer potion(4)",
  "stamina potions": "stamina potion(4)",
  "shantay pass": "shantay pass",
  "waterskins": "waterskin(4)",
  "waterskin": "waterskin(4)",
  "barcrawl card": "barcrawl card",
  "key": "key",
  "package": "package",
  "pie dish": "pie dish",
  "pot of flour": "pot of flour",
  "pots of flour": "pot of flour",
  "red dye": "red dye",
  "yellow dye": "yellow dye",
  "blue dye": "blue dye",
  "ghost skull": "ghost's skull",
  "glarial’s pebble": "glarial's pebble",
  "glarial's pebble": "glarial's pebble",
  "necklace mould": "necklace mould",
  "sickle mould": "sickle mould",
  "sapphire": "sapphire",
  "gold bar": "gold bar",
  "garlic": "garlic",
  "empty pot": "pot",
  "teak log": "teak logs",
  "forestry kit": "forestry kit",
  "arrow shafts": "headless arrow", // best-effort placeholder
  "raw sardine": "raw sardine",
  "bucket of milk": "bucket of milk",
  "beer": "beer",
  "cakes": "cake",
  "pestle/mortar": "pestle and mortar",
  "pestle and mortar": "pestle and mortar",
  "dueling ring": "ring of dueling(8)",
  "light source": "bullseye lantern",
  "addy pickaxe": "adamant pickaxe",
  "addy axe": "adamant axe",
  "amulet of ghostspeak": "ghostspeak amulet",
  "kitten/cat": "kitten",
  "wizard's mind bomb": "wizard's mind bomb",
  "wizard’s mind bomb": "wizard's mind bomb",
  "face mask": "face mask",
  "ardy cloak 1": "ardougne cloak 1",
  "ardy cloak 2": "ardougne cloak 2",
  "ardy cloak 3": "ardougne cloak 3",
  "ardy cloak 4": "ardougne cloak 4",
  "penguin suit": "clockwork suit (top)",
  "wind strike": "air rune",
  "wind strikes": "air rune",
  "water strike": "water rune",
  "water strikes": "water rune",
  "premade blurb' sp": "premade blurb' sp.",
  // Charge-dose potions — default to the full (4) charge
  "energy potion": "energy potion(4)",
  "prayer potion": "prayer potion(4)",
  "antipoison": "antipoison(4)",
  "super antipoison": "superantipoison(4)",
  "superantipoison": "superantipoison(4)",
  "stamina potion": "stamina potion(4)",
  "compost potion": "compost potion(4)",
  "ranging potion": "ranging potion(4)",
  "super ranging potion": "ranging potion(4)",
  "magic potion": "magic potion(4)",
  "combat potion": "combat potion(4)",
  "saradomin brew": "saradomin brew(4)",
  "super combat potion": "super combat potion(4)",
  "super energy potion": "super energy potion(4)",
  "absorption potion": "absorption (4)",
  // Renamed items
  "iban staff": "iban's staff",
  "ibans staff": "iban's staff",
  "rune shortsword": "rune sword", // no shortsword tier exists for rune
  "digsite pendant": "digsite pendant (5)",
  "ring of charos (a)": "ring of charos(a)",
  "red spider's eggs": "red spiders' eggs",
  "red spiders eggs": "red spiders' eggs",
  "red spider eggs": "red spiders' eggs",
  "small key": "a small key",
  "wine": "jug of wine",
  "cat": "kitten",
  "compost": "compost",
  "supercompost": "supercompost",
  "ultracompost": "ultracompost",
  "lead ore": "lead ore",
  "anti-dragon shield": "anti-dragon shield",
  "anti-dragon shields": "anti-dragon shield",
};

function stripPossessives(s: string): string {
  return s.replace(/['’]s\b/g, "");
}

function singularize(s: string): string {
  // very small heuristic — LLM will correct, but reduces noise
  if (s.endsWith("ies") && s.length > 4) return s.slice(0, -3) + "y";
  if (s.endsWith("es") && s.length > 3 && !s.endsWith("oes")) return s.slice(0, -1);
  if (s.endsWith("s") && s.length > 3 && !s.endsWith("ss")) return s.slice(0, -1);
  return s;
}

function lookupCandidates(name: string, table: ItemTable): { id: number; name: string }[] {
  const lc = name.toLowerCase().trim();
  if (!lc) return [];
  // 1. alias map
  const aliased = ALIASES[lc] ?? lc;
  // 2. exact
  const exactIds = table.byName[aliased] ?? [];
  if (exactIds.length) return exactIds.map(id => ({ id, name: byId(table, id) }));
  // 3. singular form
  const sing = singularize(aliased);
  const singIds = table.byName[sing] ?? [];
  if (singIds.length) return singIds.map(id => ({ id, name: byId(table, id) }));
  // 4. stripped possessive
  const stripped = stripPossessives(aliased);
  const strippedIds = table.byName[stripped] ?? [];
  if (strippedIds.length) return strippedIds.map(id => ({ id, name: byId(table, id) }));
  // 5. forward-substring fallback only: candidate name must contain the full
  // token. Never the reverse — that produced false matches like
  // "evil stew" -> Stew (because "evil stew".includes("stew")).
  const matches: { id: number; name: string }[] = [];
  for (const [k, ids] of Object.entries(table.byName)) {
    if (k.includes(aliased)) {
      for (const id of ids) matches.push({ id, name: k });
      if (matches.length > 8) break;
    }
  }
  return matches.slice(0, 5);
}

function byId(table: ItemTable, id: number): string {
  // small linear scan; cheap given table size and resolver runs once
  return table.items.find(i => i.id === id)?.name ?? `id:${id}`;
}

function isAbstract(name: string): { abstract: boolean; label?: string } {
  const lc = name.toLowerCase().trim();
  if (ABSTRACT_TOKENS.has(lc)) return { abstract: true, label: lc };
  if (/\bgear\b/.test(lc)) return { abstract: true, label: lc };
  if (lc.startsWith("cash") || lc.endsWith(" gp")) return { abstract: true, label: lc };
  return { abstract: false };
}

// Strict resolver used in the post-process pass. Returns ONE canonical
// candidate when there's an unambiguous match, or null otherwise. We use this
// to promote a model-emitted unresolvedItem to items[] when it's clearly safe.
//
// "Safe" = alias map hit, OR exact name match in the items table (taking the
// lowest item id when there are duplicates — those are nearly always cosmetic
// or noted variants of the canonical item, which has the lowest id).
//
// We deliberately do NOT use the substring fallback here. It's the right heuristic
// for offering candidate sets to the LLM, but unsafe for unattended promotion.
export function resolveStrict(
  rawName: string,
  items: ItemTable,
): { id: number; name: string } | null {
  const lc = rawName.toLowerCase().trim();
  if (!lc) return null;
  if (isAbstract(lc).abstract) return null;

  const tries: string[] = [];
  const aliased = ALIASES[lc];
  if (aliased) tries.push(aliased);
  tries.push(lc);
  tries.push(singularize(lc));
  tries.push(stripPossessives(lc));
  // Charge-dose fallback: many potion-like items have only "(4)"-suffixed
  // canonical names. If the bare name doesn't resolve, try with (4).
  if (!ALIASES[lc] && lc.includes("potion")) {
    tries.push(`${lc}(4)`);
    tries.push(`${singularize(lc)}(4)`);
  }

  for (const candidate of tries) {
    const ids = items.byName[candidate];
    if (!ids || ids.length === 0) continue;
    const lowestId = ids.reduce((a, b) => (a < b ? a : b));
    const name = items.items.find(i => i.id === lowestId)?.name ?? candidate;
    return { id: lowestId, name };
  }
  return null;
}

export function resolveStepItems(step: PreprocessedStep, items: ItemTable): ResolvedItem[] {
  const resolved: ResolvedItem[] = [];
  for (const tok of step.itemTokens) {
    const abs = isAbstract(tok.name);
    if (abs.abstract) {
      resolved.push({
        rawToken: tok.raw,
        qty: tok.qty,
        candidates: [],
        isAbstract: true,
        abstractLabel: abs.label,
      });
      continue;
    }
    const candidates = lookupCandidates(tok.name, items);
    resolved.push({
      rawToken: tok.raw,
      qty: tok.qty,
      candidates,
      isAbstract: false,
    });
  }
  return resolved;
}

function escapeRegex(s: string): string {
  return s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

// Normalize separators (slash/dash/colon) and collapse whitespace so that
// "RFD/Evil Dave", "RFD - Evil Dave", and "RFD: Evil Dave" all compare equal.
function normalizeForMatch(s: string): string {
  return s
    .toLowerCase()
    .replace(/[\/\-:]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

export function detectQuestsInText(
  stepText: string,
  quests: QuestTable,
): ResolvedStep["detectedQuests"] {
  const norm = normalizeForMatch(stepText);
  const hits: ResolvedStep["detectedQuests"] = [];
  for (const q of quests.quests) {
    let matched: string | undefined;

    // 1. Substring match on long aliases (>= 4 chars, separator-normalized)
    for (const alias of q.aliases) {
      if (alias.length < 4) continue;
      const a = normalizeForMatch(alias);
      if (a.length < 4) continue;
      if (norm.includes(a)) {
        matched = alias;
        break;
      }
    }

    // 2. Whole-word match on short acronyms (case-sensitive)
    if (!matched) {
      for (const sa of q.shortAliases) {
        const re = new RegExp(`\\b${escapeRegex(sa)}\\b`);
        if (re.test(stepText)) {
          matched = sa;
          break;
        }
      }
    }

    if (matched) {
      hits.push({ enumName: q.enumName, displayName: q.displayName, matchedText: matched });
    }
  }
  return hits;
}

export async function resolveAll(): Promise<ResolvedStep[]> {
  const steps = (await Bun.file(PREPROCESSED_PATH).json()) as PreprocessedStep[];
  const items = (await Bun.file(ITEM_TABLE_PATH).json()) as ItemTable;
  const quests = (await Bun.file(QUEST_TABLE_PATH).json()) as QuestTable;

  const out: ResolvedStep[] = steps.map(s => ({
    ...s,
    resolvedItems: resolveStepItems(s, items),
    detectedQuests: detectQuestsInText(s.stepText, quests),
  }));

  await Bun.write(RESOLVED_PATH, JSON.stringify(out, null, 2));

  // Quick stats
  const totalTokens = out.reduce((n, s) => n + s.resolvedItems.length, 0);
  const unresolved = out.reduce(
    (n, s) =>
      n + s.resolvedItems.filter(r => !r.isAbstract && r.candidates.length === 0).length,
    0,
  );
  const ambiguous = out.reduce(
    (n, s) => n + s.resolvedItems.filter(r => r.candidates.length > 1).length,
    0,
  );
  const stepsWithQuests = out.filter(s => s.detectedQuests.length > 0).length;
  console.log(
    `[resolve] ${out.length} steps | ${totalTokens} tokens | ${unresolved} unresolved | ${ambiguous} ambiguous | ${stepsWithQuests} steps with quest hits -> ${RESOLVED_PATH}`,
  );
  return out;
}
