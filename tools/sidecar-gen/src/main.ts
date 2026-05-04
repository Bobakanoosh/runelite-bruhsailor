import { preprocess } from "./preprocess.ts";
import { buildItemTable } from "./items-table.ts";
import { buildQuestTable } from "./quests-table.ts";
import { resolveAll } from "./resolvers.ts";
import { generateAll } from "./generate-all.ts";
import { enrichAll } from "./enrich.ts";
import { repolish } from "./repolish.ts";
import { verifyAll } from "./verify.ts";

const cmd = process.argv[2] ?? "all";

switch (cmd) {
  case "preprocess":
    await preprocess();
    break;
  case "fetch-tables":
    await buildItemTable();
    await buildQuestTable();
    break;
  case "resolve":
    await resolveAll();
    break;
  case "all":
    await preprocess();
    await buildItemTable();
    await buildQuestTable();
    await resolveAll();
    break;
  case "generate":
    await generateAll({ force: process.argv.includes("--force") });
    break;
  case "enrich":
    await enrichAll({ force: process.argv.includes("--force") });
    break;
  case "repolish":
    await repolish();
    break;
  case "verify":
    await verifyAll();
    break;
  default:
    console.error(`unknown command: ${cmd}`);
    console.error(`usage: bun run src/main.ts {preprocess|fetch-tables|resolve|all|generate|enrich|verify}`);
    process.exit(1);
}
