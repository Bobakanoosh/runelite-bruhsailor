import { createHash } from "node:crypto";
import { mkdir } from "node:fs/promises";
import { dirname } from "node:path";
import type { Guide, GuideStep, ItemToken, PreprocessedStep } from "./types.ts";
import {
  GUIDE_URL,
  GUIDE_RAW_PATH,
  PREPROCESSED_PATH,
  SHIPPED_GUIDE,
} from "./paths.ts";

async function ensureDir(path: string) {
  await mkdir(dirname(path), { recursive: true });
}

async function downloadGuide(): Promise<Guide> {
  console.log(`[preprocess] fetching ${GUIDE_URL}`);
  const res = await fetch(GUIDE_URL);
  if (!res.ok) throw new Error(`guide fetch failed: ${res.status}`);
  const text = await res.text();
  await ensureDir(GUIDE_RAW_PATH);
  await Bun.write(GUIDE_RAW_PATH, text);
  await ensureDir(SHIPPED_GUIDE);
  await Bun.write(SHIPPED_GUIDE, text);
  return JSON.parse(text) as Guide;
}

async function loadGuide(): Promise<Guide> {
  const f = Bun.file(GUIDE_RAW_PATH);
  if (await f.exists()) {
    return (await f.json()) as Guide;
  }
  return downloadGuide();
}

function flattenContent(step: GuideStep): string {
  const parts: string[] = [];
  for (const c of step.content ?? []) parts.push(c.text);
  for (const nested of step.nestedContent ?? []) {
    parts.push("\n  - " + flattenContent(nested));
  }
  return parts.join("");
}

function hashStep(text: string): string {
  return createHash("sha1").update(text).digest("hex");
}

// items_needed tokenizer.
// Splits on commas/semicolons, then attempts to extract a leading quantity.
// Examples:
//   "232 gp"           -> { qty: 232, name: "gp" }
//   "3 pots of flour"  -> { qty: 3,   name: "pots of flour" }
//   "knife"            -> { qty: null, name: "knife" }
//   "2 step authenticator" — caller may flag; we don't try to be smart here.
const QTY_PREFIX = /^(\d+(?:[.,]\d+)?)\s*(gp|x)?\s+(.+)$/i;
const COMPACT_GP = /^(\d+(?:[.,]\d+)?)(gp|k|m)\b\s*(.*)$/i;

function splitTopLevel(s: string): string[] {
  // Split on commas/semicolons that are NOT inside parentheses or brackets.
  const out: string[] = [];
  let depth = 0;
  let buf = "";
  for (const ch of s) {
    if (ch === "(" || ch === "[" || ch === "{") depth++;
    else if (ch === ")" || ch === "]" || ch === "}") depth = Math.max(0, depth - 1);
    if ((ch === "," || ch === ";") && depth === 0) {
      out.push(buf);
      buf = "";
    } else {
      buf += ch;
    }
  }
  if (buf.trim()) out.push(buf);
  return out;
}

function tokenizeItemsNeeded(raw: string | undefined): ItemToken[] {
  if (!raw) return [];
  const trimmed = raw.trim();
  if (!trimmed || trimmed.toLowerCase() === "none") return [];
  const tokens: ItemToken[] = [];
  for (const piece of splitTopLevel(trimmed)) {
    const t = piece.trim();
    if (!t) continue;
    let qty: number | null = null;
    let name = t;

    const compact = COMPACT_GP.exec(t);
    if (compact) {
      const num = Number(compact[1]!.replace(",", ""));
      const unit = compact[2]!.toLowerCase();
      qty = unit === "k" ? num * 1000 : unit === "m" ? num * 1_000_000 : num;
      name = (compact[3] ?? "gp").trim() || "gp";
    } else {
      const m = QTY_PREFIX.exec(t);
      if (m) {
        qty = Number(m[1]!.replace(",", ""));
        const unit = m[2]?.toLowerCase();
        name = (m[3] ?? "").trim();
        if (unit === "gp" && !name) name = "gp";
        else if (unit === "gp") name = `gp ${name}`.trim();
      }
    }

    tokens.push({ raw: t, qty, name: name.toLowerCase() });
  }
  return tokens;
}

export async function preprocess(): Promise<PreprocessedStep[]> {
  const guide = await loadGuide();
  const out: PreprocessedStep[] = [];
  guide.chapters.forEach((chapter, ci) => {
    chapter.sections.forEach((section, si) => {
      section.steps.forEach((step, pi) => {
        const stepText = flattenContent(step);
        out.push({
          id: `${ci + 1}.${si + 1}.${pi + 1}`,
          chapterIdx: ci + 1,
          sectionIdx: si + 1,
          stepIdx: pi + 1,
          chapterTitle: chapter.title,
          sectionTitle: section.title,
          contentHash: hashStep(stepText),
          stepText,
          metadata: step.metadata ?? {},
          itemTokens: tokenizeItemsNeeded(step.metadata?.items_needed),
        });
      });
    });
  });

  await ensureDir(PREPROCESSED_PATH);
  await Bun.write(PREPROCESSED_PATH, JSON.stringify(out, null, 2));
  console.log(
    `[preprocess] wrote ${out.length} steps -> ${PREPROCESSED_PATH}`,
  );
  return out;
}
