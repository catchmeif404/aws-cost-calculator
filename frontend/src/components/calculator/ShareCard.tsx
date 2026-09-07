"use client";

import { forwardRef } from "react";
import { useTranslations, useLocale } from "next-intl";
import { CalculationResponse, OptimizationResponse, ResourceCatalogItem } from "@/lib/api";
import { regionLabel, resourceLabel } from "./formTypes";
import DiagramSnapshotView from "./DiagramSnapshotView";
import type { DiagramSnapshot } from "./diagramSnapshot";

function formatUsd(value: number) {
  return `$${value.toFixed(2)}`;
}

function formatKrw(value: number) {
  return `₩${Math.round(value).toLocaleString("ko-KR")}`;
}

interface Props {
  projectName: string;
  region: string;
  calculation: CalculationResponse;
  optimization: OptimizationResponse | null;
  diagramSnapshot: DiagramSnapshot | null;
  resourceCatalog: ResourceCatalogItem[];
}

const ShareCard = forwardRef<HTMLDivElement, Props>(function ShareCard(
  { projectName, region, calculation, optimization, diagramSnapshot, resourceCatalog },
  ref
) {
  const t = useTranslations("calculator.shareCard");
  const locale = useLocale();

  return (
    <div
      ref={ref}
      className="flex w-[600px] flex-col gap-6 bg-white p-10 text-zinc-900"
      style={{ fontFamily: "sans-serif" }}
    >
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-[#ff9900]">
            {t("title")}
          </p>
          <h1 className="mt-1 text-xl font-bold text-zinc-900">{projectName || t("defaultProjectName")}</h1>
        </div>
        <span className="rounded-full border border-zinc-200 px-3 py-1 text-xs text-zinc-500">
          {regionLabel(region, locale)}
        </span>
      </div>

      <div className="flex flex-col items-center gap-1 rounded-2xl bg-zinc-900 py-8 text-white">
        <span className="text-sm text-zinc-400">{t("estimatedCost")}</span>
        <span className="text-4xl font-bold">{formatUsd(calculation.totalMonthlyCost)}</span>
        <span className="text-zinc-400">{formatKrw(calculation.totalMonthlyCostKrw)}</span>
      </div>

      <div className="rounded-xl border border-zinc-200">
        {calculation.resources.map((r, i) => (
          <div
            key={r.resourceId}
            className={`flex items-center justify-between px-5 py-3 text-sm ${
              i !== 0 ? "border-t border-zinc-100" : ""
            }`}
          >
            <span className="text-zinc-600">{resourceLabel(resourceCatalog, r.type)}</span>
            <span className="font-medium text-zinc-900">{formatUsd(r.monthlyCost)}</span>
          </div>
        ))}
        <div className="flex items-center justify-between border-t border-zinc-200 px-5 py-3 text-sm font-semibold">
          <span>{t("total")}</span>
          <span>{formatUsd(calculation.totalMonthlyCost)}</span>
        </div>
      </div>

      {calculation.recommendationMetadata && (
        <div className="rounded-xl border border-indigo-200 bg-indigo-50 p-5">
          <h3 className="font-medium text-indigo-900">{t("aiRecommendedConfig")}</h3>
          <p className="mt-1 text-sm font-medium text-indigo-900">
            {calculation.recommendationMetadata.tierName}
          </p>
          <p className="mt-1 text-sm text-indigo-800">{calculation.recommendationMetadata.description}</p>
          <p className="mt-3 rounded-lg bg-white/60 p-3 text-sm text-indigo-900">
            {calculation.recommendationMetadata.recommendationReason}
          </p>
        </div>
      )}

      {diagramSnapshot && (
        <DiagramSnapshotView snapshot={diagramSnapshot} title={t("diagramTitle")} compact />
      )}

      {optimization && optimization.suggestions.length > 0 && (
        <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-5">
          <h3 className="font-medium text-emerald-900">
            {t("optimizeSavings", { amount: formatUsd(optimization.estimatedSavings) })}
          </h3>
          <ul className="mt-3 flex flex-col gap-2">
            {optimization.suggestions.slice(0, 3).map((s, i) => (
              <li key={i} className="flex gap-2 text-sm text-emerald-800">
                <span>⚡</span>
                <span>{s.message}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      <p className="text-center text-xs text-zinc-400">
        {t("footer")}
      </p>
    </div>
  );
});

export default ShareCard;
