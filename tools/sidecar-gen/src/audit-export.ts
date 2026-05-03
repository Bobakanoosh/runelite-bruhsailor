// Export per-step audit packets — one JSON file per step containing the
// source text + generated mapping. Subagents read these directly.
import { mkdir } from "node:fs/promises";
import { join } from "node:path";
import type { Guide, Sidecar } from "./types.ts";
import { GUIDE_RAW_PATH, SHIPPED_SIDECAR, TOOL_DATA } from "./paths.ts";

const OUT_DIR = join(TOOL_DATA, "audit-packets");
await mkdir(OUT_DIR, { recursive: true });

const guide = (await Bun.file(GUIDE_RAW_PATH).json()) as Guide;
const sidecar = (await Bun.file(SHIPPED_SIDECAR).json()) as Sidecar;

let count = 0;
guide.chapters.forEach((ch, ci) => {
  ch.sections.forEach((sec, si) => {
    sec.steps.forEach((step, pi) => {
      const id = `${ci + 1}.${si + 1}.${pi + 1}`;
      const stepText = (step.content ?? []).map(c => c.text).join("");
      const mapping = sidecar.steps[id];
      if (!mapping) return;
      const packet = {
        stepId: id,
        section: sec.title,
        stepText,
        itemsNeeded: step.metadata?.items_needed ?? "",
        gpStack: step.metadata?.gp_stack ?? "",
        mapping: {
          questIds: mapping.questIds,
          items: mapping.items,
          abstractItems: mapping.abstractItems,
          unresolvedItems: mapping.unresolvedItems,
        },
      };
      Bun.write(join(OUT_DIR, `${id}.json`), JSON.stringify(packet, null, 2));
      count++;
    });
  });
});
console.log(`wrote ${count} audit packets to ${OUT_DIR}`);
