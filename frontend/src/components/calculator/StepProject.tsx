"use client";

import { useTranslations, useLocale } from "next-intl";
import { REGIONS, regionLabel } from "./formTypes";

interface Props {
  projectName: string;
  onChange: (name: string) => void;
  monthlyUsers: number;
  onMonthlyUsersChange: (monthlyUsers: number) => void;
  requestsPerUser: number;
  onRequestsPerUserChange: (requestsPerUser: number) => void;
  busyTrafficLevel: string;
  onBusyTrafficLevelChange: (busyTrafficLevel: string) => void;
  serviceStage: string;
  onServiceStageChange: (serviceStage: string) => void;
  region: string;
  onRegionChange: (region: string) => void;
  onNext: () => void;
  onSwitchToRecommend: () => void;
}

export default function StepProject({
  projectName,
  onChange,
  monthlyUsers,
  onMonthlyUsersChange,
  requestsPerUser,
  onRequestsPerUserChange,
  busyTrafficLevel,
  onBusyTrafficLevelChange,
  serviceStage,
  onServiceStageChange,
  region,
  onRegionChange,
  onNext,
  onSwitchToRecommend,
}: Props) {
  const t = useTranslations("calculator");
  const locale = useLocale();
  const canProceed = projectName.trim().length > 0;

  return (
    <div className="flex flex-col gap-5">
      <div className="overflow-hidden rounded-lg border shadow-sm border-slate-800 bg-slate-950">
        <div className="border-b bg-[#232f3e] px-5 py-4 text-white border-slate-800">
          <div className="text-xs font-semibold uppercase text-[#ff9900]">{t("stepProject.stepLabel")}</div>
          <h2 className="mt-1 text-lg font-semibold">{t("stepProject.title")}</h2>
          <p className="mt-1 text-sm text-slate-300">
            {t("stepProject.subtitle")}
          </p>
        </div>

        <div className="grid gap-4 p-5 lg:grid-cols-2">
      <label className="flex flex-col gap-2 lg:col-span-2">
        <span className="text-sm font-medium text-zinc-300">
          {t("projectFields.serviceName")}
        </span>
        <input
          type="text"
          value={projectName}
          onChange={(e) => onChange(e.target.value)}
          placeholder={t("projectFields.namePlaceholder")}
          className="rounded-lg border px-4 py-2.5 outline-none border-zinc-700 bg-zinc-900 text-zinc-50 focus:border-zinc-400"
        />
      </label>

      <div className="grid gap-4 sm:grid-cols-2">
        <label className="flex flex-col gap-2">
          <span className="text-sm font-medium text-zinc-300">
            {t("projectFields.monthlyUsers")}
          </span>
          <input
            type="number"
            min={0}
            value={monthlyUsers}
            onChange={(e) => onMonthlyUsersChange(Number(e.target.value))}
            className="rounded-lg border px-4 py-2.5 outline-none border-zinc-700 bg-zinc-900 text-zinc-50 focus:border-zinc-400"
          />
        </label>

        <label className="flex flex-col gap-2">
          <span className="text-sm font-medium text-zinc-300">
            {t("projectFields.requestsPerUser")}
          </span>
          <input
            type="number"
            min={0}
            value={requestsPerUser}
            onChange={(e) => onRequestsPerUserChange(Number(e.target.value))}
            className="rounded-lg border px-4 py-2.5 outline-none border-zinc-700 bg-zinc-900 text-zinc-50 focus:border-zinc-400"
          />
        </label>
      </div>

      <label className="flex flex-col gap-2">
        <span className="text-sm font-medium text-zinc-300">
          {t("projectFields.busyTraffic")}
        </span>
        <select
          value={busyTrafficLevel}
          onChange={(e) => onBusyTrafficLevelChange(e.target.value)}
          className="rounded-lg border px-4 py-2.5 outline-none border-zinc-700 bg-zinc-900 text-zinc-50 focus:border-zinc-400"
        >
          <option value="similar">{t("projectFields.trafficSimilar")}</option>
          <option value="two_to_three_times">{t("projectFields.trafficTwoToThree")}</option>
          <option value="five_plus_times">{t("projectFields.trafficFivePlus")}</option>
          <option value="unknown">{t("projectFields.trafficUnknown")}</option>
        </select>
      </label>

      <label className="flex flex-col gap-2">
        <span className="text-sm font-medium text-zinc-300">
          {t("projectFields.serviceStage")}
        </span>
        <select
          value={serviceStage}
          onChange={(e) => onServiceStageChange(e.target.value)}
          className="rounded-lg border px-4 py-2.5 outline-none border-zinc-700 bg-zinc-900 text-zinc-50 focus:border-zinc-400"
        >
          <option value="toy">{t("projectFields.stageToy")}</option>
          <option value="mvp">{t("projectFields.stageMvp")}</option>
          <option value="production">{t("projectFields.stageProduction")}</option>
          <option value="critical">{t("projectFields.stageCritical")}</option>
        </select>
      </label>

      <label className="flex flex-col gap-2 lg:col-span-2">
        <span className="text-sm font-medium text-zinc-300">
          {t("projectFields.region")}
        </span>
        <select
          value={region}
          onChange={(e) => onRegionChange(e.target.value)}
          className="rounded-lg border px-4 py-2.5 outline-none border-zinc-700 bg-zinc-900 text-zinc-50 focus:border-zinc-400"
        >
          {REGIONS.map((r) => (
            <option key={r.code} value={r.code}>
              {regionLabel(r.code, locale)}
            </option>
          ))}
        </select>
      </label>
        </div>
      </div>

      <div className="flex flex-col-reverse gap-3 sm:flex-row sm:items-center sm:justify-between">
        <button
          onClick={onSwitchToRecommend}
          className="text-sm font-medium underline-offset-2 hover:underline text-zinc-400"
        >
          {t("stepProject.switchToRecommend")}
        </button>
        <button
          onClick={onNext}
          disabled={!canProceed}
          className="rounded bg-[#ff9900] px-6 py-3 font-semibold text-[#161e2d] transition-colors hover:bg-[#f2a100] disabled:cursor-not-allowed disabled:opacity-40"
        >
          {t("stepProject.next")}
        </button>
      </div>
    </div>
  );
}
