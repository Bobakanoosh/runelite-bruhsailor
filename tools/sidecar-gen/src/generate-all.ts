// Stage 3 orchestrator: generate sidecar entries for every step.
// Concurrency-capped, resume-safe (skips steps already in the output file),
// flushes after each batch.
import Anthropic from "@anthropic-ai/sdk";
import { mkdir } from "node:fs/promises";
import { dirname } from "node:path";
import type { QuestTable, ResolvedStep, Sidecar, SidecarStep } from "./types.ts";
import { QUEST_TABLE_PATH, RESOLVED_PATH, SHIPPED_SIDECAR } from "./paths.ts";
import { generateOne } from "./generate.ts";

const CONCURRENCY = 4;
const FLUSH_EVERY = 5;
const SDK_MAX_RETRIES = 10; // SDK does exponential backoff respecting Retry-After

async function ensureDir(path: string) {
  await mkdir(dirname(path), { recursive: true });
}

async function loadExisting(): Promise<Sidecar | null> {
  const f = Bun.file(SHIPPED_SIDECAR);
  if (await f.exists()) return (await f.json()) as Sidecar;
  return null;
}

async function flush(sidecar: Sidecar) {
  await ensureDir(SHIPPED_SIDECAR);
  await Bun.write(SHIPPED_SIDECAR, JSON.stringify(sidecar, null, 2));
}

export async function generateAll(opts: { force?: boolean } = {}) {
  if (!process.env.ANTHROPIC_API_KEY) {
    throw new Error("ANTHROPIC_API_KEY not set");
  }
  const resolved = (await Bun.file(RESOLVED_PATH).json()) as ResolvedStep[];
  const quests = (await Bun.file(QUEST_TABLE_PATH).json()) as QuestTable;

  const existing = (!opts.force && (await loadExisting())) || null;
  const sidecar: Sidecar = existing ?? {
    schemaVersion: 1,
    guideUpdatedOn: "",
    steps: {},
  };

  const todo = resolved.filter(s => {
    const cur = sidecar.steps[s.id];
    if (!cur) return true;
    // If contentHash changed (guide was updated), regenerate.
    return cur.contentHash !== s.contentHash;
  });

  console.log(
    `[generate-all] ${resolved.length} total steps | ${resolved.length - todo.length} already done | ${todo.length} to generate | concurrency=${CONCURRENCY}`,
  );
  if (todo.length === 0) return;

  const client = new Anthropic({ maxRetries: SDK_MAX_RETRIES });
  const tStart = Date.now();
  let done = 0;
  let failed = 0;

  // Worker pool pattern: shared queue + N workers.
  const queue = [...todo];
  const errors: { id: string; err: unknown }[] = [];

  async function worker(id: number) {
    while (queue.length > 0) {
      const step = queue.shift();
      if (!step) break;
      try {
        const result: SidecarStep = await generateOne(step, quests, client);
        sidecar.steps[step.id] = result;
        done++;
      } catch (err) {
        failed++;
        errors.push({ id: step.id, err });
        console.error(`  [worker ${id}] step ${step.id} FAILED: ${(err as Error).message ?? err}`);
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
        await flush(sidecar);
      }
    }
  }

  await Promise.all(Array.from({ length: CONCURRENCY }, (_, i) => worker(i + 1)));

  // Final flush + summary
  // Pull guideUpdatedOn from the source guide if we can.
  try {
    const guideRaw = (await Bun.file(
      "C:/Users/Jack/Documents/Programming/oss/osrs/runelite-bruhsailor/tools/sidecar-gen/data/guide_data.raw.json",
    ).json()) as { updatedOn?: string };
    if (guideRaw.updatedOn) sidecar.guideUpdatedOn = guideRaw.updatedOn;
  } catch {}
  await flush(sidecar);

  const elapsed = ((Date.now() - tStart) / 1000).toFixed(1);
  console.log(`\n[generate-all] done in ${elapsed}s | ok=${done} fail=${failed}`);
  console.log(`[generate-all] sidecar -> ${SHIPPED_SIDECAR}`);
  if (failed > 0) {
    console.log(`[generate-all] failed steps:`, errors.map(e => e.id).join(", "));
    console.log(`[generate-all] re-run the same command to retry failures only.`);
  }
}
