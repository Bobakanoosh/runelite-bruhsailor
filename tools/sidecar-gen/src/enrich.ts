// Targeted enrichment: ask the LLM ONLY for npcs[] and locations[] for each step
// and merge them into the existing sidecar without touching items/questIds/etc.
// This avoids re-paying for fields we already have when the schema gains a new
// field (initial rollout of npcs/locations).
//
// Skip rules:
//  - Step missing from sidecar entirely → skip (run `generate` first; that path
//    handles brand-new entries with the full prompt).
//  - Step already has BOTH npcs and locations defined (even as empty arrays) → skip.
//  - Otherwise enrich.
import Anthropic from "@anthropic-ai/sdk";
import { mkdir } from "node:fs/promises";
import { dirname } from "node:path";
import type { ResolvedStep, Sidecar, SidecarStep } from "./types.ts";
import { RESOLVED_PATH, SHIPPED_SIDECAR } from "./paths.ts";

const MODEL = "claude-haiku-4-5";
const CONCURRENCY = 4;
const FLUSH_EVERY = 10;
const SDK_MAX_RETRIES = 10;

const TOOL_SCHEMA = {
  type: "object",
  properties: {
    npcs: {
      type: "array",
      items: { type: "string" },
      description:
        "Named NPCs the player must interact with in this step (talk, trade, fight, pickpocket, etc.). Use the CANONICAL OSRS WIKI PAGE TITLE — proper capitalization, real apostrophes (e.g. 'Captain Shanks', 'Aggie', 'Wizard Mizgog', 'Doric'). Skip generic monster types ('rats', 'cows'); only named NPCs. The matched name MUST appear verbatim somewhere in the step text.",
    },
    locations: {
      type: "array",
      items: { type: "string" },
      description:
        "Named in-game locations the player visits in this step (cities, regions, dungeons, landmarks). Use the CANONICAL OSRS WIKI PAGE TITLE (e.g. 'Port Sarim', 'Edgeville', 'Lumbridge Castle', 'HAM Hideout'). The location name MUST appear verbatim in the step text. Skip generic words ('the bank', 'the dungeon'). Avoid duplicating with NPC entries.",
    },
  },
  required: ["npcs", "locations"],
} as const;

interface ModelOut {
  npcs: string[];
  locations: string[];
}

function buildPrompt(step: ResolvedStep): string {
  return [
    `You are extracting inline link targets from a step of the BRUHsailer OSRS ironman guide.`,
    ``,
    `# Step ${step.id}`,
    `Chapter: ${step.chapterTitle}`,
    `Section: ${step.sectionTitle}`,
    ``,
    `## Step text`,
    step.stepText.trim(),
    ``,
    `# Your task`,
    `Call the \`emit_inline_links\` tool exactly once with two arrays:`,
    `- npcs[]: named NPCs the player engages with in this step.`,
    `- locations[]: named locations the player visits in this step.`,
    ``,
    `Every entry's name (case-insensitive) MUST appear verbatim in the step text — these become inline wiki links.`,
    `Use canonical OSRS wiki page titles (proper capitalization, real apostrophes).`,
    `Skip generic terms ("the bank", "monsters", "the dungeon"). Don't duplicate the same name across both lists.`,
  ].join("\n");
}

function parseToolUse(content: Anthropic.Messages.ContentBlock[]): ModelOut {
  for (const c of content) {
    if (c.type === "tool_use" && c.name === "emit_inline_links") {
      return c.input as ModelOut;
    }
  }
  throw new Error("model did not call emit_inline_links tool");
}

function uniqueByLower(xs: string[]): string[] {
  const seen = new Set<string>();
  const out: string[] = [];
  for (const x of xs) {
    const k = x.toLowerCase();
    if (seen.has(k)) continue;
    seen.add(k);
    out.push(x);
  }
  return out;
}

