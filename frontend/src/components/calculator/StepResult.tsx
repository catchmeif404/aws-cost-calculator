"use client";

import { useRef, useState } from "react";
import { useTranslations } from "next-intl";
import {
  ApiError,
  CalculationResponse,
  optimize,
  OptimizationResponse,
  ResourceCatalogItem,
} from "@/lib/api";
import CreditRequiredModal from "@/components/auth/CreditRequiredModal";
import ShareCard from "./ShareCard";
import SharePdfModal from "./SharePdfModal";
import DiagramSnapshotView from "./DiagramSnapshotView";
import type { DiagramSnapshot } from "./diagramSnapshot";
import { resourceLabel } from "./formTypes";
import {
  canShareFiles,
  canvasToPdfBlob,
  captureCardCanvas,
  downloadBlob,
  sharePdfBlob,
} from "@/lib/exportCard";

function formatUsd(value: number) {
  return `$${value.toFixed(2)}`;
}

function formatKrw(value: number) {
  return `₩${Math.round(value).toLocaleString("ko-KR")}`;
}

interface Props {
  loading: boolean;
  error: string | null;
  projectId: number | null;
  onRequireAuth: () => boolean;
  calculation: CalculationResponse | null;
  projectName: string;
  region: string;
  diagramSnapshot: DiagramSnapshot | null;
  resourceCatalog: ResourceCatalogItem[];
  onBack: () => void;
  onRestart: () => void;
}

