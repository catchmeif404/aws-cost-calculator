"use client";

import { useEffect, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import {
  ApiError,
  CalculationResponse,
  clearToken,
  CreditBalanceResponse,
  getMe,
  getMyCredits,
  getMyProjects,
  getResourceCatalog,
  getSavedCalculation,
  getToken,
  ProjectHistoryItem,
  ResourceCatalogItem,
  UserResponse,
} from "@/lib/api";
import { resourceLabel } from "@/components/calculator/formTypes";
import DiagramSnapshotView from "@/components/calculator/DiagramSnapshotView";
import ShareCard from "@/components/calculator/ShareCard";
import SharePdfModal from "@/components/calculator/SharePdfModal";
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

function formatDate(value: string) {
  return new Date(value).toLocaleString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export default function MyPage() {
  const t = useTranslations("mypage");
  const [user, setUser] = useState<UserResponse | null>(null);
  const [credits, setCredits] = useState<CreditBalanceResponse | null>(null);
  const [projects, setProjects] = useState<ProjectHistoryItem[]>([]);
  const [resourceCatalog, setResourceCatalog] = useState<ResourceCatalogItem[]>([]);
  const [initialToken] = useState(() => getToken());
  const [loading, setLoading] = useState(() => Boolean(getToken()));

  const [openedItem, setOpenedItem] = useState<ProjectHistoryItem | null>(null);
  const [reportLoading, setReportLoading] = useState(false);
  const [reportError, setReportError] = useState<string | null>(null);
  const [reportData, setReportData] = useState<CalculationResponse | null>(null);

  const cardRef = useRef<HTMLDivElement>(null);
  const pdfBlobRef = useRef<Blob | null>(null);
  const [generating, setGenerating] = useState(false);
  const [generateError, setGenerateError] = useState<string | null>(null);
  const [imageUrl, setImageUrl] = useState<string | null>(null);
  const [sharing, setSharing] = useState(false);
  const [shareError, setShareError] = useState<string | null>(null);

  useEffect(() => {
    if (!initialToken) {
      return;
    }

    Promise.all([getMe(), getMyCredits(), getMyProjects(), getResourceCatalog()])
      .then(([me, balance, history, catalog]) => {
        setUser(me);
        setCredits(balance);
        setProjects(history);
        setResourceCatalog(catalog);
      })
      .catch(() => {
        clearToken();
        setUser(null);
      })
      .finally(() => setLoading(false));
  }, [initialToken]);

  async function openReport(item: ProjectHistoryItem) {
    setOpenedItem(item);
    setReportLoading(true);
    setReportError(null);
    setReportData(null);
    try {
      const data = await getSavedCalculation(item.projectId);
      setReportData(data);
    } catch (e) {
      setReportError(e instanceof ApiError ? e.message : t("reportFailed"));
    } finally {
      setReportLoading(false);
    }
  }

  function closeReport() {
    setOpenedItem(null);
    setReportData(null);
    setReportError(null);
  }

  const fileName = `${(openedItem?.projectName || "aws-cost").replace(/[^\w가-힣-]+/g, "_")}-aws-cost.pdf`;

  async function handleOpenShareModal() {
    if (!cardRef.current) return;
    setGenerating(true);
    setGenerateError(null);
    try {
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

  function handleCloseShareModal() {
    setImageUrl(null);
    pdfBlobRef.current = null;
    setShareError(null);
  }

  async function handleShare() {
    if (!pdfBlobRef.current) return;
    setSharing(true);
    setShareError(null);
    try {
      const opened = await sharePdfBlob(pdfBlobRef.current, fileName, openedItem?.projectName || t("shareTitle"));
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
    <main className="min-h-screen px-4 py-8 bg-[#f7f7f4] text-stone-900 sm:px-6 lg:px-8">
      <div className="mx-auto flex w-full max-w-4xl flex-col gap-6">
        <div className="flex items-center justify-between gap-4">
          <div>
            <div className="text-xs font-semibold uppercase text-[#a32b2b]">My Page</div>
            <h1 className="mt-1 text-2xl font-bold">{t("title")}</h1>
          </div>
          <Link
            href="/calculator"
            className="rounded border px-4 py-2 text-sm font-semibold border-slate-700 text-stone-800 hover:bg-stone-100"
          >
            {t("backToCalculator")}
          </Link>
        </div>

        {loading ? (
          <div className="rounded-sm border p-6 shadow-sm border-slate-800 bg-white">
            {t("loading")}
          </div>
        ) : !user ? (
          <div className="rounded-sm border p-6 shadow-sm border-slate-800 bg-white">
            <p className="text-sm text-stone-600">{t("loginRequired")}</p>
            <Link
              href="/login"
              className="mt-4 inline-flex rounded bg-[#a32b2b] px-5 py-2.5 text-sm font-semibold text-[#ffffff] hover:bg-[#842020]"
            >
              {t("loginCta")}
            </Link>
          </div>
        ) : (
          <>
            <section className="rounded-sm border p-6 shadow-sm border-slate-800 bg-white">
              <h2 className="text-lg font-semibold">{t("accountInfo")}</h2>
              <dl className="mt-4 grid gap-3 text-sm sm:grid-cols-2">
                <div>
                  <dt className="text-stone-600">{t("nickname")}</dt>
                  <dd className="mt-1 font-medium">{user.nickname}</dd>
                </div>
                <div>
                  <dt className="text-stone-600">{t("email")}</dt>
                  <dd className="mt-1 font-medium">{user.email}</dd>
                </div>
              </dl>
            </section>

            <section className="rounded-sm border p-6 shadow-sm border-slate-800 bg-white">
              <h2 className="text-lg font-semibold">{t("credits")}</h2>
              <div className="mt-4 text-3xl font-bold text-[#a32b2b]">{credits?.balance ?? 0}</div>
              <div className="mt-4 divide-y rounded border divide-slate-800 border-slate-800">
                {(credits?.recentTransactions ?? []).slice(0, 5).map((item) => (
                  <div key={item.id} className="flex items-center justify-between px-4 py-3 text-sm">
                    <span className="text-stone-600">{item.description}</span>
                    <span className="font-semibold">{item.amount > 0 ? "+" : ""}{item.amount}</span>
                  </div>
                ))}
              </div>
            </section>

            <section className="rounded-sm border p-6 shadow-sm border-slate-800 bg-white">
              <h2 className="text-lg font-semibold">{t("history")}</h2>
              {projects.length === 0 ? (
                <p className="mt-4 text-sm text-stone-600">
                  {t("noHistory")}
                </p>
              ) : (
                <div className="mt-4 divide-y rounded border divide-slate-800 border-slate-800">
                  {projects.map((item) => (
                    <button
                      key={item.calculationId}
                      type="button"
                      onClick={() => openReport(item)}
                      className="flex w-full items-center justify-between gap-4 px-4 py-3 text-left text-sm hover:bg-stone-100"
                    >
                      <div>
                        <div className="font-medium">{item.projectName}</div>
                        <div className="mt-0.5 text-xs text-stone-600">
                          {item.region} · {formatDate(item.calculatedAt)}
                        </div>
                      </div>
                      <div className="shrink-0 text-right">
                        <div className="font-semibold">{formatUsd(item.totalMonthlyCostUsd)}</div>
                        <div className="text-xs text-stone-600">
                          {formatKrw(item.totalMonthlyCostKrw)}
                        </div>
                      </div>
                    </button>
                  ))}
                </div>
              )}
            </section>
          </>
        )}
      </div>

      {openedItem && reportData && (
        <div className="pointer-events-none fixed left-0 top-0 z-[-1] overflow-hidden" aria-hidden="true">
          <ShareCard
            ref={cardRef}
            projectName={openedItem.projectName}
            region={openedItem.region}
            calculation={reportData}
            optimization={null}
            diagramSnapshot={reportData.diagramSnapshot}
            resourceCatalog={resourceCatalog}
          />
        </div>
      )}

      {openedItem && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/45 p-4 backdrop-blur-sm">
          <div className="max-h-[90vh] w-full max-w-2xl overflow-hidden rounded-sm border shadow-2xl border-slate-800 bg-white">
            <div className="flex items-start justify-between gap-4 border-b border-stone-300 bg-stone-100 p-5 text-stone-900">
              <div>
                <div className="text-xs font-semibold uppercase text-[#a32b2b]">{t("reportLabel")}</div>
                <h3 className="mt-1 text-lg font-semibold">{openedItem.projectName}</h3>
                <p className="mt-1 text-sm text-stone-700">
                  {openedItem.region} · {formatDate(openedItem.calculatedAt)}
                </p>
              </div>
              <div className="flex shrink-0 items-start gap-2">
                {reportData && (
                  <button
                    type="button"
                    onClick={handleOpenShareModal}
                    disabled={generating}
                    className="rounded border border-stone-300 px-3 py-2 text-xs font-semibold text-stone-900 hover:bg-stone-200 disabled:opacity-50"
                  >
                    {generating ? t("exporting") : t("exportPdf")}
                  </button>
                )}
                <button
                  type="button"
                  onClick={closeReport}
                  className="grid h-8 w-8 shrink-0 place-items-center rounded border border-stone-300 text-sm text-stone-800 hover:bg-stone-200 hover:text-stone-900"
                >
                  x
                </button>
              </div>
            </div>

            <div className="max-h-[calc(90vh-104px)] overflow-y-auto p-5">
              {generateError && (
                <p className="mb-3 text-sm text-red-400">{generateError}</p>
              )}
              {reportLoading && (
                <p className="text-sm text-stone-600">{t("loading")}</p>
              )}
              {reportError && (
                <p className="text-sm text-red-400">{reportError}</p>
              )}
              {reportData && (
                // Deliberately light regardless of the app's dark theme — this is the same
                // "report" content that gets captured as the PDF/share card (ShareCard.tsx), so
                // it must always look like what gets exported: white background, black price card.
                <div className="flex flex-col gap-4 rounded-2xl border border-zinc-200 bg-white p-6 text-zinc-900">
                  <div className="flex flex-col items-center gap-1 rounded-2xl py-10 bg-white text-stone-900">
                    <span className="text-sm text-stone-600">{t("estimatedCost")}</span>
                    <span className="text-4xl font-bold">{formatUsd(reportData.totalMonthlyCost)}</span>
                    <span className="text-stone-600">
                      {formatKrw(reportData.totalMonthlyCostKrw)}
                    </span>
                  </div>

                  <div className="rounded-xl border border-zinc-200">
                    {reportData.resources.map((r, i) => (
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
                      <span>{formatUsd(reportData.totalMonthlyCost)}</span>
                    </div>
                  </div>

                  {reportData.recommendationMetadata && (
                    <div className="rounded-xl border p-5 border-indigo-200 bg-indigo-50">
                      <h3 className="flex items-center gap-2 font-medium text-indigo-900">
                        {t("aiRecommendedConfig")}
                        {reportData.recommendationMetadata.aiGenerated && (
                          <span className="rounded-full bg-indigo-600/10 px-2 py-0.5 text-xs font-normal text-indigo-700">
                            AI
                          </span>
                        )}
                      </h3>
                      <p className="mt-1 text-sm font-medium text-indigo-900">
                        {reportData.recommendationMetadata.tierName}
                      </p>
                      <p className="mt-1 text-sm text-indigo-800">
                        {reportData.recommendationMetadata.description}
                      </p>
                      <p className="mt-3 rounded-sm bg-white/60 p-3 text-sm text-indigo-900">
                        {reportData.recommendationMetadata.recommendationReason}
                      </p>
                      {reportData.recommendationMetadata.additionalRecommendations.length > 0 && (
                        <div className="mt-3 rounded-sm border p-3 text-sm border-amber-200 bg-amber-50 text-amber-900">
                          <div className="font-medium">{t("additionalRecommendations")}</div>
                          <ul className="mt-2 list-disc space-y-1 pl-5">
                            {reportData.recommendationMetadata.additionalRecommendations.map((item, index) => (
                              <li key={index}>{item}</li>
                            ))}
                          </ul>
                        </div>
                      )}
                    </div>
                  )}

                  {reportData.diagramSnapshot && (
                    <DiagramSnapshotView snapshot={reportData.diagramSnapshot} title={t("diagramTitle")} compact />
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {imageUrl && (
        <SharePdfModal
          imageUrl={imageUrl}
          fileName={fileName}
          shareSupported={canShareFiles()}
          sharing={sharing}
          shareError={shareError}
          onShare={handleShare}
          onDownload={handleDownload}
          onClose={handleCloseShareModal}
        />
      )}
    </main>
  );
}
