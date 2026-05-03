import { mkdir } from "node:fs/promises";
import { dirname } from "node:path";
import type { ItemEntry, ItemTable } from "./types.ts";
import {
  ITEMS_RAW_PATH,
  ITEM_TABLE_PATH,
  OSRSBOX_ITEMS_SUMMARY_URL,
} from "./paths.ts";

async function ensureDir(path: string) {
  await mkdir(dirname(path), { recursive: true });
}

// osrsbox items-summary.json shape:
// { "<id>": { "id": number, "name": string, "duplicate": bool, ... }, ... }
type RawItems = Record<string, ItemEntry & { duplicate?: boolean }>;

async function fetchRaw(): Promise<RawItems> {
  const cached = Bun.file(ITEMS_RAW_PATH);
  if (await cached.exists()) {
    return (await cached.json()) as RawItems;
  }
  console.log(`[items] fetching ${OSRSBOX_ITEMS_SUMMARY_URL}`);
  const res = await fetch(OSRSBOX_ITEMS_SUMMARY_URL);
  if (!res.ok) throw new Error(`items fetch failed: ${res.status}`);
  const text = await res.text();
  await ensureDir(ITEMS_RAW_PATH);
  await Bun.write(ITEMS_RAW_PATH, text);
  return JSON.parse(text) as RawItems;
}

function isUsefulItem(it: ItemEntry & { duplicate?: boolean }): boolean {
  // Skip placeholders, noted variants, and dupes — we want canonical entries.
  if (it.duplicate) return false;
  if (it.placeholder) return false;
  if (it.noted) return false;
  return true;
}

export async function buildItemTable(): Promise<ItemTable> {
  const raw = await fetchRaw();
  const items: ItemEntry[] = [];
  const byName: Record<string, number[]> = {};

  for (const it of Object.values(raw)) {
    if (!it || typeof it.id !== "number" || !it.name) continue;
    if (!isUsefulItem(it)) continue;
    items.push({
      id: it.id,
      name: it.name,
      examine: it.examine,
      members: it.members,
      stackable: it.stackable,
      noted: it.noted,
      placeholder: it.placeholder,
      duplicate: it.duplicate,
      tradeable: it.tradeable,
    });
    const key = it.name.toLowerCase();
    (byName[key] ??= []).push(it.id);
  }

  const table: ItemTable = {
    fetchedOn: new Date().toISOString(),
    items,
    byName,
  };

  await ensureDir(ITEM_TABLE_PATH);
  await Bun.write(ITEM_TABLE_PATH, JSON.stringify(table, null, 2));
  console.log(
    `[items] wrote ${items.length} items, ${Object.keys(byName).length} unique names -> ${ITEM_TABLE_PATH}`,
  );
  return table;
}
