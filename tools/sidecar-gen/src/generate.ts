import Anthropic from "@anthropic-ai/sdk";
import type { ItemTable, ResolvedStep, QuestTable, SidecarStep } from "./types.ts";
import { resolveStrict } from "./resolvers.ts";
import { ITEM_TABLE_PATH } from "./paths.ts";

const MODEL = "claude-haiku-4-5";

let cachedItems: ItemTable | null = null;
async function getItems(): Promise<ItemTable> {
  if (!cachedItems) cachedItems = (await Bun.file(ITEM_TABLE_PATH).json()) as ItemTable;
  return cachedItems;
}

// JSON schema for the structured-output tool. Keep narrow — we want the
// model to commit to specific shapes, not improvise.
const TOOL_SCHEMA = {
  type: "object",
  properties: {
    questIds: {
      type: "array",
      items: { type: "string" },
      description:
        "QuestHelperQuest enum names that this step ACTIVELY STARTS OR PROGRESSES. Drop quests merely mentioned in passing (e.g. 'lamps go on Herblore until Song of the Elves' does NOT make this a SOTE step). When specific RFD subquests are present, drop the bare RECIPE_FOR_DISASTER_START fallback.",
    },
    items: {
      type: "array",
      description:
        "Items resolved against the provided candidate lists from items_needed. ONLY use {id, name} pairs that appear in the candidates above. Pick the canonical variant when multiple candidates exist. NEVER invent an id from your training data — the OSRS item IDs you remember may be wrong. If you want to add an item that isn't in any candidate list, use contentItems[] with the name only and we will look up the id deterministically.",
      items: {
        type: "object",
        properties: {
          id: { type: "number" },
          name: { type: "string" },
          qty: { type: ["number", "null"] },
        },
        required: ["id", "name", "qty"],
      },
    },
    contentItems: {
      type: "array",
      description:
        "Items mentioned in the step text that the player needs FOR THIS STEP, but that weren't in any candidate list above. Provide the canonical OSRS item name (singular, lowercase preferred, e.g. 'cabbage', 'leather boots', 'anti-dragon shield', 'earmuffs'). DO NOT include an id — we will look it up. If you don't know the canonical name, put it in unresolvedItems[] instead.",
      items: {
        type: "object",
        properties: {
          name: { type: "string" },
          qty: { type: ["number", "null"] },
        },
        required: ["name", "qty"],
      },
    },
    abstractItems: {
      type: "array",
      description:
        "Tokens that are GENUINE ABSTRACTIONS — concepts, gear-tier categories, money. Examples: 'cash stack', 'melee gear', 'ranged gear', 'food', 'good food', 'warm clothing', '450k gp'. NEVER put a specific named item here (earmuffs, nose peg, mirror shield, slayer gloves are ALL specific items — they go in items[] with an ID, or unresolvedItems[] if you can't get an ID).",
      items: {
        type: "object",
        properties: {
          label: { type: "string" },
          rawToken: { type: "string" },
        },
        required: ["label", "rawToken"],
      },
    },
    unresolvedItems: {
      type: "array",
      description:
        "Tokens that look like items but couldn't be resolved to a candidate ID. PRESERVE THE RAW STRING — do not invent or guess item IDs. Better to leave as plain text than to mislabel.",
      items: {
        type: "object",
        properties: {
          rawToken: { type: "string" },
          qty: { type: ["number", "null"] },
        },
        required: ["rawToken", "qty"],
      },
    },
  },
  required: ["questIds", "items", "contentItems", "abstractItems", "unresolvedItems"],
} as const;