async function enrichOne(
  step: ResolvedStep,
  client: Anthropic,
): Promise<ModelOut> {
  const res = await client.messages.create({
    model: MODEL,
    max_tokens: 512,
    tools: [
      {
        name: "emit_inline_links",
        description:
          "Emit the named NPCs and locations referenced inline in one BRUHsailer guide step.",
        input_schema: TOOL_SCHEMA as any,
      },
    ],
    tool_choice: { type: "tool", name: "emit_inline_links" },
    messages: [{ role: "user", content: buildPrompt(step) }],
  });
  const out = parseToolUse(res.content);
  const cleanNpcs = uniqueByLower((out.npcs ?? []).map(s => s.trim()).filter(Boolean));
  const cleanLocs = uniqueByLower(
    (out.locations ?? [])
      .map(s => s.trim())
      .filter(Boolean)
      .filter(l => !cleanNpcs.some(n => n.toLowerCase() === l.toLowerCase())),
  );
  return { npcs: cleanNpcs, locations: cleanLocs };
}

async function ensureDir(path: string) {
  await mkdir(dirname(path), { recursive: true });
}

async function flush(sidecar: Sidecar) {
  await ensureDir(SHIPPED_SIDECAR);
  await Bun.write(SHIPPED_SIDECAR, JSON.stringify(sidecar, null, 2));
}

function needsEnrichment(s: SidecarStep | undefined): boolean {
  if (!s) return false; // generate path handles missing entries
  return s.npcs === undefined || s.locations === undefined;
}

export async function enrichAll(opts: { force?: boolean } = {}) {
  if (!process.env.ANTHROPIC_API_KEY) {
    throw new Error("ANTHROPIC_API_KEY not set");
  }
  const resolved = (await Bun.file(RESOLVED_PATH).json()) as ResolvedStep[];
  const existing = (await Bun.file(SHIPPED_SIDECAR).json()) as Sidecar;

  const todo = resolved.filter(s => {
    const cur = existing.steps[s.id];
    if (!cur) return false;
    return opts.force || needsEnrichment(cur);
  });

  console.log(
    `[enrich] ${resolved.length} total | ${todo.length} need npcs/locations | concurrency=${CONCURRENCY}`,
  );
  if (todo.length === 0) return;

  const client = new Anthropic({ maxRetries: SDK_MAX_RETRIES });
  const tStart = Date.now();
  let done = 0;
  let failed = 0;
  const queue = [...todo];
  const errors: { id: string; err: unknown }[] = [];

  async function worker(id: number) {
    while (queue.length > 0) {
      const step = queue.shift();
      if (!step) break;
      try {
        const out = await enrichOne(step, client);
        const cur = existing.steps[step.id]!;
        cur.npcs = out.npcs;
        cur.locations = out.locations;
        done++;
      } catch (err) {
        failed++;
        errors.push({ id: step.id, err });
        console.error(
          `  [worker ${id}] step ${step.id} FAILED: ${(err as Error).message ?? err}`,
        );
      }
      const total = done + failed;
      if (total % 5 === 0 || queue.length === 0) {
        const elapsed = ((Date.now() - tStart) / 1000).toFixed(1);
        const rate = total > 0 ? (total / parseFloat(elapsed)).toFixed(2) : "0.00";
        console.log(
          `  progress: ${total}/${todo.length} (ok=${done} fail=${failed}) | ${elapsed}s | ${rate}/s | queue=${queue.length}`,
        );
      }
      if (total % FLUSH_EVERY === 0) {
        await flush(existing);
      }
    }
  }

  await Promise.all(Array.from({ length: CONCURRENCY }, (_, i) => worker(i + 1)));
  await flush(existing);

  const elapsed = ((Date.now() - tStart) / 1000).toFixed(1);
  console.log(`\n[enrich] done in ${elapsed}s | ok=${done} fail=${failed}`);
  if (failed > 0) {
    console.log(`[enrich] failed steps:`, errors.map(e => e.id).join(", "));
    console.log(`[enrich] re-run the same command to retry failures only.`);
  }
}
