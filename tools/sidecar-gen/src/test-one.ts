// Run the generator against a single step and pretty-print the result.
// Usage: bun run src/test-one.ts <stepId>     (default: 2.2.19)
import type { QuestTable, ResolvedStep } from "./types.ts";
import { QUEST_TABLE_PATH, RESOLVED_PATH } from "./paths.ts";
import { generateOne, getPrompt } from "./generate.ts";

const stepId = process.argv[2] ?? "2.2.19";
const showPromptOnly = process.argv.includes("--print-prompt");

const resolved = (await Bun.file(RESOLVED_PATH).json()) as ResolvedStep[];
const quests = (await Bun.file(QUEST_TABLE_PATH).json()) as QuestTable;
const step = resolved.find(s => s.id === stepId);
if (!step) {
  console.error(`step ${stepId} not found`);
  process.exit(1);
}

if (showPromptOnly) {
  console.log(getPrompt(step, quests));
  process.exit(0);
}

if (!process.env.ANTHROPIC_API_KEY) {
  console.error("ANTHROPIC_API_KEY not set. Put it in tools/sidecar-gen/.env or export it.");
  process.exit(2);
}

console.log(`[test-one] generating mapping for step ${stepId}...`);
const t0 = Date.now();
const result = await generateOne(step, quests);
const elapsed = Date.now() - t0;

console.log(`\n=== Step ${stepId} ===`);
console.log(`Section: ${step.sectionTitle}`);
console.log(`\n--- Step text ---`);
console.log(step.stepText.trim().slice(0, 500) + (step.stepText.length > 500 ? "..." : ""));
console.log(`\n--- Items needed (raw) ---`);
console.log(step.metadata.items_needed ?? "(none)");
console.log(`\n--- Generated mapping (${elapsed}ms) ---`);
console.log(JSON.stringify(result, null, 2));