function buildPrompt(step: ResolvedStep, quests: QuestTable): string {
  const questById = new Map(quests.quests.map(q => [q.enumName, q.displayName]));
  const candidatesBlock = step.resolvedItems
    .map(it => {
      const head = `- raw: ${JSON.stringify(it.rawToken)} | qty: ${it.qty ?? "null"}`;
      if (it.isAbstract) return `${head} | abstract label: ${it.abstractLabel}`;
      if (it.candidates.length === 0) return `${head} | candidates: NONE (unresolvable)`;
      const cands = it.candidates
        .slice(0, 8)
        .map(c => `{id:${c.id}, name:${JSON.stringify(c.name)}}`)
        .join(", ");
      return `${head} | candidates: [${cands}]`;
    })
    .join("\n");

  const questBlock =
    step.detectedQuests.length === 0
      ? "(none detected)"
      : step.detectedQuests
          .map(
            q =>
              `- ${q.enumName} (${questById.get(q.enumName) ?? "?"}) — matched: ${JSON.stringify(q.matchedText)}`,
          )
          .join("\n");

  const meta = step.metadata;
  return [
    `You are mapping a step from the BRUHsailer OSRS ironman guide to structured plugin data.`,
    ``,
    `# Step ${step.id}`,
    `Chapter: ${step.chapterTitle}`,
    `Section: ${step.sectionTitle}`,
    ``,
    `## Step text`,
    step.stepText.trim(),
    ``,
    `## Step metadata`,
    `- gp_stack: ${meta.gp_stack ?? "(none)"}`,
    `- items_needed (raw): ${meta.items_needed ?? "(none)"}`,
    `- total_time: ${meta.total_time ?? "(none)"}`,
    `- skills_quests_met: ${meta.skills_quests_met ?? "(none)"}`,
    ``,
    `## Tokenized items_needed (with deterministic-resolver candidates)`,
    candidatesBlock || "(no item tokens)",
    ``,
    `## Detected quest hits (substring matches; may include false positives)`,
    questBlock,
    ``,
    `# Your task`,
    `Produce a structured mapping by calling the \`emit_step_mapping\` tool exactly once.`,
    ``,
    `Rules:`,
    `1. items[]: ONLY for tokens from items_needed that have candidates above. Use the {id, name} from a candidate. NEVER invent an id from memory — your training-data IDs may be wrong.`,
    `2. contentItems[]: for items mentioned in the step text but NOT in any candidate list above. Provide the canonical OSRS item name; we resolve the id deterministically.`,
    `3. abstractItems[]: ONLY for genuine abstractions — gear-tier labels ("melee gear", "ranged gear"), money ("cash stack", "X gp"), and category words ("food", "good food", "warm clothing"). A specifically-named item NEVER goes here. Specific items go in items[] (with id) or contentItems[] (without id) or unresolvedItems[] (raw text).`,
    `4. unresolvedItems[]: tokens you can't categorize cleanly — preserve raw text, no guessing.`,
    `5. questIds[]: include only quests this step ACTIVELY STARTS OR PROGRESSES. Drop quests mentioned for context/future reference. When specific RFD subquests are present, drop the bare RECIPE_FOR_DISASTER_START fallback.`,
    `6. Each token/item appears in EXACTLY ONE category. Never duplicate.`,
  ].join("\n");
}

function parseToolUse(content: Anthropic.Messages.ContentBlock[]): unknown {
  for (const c of content) {
    if (c.type === "tool_use" && c.name === "emit_step_mapping") return c.input;
  }
  throw new Error("model did not call emit_step_mapping tool");
}

export async function generateOne(
  step: ResolvedStep,
  quests: QuestTable,
  client: Anthropic = new Anthropic(),
): Promise<SidecarStep> {
  const prompt = buildPrompt(step, quests);
  const res = await client.messages.create({
    model: MODEL,
    max_tokens: 2048,
    tools: [
      {
        name: "emit_step_mapping",
        description: "Emit the structured mapping for one BRUHsailer guide step.",
        input_schema: TOOL_SCHEMA as any,
      },
    ],
    tool_choice: { type: "tool", name: "emit_step_mapping" },
    messages: [{ role: "user", content: prompt }],
  });

  const out = parseToolUse(res.content) as ModelOut;

  const items = await getItems();
  const post = postProcess(out, items);

  return {
    contentHash: step.contentHash,
    title: step.sectionTitle,
    ...post,
    verified: false,
    verifierConfidence: null,
    verifierFlags: [],
  };
}

