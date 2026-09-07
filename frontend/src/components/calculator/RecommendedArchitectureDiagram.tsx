"use client";

import { useEffect, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { ResourceType, ResourceCost, ResourceCatalogItem } from "@/lib/api";
import { findCatalogItem } from "./formTypes";
import { categoryBg, categoryBorder, categoryText, toneBg, toneBorder, toneText } from "./diagramSnapshot";

interface Props {
  resources: ResourceCost[];
  additionalRecommendations: string[];
  resourceCatalog: ResourceCatalogItem[];
}

interface NodeSpec {
  id: string;
  label: string;
  shortName: string;
  details: string[];
  cost?: number;
  x: number;
  y: number;
  bg: string;
  text: string;
  border: string;
}

interface Lane {
  category: string;
  nodes: NodeSpec[];
}

const NODE_WIDTH = 166;
const NODE_HEIGHT = 92;
const LANE_GAP = 56;
const ROW_GAP = 16;
const LANE_LABEL_INSET = 40;
const LANE_PADDING = 12;
const MARGIN_X = 32;
const MARGIN_Y = 64;
const SUPPORT_ROW_GAP = 44;
const BOTTOM_MARGIN = 32;

// Left-to-right request-flow order for the main lanes. AI recommendations now pick freely from
// the full resource catalog (not a hand-picked template), so positions/connections are derived
// from each resource's catalog category instead of a per-type whitelist — any category not
// listed here (there shouldn't be any; this mirrors ResourceCatalogService's 9 categories) is
// still appended as a trailing lane instead of being dropped.
const FLOW_CATEGORY_ORDER = [
  "Networking & Content Delivery",
  "Compute",
  "Application Integration",
  "Analytics",
  "AI/ML",
  "Database",
  "Storage",
];

// Cross-cutting categories (keys, alarms, audit trails...) don't belong in a left-to-right
// request flow — they render as an unconnected row underneath the flow instead of a lane.
const SUPPORT_CATEGORIES = ["Security", "Management & Governance"];

function metaFor(catalog: ResourceCatalogItem[], type: ResourceType) {
  const item = findCatalogItem(catalog, type);
  return {
    label: item?.title ?? type,
    shortName: item?.shortName ?? type.slice(0, 4),
    category: item?.category,
  };
}

function formatUsd(value: number) {
  return `$${value.toFixed(2)}`;
}

function has(resources: ResourceCost[], type: ResourceType) {
  return resources.some((resource) => resource.type === type);
}

// Detail lines come straight from the catalog's own cost-relevant fields instead of a per-type
// switch, so a resource type this diagram has never seen before still shows meaningful details.
function resourceDetails(catalogItem: ResourceCatalogItem | undefined, config: Record<string, unknown>): string[] {
  if (!catalogItem) return [];
  return catalogItem.fields
    .filter((field) => field.costRelevant)
    .slice(0, 2)
    .map((field) => {
      const value = config[field.key] ?? field.defaultValue;
      if (field.type === "select") {
        const option = field.options.find((o) => String(o.value) === String(value));
        return option?.label ?? String(value);
      }
      const numeric = Number(value);
      return `${field.label} ${Number.isFinite(numeric) ? numeric.toLocaleString("ko-KR") : String(value)}`;
    });
}

function nodeFor(resource: ResourceCost, catalog: ResourceCatalogItem[], x: number, y: number): NodeSpec {
  const meta = metaFor(catalog, resource.type);
  return {
    id: resource.type,
    label: meta.label,
    shortName: meta.shortName,
    details: resourceDetails(findCatalogItem(catalog, resource.type), resource.configuration ?? {}),
    cost: resource.monthlyCost,
    x,
    y,
    bg: categoryBg(meta.category),
    text: categoryText(meta.category),
    border: categoryBorder(meta.category),
  };
}

function buildLanes(resources: ResourceCost[], catalog: ResourceCatalogItem[]): Lane[] {
  const byCategory = new Map<string, ResourceCost[]>();
  for (const resource of resources) {
    const category = metaFor(catalog, resource.type).category ?? "Other";
    if (SUPPORT_CATEGORIES.includes(category)) continue;
    byCategory.set(category, [...(byCategory.get(category) ?? []), resource]);
  }

  const order = [
    ...FLOW_CATEGORY_ORDER,
    ...[...byCategory.keys()].filter((category) => !FLOW_CATEGORY_ORDER.includes(category)),
  ];

  return order
    .filter((category) => byCategory.has(category))
    .map((category, laneIndex) => ({
      category,
      nodes: (byCategory.get(category) ?? []).map((resource, rowIndex) =>
        nodeFor(
          resource,
          catalog,
          MARGIN_X + laneIndex * (NODE_WIDTH + LANE_GAP),
          MARGIN_Y + rowIndex * (NODE_HEIGHT + ROW_GAP)
        )
      ),
    }));
}

function buildSupportRow(resources: ResourceCost[], catalog: ResourceCatalogItem[], y: number): NodeSpec[] {
  const supportResources = resources.filter((resource) =>
    SUPPORT_CATEGORIES.includes(metaFor(catalog, resource.type).category ?? "")
  );
  return supportResources.map((resource, index) =>
    nodeFor(resource, catalog, MARGIN_X + index * (NODE_WIDTH + LANE_GAP), y)
  );
}

// Text hints extracted from the advisor's free-text "additional recommendations" — kept separate
// from real priced resources, and skipped when the advisor already added the matching resource
// itself (e.g. it directly included a COGNITO resource instead of just suggesting one).
function buildHintRow(recommendations: string[], resources: ResourceCost[], y: number, pricedSeparatelyLabel: string): NodeSpec[] {
  const text = recommendations.join(" ").toLowerCase();
  const candidates: Array<{
    key: string;
    label: string;
    shortName: string;
    matches: string[];
    tone: string;
    type?: ResourceType;
  }> = [
    { key: "route53", label: "Route 53", shortName: "DNS", matches: ["route 53", "dns"], tone: "bg-purple-100 text-purple-800 border-purple-200", type: "ROUTE53" },
    { key: "acm", label: "ACM", shortName: "TLS", matches: ["acm", "https", "인증서"], tone: "bg-emerald-100 text-emerald-800 border-emerald-200" },
    { key: "cognito", label: "Cognito", shortName: "AUTH", matches: ["cognito", "로그인", "인증"], tone: "bg-pink-100 text-pink-800 border-pink-200", type: "COGNITO" },
    { key: "sqs", label: "SQS/EventBridge", shortName: "QUEUE", matches: ["sqs", "eventbridge", "비동기", "재시도"], tone: "bg-yellow-100 text-yellow-800 border-yellow-200" },
    { key: "cloudwatch", label: "CloudWatch", shortName: "OBS", matches: ["cloudwatch", "로그", "알람", "지표"], tone: "bg-slate-100 text-slate-800 border-slate-200", type: "CLOUDWATCH" },
  ];

  return candidates
    .filter((candidate) => candidate.matches.some((match) => text.includes(match)))
    .filter((candidate) => !candidate.type || !has(resources, candidate.type))
    .map((candidate, index) => ({
      id: `hint-${candidate.key}`,
      label: candidate.label,
      shortName: candidate.shortName,
      details: [pricedSeparatelyLabel],
      x: MARGIN_X + index * (NODE_WIDTH + LANE_GAP),
      y,
      bg: toneBg(candidate.tone),
      text: toneText(candidate.tone),
      border: toneBorder(candidate.tone),
    }));
}

function path(from: NodeSpec, to: NodeSpec) {
  const startX = from.x + NODE_WIDTH;
  const startY = from.y + NODE_HEIGHT / 2;
  const endX = to.x;
  const endY = to.y + NODE_HEIGHT / 2;
  const mid = Math.max(48, Math.abs(endX - startX) * 0.45);
  return `M ${startX} ${startY} C ${startX + mid} ${startY}, ${endX - mid} ${endY}, ${endX} ${endY}`;
}

// Every node in a lane connects to every node in the nearest lane to its left, instead of a
// hand-picked whitelist of type pairs — this is what lets an arbitrary, AI-composed combination
// of catalog resources still render as a connected flow rather than isolated boxes.
function flowConnections(lanes: Lane[]): Array<[NodeSpec, NodeSpec]> {
  const pairs: Array<[NodeSpec, NodeSpec]> = [];
  for (let i = 1; i < lanes.length; i++) {
    for (const to of lanes[i].nodes) {
      for (const from of lanes[i - 1].nodes) {
        pairs.push([from, to]);
      }
    }
  }
  return pairs;
}

export default function RecommendedArchitectureDiagram({ resources, additionalRecommendations, resourceCatalog }: Props) {
  const t = useTranslations("calculator.recommendedDiagram");
  const lanes = buildLanes(resources, resourceCatalog);
  const maxFlowRows = Math.max(1, ...lanes.map((lane) => lane.nodes.length));
  const flowStackBottom = MARGIN_Y + maxFlowRows * (NODE_HEIGHT + ROW_GAP) - ROW_GAP;

  const supportY = flowStackBottom + SUPPORT_ROW_GAP;
  const support = buildSupportRow(resources, resourceCatalog, supportY);

  const hintY = supportY + (support.length > 0 ? NODE_HEIGHT + ROW_GAP + 24 : 0);
  const hints = buildHintRow(additionalRecommendations, resources, hintY, t("pricedSeparately"));

  const pairs = flowConnections(lanes);

  const lastRowY = hints.length > 0 ? hintY : support.length > 0 ? supportY : flowStackBottom;
  const canvasHeight = lastRowY + NODE_HEIGHT + BOTTOM_MARGIN;
  const laneCount = Math.max(lanes.length, support.length, hints.length, 1);
  const canvasWidth = MARGIN_X * 2 + laneCount * (NODE_WIDTH + LANE_GAP);

  const containerRef = useRef<HTMLDivElement>(null);
  const [scale, setScale] = useState(1);

  // Fit to whatever width the modal actually gives it instead of relying on the user to discover
  // a horizontal scrollbar — see DiagramSnapshotView for the same fix and the reasoning (this is
  // read-only, unlike VisualArchitectureBuilder's editable canvas, so shrinking to fit costs
  // nothing).
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;
    const updateScale = () => {
      const availableWidth = container.clientWidth;
      if (availableWidth > 0) {
        setScale(Math.min(1, availableWidth / canvasWidth));
      }
    };
    updateScale();
    const observer = new ResizeObserver(updateScale);
    observer.observe(container);
    return () => observer.disconnect();
  }, [canvasWidth]);

  // Deliberately light regardless of the app's dark theme — the resource tones in diagramSnapshot.ts
  // are light-only (shared with the always-light ShareCard), so a dark shell around them made
  // resource cards hard to read, same issue as the drag-builder canvas and DiagramSnapshotView.
  return (
    <div className="overflow-hidden rounded-xl border border-zinc-200 bg-white">
      <div className="flex items-center justify-between border-b border-zinc-100 px-4 py-3">
        <h4 className="text-sm font-semibold text-zinc-900">{t("title")}</h4>
        <span className="text-xs text-zinc-500">{t("readOnly")}</span>
      </div>
      <div
        ref={containerRef}
        className="relative max-h-[560px] overflow-y-auto overflow-x-hidden bg-[linear-gradient(#f4f4f5_1px,transparent_1px),linear-gradient(90deg,#f4f4f5_1px,transparent_1px)] bg-[size:32px_32px] p-4"
      >
        <div className="relative" style={{ width: canvasWidth * scale, height: canvasHeight * scale }}>
          <div
            className="relative"
            style={{ width: canvasWidth, height: canvasHeight, transform: `scale(${scale})`, transformOrigin: "top left" }}
          >
            {lanes.map((lane) => (
              <div
                key={lane.category}
                className="absolute rounded-xl border-2 border-dashed border-zinc-300 bg-zinc-50/40 p-2 text-[11px] font-semibold text-zinc-500"
                style={{
                  left: lane.nodes[0].x - LANE_PADDING,
                  top: MARGIN_Y - LANE_LABEL_INSET,
                  width: NODE_WIDTH + LANE_PADDING * 2,
                  height: LANE_LABEL_INSET + maxFlowRows * (NODE_HEIGHT + ROW_GAP) - ROW_GAP + LANE_PADDING,
                }}
              >
                {lane.category}
              </div>
            ))}
            <svg className="pointer-events-none absolute inset-0 z-10 h-full w-full overflow-visible">
              {pairs.map(([from, to]) => (
                <path key={`${from.id}-${to.id}`} d={path(from, to)} className="fill-none stroke-zinc-500" strokeWidth={2} />
              ))}
            </svg>
            {[...lanes.flatMap((lane) => lane.nodes), ...support, ...hints].map((node) => (
              <div
                key={node.id}
                style={{ left: node.x, top: node.y, width: NODE_WIDTH, height: NODE_HEIGHT }}
                className={`absolute z-20 flex items-center gap-2 rounded-lg border p-2 shadow-sm ${node.bg} ${node.border}`}
              >
                <span
                  className={`grid h-10 w-12 shrink-0 place-items-center rounded-md border bg-white text-xs font-bold ${node.border} ${node.text}`}
                >
                  {node.shortName}
                </span>
                <span className="min-w-0 flex-1">
                  <span className="block truncate text-xs font-semibold text-zinc-800">{node.label}</span>
                  {node.cost !== undefined && (
                    <span className="block text-xs font-medium text-zinc-600">{formatUsd(node.cost)}</span>
                  )}
                  {node.details.map((detail) => (
                    <span key={detail} className="block truncate text-[11px] text-zinc-500">
                      {detail}
                    </span>
                  ))}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
