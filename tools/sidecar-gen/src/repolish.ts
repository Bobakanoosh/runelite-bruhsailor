// Re-run the deterministic post-resolver against an existing sidecar.
// Use this after expanding the alias map: cheap, no LLM calls, idempotent.
//
// For each unresolvedItem in each step:
//   - try resolveStrict
//   - if it resolves, promote to items[] with source="content"
//   - if not, leave it alone (preserve raw)
import type { ItemTable, Sidecar } from "./types.ts";
import { ITEM_TABLE_PATH, SHIPPED_SIDECAR } from "./paths.ts";
import { resolveStrict } from "./resolvers.ts";

export async function repolish() {
  const items = (await Bun.file(ITEM_TABLE_PATH).json()) as ItemTable;
  const sidecar = (await Bun.file(SHIPPED_SIDECAR).json()) as Sidecar;

  let promoted = 0;
  let stillUnresolved = 0;
  let stepsAffected = 0;

  for (const step of Object.values(sidecar.steps)) {
    const remaining: typeof step.unresolvedItems = [];
    let stepPromoted = 0;
    for (const u of step.unresolvedItems) {
      const hit = resolveStrict(u.rawToken, items);
      if (hit && !step.items.some(i => i.id === hit.id)) {
        step.items.push({
          id: hit.id,
          name: hit.name,
          qty: u.qty,
          source: "content",
        });
        stepPromoted++;
      } else {
        remaining.push(u);
      }
    }
    if (stepPromoted > 0) {
      stepsAffected++;
      promoted += stepPromoted;
    }
    stillUnresolved += remaining.length;
    step.unresolvedItems = remaining;

    // Also drop now-claimed names from abstractItems (defensive — earlier dedupe
    // ran before these aliases existed).
    const claimed = new Set<string>();
    for (const it of step.items) claimed.add(it.name.toLowerCase());
    step.abstractItems = step.abstractItems.filter(
      a => !claimed.has(a.label.toLowerCase()) && !claimed.has(a.rawToken.toLowerCase()),
    );
  }

  await Bun.write(SHIPPED_SIDECAR, JSON.stringify(sidecar, null, 2));
  console.log(`[repolish] promoted ${promoted} tokens to items[] across ${stepsAffected} steps`);
  console.log(`[repolish] remaining unresolved tokens: ${stillUnresolved}`);
  console.log(`[repolish] saved -> ${SHIPPED_SIDECAR}`);
}
