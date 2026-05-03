// BRUHsailer guide JSON shape

export interface GuideText {
  text: string;
  formatting?: {
    bold?: boolean;
    italic?: boolean;
    underline?: boolean;
    fontSize?: number;
    color?: { r: number; g: number; b: number };
    link?: string;
  };
}

export interface GuideStep {
  content: GuideText[];
  nestedContent: GuideStep[];
  metadata: {
    gp_stack?: string;
    items_needed?: string;
    total_time?: string;
    skills_quests_met?: string;
  };
}

export interface GuideSection {
  title: string;
  steps: GuideStep[];
  footnotes?: unknown[];
}

export interface GuideChapter {
  title: string;
  sections: GuideSection[];
}

export interface Guide {
  updatedOn: string;
  title: string;
  chapters: GuideChapter[];
}

// Preprocessed step (stage 0 output)

export interface ItemToken {
  raw: string;        // original token, e.g. "232 gp" or "knife"
  qty: number | null; // parsed quantity, null when absent
  name: string;       // normalized name, e.g. "gp", "knife"
}

export interface PreprocessedStep {
  id: string;             // "1.1.1"
  chapterIdx: number;
  sectionIdx: number;
  stepIdx: number;
  chapterTitle: string;
  sectionTitle: string;
  contentHash: string;    // sha1 of concatenated content text
  stepText: string;       // concatenated content[].text
  metadata: GuideStep["metadata"];
  itemTokens: ItemToken[]; // tokenized items_needed
}

// Lookup tables (stage 1 output)

export interface ItemEntry {
  id: number;
  name: string;
  examine?: string;
  members?: boolean;
  stackable?: boolean;
  noted?: boolean;
  placeholder?: boolean;
  duplicate?: boolean;
  tradeable?: boolean;
}

export interface ItemTable {
  fetchedOn: string;
  items: ItemEntry[];
  // index maps lowercased name -> array of item ids (multiple for variants)
  byName: Record<string, number[]>;
}

export interface QuestEntry {
  enumName: string;       // e.g. "COOKS_ASSISTANT"
  displayName: string;    // e.g. "Cook's Assistant"
  aliases: string[];      // case-insensitive substring aliases (>= 4 chars)
  shortAliases: string[]; // case-sensitive whole-word aliases (e.g. "RFD", "DT")
}

export interface QuestTable {
  fetchedOn: string;
  quests: QuestEntry[];
}

// Resolver output (stage 2)

export interface ResolvedItem {
  rawToken: string;
  qty: number | null;
  candidates: { id: number; name: string }[]; // empty when unresolvable
  isAbstract: boolean;
  abstractLabel?: string;
}

export interface ResolvedStep extends PreprocessedStep {
  resolvedItems: ResolvedItem[];
  detectedQuests: { enumName: string; displayName: string; matchedText: string }[];
}

// Final sidecar entry (stage 3+ output)

export interface SidecarItem {
  id: number;
  name: string;
  qty: number | null;
  source: "items_needed" | "content";
}

export interface SidecarAbstractItem {
  label: string;
  rawToken: string;
}

export interface SidecarUnresolvedItem {
  rawToken: string;
  qty: number | null;
}

export interface SidecarStep {
  contentHash: string;
  title: string;
  questIds: string[];
  items: SidecarItem[];
  abstractItems: SidecarAbstractItem[];
  unresolvedItems: SidecarUnresolvedItem[];
  verified: boolean;
  verifierConfidence: number | null;
  verifierFlags: string[];
}

export interface Sidecar {
  schemaVersion: 1;
  guideUpdatedOn: string;
  steps: Record<string, SidecarStep>;
}
