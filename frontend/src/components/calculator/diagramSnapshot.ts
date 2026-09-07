export interface DiagramSnapshotItem {
  id: string;
  title: string;
  shortName: string;
  description: string;
  tone: string;
  category?: string;
  details: string[];
  x: number;
  y: number;
  width: number;
  height: number;
  itemType: "group" | "resource";
}

export interface DiagramSnapshotConnection {
  from: string;
  to: string;
}

// Tailwind (v4, zero-config content detection) only scans files under frontend/ — it never sees
// the "bg-X-100 text-X-800 border-X-200" tone strings that live in the backend's
// ResourceCatalogService.java, so classes built purely from `item.tone`/`node.tone` never
// actually get generated and silently render uncolored. This table is the fix: it's the same
// 9-category color grouping as the backend, but spelled out here as literal class names so the
// build's scanner can find them. Keyed by ResourceCatalogItem.category.
const CATEGORY_TONE: Record<string, { bg: string; text: string; border: string }> = {
  Compute: { bg: "bg-orange-100", text: "text-orange-800", border: "border-orange-200" },
  Database: { bg: "bg-blue-100", text: "text-blue-800", border: "border-blue-200" },
  Storage: { bg: "bg-green-100", text: "text-green-800", border: "border-green-200" },
  "Networking & Content Delivery": { bg: "bg-violet-100", text: "text-violet-800", border: "border-violet-200" },
  Security: { bg: "bg-red-100", text: "text-red-800", border: "border-red-200" },
  "Application Integration": { bg: "bg-fuchsia-100", text: "text-fuchsia-800", border: "border-fuchsia-200" },
  "Management & Governance": { bg: "bg-cyan-100", text: "text-cyan-800", border: "border-cyan-200" },
  Analytics: { bg: "bg-pink-100", text: "text-pink-800", border: "border-pink-200" },
  "AI/ML": { bg: "bg-teal-100", text: "text-teal-800", border: "border-teal-200" },
};
const FALLBACK_TONE = { bg: "bg-zinc-100", text: "text-zinc-800", border: "border-zinc-200" };

export function categoryBg(category?: string): string {
  return (category && CATEGORY_TONE[category]?.bg) || FALLBACK_TONE.bg;
}
export function categoryText(category?: string): string {
  return (category && CATEGORY_TONE[category]?.text) || FALLBACK_TONE.text;
}
export function categoryBorder(category?: string): string {
  return (category && CATEGORY_TONE[category]?.border) || FALLBACK_TONE.border;
}

// Only for tone strings that are ALREADY known to be literal, build-time-visible class names —
// e.g. RecommendedArchitectureDiagram's hardcoded support-node tones. Splits the fixed
// "bg-X-100 text-X-800 border-X-200" triple apart the same way categoryBg/Text/Border do, so a
// card can use the background/border and a badge inside it the border/text, without the two
// fighting over the same class. Do NOT use this on a tone string that only exists at runtime
// (e.g. straight from the catalog API) — see the Tailwind scanning note above.
export function toneBg(tone: string): string {
  return tone.split(" ")[0] ?? "";
}
export function toneText(tone: string): string {
  return tone.split(" ")[1] ?? "";
}
export function toneBorder(tone: string): string {
  return tone.split(" ")[2] ?? "";
}

export interface DiagramSnapshot {
  items: DiagramSnapshotItem[];
  connections: DiagramSnapshotConnection[];
  width: number;
  height: number;
}