export default function StepResult({
  loading,
  error,
  projectId,
  onRequireAuth,
  calculation,
  projectName,
  region,
  diagramSnapshot,
  resourceCatalog,
  onBack,
  onRestart,
}: Props) {
  const t = useTranslations("calculator.stepResult");
  const cardRef = useRef<HTMLDivElement>(null);
  const pdfBlobRef = useRef<Blob | null>(null);

  const [generating, setGenerating] = useState(false);
  const [generateError, setGenerateError] = useState<string | null>(null);
  const [imageUrl, setImageUrl] = useState<string | null>(null);
  const [sharing, setSharing] = useState(false);
  const [shareError, setShareError] = useState<string | null>(null);

  // Cost optimization is a separate, on-demand, AI-driven action the user triggers after seeing
  // the calculation — unlike calculation/explanation, it spends 1 credit each time.
  const [optimization, setOptimization] = useState<OptimizationResponse | null>(null);
  const [optimizing, setOptimizing] = useState(false);
  const [optimizeError, setOptimizeError] = useState<string | null>(null);
  const [creditModalOpen, setCreditModalOpen] = useState(false);

  async function handleOptimize() {
    if (!projectId) return;
    if (!onRequireAuth()) return;

    setOptimizing(true);
    setOptimizeError(null);
    try {
      const res = await optimize(projectId);
      setOptimization(res);
    } catch (e) {
      if (e instanceof ApiError && e.status === 402) {
        setCreditModalOpen(true);
      } else {
        setOptimizeError(e instanceof ApiError ? e.message : t("optimizeFailed"));
      }
    } finally {
      setOptimizing(false);
    }
  }

  if (loading) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/45 p-4 backdrop-blur-sm">
        <div className="w-full max-w-sm overflow-hidden rounded-lg border text-center shadow-2xl border-slate-800 bg-slate-950">
          <div className="bg-[#232f3e] px-6 py-4 text-left text-white">
            <div className="text-xs font-semibold uppercase text-[#ff9900]">Calculating</div>
            <h3 className="mt-1 text-base font-semibold">{t("calculatingTitle")}</h3>
          </div>
          <div className="p-6">
            <div className="mx-auto h-10 w-10 animate-spin rounded-full border-2 border-t-[#ff9900] border-slate-800" />
            <p className="mt-4 text-sm text-slate-400">
              {t("calculatingBody")}
            </p>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col gap-4">
        <div className="rounded-xl border p-5 text-sm border-red-900 bg-red-950 text-red-300">
          {error}
        </div>
        <button
          onClick={onBack}
          className="rounded-full border px-5 py-3 font-medium border-zinc-700 text-zinc-300 hover:bg-zinc-900"
        >
          {t("backToStart")}
        </button>
      </div>
    );
  }

  if (!calculation) {
    return null;
  }

  const fileName = `${(projectName || "aws-cost").replace(/[^\w가-힣-]+/g, "_")}-aws-cost.pdf`;

  async function handleOpenShareModal() {
    if (!cardRef.current) return;
    setGenerating(true);
    setGenerateError(null);
    try {
      // Capture once, then derive both the on-screen preview image and the
      // shareable/downloadable PDF from the same canvas.
      const canvas = await captureCardCanvas(cardRef.current);
      const blob = await canvasToPdfBlob(canvas);
      pdfBlobRef.current = blob;
      setImageUrl(canvas.toDataURL("image/png"));
    } catch (e) {
      console.error("PDF generation failed:", e);
      setGenerateError(t("pdfFailed"));
    } finally {
      setGenerating(false);
    }
  }

  function handleCloseModal() {
    setImageUrl(null);
    pdfBlobRef.current = null;
    setShareError(null);
  }

  async function handleShare() {
    if (!pdfBlobRef.current) return;
    setSharing(true);
    setShareError(null);
    try {
      const opened = await sharePdfBlob(pdfBlobRef.current, fileName, projectName || t("shareTitle"));
      if (!opened) {
        setShareError(t("shareUnsupported"));
      }
    } catch {
      setShareError(t("shareFailed"));
    } finally {
      setSharing(false);
    }
  }

  function handleDownload() {
    if (!pdfBlobRef.current) return;
    downloadBlob(pdfBlobRef.current, fileName);
  }

  return (
    <div className="flex flex-col gap-6">
      <CreditRequiredModal open={creditModalOpen} onClose={() => setCreditModalOpen(false)} />
      {optimizing && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/45 p-4 backdrop-blur-sm">
          <div className="w-full max-w-sm overflow-hidden rounded-lg border text-center shadow-2xl border-slate-800 bg-slate-950">
            <div className="bg-[#232f3e] px-6 py-4 text-left text-white">
              <div className="text-xs font-semibold uppercase text-[#ff9900]">Analyzing</div>
              <h3 className="mt-1 text-base font-semibold">{t("analyzingTitle")}</h3>
            </div>
            <div className="p-6">
              <div className="mx-auto h-10 w-10 animate-spin rounded-full border-2 border-t-[#ff9900] border-slate-800" />
              <p className="mt-4 text-sm text-slate-400">
                {t("analyzingBody")}
              </p>
            </div>
          </div>
        </div>
      )}
      {/* Rendered off-screen so it can still be captured as the PDF/share card content.
          Deliberately NOT display:none / visibility:hidden / opacity:0 — html2canvas can
          capture a blank canvas for those. Instead it's fully opaque, positioned at the
          top-left corner, and pushed behind the rest of the page with a negative z-index so
          the real page content (which has an opaque background) visually covers it. */}
      <div
        className="pointer-events-none fixed left-0 top-0 z-[-1] overflow-hidden"
        aria-hidden="true"
      >
        <ShareCard
          ref={cardRef}
          projectName={projectName}
          region={region}
          calculation={calculation}
          optimization={optimization}
          diagramSnapshot={diagramSnapshot}
          resourceCatalog={resourceCatalog}
        />
      </div>

      <div className="flex items-start justify-between gap-4">
        <h2 className="text-lg font-semibold text-zinc-50">
          {t("title")}
        </h2>
        <button
          onClick={handleOpenShareModal}
          disabled={generating}
          className="shrink-0 rounded-full px-4 py-2 text-sm font-medium disabled:opacity-50 bg-[#ff9900] text-[#161e2d] hover:bg-[#f2a100]"
        >
          {generating ? t("exporting") : t("exportPdf")}
        </button>
      </div>
      {generateError && <p className="text-sm text-red-400">{generateError}</p>}

      {/* Deliberately light regardless of the app's dark theme — this is the same "report" content
          that gets captured as the PDF/share card (ShareCard.tsx), so it must always look like
          what gets exported: white background, black price card. */}
      <div className="flex flex-col gap-6 rounded-2xl border border-zinc-200 bg-white p-6 text-zinc-900">
        <div className="flex flex-col items-center gap-1 rounded-2xl bg-zinc-900 py-10 text-white">
          <span className="text-sm text-zinc-400">{t("estimatedCost")}</span>
          <span className="text-4xl font-bold">
            {formatUsd(calculation.totalMonthlyCost)}
          </span>
          <span className="text-zinc-400">
            {formatKrw(calculation.totalMonthlyCostKrw)}
          </span>
        </div>

        <div className="rounded-xl border border-zinc-200">
          {calculation.resources.map((r, i) => (
            <div
              key={r.resourceId}
              className={`flex items-center justify-between px-5 py-3 text-sm ${ i !== 0 ? "border-t border-zinc-100" : "" }`}
            >
              <span className="text-zinc-600">
                {resourceLabel(resourceCatalog, r.type)}
              </span>
              <span className="font-medium text-zinc-900">
                {formatUsd(r.monthlyCost)}
              </span>
            </div>
          ))}
          <div className="flex items-center justify-between border-t px-5 py-3 text-sm font-semibold border-zinc-200">
            <span>{t("total")}</span>
            <span>{formatUsd(calculation.totalMonthlyCost)}</span>
          </div>
        </div>

        {calculation.recommendationMetadata && (
          <div className="rounded-xl border p-5 border-indigo-200 bg-indigo-50">
            <h3 className="flex items-center gap-2 font-medium text-indigo-900">
              {t("aiRecommendedConfig")}
              {calculation.recommendationMetadata.aiGenerated && (
                <span className="rounded-full bg-indigo-600/10 px-2 py-0.5 text-xs font-normal text-indigo-700">
                  AI
                </span>
              )}
            </h3>
            <p className="mt-1 text-sm font-medium text-indigo-900">
              {calculation.recommendationMetadata.tierName}
            </p>
            <p className="mt-1 text-sm text-indigo-800">
              {calculation.recommendationMetadata.description}
            </p>
            <p className="mt-3 rounded-lg bg-white/60 p-3 text-sm text-indigo-900">
              {calculation.recommendationMetadata.recommendationReason}
            </p>
            {calculation.recommendationMetadata.additionalRecommendations.length > 0 && (
              <div className="mt-3 rounded-lg border p-3 text-sm border-amber-200 bg-amber-50 text-amber-900">
                <div className="font-medium">{t("additionalRecommendations")}</div>
                <ul className="mt-2 list-disc space-y-1 pl-5">
                  {calculation.recommendationMetadata.additionalRecommendations.map((item, index) => (
                    <li key={index}>{item}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}

        {diagramSnapshot && (
          <DiagramSnapshotView snapshot={diagramSnapshot} title={t("diagramTitle")} />
        )}

        <div className="rounded-xl border p-5 border-emerald-200 bg-emerald-50">
          <h3 className="flex items-center gap-2 font-medium text-emerald-900">
            {t("aiOptimization")}
            {optimization && (
              <span
                className={
                  optimization.aiGenerated
                    ? "rounded-full bg-emerald-600/10 px-2 py-0.5 text-xs font-normal text-emerald-700"
                    : "rounded-full bg-zinc-100 px-2 py-0.5 text-xs font-normal text-zinc-500"
                }
              >
                {optimization.aiGenerated ? `AI · ${optimization.provider}` : t("ruleBased")}
              </span>
            )}
          </h3>

          {!optimization ? (
            <>
              <p className="mt-2 text-sm text-emerald-800">
                {t("optimizeIntro")}
              </p>
              <button
                onClick={handleOptimize}
                disabled={optimizing || !projectId}
                className="mt-3 rounded-full px-4 py-2 text-sm font-medium text-white disabled:opacity-50 bg-emerald-700 hover:bg-emerald-600"
              >
                {optimizing ? t("analyzing") : t("optimizeCta")}
              </button>
            </>
          ) : (
            <>
              <div className="mt-3 flex items-center justify-between text-sm text-emerald-900">
                <span>{t("currentCost")}</span>
                <span>{formatUsd(optimization.currentMonthlyCost)}</span>
              </div>
              <div className="mt-1 flex items-center justify-between text-sm text-emerald-900">
                <span>{t("optimizedCost")}</span>
                <span>{formatUsd(optimization.optimizedMonthlyCost)}</span>
              </div>
              <div className="mt-1 flex items-center justify-between text-sm font-semibold text-emerald-900">
                <span>{t("estimatedSavings")}</span>
                <span>{formatUsd(optimization.estimatedSavings)} {t("perMonth")}</span>
              </div>

              {optimization.suggestions.length > 0 ? (
                <ul className="mt-4 flex flex-col gap-2">
                  {optimization.suggestions.map((s, i) => (
                    <li
                      key={i}
                      className="flex gap-2 text-sm text-emerald-800"
                    >
                      <span>⚡</span>
                      <span>{s.message}</span>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="mt-3 text-sm text-emerald-800">
                  {t("noSavingsFound")}
                </p>
              )}

              <button
                onClick={handleOptimize}
                disabled={optimizing}
                className="mt-4 text-sm font-medium underline-offset-2 hover:underline disabled:opacity-50 text-emerald-700"
              >
                {optimizing ? t("analyzing") : t("reOptimize")}
              </button>
            </>
          )}

          {optimizeError && (
            <p className="mt-2 text-sm text-red-600">{optimizeError}</p>
          )}
        </div>
      </div>

      <div className="flex gap-3">
        <button
          onClick={onBack}
          className="rounded-full border px-5 py-3 font-medium border-zinc-700 text-zinc-300 hover:bg-zinc-900"
        >
          {t("editConfig")}
        </button>
        <button
          onClick={onRestart}
          className="flex-1 rounded-full px-5 py-3 font-medium bg-[#ff9900] text-[#161e2d] hover:bg-[#f2a100]"
        >
          {t("restart")}
        </button>
      </div>

      {imageUrl && (
        <SharePdfModal
          imageUrl={imageUrl}
          fileName={fileName}
          shareSupported={canShareFiles()}
          sharing={sharing}
          shareError={shareError}
          onShare={handleShare}
          onDownload={handleDownload}
          onClose={handleCloseModal}
        />
      )}
    </div>
  );
}
