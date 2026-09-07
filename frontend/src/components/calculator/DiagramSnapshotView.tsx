"use client";

import { useEffect, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import type { DiagramSnapshot } from "./diagramSnapshot";
import { categoryBg, categoryBorder, categoryText } from "./diagramSnapshot";

interface Props {
  snapshot: DiagramSnapshot;
  title?: string;
  compact?: boolean;
}

function center(item: { x: number; y: number; width: number; height: number }) {
  return {
    x: item.x + item.width / 2,
    y: item.y + item.height / 2,
  };
}

export default function DiagramSnapshotView({ snapshot, title, compact = false }: Props) {
  const t = useTranslations("calculator.diagramSnapshotView");
  const resolvedTitle = title ?? t("defaultTitle");
  const resources = snapshot.items.filter((item) => item.itemType === "resource");
  const groups = snapshot.items.filter((item) => item.itemType === "group");
  const containerRef = useRef<HTMLDivElement>(null);
  const [scale, setScale] = useState(1);

  // Fit the diagram to whatever width it's actually given instead of relying on the user to
  // discover a horizontal scrollbar. A narrow container (e.g. the mypage history report's modal,
  // max-w-2xl) previously left a real recommendation's multi-lane diagram scrollable but with no
  // visible scrollbar affordance, so it just looked clipped. This is read-only, so shrinking to
  // fit costs nothing — unlike VisualArchitectureBuilder's editable canvas, which keeps its own
  // pan/zoom since you're actively working in it there.
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;
    const updateScale = () => {
      const availableWidth = container.clientWidth;
      if (availableWidth > 0) {
        setScale(Math.min(1, availableWidth / snapshot.width));
      }
    };
    updateScale();
    const observer = new ResizeObserver(updateScale);
    observer.observe(container);
    return () => observer.disconnect();
  }, [snapshot.width]);

  if (snapshot.items.length === 0) {
    return null;
  }

  const itemById = Object.fromEntries(snapshot.items.map((item) => [item.id, item]));

  // Deliberately light regardless of the app's dark theme — the resource tones in diagramSnapshot.ts
  // are light-only (shared with the always-light ShareCard), so a dark shell around them made
  // resource cards hard to read, same issue as the drag-builder canvas.
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4">
      <div className="mb-3 flex items-center justify-between gap-3">
        <div>
          <div className="text-xs font-semibold uppercase text-[#ff9900]">Architecture</div>
          <h3 className="mt-1 text-sm font-semibold text-slate-900">{resolvedTitle}</h3>
        </div>
        <div className="text-xs text-slate-500">
          {t("summary", { groups: groups.length, resources: resources.length })}
        </div>
      </div>

      <div
        ref={containerRef}
        className="relative max-h-[520px] overflow-y-auto overflow-x-hidden rounded-lg border border-dashed border-slate-300 bg-[linear-gradient(#e5e7eb_1px,transparent_1px),linear-gradient(90deg,#e5e7eb_1px,transparent_1px)] bg-[size:24px_24px]"
        style={{ minHeight: compact ? 240 : 340 }}
      >
        <div className="relative" style={{ width: snapshot.width * scale, height: snapshot.height * scale }}>
          <div
            className="relative"
            style={{ width: snapshot.width, height: snapshot.height, transform: `scale(${scale})`, transformOrigin: "top left" }}
          >
            <svg
              className="absolute inset-0"
              width={snapshot.width}
              height={snapshot.height}
              viewBox={`0 0 ${snapshot.width} ${snapshot.height}`}
            >
              {snapshot.connections.map((connection, index) => {
                const from = itemById[connection.from];
                const to = itemById[connection.to];
                if (!from || !to) return null;
                const a = center(from);
                const b = center(to);
                return (
                  <line
                    key={`${connection.from}-${connection.to}-${index}`}
                    x1={a.x}
                    y1={a.y}
                    x2={b.x}
                    y2={b.y}
                    stroke="#64748b"
                    strokeWidth={2}
                    strokeDasharray="5 4"
                  />
                );
              })}
            </svg>

            {groups.map((item) => (
              <div
                key={item.id}
                className={`absolute rounded-lg border-2 border-dashed p-2 ${item.tone}`}
                style={{
                  left: item.x,
                  top: item.y,
                  width: item.width,
                  height: item.height,
                }}
              >
                <div className="text-xs font-semibold">{item.title}</div>
                <div className="text-[11px] opacity-75">{item.description}</div>
              </div>
            ))}

            {resources.map((item) => (
              <div
                key={item.id}
                className={`absolute flex items-center gap-2 rounded-lg border p-2 shadow-sm ${categoryBg(item.category)} ${categoryBorder(item.category)}`}
                style={{
                  left: item.x,
                  top: item.y,
                  width: item.width,
                  minHeight: compact ? 70 : 86,
                }}
              >
                <span
                  className={`grid h-10 w-12 shrink-0 place-items-center rounded-md border bg-white text-xs font-bold ${categoryBorder(item.category)} ${categoryText(item.category)}`}
                >
                  {item.shortName}
                </span>
                <span className="min-w-0">
                  <span className="block truncate text-xs font-semibold text-slate-900">{item.title}</span>
                  <span className="block truncate text-[11px] text-slate-500">{item.description}</span>
                  {!compact &&
                    item.details.slice(0, 2).map((detail) => (
                      <span key={detail} className="block truncate text-[10px] text-slate-500">
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
