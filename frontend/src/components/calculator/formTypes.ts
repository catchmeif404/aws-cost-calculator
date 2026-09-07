import type { ResourceCatalogItem } from "@/lib/api";

let keyCounter = 0;
export function newKey(): string {
  return `k${keyCounter++}`;
}

// One instance of a catalog resource type the user has added to their architecture. `type` and
// `kind` mirror the ResourceCatalogItem it was created from; `config` holds this instance's
// field values, keyed by ResourceCatalogField.key. Shape (not just values) is fully catalog-driven
// so adding a new backend ResourceType needs no frontend change.
export interface ResourceInstance {
  _key: string;
  type: string;
  kind: string;
  config: Record<string, unknown>;
}

export function createResourceInstance(catalogItem: ResourceCatalogItem): ResourceInstance {
  return {
    _key: newKey(),
    type: catalogItem.type,
    kind: catalogItem.kind,
    config: { ...catalogItem.defaults },
  };
}

// Like createResourceInstance, but seeded from an already-decided configuration (e.g. one an AI
// recommendation composed) instead of the catalog's bare defaults — used when a recommended
// architecture is handed to the manual builder for editing.
export function createResourceInstanceFromConfig(
  catalogItem: ResourceCatalogItem,
  config: Record<string, unknown>
): ResourceInstance {
  return {
    _key: newKey(),
    type: catalogItem.type,
    kind: catalogItem.kind,
    config: { ...catalogItem.defaults, ...config },
  };
}

// Shared lookups so result/share-card screens never hardcode a per-type label map — they render
// whatever the backend's catalog says. Falls back to the raw type string if the catalog hasn't
// loaded yet or (shouldn't happen) doesn't know about a type, rather than throwing.
export function findCatalogItem(
  catalog: ResourceCatalogItem[],
  type: string
): ResourceCatalogItem | undefined {
  return catalog.find((item) => item.type === type);
}

export function resourceLabel(catalog: ResourceCatalogItem[], type: string): string {
  return findCatalogItem(catalog, type)?.title ?? type;
}

export interface LayoutRect {
  x: number;
  y: number;
  width: number;
  height: number;
}

const LAYOUT_NODE_WIDTH = 178;
const LAYOUT_NODE_HEIGHT = 104;
const LAYOUT_LANE_GAP = 56;
const LAYOUT_ROW_GAP = 16;
const LAYOUT_MARGIN_X = 40;
const LAYOUT_MARGIN_Y = 40;
const LAYOUT_SUPPORT_GAP = 40;
const LAYOUT_LANE_PADDING = 16;
const LAYOUT_LANE_LABEL_INSET = 32;

// Same left-to-right, category-grouped flow RecommendedArchitectureDiagram uses for its read-only
// preview (Networking -> Compute -> ... -> Storage, with Security/Governance as an unconnected
// row underneath) — mirrored here, keyed by ResourceInstance._key, so applying a recommendation
// to the visual builder seeds it with a layout resembling what the user just looked at, instead
// of VisualArchitectureBuilder's generic index-ordered grid (its fallback for resources with no
// known position at all).
const LAYOUT_FLOW_CATEGORY_ORDER = [
  "Networking & Content Delivery",
  "Compute",
  "Application Integration",
  "Analytics",
  "AI/ML",
  "Database",
  "Storage",
];
const LAYOUT_SUPPORT_CATEGORIES = ["Security", "Management & Governance"];

// VisualArchitectureBuilder's fixed group palette (VPC/AZ/Subnet/Compute-cluster) has its own
// small, fixed set of tones — reused here (cycling through them) for the synthetic per-category
// "영역" boxes instead of inventing a 6th/7th/... color scheme, so a recommendation-seeded group
// looks like something the user could've drawn from that same palette themselves.
const LAYOUT_GROUP_TONES = [
  "border-emerald-400 bg-emerald-50/70 text-emerald-900 dark:bg-emerald-950/30 dark:text-emerald-200",
  "border-sky-400 bg-sky-50/70 text-sky-900 dark:bg-sky-950/30 dark:text-sky-200",
  "border-cyan-400 bg-cyan-50/70 text-cyan-900 dark:bg-cyan-950/30 dark:text-cyan-200",
  "border-indigo-400 bg-indigo-50/70 text-indigo-900 dark:bg-indigo-950/30 dark:text-indigo-200",
  "border-orange-400 bg-orange-50/70 text-orange-900 dark:bg-orange-950/30 dark:text-orange-200",
];

export interface LayoutGroupSeed {
  id: string;
  // Matches VisualArchitectureBuilder's GroupKind union structurally — kept as a literal here
  // rather than imported since that type isn't exported, and the value itself is inert after
  // creation (only used to look up the initial palette preset, which this seed already bypasses).
  kind: "vpc";
  title: string;
  shortName: string;
  description: string;
  tone: string;
}

export interface LayoutConnection {
  from: string;
  to: string;
}

export interface CategoryLaneLayout {
  rects: Record<string, LayoutRect>;
  groups: LayoutGroupSeed[];
  connections: LayoutConnection[];
}

