"use client";

import type { ReactNode } from "react";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useTranslations, useLocale } from "next-intl";
import { CalculatorForm, createResourceInstance, REGIONS, regionLabel } from "./formTypes";
import CatalogFieldInput, { catalogFieldValue } from "./CatalogFieldInput";
import type { ResourceCatalogItem } from "@/lib/api";
import type { DiagramSnapshot, DiagramSnapshotItem } from "./diagramSnapshot";
import { categoryBg, categoryBorder, categoryText } from "./diagramSnapshot";

type GroupKind = "vpc" | "publicSubnet" | "privateSubnet" | "ecsCluster" | "availabilityZone";

interface PaletteItem {
  kind: string;
  title: string;
  shortName: string;
  description: string;
  tone: string;
}

interface DiagramNode {
  id: string;
  kind: string;
  title: string;
  shortName: string;
  description: string;
  tone: string;
  category: string;
}

interface DiagramGroup extends PaletteItem {
  id: string;
  kind: GroupKind;
}

interface Connection {
  from: string;
  to: string;
}

interface Rect {
  x: number;
  y: number;
  width: number;
  height: number;
}

interface Anchor {
  x: number;
  y: number;
  dx: number;
  dy: number;
}

interface PointerAction {
  type: "move" | "resize" | "pan";
  id: string;
  offsetX: number;
  offsetY: number;
  startWidth: number;
  startHeight: number;
}

interface Props {
  form: CalculatorForm;
  resourceCatalog: ResourceCatalogItem[];
  catalogError: string | null;
  onChange: (form: CalculatorForm) => void;
  onCalculate: (form: CalculatorForm) => void;
  onEditDetails: () => void;
  onDiagramChange: (snapshot: DiagramSnapshot | null) => void;
  initialRects?: Record<string, Rect>;
  initialGroups?: DiagramGroup[];
  initialConnections?: Connection[];
}

// Kept light — rendered inside the always-light "시스템 구성도" panel below.
function EditorField({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="flex items-center justify-between gap-3 text-xs">
      <span className="text-zinc-500">{label}</span>
      {children}
    </label>
  );
}

function ResourceEditorShell({ title, children }: { title: string; children: ReactNode }) {
  const t = useTranslations("calculator.visualBuilder");
  return (
    <div className="mt-4 rounded-lg border border-slate-200 bg-slate-50 p-4">
      <div className="mb-3 flex items-center justify-between">
        <h3 className="text-sm font-semibold text-slate-900">{t("resourceSetting")}</h3>
        <span className="text-xs text-zinc-500">{title}</span>
      </div>
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">{children}</div>
    </div>
  );
}

const nodeWidth = 178;
const nodeHeight = 104;
const groupWidth = 340;
const groupHeight = 210;
const minGroupWidth = 220;
const minGroupHeight = 140;
const minCanvasScale = 0.2;
const maxCanvasScale = 1.8;
const canvasScaleStep = 0.1;
const canvasFitPadding = 24;
const editorSelectClass =
  "w-44 rounded-md border border-zinc-300 bg-white px-2 py-1.5 text-xs text-zinc-900 outline-none focus:border-zinc-900";
const editorNumberClass = editorSelectClass + " w-28";
const paletteShortNameClass =
  "grid h-10 w-12 shrink-0 place-items-center overflow-hidden rounded-md border px-1 text-center text-[10px] font-bold leading-none";
const nodeShortNameClass =
  "grid h-11 w-12 shrink-0 place-items-center overflow-hidden rounded-md border px-1 text-center text-[10px] font-bold leading-none";

// Diagram-drawing groups (VPC/Subnet/compute boxes) are a canvas convenience, not an AWS
// resource the backend prices — they stay a small fixed palette, unlike the resource palette
// below which is fully catalog-driven. A function (not a module-level constant) since the
// descriptions are translated and useTranslations only works inside a component.
function buildGroupOptions(t: ReturnType<typeof useTranslations<"calculator.visualBuilder">>): PaletteItem[] {
  return [
    // Diagram canvas stays light regardless of the app's dark theme (see the canvas container
    // below) — placed resource cards use the shared categoryBg/Text/Border tones from
    // diagramSnapshot.ts, which are light-only, so a dark canvas made them unreadable.
    { kind: "vpc", title: "VPC", shortName: "VPC", description: t("groupVpc"), tone: "border-emerald-400 bg-emerald-50/70 text-emerald-900" },
    { kind: "availabilityZone", title: "Availability Zone", shortName: "AZ", description: t("groupAz"), tone: "border-sky-400 bg-sky-50/70 text-sky-900" },
    { kind: "publicSubnet", title: "Public Subnet", shortName: "PUB", description: t("groupPublicSubnet"), tone: "border-cyan-400 bg-cyan-50/70 text-cyan-900" },
    { kind: "privateSubnet", title: "Private Subnet", shortName: "PRI", description: t("groupPrivateSubnet"), tone: "border-indigo-400 bg-indigo-50/70 text-indigo-900" },
    { kind: "ecsCluster", title: "Compute Group", shortName: "CMP", description: t("groupCompute"), tone: "border-orange-400 bg-orange-50/70 text-orange-900" },
  ];
}

const resourcePalettePriority = ["ecs", "rds", "redis", "s3", "alb"];

