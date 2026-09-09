"use client";

import { useRef, useState } from "react";
import { useTranslations, useLocale } from "next-intl";
import {
  ApiError,
  RecommendationMetadata,
  RecommendationResponse,
  recommendArchitecture,
  ResourceCatalogItem,
} from "@/lib/api";
import {
  CategoryLaneLayout,
  computeCategoryLaneLayout,
  createResourceInstanceFromConfig,
  findCatalogItem,
  REGIONS,
  regionLabel,
  resourceLabel,
  ResourceInstance,
} from "./formTypes";
import RecommendedArchitectureDiagram from "./RecommendedArchitectureDiagram";
import RecommendationShareCard from "./RecommendationShareCard";
import SharePdfModal from "./SharePdfModal";
import {
  canShareFiles,
  canvasToPdfBlob,
  captureCardCanvas,
  downloadBlob,
  sharePdfBlob,
} from "@/lib/exportCard";
import CreditRequiredModal from "@/components/auth/CreditRequiredModal";

function formatUsd(value: number) {
  return `$${value.toFixed(2)}`;
}

function formatKrw(value: number) {
  return `₩${Math.round(value).toLocaleString("ko-KR")}`;
}

export interface AppliedRecommendation {
  projectName: string;
  monthlyUsers: number;
  requestsPerUser: number;
  busyTrafficLevel: string;
  serviceStage: string;
  region: string;
  resources: ResourceInstance[];
  initialLayout: CategoryLaneLayout;
  recommendationMetadata: RecommendationMetadata;
}

interface Props {
  onBack: () => void;
  onRequireAuth: () => boolean;
  onApply: (recommendation: AppliedRecommendation) => void;
  resourceCatalog: ResourceCatalogItem[];
}

