"use client";

import { useCallback, useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import {
  addResource,
  ApiError,
  calculate,
  CalculationResponse,
  createProject,
  getResourceCatalog,
  RecommendationMetadata,
  ResourceCatalogItem,
  getToken,
} from "@/lib/api";
import LoginModal from "@/components/auth/LoginModal";
import {
  CalculatorForm,
  CategoryLaneLayout,
  createResourceInstance,
  DEFAULT_FORM,
  ResourceInstance,
  STARTER_RESOURCE_KINDS,
} from "./formTypes";
import StepProject from "./StepProject";
import StepArchitecture from "./StepArchitecture";
import StepResult from "./StepResult";
import RecommendPanel, { AppliedRecommendation } from "./RecommendPanel";
import VisualArchitectureBuilder from "./VisualArchitectureBuilder";
import type { DiagramSnapshot } from "./diagramSnapshot";

type Step = 1 | 2 | 3;
type Mode = "wizard" | "visual" | "recommend";

function buildStarterResources(catalog: ResourceCatalogItem[]): ResourceInstance[] {
  return STARTER_RESOURCE_KINDS.map((kind) => catalog.find((c) => c.kind === kind))
    .filter((item): item is ResourceCatalogItem => item !== undefined)
    .map(createResourceInstance);
}

export default function Calculator() {
  const t = useTranslations("calculator");
  const [mode, setMode] = useState<Mode>("wizard");
  const [step, setStep] = useState<Step>(1);
  const [form, setForm] = useState<CalculatorForm>(DEFAULT_FORM);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [projectId, setProjectId] = useState<number | null>(null);
  const [calculation, setCalculation] = useState<CalculationResponse | null>(null);

  const [resourceCatalog, setResourceCatalog] = useState<ResourceCatalogItem[]>([]);
  const [catalogError, setCatalogError] = useState<string | null>(null);
  const [loginModalOpen, setLoginModalOpen] = useState(false);
  const [visualDiagramSnapshot, setVisualDiagramSnapshot] = useState<DiagramSnapshot | null>(null);
  const [visualInitialLayout, setVisualInitialLayout] = useState<CategoryLaneLayout | undefined>(undefined);
  const [recommendationMetadata, setRecommendationMetadata] = useState<RecommendationMetadata | null>(null);

  useEffect(() => {
    let ignore = false;

    getResourceCatalog()
      .then((catalog) => {
        if (!ignore) {
          setResourceCatalog(catalog);
          setCatalogError(null);
          // Seed the same starter architecture the old hardcoded DEFAULT_FORM used to ship
          // with, but only once — and only if the user hasn't already added/removed anything
          // while this was loading. Can't be synchronous anymore since the catalog is async.
          setForm((current) =>
            current.resources.length === 0
              ? { ...current, resources: buildStarterResources(catalog) }
              : current
          );
        }
      })
      .catch((e) => {
        if (!ignore) {
          setCatalogError(e instanceof ApiError ? e.message : t("catalogError"));
        }
      });

    return () => {
      ignore = true;
    };
  }, [t]);

  function applyRecommendation(recommendation: AppliedRecommendation) {
    setForm((current) => ({
      ...current,
      projectName: recommendation.projectName,
      monthlyUsers: recommendation.monthlyUsers,
      requestsPerUser: recommendation.requestsPerUser,
      busyTrafficLevel: recommendation.busyTrafficLevel,
      serviceStage: recommendation.serviceStage,
      region: recommendation.region,
      resources: recommendation.resources,
    }));
    setVisualInitialLayout(recommendation.initialLayout);
    setRecommendationMetadata(recommendation.recommendationMetadata);
    // If the user already calculated once from the drag builder earlier in this session, `step`
    // is left at 3 from that — without resetting it here, mode="visual" + step===3 would match
    // the result-screen condition below and show that OLD calculation instead of the visual
    // builder for the just-applied recommendation.
    setProjectId(null);
    setCalculation(null);
    setError(null);
    setStep(2);
    setMode("visual");
  }

  function requireAuth() {
    if (getToken()) {
      return true;
    }
    setLoginModalOpen(true);
    return false;
  }

  const handleDiagramChange = useCallback((snapshot: DiagramSnapshot | null) => {
    setVisualDiagramSnapshot(snapshot);
  }, []);

  // No default parameter here on purpose: a default like `= form` silently breaks if this
  // function is ever wired directly to `onClick` (React passes the click SyntheticEvent as the
  // first argument, which would override the intended default). Always pass `form` explicitly.
  async function runCalculation(inputForm: CalculatorForm) {
    if (!requireAuth()) {
      return;
    }

    setForm(inputForm);
    // visualDiagramSnapshot/recommendationMetadata are only meaningful while still in the drag-
    // builder flow they came from — calculating from the wizard flow instead should carry neither
    // forward. The setState calls below only take effect on the next render, so diagramToSend/
    // metadataToSend (used further down, for the actual API call) have to gate on `mode` the same
    // way rather than reading the state variables directly.
    const diagramToSend = mode === "visual" ? visualDiagramSnapshot : null;
    const metadataToSend = mode === "visual" ? recommendationMetadata : null;
    if (mode !== "visual") {
      setVisualDiagramSnapshot(null);
      setRecommendationMetadata(null);
    }

    if (!inputForm.projectName.trim()) {
      setStep(3);
      setLoading(false);
      setError(t("errors.emptyName"));
      setProjectId(null);
      setCalculation(null);
      return;
    }

    setStep(3);
    setLoading(true);
    setError(null);
    setProjectId(null);
    setCalculation(null);

    try {
      const project = await createProject({
        name: inputForm.projectName,
        region: inputForm.region,
        monthlyUsers: inputForm.monthlyUsers,
        requestsPerUser: inputForm.requestsPerUser,
        busyTrafficLevel: inputForm.busyTrafficLevel,
        serviceStage: inputForm.serviceStage,
      });
      setProjectId(project.id);

      for (const item of inputForm.resources) {
        await addResource(project.id, item.type, item.config);
      }

      const calcResult = await calculate(project.id, diagramToSend, metadataToSend);
      setCalculation(calcResult);
      setLoading(false);
    } catch (e) {
      // Calculating (createProject/addResource/calculate) never spends credits, so there's no
      // 402 case to special-case here — just the generic error path. Cost optimization is a
      // separate, credit-gated action the user triggers from StepResult after this succeeds.
      setError(e instanceof ApiError ? e.message : t("errors.unknown"));
      setLoading(false);
    }
  }

  function restart() {
    setForm({ ...DEFAULT_FORM, resources: buildStarterResources(resourceCatalog) });
    if (mode !== "visual") {
      setVisualDiagramSnapshot(null);
      setRecommendationMetadata(null);
    }
    setProjectId(null);
    setCalculation(null);
    setError(null);
    setStep(1);
  }

  const shellClass = mode === "visual" ? "w-full" : "mx-auto w-full max-w-6xl";
  const tabClass = (targetMode: Mode) =>
    `rounded-md px-3 py-2 transition-colors ${
      mode === targetMode
        ? "bg-[#ff9900] text-[#161e2d] shadow-sm"
        : "text-slate-200 hover:bg-white/10 hover:text-white"
    }`;

  return (
    <div className="w-full">
      <LoginModal
        open={loginModalOpen}
        onClose={() => setLoginModalOpen(false)}
        onSuccess={() => window.location.reload()}
      />
      <div className={`${shellClass} mb-6 grid grid-cols-3 rounded-lg border border-slate-700 bg-[#232f3e] p-1 text-sm font-semibold shadow-sm`}>
        <button
          type="button"
          onClick={() => {
            setMode("wizard");
            setStep(step === 3 ? 1 : step);
          }}
          className={tabClass("wizard")}
        >
          {t("tabs.wizard")}
        </button>
        <button
          type="button"
          onClick={() => setMode("visual")}
          className={tabClass("visual")}
        >
          {t("tabs.visual")}
        </button>
        <button
          type="button"
          onClick={() => setMode("recommend")}
          className={tabClass("recommend")}
        >
          {t("tabs.recommend")}
        </button>
      </div>

      {mode === "visual" && step !== 3 && (
        <VisualArchitectureBuilder
          form={form}
          resourceCatalog={resourceCatalog}
          catalogError={catalogError}
          onChange={setForm}
          onCalculate={runCalculation}
          onEditDetails={() => {
            setMode("wizard");
            setStep(2);
          }}
          onDiagramChange={handleDiagramChange}
          initialRects={visualInitialLayout?.rects}
          initialGroups={visualInitialLayout?.groups}
          initialConnections={visualInitialLayout?.connections}
        />
      )}

      {mode === "recommend" && (
        <div className={shellClass}>
          <RecommendPanel
            onBack={() => setMode("wizard")}
            onRequireAuth={requireAuth}
            onApply={applyRecommendation}
            resourceCatalog={resourceCatalog}
          />
        </div>
      )}

      {mode === "wizard" && step !== 3 && (
        <div className={shellClass}>
      <div className="mb-6 flex items-center gap-2">
        {[1, 2, 3].map((s) => (
          <div
            key={s}
            className={`h-1.5 flex-1 rounded-full ${ s <= step ? "bg-[#ff9900]" : "bg-slate-200 bg-slate-800" }`}
          />
        ))}
      </div>

      {step === 1 && (
        <StepProject
          projectName={form.projectName}
          onChange={(projectName) => setForm({ ...form, projectName })}
          monthlyUsers={form.monthlyUsers}
          onMonthlyUsersChange={(monthlyUsers) => setForm({ ...form, monthlyUsers })}
          requestsPerUser={form.requestsPerUser}
          onRequestsPerUserChange={(requestsPerUser) => setForm({ ...form, requestsPerUser })}
          busyTrafficLevel={form.busyTrafficLevel}
          onBusyTrafficLevelChange={(busyTrafficLevel) => setForm({ ...form, busyTrafficLevel })}
          serviceStage={form.serviceStage}
          onServiceStageChange={(serviceStage) => setForm({ ...form, serviceStage })}
          region={form.region}
          onRegionChange={(region) => setForm({ ...form, region })}
          onNext={() => setStep(2)}
          onSwitchToRecommend={() => setMode("recommend")}
        />
      )}

      {step === 2 && (
        <StepArchitecture
          form={form}
          resourceCatalog={resourceCatalog}
          catalogError={catalogError}
          onChange={setForm}
          onBack={() => setStep(1)}
          onNext={() => runCalculation(form)}
        />
      )}
        </div>
      )}

      {mode !== "recommend" && step === 3 && (
        <div className={shellClass}>
        <StepResult
          loading={loading}
          error={error}
          projectId={projectId}
          onRequireAuth={requireAuth}
          calculation={calculation}
          projectName={form.projectName}
          region={form.region}
          diagramSnapshot={visualDiagramSnapshot}
          resourceCatalog={resourceCatalog}
          onBack={() => setStep(2)}
          onRestart={restart}
        />
        </div>
      )}
    </div>
  );
}