function resourcePaletteOrder(item: ResourceCatalogItem) {
  const priority = resourcePalettePriority.indexOf(item.kind);
  return priority === -1 ? resourcePalettePriority.length : priority;
}

function defaultNodeRect(index: number): Rect {
  return { x: 70 + (index % 4) * 220, y: 100 + Math.floor(index / 4) * 140, width: nodeWidth, height: nodeHeight };
}

function defaultGroupRect(index: number): Rect {
  return { x: 36 + (index % 2) * 380, y: 70 + Math.floor(index / 2) * 240, width: groupWidth, height: groupHeight };
}

function anchorsFor(from: Rect, to: Rect): { start: Anchor; end: Anchor } {
  const fromCenter = { x: from.x + from.width / 2, y: from.y + from.height / 2 };
  const toCenter = { x: to.x + to.width / 2, y: to.y + to.height / 2 };
  const deltaX = toCenter.x - fromCenter.x;
  const deltaY = toCenter.y - fromCenter.y;

  if (Math.abs(deltaX) >= Math.abs(deltaY)) {
    if (deltaX >= 0) {
      return {
        start: { x: from.x + from.width, y: fromCenter.y, dx: 1, dy: 0 },
        end: { x: to.x, y: toCenter.y, dx: -1, dy: 0 },
      };
    }
    return {
      start: { x: from.x, y: fromCenter.y, dx: -1, dy: 0 },
      end: { x: to.x + to.width, y: toCenter.y, dx: 1, dy: 0 },
    };
  }

  if (deltaY >= 0) {
    return {
      start: { x: fromCenter.x, y: from.y + from.height, dx: 0, dy: 1 },
      end: { x: toCenter.x, y: to.y, dx: 0, dy: -1 },
    };
  }
  return {
    start: { x: fromCenter.x, y: from.y, dx: 0, dy: -1 },
    end: { x: toCenter.x, y: to.y + to.height, dx: 0, dy: 1 },
  };
}

function connectorPath(from: Rect, to: Rect) {
  const { start, end } = anchorsFor(from, to);
  const distance = Math.hypot(end.x - start.x, end.y - start.y);
  const controlDistance = Math.max(48, distance * 0.35);
  const startControl = {
    x: start.x + start.dx * controlDistance,
    y: start.y + start.dy * controlDistance,
  };
  const endControl = {
    x: end.x + end.dx * controlDistance,
    y: end.y + end.dy * controlDistance,
  };
  return `M ${start.x} ${start.y} C ${startControl.x} ${startControl.y}, ${endControl.x} ${endControl.y}, ${end.x} ${end.y}`;
}

function clampCanvasScale(scale: number) {
  return Math.max(minCanvasScale, Math.min(maxCanvasScale, Number(scale.toFixed(2))));
}