export default function RecommendPanel({ onBack, onRequireAuth, onApply, resourceCatalog }: Props) {
  const t = useTranslations("calculator.recommendPanel");
  const tFields = useTranslations("calculator.projectFields");
  const locale = useLocale();
  const cardRef = useRef<HTMLDivElement>(null);
  const pdfBlobRef = useRef<Blob | null>(null);
  const [serviceName, setServiceName] = useState("My Awesome API");
  const [monthlyUsers, setMonthlyUsers] = useState("10000");
  const [requestsPerUser, setRequestsPerUser] = useState("100");
  const [busyTrafficLevel, setBusyTrafficLevel] = useState("two_to_three_times");
  const [serviceStage, setServiceStage] = useState("mvp");
  const [serviceType, setServiceType] = useState("web_api");
  const [serviceDescription, setServiceDescription] = useState(
    t("describeServiceDefault")
  );
  const [region, setRegion] = useState(REGIONS[0].code);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<RecommendationResponse | null>(null);
  const [generatingPdf, setGeneratingPdf] = useState(false);
  const [pdfError, setPdfError] = useState<string | null>(null);
  const [pdfImageUrl, setPdfImageUrl] = useState<string | null>(null);
  const [sharingPdf, setSharingPdf] = useState(false);
  const [shareError, setShareError] = useState<string | null>(null);
  const [creditModalOpen, setCreditModalOpen] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!onRequireAuth()) {
      return;
    }
    if (!serviceName.trim()) {
      setError(t("nameRequired"));
      return;
    }

    const monthlyUsersNumber = Number(monthlyUsers);
    const requestsPerUserNumber = Number(requestsPerUser);
    if (monthlyUsersNumber < 0 || requestsPerUserNumber < 0) {
      setError(t("invalidScale"));
      return;
    }

    setLoading(true);
    setError(null);
    setResult(null);
    try {
      const res = await recommendArchitecture({
        serviceName,
        monthlyUsers: monthlyUsersNumber,
        requestsPerUser: requestsPerUserNumber,
        busyTrafficLevel,
        serviceStage,
        serviceType,
        serviceDescription,
        region,
      });
      setResult(res);
    } catch (e) {
      if (e instanceof ApiError && e.status === 402) {
        setCreditModalOpen(true);
        setError(null);
      } else {
        setError(e instanceof ApiError ? e.message : t("fetchFailed"));
      }
    } finally {
      setLoading(false);
    }
  }

  const fileName = `${(serviceName || "aws-recommendation").replace(/[^\w가-힣-]+/g, "_")}-aws-recommendation.pdf`;

  async function handleOpenPdfModal() {
    if (!cardRef.current) return;
    setGeneratingPdf(true);
    setPdfError(null);
    try {
      const canvas = await captureCardCanvas(cardRef.current);
      const blob = await canvasToPdfBlob(canvas);
      pdfBlobRef.current = blob;
      setPdfImageUrl(canvas.toDataURL("image/png"));
    } catch (e) {
      console.error("Recommendation PDF generation failed:", e);
      setPdfError(t("pdfFailed"));
    } finally {
      setGeneratingPdf(false);
    }
  }

  function handleClosePdfModal() {
    setPdfImageUrl(null);
    pdfBlobRef.current = null;
    setShareError(null);
  }

  async function handleSharePdf() {
    if (!pdfBlobRef.current) return;
    setSharingPdf(true);
    setShareError(null);
    try {
      const opened = await sharePdfBlob(pdfBlobRef.current, fileName, serviceName || t("shareTitle"));
      if (!opened) {
        setShareError(t("shareUnsupported"));
      }
    } catch {
      setShareError(t("shareFailed"));
    } finally {
      setSharingPdf(false);
    }
  }

  function handleDownloadPdf() {
    if (!pdfBlobRef.current) return;
    downloadBlob(pdfBlobRef.current, fileName);
  }

  function handleApply() {
    if (!result) return;
    const resources = result.resources
      .map((r) => {
        const catalogItem = findCatalogItem(resourceCatalog, r.type);
        return catalogItem ? createResourceInstanceFromConfig(catalogItem, r.configuration ?? {}) : null;
      })
      .filter((r): r is ResourceInstance => r !== null);

    onApply({
      projectName: serviceName,
      monthlyUsers: Number(monthlyUsers) || 0,
      requestsPerUser: Number(requestsPerUser) || 0,
      busyTrafficLevel,
      serviceStage,
      region,
      resources,
      initialLayout: computeCategoryLaneLayout(resources, resourceCatalog),
      recommendationMetadata: {
        tierName: result.tierName,
        description: result.description,
        recommendationReason: result.recommendationReason,
        additionalRecommendations: result.additionalRecommendations,
        aiGenerated: result.aiGenerated,
      },
    });
    setResult(null);
  }

  return (
    <div className="flex flex-col gap-6">
      <CreditRequiredModal open={creditModalOpen} onClose={() => setCreditModalOpen(false)} />
      {result && (
        <div className="pointer-events-none fixed left-0 top-0 z-[-1] overflow-hidden" aria-hidden="true">
          <div ref={cardRef}>
            <RecommendationShareCard result={result} serviceName={serviceName} resourceCatalog={resourceCatalog} />
          </div>
        </div>
      )}

      <div className="rounded-sm border p-5 shadow-sm border-slate-800 bg-white">
        <div className="text-xs font-semibold uppercase text-[#a32b2b]">{t("eyebrow")}</div>
        <h2 className="mt-1 text-lg font-semibold text-stone-900">
          {t("title")}
        </h2>
        <p className="mt-1 text-sm text-stone-600">
          {t("subtitle")}
        </p>
      </div>

      <form
        onSubmit={handleSubmit}
        className="grid gap-4 rounded-sm border p-5 shadow-sm border-slate-800 bg-white lg:grid-cols-2"
      >
        <div className="grid gap-4">
          <label className="flex flex-col gap-2">
            <span className="text-sm font-medium text-stone-700">{t("serviceName")}</span>
            <input
              value={serviceName}
              onChange={(e) => setServiceName(e.target.value)}
              className="w-full rounded-sm border px-4 py-2.5 outline-none focus:border-zinc-900 border-zinc-700 bg-white text-zinc-50"
              placeholder="My Awesome API"
            />
          </label>

          <label className="flex flex-col gap-2">
            <span className="text-sm font-medium text-stone-700">
              {tFields("busyTraffic")}
            </span>
            <select
              value={busyTrafficLevel}
              onChange={(e) => setBusyTrafficLevel(e.target.value)}
              className="rounded-sm border px-4 py-2.5 text-sm outline-none focus:border-zinc-900 border-zinc-700 bg-white text-zinc-50"
            >
              <option value="similar">{tFields("trafficSimilar")}</option>
              <option value="two_to_three_times">{tFields("trafficTwoToThree")}</option>
              <option value="five_plus_times">{tFields("trafficFivePlus")}</option>
              <option value="unknown">{tFields("trafficUnknown")}</option>
            </select>
          </label>
        </div>

        <div className="grid gap-3 sm:grid-cols-2">
          <label className="flex flex-col gap-2">
            <span className="text-sm font-medium text-stone-700">{tFields("monthlyUsers")}</span>
            <input
              type="number"
              min={0}
              value={monthlyUsers}
              onChange={(e) => setMonthlyUsers(e.target.value)}
              className="w-full rounded-sm border px-4 py-2.5 outline-none focus:border-zinc-900 border-zinc-700 bg-white text-zinc-50"
            />
          </label>
          <label className="flex flex-col gap-2">
            <span className="text-sm font-medium text-stone-700">{tFields("requestsPerUser")}</span>
            <input
              type="number"
              min={0}
              value={requestsPerUser}
              onChange={(e) => setRequestsPerUser(e.target.value)}
              className="w-full rounded-sm border px-4 py-2.5 outline-none focus:border-zinc-900 border-zinc-700 bg-white text-zinc-50"
            />
          </label>
        </div>

        <div className="grid gap-3 sm:grid-cols-2">
          <label className="flex flex-col gap-2">
            <span className="text-sm font-medium text-stone-700">{t("serviceType")}</span>
            <select
              value={serviceType}
              onChange={(e) => setServiceType(e.target.value)}
              className="rounded-sm border px-4 py-2.5 text-sm outline-none focus:border-zinc-900 border-zinc-700 bg-white text-zinc-50"
            >
              <option value="web_api">{t("typeWebApi")}</option>
              <option value="mobile_app">{t("typeMobileApp")}</option>
              <option value="static_site">{t("typeStaticSite")}</option>
              <option value="batch_worker">{t("typeBatchWorker")}</option>
              <option value="internal_admin">{t("typeInternalAdmin")}</option>
            </select>
          </label>
          <label className="flex flex-col gap-2">
            <span className="text-sm font-medium text-stone-700">{tFields("serviceStage")}</span>
            <select
              value={serviceStage}
              onChange={(e) => setServiceStage(e.target.value)}
              className="rounded-sm border px-4 py-2.5 text-sm outline-none focus:border-zinc-900 border-zinc-700 bg-white text-zinc-50"
            >
              <option value="toy">{tFields("stageToy")}</option>
              <option value="mvp">{tFields("stageMvp")}</option>
              <option value="production">{tFields("stageProduction")}</option>
              <option value="critical">{tFields("stageCritical")}</option>
            </select>
          </label>
        </div>

        <label className="flex flex-col gap-2 lg:row-span-2">
          <span className="text-sm font-medium text-stone-700">{t("describeService")}</span>
          <textarea
            value={serviceDescription}
            onChange={(e) => setServiceDescription(e.target.value)}
            rows={7}
            className="min-h-[188px] w-full resize-y rounded-sm border px-4 py-3 text-sm outline-none focus:border-zinc-900 border-zinc-700 bg-white text-zinc-50"
            placeholder={t("describeServicePlaceholder")}
          />
        </label>

        <div className="grid gap-3 sm:grid-cols-2">
          <label className="flex flex-col gap-2">
            <span className="text-sm font-medium text-stone-700">{tFields("region")}</span>
            <select
              value={region}
              onChange={(e) => setRegion(e.target.value)}
              className="rounded-sm border px-4 py-2.5 text-sm outline-none focus:border-zinc-900 border-zinc-700 bg-white text-zinc-50"
            >
              {REGIONS.map((r) => (
                <option key={r.code} value={r.code}>
                  {regionLabel(r.code, locale)}
                </option>
              ))}
            </select>
          </label>
        </div>

        <button
          type="submit"
          disabled={loading}
          className="rounded bg-[#a32b2b] px-5 py-2.5 font-semibold text-[#ffffff] transition-colors hover:bg-[#842020] disabled:opacity-50 lg:col-span-2"
        >
          {loading ? t("loading") : t("submit")}
        </button>
      </form>

      {error && (
        <div className="rounded-xl border p-4 text-sm border-red-900 bg-red-950 text-red-300">
          {error}
        </div>
      )}

      {(loading || result) && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/45 p-4 backdrop-blur-sm">
          {loading && (
            <div className="w-full max-w-sm overflow-hidden rounded-sm border text-center shadow-2xl border-slate-800 bg-white">
              <div className="bg-stone-100 px-6 py-4 text-left text-stone-900">
                <div className="text-xs font-semibold uppercase text-[#a32b2b]">Analyzing</div>
                <h3 className="mt-1 text-base font-semibold">{t("analyzingTitle")}</h3>
              </div>
              <div className="p-6">
                <div className="mx-auto h-10 w-10 animate-spin rounded-full border-2 border-t-[#a32b2b] border-slate-800" />
                <p className="mt-4 text-sm text-stone-600">
                  {t("analyzingBody")}
                </p>
              </div>
            </div>
          )}

          {!loading && result && (
            <div className="max-h-[90vh] w-full max-w-5xl overflow-hidden rounded-sm border shadow-2xl border-slate-800 bg-white">
              <div className="flex flex-wrap items-start justify-between gap-4 border-b border-stone-300 bg-stone-100 p-5 text-stone-900">
                <div className="min-w-0">
                  <div className="text-xs font-semibold uppercase text-[#a32b2b]">
                    {result.analysisMode}
                  </div>
                  <h3 className="mt-1 text-lg font-semibold">{result.tierName}</h3>
                  <p className="mt-1 text-sm text-stone-700">{result.description}</p>
                </div>
                <div className="flex shrink-0 items-start gap-2">
                  <button
                    type="button"
                    onClick={handleApply}
                    className="rounded bg-[#a32b2b] px-3 py-2 text-xs font-semibold text-[#ffffff] hover:bg-[#842020]"
                  >
                    {t("editConfig")}
                  </button>
                  <button
                    type="button"
                    onClick={handleOpenPdfModal}
                    disabled={generatingPdf}
                    className="rounded border border-stone-300 px-3 py-2 text-xs font-semibold text-stone-900 hover:bg-stone-200"
                  >
                    {generatingPdf ? t("exporting") : t("exportPdf")}
                  </button>
                  <button
                    type="button"
                    onClick={() => setResult(null)}
                    className="grid h-8 w-8 place-items-center rounded border border-stone-300 text-sm text-stone-800 hover:bg-stone-200 hover:text-stone-900"
                  >
                    x
                  </button>
                </div>
              </div>

              <div className="max-h-[calc(90vh-116px)] overflow-y-auto p-5">
                {pdfError && <p className="mb-3 text-sm text-red-400">{pdfError}</p>}

                {/* Same "예상 월 비용" card + itemized/Total list styling as the wizard/drag-builder
                    report (StepResult) — this used to be a header price readout plus a bare list,
                    which made the AI recommendation's cost section look inconsistent with the
                    other two report styles even though the same CostEngine backs all three.
                    Deliberately light regardless of the app's dark theme — this is the same
                    "report" content that gets captured as the PDF/share card (ShareCard.tsx), so
                    it must always look like what gets exported: white background, black price card. */}
                <div className="flex flex-col gap-4 rounded-2xl border border-zinc-200 bg-white p-6 text-zinc-900">
                  <div className="flex flex-col items-center gap-1 rounded-2xl py-10 bg-white text-stone-900">
                    <span className="text-sm text-stone-600">{t("estimatedCost")}</span>
                    <span className="text-4xl font-bold">{formatUsd(result.totalMonthlyCostUsd)}</span>
                    <span className="text-stone-600">
                      {formatKrw(result.totalMonthlyCostKrw)}
                    </span>
                  </div>

                  <div className="rounded-sm p-3 text-sm bg-zinc-50 text-zinc-600">
                    {result.recommendationReason}
                  </div>

                  <RecommendedArchitectureDiagram
                    resources={result.resources}
                    additionalRecommendations={result.additionalRecommendations}
                    resourceCatalog={resourceCatalog}
                  />

                  {result.additionalRecommendations.length > 0 && (
                    <div className="rounded-sm border p-3 text-sm border-amber-200 bg-amber-50 text-amber-900">
                      <div className="font-medium">{t("additionalRecommendations")}</div>
                      <ul className="mt-2 list-disc space-y-1 pl-5">
                        {result.additionalRecommendations.map((item, index) => (
                          <li key={index}>{item}</li>
                        ))}
                      </ul>
                    </div>
                  )}

                  <div className="rounded-xl border border-zinc-200">
                    {result.resources.map((r, i) => (
                      <div
                        key={i}
                        className={`flex items-center justify-between px-5 py-3 text-sm ${ i !== 0 ? "border-t border-zinc-100" : "" }`}
                      >
                        <span className="text-zinc-600">{resourceLabel(resourceCatalog, r.type)}</span>
                        <span className="font-medium text-zinc-900">{formatUsd(r.monthlyCost)}</span>
                      </div>
                    ))}
                    <div className="flex items-center justify-between border-t px-5 py-3 text-sm font-semibold border-zinc-200">
                      <span>{t("total")}</span>
                      <span>{formatUsd(result.totalMonthlyCostUsd)}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      <button
        onClick={onBack}
        className="self-start text-sm font-medium underline-offset-2 hover:underline text-stone-600"
      >
        {t("back")}
      </button>

      {pdfImageUrl && (
        <SharePdfModal
          imageUrl={pdfImageUrl}
          fileName={fileName}
          shareSupported={canShareFiles()}
          sharing={sharingPdf}
          shareError={shareError}
          onShare={handleSharePdf}
          onDownload={handleDownloadPdf}
          onClose={handleClosePdfModal}
        />
      )}
    </div>
  );
}
