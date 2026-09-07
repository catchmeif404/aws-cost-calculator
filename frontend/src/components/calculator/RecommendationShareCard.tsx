"use client";

import { useTranslations } from "next-intl";
import { RecommendationResponse, ResourceCatalogItem } from "@/lib/api";
import { resourceLabel } from "./formTypes";
import RecommendedArchitectureDiagram from "./RecommendedArchitectureDiagram";

function formatUsd(value: number) {
  return `$${value.toFixed(2)}`;
}

function formatKrw(value: number) {
  return `₩${Math.round(value).toLocaleString("ko-KR")}`;
}

interface Props {
  result: RecommendationResponse;
  serviceName: string;
  resourceCatalog: ResourceCatalogItem[];
}

export default function RecommendationShareCard({ result, serviceName, resourceCatalog }: Props) {
  const t = useTranslations("calculator.recommendPanel");
  const tCard = useTranslations("calculator.shareCard");
  return (
    <div className="flex w-[760px] flex-col gap-5 bg-white p-10 text-zinc-900" style={{ fontFamily: "sans-serif" }}>
      <div>
        <p className="text-xs font-semibold uppercase tracking-wide text-[#ff9900]">{t("pdfEyebrow")}</p>
        <h1 className="mt-1 text-2xl font-bold">{serviceName || result.tierName}</h1>
        <p className="mt-1 text-sm text-zinc-500">{result.description}</p>
      </div>

      {/* Same black price card as ShareCard.tsx (the regular calculator's export card) — this
          used to be a plain right-aligned price readout, which made the AI recommendation's PDF
          look inconsistent with the other report exports even though the same CostEngine backs
          both. */}
      <div className="flex flex-col items-center gap-1 rounded-2xl bg-zinc-900 py-8 text-white">
        <span className="text-sm text-zinc-400">{tCard("estimatedCost")}</span>
        <span className="text-4xl font-bold">{formatUsd(result.totalMonthlyCostUsd)}</span>
        <span className="text-zinc-400">{formatKrw(result.totalMonthlyCostKrw)}</span>
      </div>

      <div className="rounded-xl bg-zinc-50 p-4 text-sm text-zinc-700">
        {result.recommendationReason}
      </div>

      <RecommendedArchitectureDiagram
        resources={result.resources}
        additionalRecommendations={result.additionalRecommendations}
        resourceCatalog={resourceCatalog}
      />

      {result.additionalRecommendations.length > 0 && (
        <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
          <div className="font-semibold">{t("additionalRecommendations")}</div>
          <ul className="mt-2 list-disc space-y-1 pl-5">
            {result.additionalRecommendations.map((item, index) => (
              <li key={index}>{item}</li>
            ))}
          </ul>
        </div>
      )}

      <div className="rounded-xl border border-zinc-200">
        {result.resources.map((resource, index) => (
          <div
            key={index}
            className={`flex items-center justify-between px-5 py-3 text-sm ${index === 0 ? "" : "border-t border-zinc-100"}`}
          >
            <span className="text-zinc-600">{resourceLabel(resourceCatalog, resource.type)}</span>
            <span className="font-medium text-zinc-900">{formatUsd(resource.monthlyCost)}</span>
          </div>
        ))}
        <div className="flex items-center justify-between border-t border-zinc-200 px-5 py-3 text-sm font-semibold">
          <span>{t("total")}</span>
          <span>{formatUsd(result.totalMonthlyCostUsd)}</span>
        </div>
      </div>

      <p className="text-center text-xs text-zinc-400">
        {t("pdfFooter")}
      </p>
    </div>
  );
}