export default function VisualArchitectureBuilder({
  form,
  resourceCatalog,
  catalogError,
  onChange,
  onCalculate,
  onEditDetails,
  onDiagramChange,
  initialRects,
  initialGroups,
  initialConnections,
}: Props) {
  const t = useTranslations("calculator.visualBuilder");
  const tFields = useTranslations("calculator.projectFields");
  const locale = useLocale();
  const groupOptions = useMemo(() => buildGroupOptions(t), [t]);
  const groupByKind = useMemo(
    () => Object.fromEntries(groupOptions.map((option) => [option.kind, option])) as Record<GroupKind, PaletteItem>,
    [groupOptions]
  );
  const canvasRef = useRef<HTMLDivElement>(null);
  const groupIdRef = useRef(0);
  // Lazy initializers so these only ever read the caller's initial layout once, on mount — e.g.
  // when a recommendation is applied and this component mounts fresh with `form.resources`,
  // `initialRects`, `initialGroups`, and `initialConnections` all set together (see
  // Calculator.tsx's applyRecommendation). Without it, every node would fall back to
  // defaultNodeRect's generic index-ordered grid with no groups/connections at all, instead of
  // the categorized flow layout (with its lane boxes and cross-lane lines) the recommendation
  // preview showed.
  const [groups, setGroups] = useState<DiagramGroup[]>(() => initialGroups ?? []);
  const [rects, setRects] = useState<Record<string, Rect>>(() => initialRects ?? {});
  const [connections, setConnections] = useState<Connection[]>(() => initialConnections ?? []);
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);
  const [selectedConnectionIndex, setSelectedConnectionIndex] = useState<number | null>(null);
  const [editingNodeId, setEditingNodeId] = useState<string | null>(null);
  const [resourceSearch, setResourceSearch] = useState("");
  const [pointerAction, setPointerAction] = useState<PointerAction | null>(null);
  const [canvasOffset, setCanvasOffset] = useState({ x: 0, y: 0 });
  const [canvasScale, setCanvasScale] = useState(1);

  // The resource palette, its editor fields, and its default values all come from the backend
  // catalog now — no per-type fallback list. If the catalog hasn't loaded yet (or failed), the
  // palette is simply empty; `catalogError` below explains why.
  const resourceByKind = useMemo(
    () => Object.fromEntries(resourceCatalog.map((item) => [item.kind, item])) as Record<string, ResourceCatalogItem>,
    [resourceCatalog]
  );
  const resourcePalette = useMemo(
    () =>
      [...resourceCatalog].sort((a, b) => {
        const priorityDiff = resourcePaletteOrder(a) - resourcePaletteOrder(b);
        return priorityDiff !== 0 ? priorityDiff : a.title.localeCompare(b.title);
      }),
    [resourceCatalog]
  );
  const filteredResourcePalette = useMemo(() => {
    const query = resourceSearch.trim().toLowerCase();
    if (!query) {
      return resourcePalette;
    }
    return resourcePalette.filter((item) =>
      [item.title, item.shortName, item.type, item.kind, item.description, item.category]
        .some((value) => value.toLowerCase().includes(query))
    );
  }, [resourcePalette, resourceSearch]);
  const totalResources = form.resources.length;
  const canCalculate = form.projectName.trim().length > 0 && totalResources > 0;
  const monthlyRequests = form.monthlyUsers * form.requestsPerUser;

  const nodes = useMemo<DiagramNode[]>(() => {
    const countByKind: Record<string, number> = {};
    return form.resources.flatMap((item) => {
      const catalogItem = resourceByKind[item.kind];
      if (!catalogItem) {
        return [];
      }
      const index = (countByKind[item.kind] = (countByKind[item.kind] ?? 0) + 1);
      const node: DiagramNode = {
        id: item._key,
        kind: item.kind,
        title: `${catalogItem.title} #${index}`,
        shortName: catalogItem.shortName,
        description: catalogItem.description,
        tone: catalogItem.tone,
        category: catalogItem.category,
      };
      return [node];
    });
  }, [form.resources, resourceByKind]);
  const editingNode = nodes.find((node) => node.id === editingNodeId) ?? null;

  function rectFor(id: string): Rect | null {
    if (rects[id]) {
      return rects[id];
    }
    const nodeIndex = nodes.findIndex((node) => node.id === id);
    if (nodeIndex !== -1) {
      return defaultNodeRect(nodeIndex);
    }
    const groupIndex = groups.findIndex((group) => group.id === id);
    if (groupIndex !== -1) {
      return defaultGroupRect(groupIndex);
    }
    return null;
  }

  const resourceDetails = useCallback(
    (node: DiagramNode) => {
      const item = form.resources.find((resource) => resource._key === node.id);
      const catalogItem = resourceByKind[node.kind];
      if (!item || !catalogItem) {
        return [];
      }
      const preferred = catalogItem.fields.filter((field) => field.costRelevant);
      const chosen = (preferred.length > 0 ? preferred : catalogItem.fields).slice(0, 3);
      return chosen.map((field) => {
        const raw = catalogFieldValue(field, item.config);
        const value = typeof raw === "number" ? raw.toLocaleString("ko-KR") : String(raw);
        return `${field.label}: ${value}`;
      });
    },
    [form.resources, resourceByKind]
  );

  const diagramSnapshot = useMemo<DiagramSnapshot | null>(() => {
    const groupItems: DiagramSnapshotItem[] = groups.map((group, index) => {
      const rect = rects[group.id] ?? defaultGroupRect(index);
      return {
        id: group.id,
        title: group.title,
        shortName: group.shortName,
        description: group.description,
        tone: group.tone,
        details: [],
        x: rect.x,
        y: rect.y,
        width: rect.width,
        height: rect.height,
        itemType: "group",
      };
    });
    const nodeItems: DiagramSnapshotItem[] = nodes.map((node, index) => {
      const rect = rects[node.id] ?? defaultNodeRect(index);
      return {
        id: node.id,
        title: node.title,
        shortName: node.shortName,
        description: node.description,
        tone: node.tone,
        category: node.category,
        details: resourceDetails(node),
        x: rect.x,
        y: rect.y,
        width: rect.width,
        height: rect.height,
        itemType: "resource",
      };
    });
    const items = [...groupItems, ...nodeItems];
    if (items.length === 0) {
      return null;
    }

    const padding = 32;
    const minX = Math.min(...items.map((item) => item.x));
    const minY = Math.min(...items.map((item) => item.y));
    const maxX = Math.max(...items.map((item) => item.x + item.width));
    const maxY = Math.max(...items.map((item) => item.y + item.height));
    const offsetX = Math.min(0, minX) - padding;
    const offsetY = Math.min(0, minY) - padding;
    const normalizedItems = items.map((item) => ({
      ...item,
      x: item.x - offsetX,
      y: item.y - offsetY,
    }));

    return {
      items: normalizedItems,
      connections,
      width: Math.max(760, maxX - offsetX + padding),
      height: Math.max(420, maxY - offsetY + padding),
    };
  }, [connections, groups, nodes, rects, resourceDetails]);

  useEffect(() => {
    onDiagramChange(diagramSnapshot);
  }, [diagramSnapshot, onDiagramChange]);

  // Runs once, only when this component mounts with a seeded layout (an applied AI
  // recommendation) — that's the case most likely to already span more lanes/rows than the
  // canvas viewport at 100% zoom. Deliberately not re-run on every diagramSnapshot change: that
  // would re-center/re-zoom out from under the user while they're mid-edit adding resources.
  useEffect(() => {
    if (!initialRects || Object.keys(initialRects).length === 0) {
      return;
    }
    const raf = requestAnimationFrame(fitToContent);
    return () => cancelAnimationFrame(raf);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function canvasPoint(clientX: number, clientY: number) {
    const rect = canvasRef.current?.getBoundingClientRect();
    if (!rect) {
      return null;
    }
    return {
      x: (clientX - rect.left - canvasOffset.x) / canvasScale,
      y: (clientY - rect.top - canvasOffset.y) / canvasScale,
    };
  }

  function boundedRect(event: React.DragEvent<HTMLDivElement>, width: number, height: number): Rect {
    const point = canvasPoint(event.clientX, event.clientY);
    if (!point) {
      return { ...defaultNodeRect(totalResources), width, height };
    }
    return {
      x: point.x - width / 2,
      y: point.y - height / 2,
      width,
      height,
    };
  }

  function addResource(catalogItem: ResourceCatalogItem, rect?: Rect) {
    const item = createResourceInstance(catalogItem);
    onChange({ ...form, resources: [...form.resources, item] });
    setRects((current) => ({ ...current, [item._key]: rect ?? defaultNodeRect(totalResources) }));
  }

  function addGroup(kind: GroupKind, rect?: Rect) {
    const option = groupByKind[kind];
    const id = `${kind}-${groupIdRef.current++}`;
    setGroups((current) => [...current, { ...option, id, kind }]);
    setRects((current) => ({ ...current, [id]: rect ?? defaultGroupRect(groups.length) }));
  }

  function removeResource(id: string) {
    onChange({ ...form, resources: form.resources.filter((item) => item._key !== id) });
    removeDiagramItem(id);
  }

  function removeGroup(id: string) {
    setGroups((current) => current.filter((group) => group.id !== id));
    removeDiagramItem(id);
  }

  function removeDiagramItem(id: string) {
    setConnections((current) => current.filter((connection) => connection.from !== id && connection.to !== id));
    setSelectedConnectionIndex(null);
    setRects((current) => {
      const next = { ...current };
      delete next[id];
      return next;
    });
    setSelectedNodeId((current) => (current === id ? null : current));
    setEditingNodeId((current) => (current === id ? null : current));
  }

  function handleDrop(event: React.DragEvent<HTMLDivElement>) {
    event.preventDefault();
    const itemType = event.dataTransfer.getData("application/aws-diagram-type");
    const kind = event.dataTransfer.getData("application/aws-diagram-kind");
    if (itemType === "group" && kind in groupByKind) {
      addGroup(kind as GroupKind, boundedRect(event, groupWidth, groupHeight));
    }
    const catalogItem = resourceByKind[kind];
    if (itemType === "resource" && catalogItem) {
      addResource(catalogItem, boundedRect(event, nodeWidth, nodeHeight));
    }
  }

  function beginMove(event: React.PointerEvent<HTMLDivElement>, id: string) {
    if (event.button !== 0) {
      return;
    }
    const currentRect = rectFor(id);
    const point = canvasPoint(event.clientX, event.clientY);
    if (!point || !currentRect) {
      return;
    }
    event.preventDefault();
    event.stopPropagation();
    setSelectedConnectionIndex(null);
    if (nodes.some((node) => node.id === id)) {
      setEditingNodeId(id);
    }
    event.currentTarget.setPointerCapture(event.pointerId);
    setRects((current) => ({ ...current, [id]: currentRect }));
    setPointerAction({
      type: "move",
      id,
      offsetX: point.x - currentRect.x,
      offsetY: point.y - currentRect.y,
      startWidth: currentRect.width,
      startHeight: currentRect.height,
    });
  }

  function beginPan(event: React.PointerEvent<HTMLDivElement>) {
    if (event.button !== 0 || event.target !== event.currentTarget) {
      return;
    }
    event.preventDefault();
    setSelectedNodeId(null);
    setSelectedConnectionIndex(null);
    event.currentTarget.setPointerCapture(event.pointerId);
    setPointerAction({
      type: "pan",
      id: "canvas",
      offsetX: event.clientX - canvasOffset.x,
      offsetY: event.clientY - canvasOffset.y,
      startWidth: 0,
      startHeight: 0,
    });
  }

  function beginResize(event: React.PointerEvent<HTMLDivElement>, id: string) {
    if (event.button !== 0) {
      return;
    }
    const currentRect = rectFor(id);
    const point = canvasPoint(event.clientX, event.clientY);
    if (!currentRect || !point) {
      return;
    }
    event.preventDefault();
    event.stopPropagation();
    event.currentTarget.setPointerCapture(event.pointerId);
    setPointerAction({
      type: "resize",
      id,
      offsetX: point.x,
      offsetY: point.y,
      startWidth: currentRect.width,
      startHeight: currentRect.height,
    });
  }

  function handlePointerMove(event: React.PointerEvent<HTMLDivElement>) {
    if (!pointerAction) {
      return;
    }
    if (pointerAction.type === "pan") {
      setCanvasOffset({
        x: event.clientX - pointerAction.offsetX,
        y: event.clientY - pointerAction.offsetY,
      });
      return;
    }

    const currentRect = rectFor(pointerAction.id);
    const point = canvasPoint(event.clientX, event.clientY);
    if (!point || !currentRect) {
      return;
    }

    if (pointerAction.type === "move") {
      setRects((current) => ({
        ...current,
        [pointerAction.id]: {
          ...currentRect,
          x: point.x - pointerAction.offsetX,
          y: point.y - pointerAction.offsetY,
        },
      }));
      return;
    }

    const nextWidth = Math.max(minGroupWidth, pointerAction.startWidth + point.x - pointerAction.offsetX);
    const nextHeight = Math.max(minGroupHeight, pointerAction.startHeight + point.y - pointerAction.offsetY);
    setRects((current) => ({
      ...current,
      [pointerAction.id]: { ...currentRect, width: nextWidth, height: nextHeight },
    }));
  }

  function endPointerAction() {
    setPointerAction(null);
  }

  function connectNode(id: string) {
    setSelectedConnectionIndex(null);
    if (!selectedNodeId) {
      setSelectedNodeId(id);
      return;
    }
    if (selectedNodeId === id) {
      setSelectedNodeId(null);
      return;
    }
    const exists = connections.some(
      (connection) =>
        (connection.from === selectedNodeId && connection.to === id) ||
        (connection.from === id && connection.to === selectedNodeId)
    );
    if (!exists) {
      setConnections((current) => [...current, { from: selectedNodeId, to: id }]);
    }
    setSelectedNodeId(null);
  }

  function startDrag(event: React.DragEvent<HTMLButtonElement>, type: "group" | "resource", kind: string) {
    event.dataTransfer.setData("application/aws-diagram-type", type);
    event.dataTransfer.setData("application/aws-diagram-kind", kind);
  }

  function removeSelectedConnection() {
    if (selectedConnectionIndex === null) {
      return;
    }
    setConnections((current) => current.filter((_, index) => index !== selectedConnectionIndex));
    setSelectedConnectionIndex(null);
  }

  function selectConnection(event: React.MouseEvent<SVGPathElement>, index: number) {
    event.stopPropagation();
    setSelectedNodeId(null);
    setSelectedConnectionIndex(index);
  }

  function applyZoom(nextScale: number, clientX?: number, clientY?: number) {
    const scale = clampCanvasScale(nextScale);
    if (scale === canvasScale) {
      return;
    }

    const rect = canvasRef.current?.getBoundingClientRect();
    if (rect && clientX !== undefined && clientY !== undefined) {
      const point = canvasPoint(clientX, clientY);
      if (point) {
        setCanvasOffset({
          x: clientX - rect.left - point.x * scale,
          y: clientY - rect.top - point.y * scale,
        });
      }
    }
    setCanvasScale(scale);
  }

  function handleWheel(event: React.WheelEvent<HTMLDivElement>) {
    if (!event.ctrlKey && !event.metaKey) {
      return;
    }
    event.preventDefault();
    applyZoom(canvasScale + (event.deltaY > 0 ? -canvasScaleStep : canvasScaleStep), event.clientX, event.clientY);
  }

  function resetCanvasView() {
    setCanvasOffset({ x: 0, y: 0 });
    setCanvasScale(1);
  }

  // The canvas is overflow-hidden with a custom pan/zoom (not native scroll), so a layout wider
  // or taller than the visible viewport at 100% just gets clipped with no way to see the rest —
  // unlike RecommendedArchitectureDiagram/DiagramSnapshotView, which are read-only and scrollable.
  // This computes the scale/offset needed to show the whole diagram at once instead, same idea as
  // "zoom to fit" in a drawing tool.
  function fitToContent() {
    const container = canvasRef.current;
    if (!container || !diagramSnapshot) {
      resetCanvasView();
      return;
    }

    const viewportWidth = container.clientWidth;
    const viewportHeight = container.clientHeight;
    const scale = Math.min(
      1,
      (viewportWidth - canvasFitPadding * 2) / diagramSnapshot.width,
      (viewportHeight - canvasFitPadding * 2) / diagramSnapshot.height
    );
    const clampedScale = Math.max(minCanvasScale, Number(scale.toFixed(2)));
    setCanvasScale(clampedScale);
    setCanvasOffset({
      x: (viewportWidth - diagramSnapshot.width * clampedScale) / 2,
      y: (viewportHeight - diagramSnapshot.height * clampedScale) / 2,
    });
  }

  function renderResourceEditor() {
    if (!editingNode) {
      return (
        <div className="mt-4 rounded-lg border p-4 text-sm border-slate-200 bg-slate-50 text-slate-500">
          {t("selectToEdit")}
        </div>
      );
    }

    const item = form.resources.find((resource) => resource._key === editingNode.id);
    const catalogItem = resourceByKind[editingNode.kind];
    if (!item || !catalogItem) {
      return null;
    }

    const update = (config: Record<string, unknown>) =>
      onChange({
        ...form,
        resources: form.resources.map((resource) => (resource._key === item._key ? { ...resource, config } : resource)),
      });

    return (
      <ResourceEditorShell title={editingNode.title}>
        {catalogItem.fields.length === 0 ? (
          <p className="text-sm text-zinc-500 sm:col-span-2 lg:col-span-3">{catalogItem.description}</p>
        ) : (
          catalogItem.fields.map((field) => (
            <EditorField key={field.key} label={field.label}>
              <CatalogFieldInput
                field={field}
                config={item.config}
                onChange={update}
                className={field.type === "select" ? editorSelectClass : editorNumberClass}
              />
            </EditorField>
          ))
        )}
      </ResourceEditorShell>
    );
  }

  return (
    <div className="grid min-w-0 grid-cols-[minmax(0,1fr)] items-start gap-6 lg:grid-cols-[280px_minmax(0,1fr)] [&>div]:min-w-0">
      <aside className="flex max-h-[860px] min-h-0 flex-col gap-4 overflow-hidden rounded-lg border p-4 shadow-sm border-slate-800 bg-white lg:sticky lg:top-6 lg:h-[calc(100vh-3rem)] lg:max-h-none">
        <div className="shrink-0">
          <div className="-mx-4 -mt-4 mb-4 bg-stone-100 px-4 py-3">
            <div className="text-xs font-semibold uppercase text-[#ff9900]">{t("palette")}</div>
            <h2 className="mt-1 text-sm font-semibold text-white">{t("components")}</h2>
            {resourceCatalog.length === 0 && !catalogError && (
              <p className="mt-1 text-xs text-stone-700">{t("loadingCatalog")}</p>
            )}
          </div>
          {catalogError && (
            <div className="mb-3 rounded border px-3 py-2 text-xs border-amber-900 bg-amber-950 text-amber-200">
              {catalogError}
            </div>
          )}
          <h2 className="text-sm font-semibold text-slate-50">{t("areas")}</h2>
          <div className="mt-3 grid gap-2">
            {groupOptions.map((option) => (
              <button
                key={option.kind}
                type="button"
                draggable
                onClick={() => addGroup(option.kind as GroupKind)}
                onDragStart={(event) => startDrag(event, "group", option.kind)}
                className={`flex items-center gap-3 rounded-lg border p-2 text-left transition hover:brightness-95 ${option.tone}`}
              >
                <span className="grid h-9 w-12 shrink-0 place-items-center rounded-md border border-current text-xs font-bold">
                  {option.shortName}
                </span>
                <span>
                  <span className="block text-sm font-medium">{option.title}</span>
                  <span className="block text-xs opacity-75">{option.description}</span>
                </span>
              </button>
            ))}
          </div>
        </div>

        <div className="flex min-h-0 flex-1 flex-col">
          <div className="flex shrink-0 items-center justify-between gap-2">
            <h2 className="text-sm font-semibold text-slate-50">{t("resources")}</h2>
            <span className="text-xs text-stone-600">{filteredResourcePalette.length}/{resourceCatalog.length}</span>
          </div>
          <input
            value={resourceSearch}
            onChange={(event) => setResourceSearch(event.target.value)}
            placeholder={t("searchResources")}
            className="mt-3 w-full shrink-0 rounded-md border px-3 py-2 text-sm outline-none transition border-slate-800 bg-stone-100 text-zinc-50 placeholder:text-zinc-500 focus:border-[#ff9900]"
          />
          {/* Mobile keeps a fixed max-height (no reliable viewport-relative flex context without
              `lg:sticky`'s h-[calc]); desktop instead fills the remaining column space via flex-1,
              which — unlike the old h-[calc(100%-1.75rem)] — doesn't need to guess how tall the
              header+search row above it actually is. */}
          <div className="mt-3 grid max-h-[520px] min-h-0 gap-2 overflow-y-auto pr-1 lg:max-h-none lg:flex-1">
            {filteredResourcePalette.map((option) => (
              <button
                key={option.kind}
                type="button"
                draggable
                onClick={() => addResource(option)}
                onDragStart={(event) => startDrag(event, "resource", option.kind)}
                className="flex items-center gap-3 rounded-md border p-2 text-left transition border-slate-800 bg-stone-100 hover:border-[#ff9900]"
              >
                <span
                  className={`${paletteShortNameClass} ${categoryBg(option.category)} ${categoryText(option.category)} ${categoryBorder(option.category)}`}
                >
                  {option.shortName}
                </span>
                <span className="min-w-0 flex-1">
                  <span className="block truncate text-sm font-medium text-zinc-50">{option.title}</span>
                  <span className="block truncate text-xs text-stone-600">{option.description}</span>
                </span>
              </button>
            ))}
            {filteredResourcePalette.length === 0 && (
              <div className="rounded-md border border-dashed px-3 py-6 text-center text-sm border-slate-800 text-stone-600">
                {t("noResults")}
              </div>
            )}
          </div>
        </div>
      </aside>

      <section className="flex flex-col gap-5">
        <div className="rounded-lg border p-5 shadow-sm border-slate-800 bg-white">
          <div className="grid gap-4 md:grid-cols-2">
            <label className="flex flex-col gap-2">
              <span className="text-sm font-medium text-stone-700">{tFields("serviceName")}</span>
              <input
                value={form.projectName}
                onChange={(event) => onChange({ ...form, projectName: event.target.value })}
                placeholder={tFields("namePlaceholder")}
                className="rounded-lg border px-3 py-2 text-sm outline-none focus:border-zinc-900 border-zinc-700 bg-white text-zinc-50"
              />
            </label>
            <label className="flex flex-col gap-2">
              <span className="text-sm font-medium text-stone-700">{tFields("region")}</span>
              <select
                value={form.region}
                onChange={(event) => onChange({ ...form, region: event.target.value })}
                className="rounded-lg border px-3 py-2 text-sm outline-none focus:border-zinc-900 border-zinc-700 bg-white text-zinc-50"
              >
                {REGIONS.map((region) => (
                  <option key={region.code} value={region.code}>
                    {regionLabel(region.code, locale)}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="mt-4 grid gap-4 md:grid-cols-4">
            <label className="flex flex-col gap-2">
              <span className="text-sm font-medium text-stone-700">{tFields("monthlyUsers")}</span>
              <input
                type="number"
                min={0}
                value={form.monthlyUsers}
                onChange={(event) => onChange({ ...form, monthlyUsers: Number(event.target.value) })}
                className="rounded-lg border px-3 py-2 text-sm outline-none focus:border-zinc-900 border-zinc-700 bg-white text-zinc-50"
              />
            </label>
            <label className="flex flex-col gap-2">
              <span className="text-sm font-medium text-stone-700">{tFields("requestsPerUser")}</span>
              <input
                type="number"
                min={0}
                value={form.requestsPerUser}
                onChange={(event) => onChange({ ...form, requestsPerUser: Number(event.target.value) })}
                className="rounded-lg border px-3 py-2 text-sm outline-none focus:border-zinc-900 border-zinc-700 bg-white text-zinc-50"
              />
            </label>
            <label className="flex flex-col gap-2">
              <span className="text-sm font-medium text-stone-700">{tFields("busyTrafficShort")}</span>
              <select
                value={form.busyTrafficLevel}
                onChange={(event) => onChange({ ...form, busyTrafficLevel: event.target.value })}
                className="rounded-lg border px-3 py-2 text-sm outline-none focus:border-zinc-900 border-zinc-700 bg-white text-zinc-50"
              >
                <option value="similar">{tFields("trafficSimilar")}</option>
                <option value="two_to_three_times">{tFields("trafficTwoToThree")}</option>
                <option value="five_plus_times">{tFields("trafficFivePlus")}</option>
                <option value="unknown">{tFields("trafficUnknown")}</option>
              </select>
            </label>
            <label className="flex flex-col gap-2">
              <span className="text-sm font-medium text-stone-700">{tFields("serviceStageShort")}</span>
              <select
                value={form.serviceStage}
                onChange={(event) => onChange({ ...form, serviceStage: event.target.value })}
                className="rounded-lg border px-3 py-2 text-sm outline-none focus:border-zinc-900 border-zinc-700 bg-white text-zinc-50"
              >
                <option value="toy">{tFields("stageToy")}</option>
                <option value="mvp">{tFields("stageMvp")}</option>
                <option value="production">{tFields("stageProduction")}</option>
                <option value="critical">{tFields("stageCritical")}</option>
              </select>
            </label>
          </div>
        </div>

        {/* Deliberately light regardless of the app's dark theme — the diagram canvas and its
            resource cards use light-only tones (see diagramSnapshot.ts), so keeping the whole
            "시스템 구성도" panel light avoids a dark toolbar clashing with a light canvas. */}
        <div className="rounded-lg border border-slate-200 bg-white p-4 text-slate-900 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <div className="text-xs font-semibold uppercase text-[#ff9900]">{t("visualBuilderLabel")}</div>
              <h2 className="mt-1 text-lg font-semibold text-slate-900">{t("diagramTitle")}</h2>
              <p className="mt-1 text-sm text-zinc-500">
                {t("requestBasis", { count: monthlyRequests.toLocaleString() })}
              </p>
            </div>
            <div className="flex items-center gap-2 text-sm text-zinc-500">
              <span>{t("areasCount", { count: groups.length })}</span>
              <span>{t("resourcesCount", { count: totalResources })}</span>
              <span>{t("connectionsCount", { count: connections.length })}</span>
              <button
                type="button"
                onClick={() => applyZoom(canvasScale - canvasScaleStep)}
                className="grid h-7 w-7 place-items-center rounded border border-slate-200 text-xs hover:border-[#ff9900] hover:bg-slate-100"
              >
                -
              </button>
              <span className="w-12 text-center text-xs tabular-nums">{Math.round(canvasScale * 100)}%</span>
              <button
                type="button"
                onClick={() => applyZoom(canvasScale + canvasScaleStep)}
                className="grid h-7 w-7 place-items-center rounded border border-slate-200 text-xs hover:border-[#ff9900] hover:bg-slate-100"
              >
                +
              </button>
              <button
                type="button"
                onClick={fitToContent}
                className="rounded border border-slate-200 px-2 py-1 text-xs hover:border-[#ff9900] hover:bg-slate-100"
              >
                {t("resetView")}
              </button>
              <button
                type="button"
                onClick={removeSelectedConnection}
                disabled={selectedConnectionIndex === null}
                className="rounded border border-slate-200 px-2 py-1 text-xs hover:border-[#ff9900] hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-40"
              >
                {t("removeLine")}
              </button>
            </div>
          </div>

          <div
            ref={canvasRef}
            onDragOver={(event) => event.preventDefault()}
            onDrop={handleDrop}
            onPointerMove={handlePointerMove}
            onPointerUp={endPointerAction}
            onPointerLeave={endPointerAction}
            onPointerDown={beginPan}
            onWheel={handleWheel}
            className="relative mt-4 h-[640px] overflow-hidden rounded-lg border border-dashed border-slate-300 bg-[linear-gradient(#e5e7eb_1px,transparent_1px),linear-gradient(90deg,#e5e7eb_1px,transparent_1px)] bg-[size:32px_32px]"
          >
            {groups.length === 0 && nodes.length === 0 && (
              <div className="absolute inset-0 grid place-items-center text-center text-sm text-zinc-500">
                {t("emptyCanvas")}
              </div>
            )}

            <div
              className="absolute inset-0"
              onPointerDown={beginPan}
              style={{
                transform: `translate(${canvasOffset.x}px, ${canvasOffset.y}px) scale(${canvasScale})`,
                transformOrigin: "0 0",
              }}
            >
            {groups.map((group, index) => {
              const rect = rects[group.id] ?? defaultGroupRect(index);
              return (
                <div
                  key={group.id}
                  onDragOver={(event) => event.preventDefault()}
                  onDrop={handleDrop}
                  onPointerDown={(event) => beginMove(event, group.id)}
                  onPointerMove={handlePointerMove}
                  onPointerUp={endPointerAction}
                  style={{ left: rect.x, top: rect.y, width: rect.width, height: rect.height }}
                  className={`absolute z-0 cursor-move touch-none rounded-xl border-2 border-dashed p-3 ${group.tone}`}
                >
                  <div className="flex items-center justify-between">
                    <div>
                      <div className="text-sm font-semibold">{group.title}</div>
                      <div className="text-xs opacity-75">{group.description}</div>
                    </div>
                    <button
                      type="button"
                      onClick={() => removeGroup(group.id)}
                      onPointerDown={(event) => event.stopPropagation()}
                      draggable={false}
                      className="grid h-6 w-6 place-items-center rounded-full border border-current bg-white/60 text-xs hover:bg-white"
                    >
                      x
                    </button>
                  </div>
                  <div
                    onPointerDown={(event) => beginResize(event, group.id)}
                    className="absolute bottom-1 right-1 h-5 w-5 cursor-se-resize rounded-sm border-b-2 border-r-2 border-current opacity-70"
                  />
                </div>
              );
            })}

            <svg className="pointer-events-none absolute inset-0 z-10 h-full w-full overflow-visible">
              {connections.map((connection, index) => {
                const fromIndex = nodes.findIndex((node) => node.id === connection.from);
                const toIndex = nodes.findIndex((node) => node.id === connection.to);
                const from = rects[connection.from] ?? defaultNodeRect(fromIndex);
                const to = rects[connection.to] ?? defaultNodeRect(toIndex);
                const selected = selectedConnectionIndex === index;
                const path = connectorPath(from, to);
                return (
                  <g key={`${connection.from}-${connection.to}-${index}`}>
                    <path
                      d={path}
                      onClick={(event) => selectConnection(event, index)}
                      className="pointer-events-auto fill-none stroke-transparent"
                      strokeWidth={16}
                      style={{ cursor: "pointer" }}
                    />
                    <path
                      d={path}
                      className={`fill-none ${selected ? "stroke-red-500" : "stroke-zinc-500"}`}
                      strokeWidth={selected ? 3 : 2}
                    />
                  </g>
                );
              })}
            </svg>

            {nodes.map((node, index) => {
              const rect = rects[node.id] ?? defaultNodeRect(index);
              const selected = selectedNodeId === node.id;
              const details = resourceDetails(node);
              return (
                <div
                  key={node.id}
                  onPointerDown={(event) => beginMove(event, node.id)}
                  onPointerMove={handlePointerMove}
                  onPointerUp={endPointerAction}
                  style={{ left: rect.x, top: rect.y, width: nodeWidth, height: nodeHeight }}
                  className={`absolute z-20 flex cursor-move touch-none items-center gap-2 rounded-lg border p-2 shadow-sm transition ${categoryBg(node.category)} ${
                    selected ? "border-zinc-900 ring-2 ring-zinc-900/15" : categoryBorder(node.category)
                  }`}
                >
                  <button
                    type="button"
                    onPointerDown={(event) => event.stopPropagation()}
                    onClick={() => connectNode(node.id)}
                    className={`${nodeShortNameClass} bg-white ${categoryBorder(node.category)} ${categoryText(node.category)}`}
                  >
                    {node.shortName}
                  </button>
                  <div className="min-w-0 flex-1">
                    <div className="truncate text-sm font-medium text-zinc-900">{node.title}</div>
                    <div className="truncate text-xs text-zinc-500">{node.description}</div>
                    {details.map((detail) => (
                      <div key={detail} className="truncate text-[11px] text-zinc-500">
                        {detail}
                      </div>
                    ))}
                  </div>
                  <button
                    type="button"
                    onClick={() => removeResource(node.id)}
                    onPointerDown={(event) => event.stopPropagation()}
                    className="absolute -right-2 -top-2 grid h-6 w-6 place-items-center rounded-full border border-zinc-200 bg-white text-xs text-zinc-500 shadow-sm hover:text-red-600"
                  >
                    x
                  </button>
                </div>
              );
            })}
            </div>
          </div>

          {renderResourceEditor()}

          <div className="mt-3 text-xs text-zinc-500">
            {t("canvasHint")}
          </div>
        </div>

        <div className="flex flex-wrap items-center justify-end gap-3">
          <button
            type="button"
            onClick={onEditDetails}
            className="rounded border px-5 py-3 text-sm font-semibold hover:border-[#ff9900] border-slate-700 text-stone-700 hover:bg-stone-100"
          >
            {t("editDetails")}
          </button>
          <button
            type="button"
            onClick={() => onCalculate(form)}
            disabled={!canCalculate}
            className="rounded bg-[#a32b2b] px-5 py-3 text-sm font-semibold text-white hover:bg-[#f2a100] disabled:cursor-not-allowed disabled:opacity-40"
          >
            {t("calculate")}
          </button>
        </div>
      </section>
    </div>
  );
}
