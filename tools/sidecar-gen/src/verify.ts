// Stage 4: Sonnet verifier. Scores every sidecar entry against its source step
// and emits a confidence + flags so a human reviewer can prioritize.
//
// Verifier flags (controlled vocabulary — keeps the human queue scannable):
//   item-mismatch   — items[] contains an entry that doesn't fit the step
//   missing-item    — step text/items_needed mentions an item that wasn't captured
//   wrong-quest     — questIds includes a quest the step doesn't actually progress
//   missing-quest   — step progresses a quest that's not in questIds
//   abstract-misuse — abstractItems holds something that should be a specific item
//   qty-suspicious  — quantity looks wrong (off by 10x, missing, etc.)
//   tbd-step        — items_needed is "tbd" (guide is incomplete here)
//   trivial         — empty or near-empty mapping; nothing to verify
import Anthropic from "@anthropic-ai/sdk";
import type {
  QuestTable,
  ResolvedStep,
  Sidecar,
  SidecarStep,
} from "./types.ts";
import { QUEST_TABLE_PATH, RESOLVED_PATH, SHIPPED_SIDECAR } from "./paths.ts";

const MODEL = "claude-sonnet-4-6";
const CONCURRENCY = 4;
const FLUSH_EVERY = 10;
const SDK_MAX_RETRIES = 10;

const VERIFY_TOOL = {
  type: "object",
  properties: {
    confidence: {
      type: "number",
      description:
        "Confidence in the mapping, 0.0 to 1.0. Roughly: 0.95+ means looks solid, 0.7-0.95 means minor issues likely, <0.7 means clear problems present.",
    },
    flags: {
      type: "array",
      description:
        "Specific issues. Use the controlled vocabulary: item-mismatch, missing-item, wrong-quest, missing-quest, abstract-misuse, qty-suspicious, tbd-step, trivial.",
      items: {
        type: "string",
        enum: [
          "item-mismatch",
          "missing-item",
          "wrong-quest",
          "missing-quest",
          "abstract-misuse",
          "qty-suspicious",
          "tbd-step",
          "trivial",
        ],
      },
    },
    notes: {
      type: "string",
      description:
        "Short reviewer-targeted comments (1-3 sentences max). Cite the specific item or quest that's wrong/missing.",
    },
  },
  required: ["confidence", "flags", "notes"],
} as const;

interface VerifyOut {
  confidence: number;
  flags: string[];
  notes: string;
}

function buildVerifyPrompt(step: ResolvedStep, mapping: SidecarStep): string {
  const meta = step.metadata;
  const itemsBlock = mapping.items
    .map(i => `  - ${i.id} ${i.name}${i.qty != null ? ` x${i.qty}` : ""} [${i.source}]`)
    .join("\n");
  const abstractBlock = mapping.abstractItems
    .map(a => `  - label: "${a.label}" | rawToken: "${a.rawToken}"`)
    .join("\n");
  const unresolvedBlock = mapping.unresolvedItems
    .map(u => `  - "${u.rawToken}"${u.qty != null ? ` x${u.qty}` : ""}`)
    .join("\n");

  return [
    `You are auditing a generated mapping from the BRUHsailer OSRS ironman guide. Score it.`,
    ``,
    `# Source step ${step.id}`,
    `Section: ${step.sectionTitle}`,
    ``,
    `## Step text`,
    step.stepText.trim(),
    ``,
    `## items_needed (raw)`,
    meta.items_needed ?? "(none)",
    ``,
    `## gp_stack`,
    meta.gp_stack ?? "(none)",
    ``,
    `# Generated mapping`,
    `## questIds`,
    mapping.questIds.length === 0 ? "  (none)" : mapping.questIds.map(q => `  - ${q}`).join("\n"),
    ``,
    `## items[]`,
    itemsBlock || "  (none)",
    ``,
    `## abstractItems[]`,
    abstractBlock || "  (none)",
    ``,
    `## unresolvedItems[]`,
    unresolvedBlock || "  (none)",
    ``,
    `# Your task`,
    `Call the \`verify\` tool exactly once.`,
    ``,
    `Score the mapping against the source step:`,
    `- Are items[] correct? Any obvious mismatches (wrong tier, wrong variant)?`,
    `- Are quantities reasonable for an ironman at this stage of the guide?`,
    `- Are quests in questIds[] actually started/progressed THIS step (not just mentioned)?`,
    `- Is anything from items_needed or step text MISSING that should be captured?`,
    `- Are any abstractItems actually specific items that should be in items[]?`,
    ``,
    `Be precise but not pedantic. A missing minor detail is worth a lower confidence, not necessarily a flag. Only flag real problems.`,
    `If items_needed is "tbd" the source itself is incomplete — flag tbd-step but don't penalize confidence below 0.85.`,
    `If the step is purely informational/setup with no items or quests, flag trivial and use confidence 0.95.`,
  ].join("\n");
}