export function computeCategoryLaneLayout(
  resources: ResourceInstance[],
  catalog: ResourceCatalogItem[]
): CategoryLaneLayout {
  const byCategory = new Map<string, ResourceInstance[]>();
  const supportItems: ResourceInstance[] = [];

  for (const resource of resources) {
    const category = findCatalogItem(catalog, resource.type)?.category ?? "Other";
    if (LAYOUT_SUPPORT_CATEGORIES.includes(category)) {
      supportItems.push(resource);
      continue;
    }
    byCategory.set(category, [...(byCategory.get(category) ?? []), resource]);
  }

  const laneOrder = [
    ...LAYOUT_FLOW_CATEGORY_ORDER,
    ...[...byCategory.keys()].filter((category) => !LAYOUT_FLOW_CATEGORY_ORDER.includes(category)),
  ].filter((category) => byCategory.has(category));

  const rects: Record<string, LayoutRect> = {};
  const groups: LayoutGroupSeed[] = [];
  const connections: LayoutConnection[] = [];
  let maxRows = 1;
  let previousLaneItems: ResourceInstance[] | null = null;

  laneOrder.forEach((category, laneIndex) => {
    const items = byCategory.get(category) ?? [];
    maxRows = Math.max(maxRows, items.length);

    const laneX = LAYOUT_MARGIN_X + laneIndex * (LAYOUT_NODE_WIDTH + LAYOUT_LANE_GAP);
    items.forEach((resource, rowIndex) => {
      rects[resource._key] = {
        x: laneX,
        y: LAYOUT_MARGIN_Y + rowIndex * (LAYOUT_NODE_HEIGHT + LAYOUT_ROW_GAP),
        width: LAYOUT_NODE_WIDTH,
        height: LAYOUT_NODE_HEIGHT,
      };
    });

    // One group ("영역") box per lane, enclosing that lane's nodes — mirrors the dashed
    // category-labeled boxes RecommendedArchitectureDiagram draws for the same lanes (support
    // categories don't get one there either, so skipped here too).
    const groupId = `rec-lane-${laneIndex}`;
    groups.push({
      id: groupId,
      kind: "vpc",
      title: category,
      shortName: category.replace(/[^a-zA-Z0-9가-힣]/g, "").slice(0, 4).toUpperCase() || "GRP",
      description: "AI 추천 구성 영역",
      tone: LAYOUT_GROUP_TONES[laneIndex % LAYOUT_GROUP_TONES.length],
    });
    rects[groupId] = {
      x: laneX - LAYOUT_LANE_PADDING,
      y: LAYOUT_MARGIN_Y - LAYOUT_LANE_LABEL_INSET,
      width: LAYOUT_NODE_WIDTH + LAYOUT_LANE_PADDING * 2,
      height:
        LAYOUT_LANE_LABEL_INSET + items.length * (LAYOUT_NODE_HEIGHT + LAYOUT_ROW_GAP) - LAYOUT_ROW_GAP + LAYOUT_LANE_PADDING,
    };

    // Every node in this lane connects to every node in the previous lane — same all-pairs
    // adjacent-lane rule RecommendedArchitectureDiagram uses, so an arbitrary AI-composed
    // combination of categories still renders as a connected flow instead of isolated boxes.
    if (previousLaneItems) {
      for (const to of items) {
        for (const from of previousLaneItems) {
          connections.push({ from: from._key, to: to._key });
        }
      }
    }
    previousLaneItems = items;
  });

  const supportY = LAYOUT_MARGIN_Y + maxRows * (LAYOUT_NODE_HEIGHT + LAYOUT_ROW_GAP) + LAYOUT_SUPPORT_GAP;
  supportItems.forEach((resource, index) => {
    rects[resource._key] = {
      x: LAYOUT_MARGIN_X + index * (LAYOUT_NODE_WIDTH + LAYOUT_LANE_GAP),
      y: supportY,
      width: LAYOUT_NODE_WIDTH,
      height: LAYOUT_NODE_HEIGHT,
    };
  });

  return { rects, groups, connections };
}

export interface CalculatorForm {
  projectName: string;
  monthlyUsers: number;
  requestsPerUser: number;
  busyTrafficLevel: string;
  serviceStage: string;
  region: string;
  resources: ResourceInstance[];
}

export const REGIONS = [
  { code: "ap-northeast-2", label: "서울 (ap-northeast-2)", labelEn: "Seoul (ap-northeast-2)" },
  { code: "ap-northeast-1", label: "도쿄 (ap-northeast-1)", labelEn: "Tokyo (ap-northeast-1)" },
  { code: "us-east-1", label: "버지니아 (us-east-1)", labelEn: "N. Virginia (us-east-1)" },
  { code: "us-west-2", label: "오레곤 (us-west-2)", labelEn: "Oregon (us-west-2)" },
  { code: "eu-central-1", label: "프랑크푸르트 (eu-central-1)", labelEn: "Frankfurt (eu-central-1)" },
];

export function regionLabel(code: string, locale: string): string {
  const region = REGIONS.find((r) => r.code === code);
  if (!region) return code;
  return locale === "en" ? region.labelEn : region.label;
}

// Starter architecture shown to a new user before they've touched anything — a UX choice of
// which catalog *kinds* to pre-populate, not a hardcoded config shape (values still come from
// each catalog item's own `defaults`). Applied once the catalog has loaded; see Calculator.tsx.
export const STARTER_RESOURCE_KINDS = ["ecs", "rds", "s3", "alb"];

export const DEFAULT_FORM: CalculatorForm = {
  projectName: "",
  monthlyUsers: 10_000,
  requestsPerUser: 100,
  busyTrafficLevel: "similar",
  serviceStage: "mvp",
  region: "ap-northeast-2",
  resources: [],
};
