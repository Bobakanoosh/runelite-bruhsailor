// Aggregate per-step audit verdicts into a single report.
import { readdir } from "node:fs/promises";
import { join } from "node:path";
import { TOOL_DATA } from "./paths.ts";

interface Verdict {
  stepId: string;
  accurate: boolean;
  confidence: number;
  issues: string[];
  notes?: string;
}

const VERDICTS_DIR = join(TOOL_DATA, "audit-verdicts");
const OUT_PATH = join(TOOL_DATA, "audit-report.md");

const files = (await readdir(VERDICTS_DIR)).filter(f => f.endsWith(".json"));
const verdicts: Verdict[] = [];
for (const f of files) {
  try {
    const v = (await Bun.file(join(VERDICTS_DIR, f)).json()) as Verdict;
    verdicts.push(v);
  } catch (e) {
    console.error(`failed to parse ${f}:`, e);
  }
}

verdicts.sort((a, b) => {
  const pa = a.stepId.split(".").map(Number);
  const pb = b.stepId.split(".").map(Number);
  return (pa[0]! - pb[0]!) || (pa[1]! - pb[1]!) || (pa[2]! - pb[2]!);
});

const total = verdicts.length;
const accurate = verdicts.filter(v => v.accurate).length;
const inaccurate = total - accurate;

const conf = verdicts.map(v => v.confidence).sort((a, b) => a - b);
const median = conf[Math.floor(conf.length / 2)] ?? 0;
const mean = conf.reduce((a, b) => a + b, 0) / conf.length;
const high = conf.filter(c => c >= 0.9).length;
const mid = conf.filter(c => c >= 0.7 && c < 0.9).length;
const low = conf.filter(c => c < 0.7).length;

const issuePatterns: Record<string, number> = {};
for (const v of verdicts) {
  for (const issueRaw of v.issues ?? []) {
    const issue = typeof issueRaw === "string" ? issueRaw : JSON.stringify(issueRaw);
    const lower = issue.toLowerCase();
    const buckets: [string, RegExp][] = [
      ["missing-item", /missing|not.*captured|should be.*item|should resolve|not in items|not mapped/],
      ["wrong-quest", /wrong.*quest|invalid.*quest|extraneous.*quest|not.*progressed|not actively|should not be in questIds|incorrectly.*included|spurious.*quest/],
      ["spurious-item", /spurious|extraneous|not.*step|not mentioned|should not be in items|hallucinat|wrong item|unrelated|polluting/],
      ["unresolved-resolvable", /should.*resolved|left unresolved|misclassified.*unresolved|should be in items.*not unresolved|item.*resolvable/],
      ["abstract-misuse", /abstract.*specific|specific.*abstract|abstractItems.*item|cash stack.*duplic/],
      ["qty-issue", /qty|quantity|qty:.*null|missing.*count|qty needs|incorrect quantity/],
      ["tier-mismatch", /wrong tier|wrong.*variant|tier.*mismatch|cut sapphire|wrong item ID/],
      ["tbd-step", /\btbd\b/],
    ];
    let matched = false;
    for (const [name, re] of buckets) {
      if (re.test(lower)) {
        issuePatterns[name] = (issuePatterns[name] ?? 0) + 1;
        matched = true;
        break;
      }
    }
    if (!matched) issuePatterns["other"] = (issuePatterns["other"] ?? 0) + 1;
  }
}

const lines: string[] = [];
lines.push(`# BRUHsailer Sidecar Audit Report`);
lines.push(`Generated: ${new Date().toISOString()}`);
lines.push(`Auditor: ${total} parallel Haiku subagents`);
lines.push(``);
lines.push(`## Summary`);
lines.push(``);
lines.push(`- **Total steps audited:** ${total}`);
lines.push(`- **Accurate:** ${accurate} (${((accurate / total) * 100).toFixed(1)}%)`);
lines.push(`- **Inaccurate:** ${inaccurate} (${((inaccurate / total) * 100).toFixed(1)}%)`);
lines.push(``);
lines.push(`## Confidence Distribution`);
lines.push(``);
lines.push(`- **Mean:** ${mean.toFixed(3)}`);
lines.push(`- **Median:** ${median.toFixed(3)}`);
lines.push(`- **High (≥0.90):** ${high} (${((high / total) * 100).toFixed(1)}%)`);
lines.push(`- **Mid (0.70-0.90):** ${mid} (${((mid / total) * 100).toFixed(1)}%)`);
lines.push(`- **Low (<0.70):** ${low} (${((low / total) * 100).toFixed(1)}%)`);
lines.push(``);
lines.push(`## Issue Pattern Counts`);
lines.push(``);
const sortedPatterns = Object.entries(issuePatterns).sort((a, b) => b[1] - a[1]);
for (const [k, v] of sortedPatterns) lines.push(`- **${k}:** ${v}`);
lines.push(``);
lines.push(`## Lowest Confidence Steps (review priority)`);
lines.push(``);
lines.push(`| Step | Conf | Accurate | Issues | Notes (truncated) |`);
lines.push(`|------|------|----------|--------|---------------------|`);
const lowestN = [...verdicts].sort((a, b) => a.confidence - b.confidence).slice(0, 30);
for (const v of lowestN) {
  const notes = (v.notes ?? "").replace(/\|/g, "\\|").replace(/\n/g, " ").slice(0, 120);
  lines.push(
    `| ${v.stepId} | ${v.confidence.toFixed(2)} | ${v.accurate} | ${v.issues.length} | ${notes} |`,
  );
}
lines.push(``);
lines.push(`## Highest Confidence Steps (sanity check)`);
lines.push(``);
lines.push(`| Step | Conf | Issues |`);
lines.push(`|------|------|--------|`);
const highestN = [...verdicts].sort((a, b) => b.confidence - a.confidence).slice(0, 10);
for (const v of highestN) {
  lines.push(`| ${v.stepId} | ${v.confidence.toFixed(2)} | ${v.issues.length} |`);
}
lines.push(``);
lines.push(`## All Verdicts`);
lines.push(``);
lines.push(`| Step | Conf | Accurate | #Issues |`);
lines.push(`|------|------|----------|---------|`);
for (const v of verdicts) {
  lines.push(`| ${v.stepId} | ${v.confidence.toFixed(2)} | ${v.accurate} | ${v.issues.length} |`);
}

await Bun.write(OUT_PATH, lines.join("\n"));
console.log(`[aggregate] ${total} verdicts processed -> ${OUT_PATH}`);
console.log(`[aggregate] accurate=${accurate} (${((accurate / total) * 100).toFixed(1)}%) | mean conf=${mean.toFixed(3)}`);
