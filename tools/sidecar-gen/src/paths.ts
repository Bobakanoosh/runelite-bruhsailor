import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
export const TOOL_ROOT = join(here, "..");
export const REPO_ROOT = join(TOOL_ROOT, "..", "..");

export const TOOL_DATA = join(TOOL_ROOT, "data");
export const PLUGIN_RESOURCES = join(REPO_ROOT, "src", "main", "resources");

// Inputs cached locally to avoid re-downloading
export const GUIDE_RAW_PATH = join(TOOL_DATA, "guide_data.raw.json");
export const ITEMS_RAW_PATH = join(TOOL_DATA, "items-summary.json");

// Stage outputs
export const PREPROCESSED_PATH = join(TOOL_DATA, "preprocessed.json");
export const ITEM_TABLE_PATH = join(TOOL_DATA, "items.json");
export const QUEST_TABLE_PATH = join(TOOL_DATA, "quests.json");
export const RESOLVED_PATH = join(TOOL_DATA, "resolved.json");

// Final shipped artifacts (consumed by the plugin)
export const SHIPPED_GUIDE = join(PLUGIN_RESOURCES, "guide_data.json");
export const SHIPPED_SIDECAR = join(PLUGIN_RESOURCES, "step_mappings.json");

// Remote sources
export const GUIDE_URL =
  "https://raw.githubusercontent.com/umkyzn/BRUHsailer/main/data/guide_data.json";
export const OSRSBOX_ITEMS_SUMMARY_URL =
  "https://raw.githubusercontent.com/0xNeffarion/osrsreboxed-db/master/docs/items-summary.json";
export const QUEST_HELPER_ENUM_URL =
  "https://raw.githubusercontent.com/Zoinkwiz/quest-helper/master/src/main/java/com/questhelper/questinfo/QuestHelperQuest.java";
