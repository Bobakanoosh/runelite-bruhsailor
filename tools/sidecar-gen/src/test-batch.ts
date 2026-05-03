// Run the generator against a list of step IDs, write each input + output to a
// single readable file so the human can review.
// Usage: bun run src/test-batch.ts <id1> <id2> ...
import type { QuestTable, ResolvedStep } from "./types.ts";
import { QUEST_TABLE_PATH, RESOLVED_PATH } from "./paths.ts";
import { generateOne } from "./generate.ts";
import { join } from "node:path";
import { TOOL_DATA } from "./paths.ts";

const ids = process.argv.slice(2);
if (ids.length === 0) {
  console.error("usage: bun run src/test-batch.ts <stepId> [stepId...]");
  process.exit(1);
}
if (!process.env.ANTHROPIC_API_KEY) {
  console.error("ANTHROPIC_API_KEY not set");
  process.exit(2);
}

const resolved = (await Bun.file(RESOLVED_PATH).json()) as ResolvedStep[];
const quests = (await Bun.file(QUEST_TABLE_PATH).json()) as QuestTable;
const outPath = join(TOOL_DATA, "test-batch-output.md");

const sections: string[] = [
  `# Sidecar generation — batch test`,
  `Generated: ${new Date().toISOString()}`,
  `Steps: ${ids.join(", ")}`,
  ``,
];

for (const id of ids) {
  const step = resolved.find(s => s.id === id);
  if (!step) {
    sections.push(`## Step ${id}\n\n*not found*\n`);
    continue;
  }
  console.log(`[batch] generating ${id}...`);
  const t0 = Date.now();
  const result = await generateOne(step, quests);
  const elapsed = Date.now() - t0;

  sections.push(`---`);
  sections.push(``);
  sections.push(`## Step ${id} — ${step.sectionTitle}`);
  sections.push(``);
  sections.push(`**Step text:**`);
  sections.push(``);
  sections.push("```");
  sections.push(step.stepText.trim());
  sections.push("```");
  sections.push(``);
  sections.push(`**items_needed (raw):** \`${step.metadata.items_needed ?? "(none)"}\``);
  sections.push(`**gp_stack:** \`${step.metadata.gp_stack ?? "(none)"}\``);
  sections.push(``);
  sections.push(`**Detected quests (deterministic, may include false positives):**`);
  if (step.detectedQuests.length === 0) sections.push(`- (none)`);
  for (const q of step.detectedQuests)
    sections.push(`- ${q.enumName} — matched: \`${q.matchedText}\``);
  sections.push(``);
  sections.push(`**Generated mapping (${elapsed}ms):**`);
  sections.push(``);
  sections.push("```json");
  sections.push(JSON.stringify(result, null, 2));
  sections.push("```");
  sections.push(``);
}

await Bun.write(outPath, sections.join("\n"));
console.log(`\n[batch] wrote ${outPath}`);
