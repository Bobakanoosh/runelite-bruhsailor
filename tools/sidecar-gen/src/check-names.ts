import { ITEM_TABLE_PATH } from "./paths.ts";
import type { ItemTable } from "./types.ts";

const items = (await Bun.file(ITEM_TABLE_PATH).json()) as ItemTable;
const lookups = [
  "energy potion(4)", "prayer potion(4)", "antipoison(4)", "stamina potion(4)",
  "compost potion(4)", "ibans staff", "iban staff", "rune shortsword",
  "digsite pendant (5)", "ring of charos(a)", "ring of charos",
  "red spiders eggs", "red spider eggs",
];
for (const n of lookups) console.log(JSON.stringify(n), "->", items.byName[n]);

const prefixes = ["iban", "digsite", "charos", "compost", "medallion", "rune sword", "rune shortsword", "small key", "energy potion", "prayer potion", "antipoison"];
for (const p of prefixes) {
  console.log("\n--- " + p + " ---");
  Object.keys(items.byName)
    .filter(k => k.includes(p))
    .slice(0, 8)
    .forEach(k => console.log("  " + k + " -> " + items.byName[k]));
}

const wordSearches = ["spider", "vile vigour", "whirlpool", "cheese potato", "compostable", "wine"];
for (const w of wordSearches) {
  console.log("\n--- " + w + " ---");
  Object.keys(items.byName)
    .filter(k => k.includes(w))
    .slice(0, 8)
    .forEach(k => console.log("  " + k + " -> " + items.byName[k]));
}