interface ModelOut {
  questIds: string[];
  items: { id: number; name: string; qty: number | null }[]; // no source field — we assign it
  contentItems: { name: string; qty: number | null }[];
  abstractItems: SidecarStep["abstractItems"];
  unresolvedItems: SidecarStep["unresolvedItems"];
}

interface PostProcessed {
  questIds: string[];
  items: SidecarStep["items"];
  abstractItems: SidecarStep["abstractItems"];
  unresolvedItems: SidecarStep["unresolvedItems"];
}

// Validate, resolve, and dedupe. Hard rule: every items[] entry must be
// internally consistent with the items table (id ↔ name match). Anything that
// fails validation is demoted to unresolvedItems and re-attempted via resolveStrict.
export function postProcess(out: ModelOut, items: ItemTable): PostProcessed {
  const finalItems: SidecarStep["items"] = [];
  const finalUnresolved: SidecarStep["unresolvedItems"] = [];

  // 1. Validate items[]: each {id, name} pair must match the items table.
  for (const it of out.items) {
    const real = items.items.find(i => i.id === it.id);
    if (real && real.name.toLowerCase() === it.name.toLowerCase()) {
      finalItems.push({ id: it.id, name: real.name, qty: it.qty, source: "items_needed" });
    } else {
      // Mismatch: model invented or got it wrong. Try to recover by name.
      const recovered = resolveStrict(it.name, items);
      if (recovered) {
        finalItems.push({
          id: recovered.id,
          name: recovered.name,
          qty: it.qty,
          source: "items_needed",
        });
      } else {
        finalUnresolved.push({ rawToken: it.name, qty: it.qty });
      }
    }
  }

  // 2. Resolve contentItems[] via the strict deterministic resolver.
  for (const c of out.contentItems) {
    const hit = resolveStrict(c.name, items);
    if (hit) {
      finalItems.push({ id: hit.id, name: hit.name, qty: c.qty, source: "content" });
    } else {
      finalUnresolved.push({ rawToken: c.name, qty: c.qty });
    }
  }

  // 3. Try to resolve unresolvedItems[] from the model too, for free promotion.
  for (const u of out.unresolvedItems) {
    const hit = resolveStrict(u.rawToken, items);
    if (hit) {
      finalItems.push({ id: hit.id, name: hit.name, qty: u.qty, source: "content" });
    } else {
      finalUnresolved.push(u);
    }
  }

  // 4. Dedupe items[] by id (keep first occurrence).
  const seenIds = new Set<number>();
  const dedupedItems = finalItems.filter(it => {
    if (seenIds.has(it.id)) return false;
    seenIds.add(it.id);
    return true;
  });

  // 5. Normalize-and-dedupe across categories. Two strings count as duplicates
  //    after collapsing whitespace and stripping punctuation, so
  //    "shayzien boots (5)" matches "shayzien boots 5".
  const norm = (s: string) =>
    s
      .toLowerCase()
      .replace(/[()'’`.,]/g, "")
      .replace(/\s+/g, " ")
      .trim();

  const claimed = new Set<string>();
  for (const it of dedupedItems) claimed.add(norm(it.name));

  const dedupedUnresolved: SidecarStep["unresolvedItems"] = [];
  const seenUnresolved = new Set<string>();
  for (const u of finalUnresolved) {
    const k = norm(u.rawToken);
    if (claimed.has(k) || seenUnresolved.has(k)) continue;
    seenUnresolved.add(k);
    dedupedUnresolved.push(u);
  }
  for (const u of dedupedUnresolved) claimed.add(norm(u.rawToken));

  // 6. Drop abstractItems whose label/rawToken collides with claimed names.
  const dedupedAbstract = out.abstractItems.filter(a => {
    return !claimed.has(norm(a.label)) && !claimed.has(norm(a.rawToken));
  });

  return {
    questIds: out.questIds,
    items: dedupedItems,
    abstractItems: dedupedAbstract,
    unresolvedItems: dedupedUnresolved,
  };
}

export function getPrompt(step: ResolvedStep, quests: QuestTable): string {
  return buildPrompt(step, quests);
}