function parseTool(content: Anthropic.Messages.ContentBlock[]): VerifyOut {
  for (const c of content) {
    if (c.type === "tool_use" && c.name === "verify") return c.input as VerifyOut;
  }
  throw new Error("model did not call verify tool");
}

export async function verifyAll() {
  if (!process.env.ANTHROPIC_API_KEY) throw new Error("ANTHROPIC_API_KEY not set");

  const resolved = (await Bun.file(RESOLVED_PATH).json()) as ResolvedStep[];
  const sidecar = (await Bun.file(SHIPPED_SIDECAR).json()) as Sidecar;
  const _quests = (await Bun.file(QUEST_TABLE_PATH).json()) as QuestTable;

  const ids = Object.keys(sidecar.steps);
  // Skip steps already verified (verifierConfidence != null AND no schema-bump).
  const todo = ids.filter(id => sidecar.steps[id]!.verifierConfidence == null);
  console.log(
    `[verify] ${ids.length} total | ${ids.length - todo.length} already verified | ${todo.length} to verify | concurrency=${CONCURRENCY}`,
  );
  if (todo.length === 0) return;

  const client = new Anthropic({ maxRetries: SDK_MAX_RETRIES });
  const tStart = Date.now();
  const queue = [...todo];
  let done = 0, failed = 0;
  const errs: { id: string; err: unknown }[] = [];

  async function worker(wid: number) {
    while (queue.length > 0) {
      const id = queue.shift();
      if (!id) break;
      const mapping = sidecar.steps[id]!;
      const step = resolved.find(s => s.id === id);
      if (!step) {
        failed++;
        continue;
      }
      try {
        const res = await client.messages.create({
          model: MODEL,
          max_tokens: 1024,
          tools: [
            {
              name: "verify",
              description: "Emit a verification verdict for one mapping.",
              input_schema: VERIFY_TOOL as any,
            },
          ],
          tool_choice: { type: "tool", name: "verify" },
          messages: [{ role: "user", content: buildVerifyPrompt(step, mapping) }],
        });
        const out = parseTool(res.content);
        mapping.verifierConfidence = out.confidence;
        mapping.verifierFlags = out.flags;
        // Stash the notes alongside the flags (extending the schema slightly).
        (mapping as any).verifierNotes = out.notes;
        done++;
      } catch (err) {
        failed++;
        errs.push({ id, err });
        console.error(`  [worker ${wid}] step ${id} FAILED: ${(err as Error).message ?? err}`);
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
        await Bun.write(SHIPPED_SIDECAR, JSON.stringify(sidecar, null, 2));
      }
    }
  }

  await Promise.all(Array.from({ length: CONCURRENCY }, (_, i) => worker(i + 1)));
  await Bun.write(SHIPPED_SIDECAR, JSON.stringify(sidecar, null, 2));

  const elapsed = ((Date.now() - tStart) / 1000).toFixed(1);
  console.log(`\n[verify] done in ${elapsed}s | ok=${done} fail=${failed}`);
  if (failed > 0) console.log(`[verify] failed:`, errs.map(e => e.id).join(", "));

  // Summary stats
  const conf: number[] = [];
  const flagCounts: Record<string, number> = {};
  for (const s of Object.values(sidecar.steps)) {
    if (s.verifierConfidence != null) conf.push(s.verifierConfidence);
    for (const f of s.verifierFlags ?? []) flagCounts[f] = (flagCounts[f] ?? 0) + 1;
  }
  conf.sort((a, b) => a - b);
  const median = conf[Math.floor(conf.length / 2)] ?? 0;
  const mean = conf.reduce((a, b) => a + b, 0) / Math.max(1, conf.length);
  const lowConf = conf.filter(c => c < 0.85).length;
  console.log(`\n[verify] confidence median=${median.toFixed(2)} mean=${mean.toFixed(2)} | <0.85: ${lowConf}/${conf.length}`);
  console.log(`[verify] flag counts:`, flagCounts);
}
