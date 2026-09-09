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
      <div className="overflow-hidden border-y border-stone-300 bg-[#ffffff]">
        <div className="border-b border-stone-300 bg-[#eceee9] px-5 py-5 text-stone-900 sm:px-6">
          <div className="flex items-center justify-between gap-4">
            <div className="text-xs font-semibold uppercase tracking-[0.16em] text-[#a32b2b]">{t("stepProject.stepLabel")}</div>
            <span className="text-xs text-stone-9000">01 / 03</span>
          </div>
          <h2 className="mt-2 text-xl font-semibold tracking-normal">{t("stepProject.title")}</h2>
          <p className="mt-1 text-sm text-stone-700">
            {t("stepProject.subtitle")}
          </p>
        </div>

        <div className="grid gap-4 p-5 sm:p-6 lg:grid-cols-2">
      <label className="flex flex-col gap-2 lg:col-span-2">
        <span className="text-xs font-semibold uppercase tracking-wide text-stone-600">
          {t("projectFields.serviceName")}
        </span>
        <input
          type="text"
          value={projectName}
          onChange={(e) => onChange(e.target.value)}
          placeholder={t("projectFields.namePlaceholder")}
          className="rounded-sm border border-stone-300 bg-[#f7f7f4] px-4 py-3 text-sm text-stone-900 outline-none transition-colors placeholder:text-slate-600 focus:border-[#a32b2b]/70 focus:ring-2 focus:ring-[#a32b2b]/10"
        />
      </label>

      <div className="grid gap-4 sm:grid-cols-2">
        <label className="flex flex-col gap-2">
          <span className="text-xs font-semibold uppercase tracking-wide text-stone-600">
            {t("projectFields.monthlyUsers")}
          </span>
          <input
            type="number"
            min={0}
            value={monthlyUsers}
            onChange={(e) => onMonthlyUsersChange(Number(e.target.value))}
            className="rounded-sm border border-stone-300 bg-[#f7f7f4] px-4 py-3 text-sm text-stone-900 outline-none transition-colors focus:border-[#a32b2b]/70 focus:ring-2 focus:ring-[#a32b2b]/10"
          />
        </label>

        <label className="flex flex-col gap-2">
          <span className="text-xs font-semibold uppercase tracking-wide text-stone-600">
            {t("projectFields.requestsPerUser")}
          </span>
          <input
            type="number"
            min={0}
            value={requestsPerUser}
            onChange={(e) => onRequestsPerUserChange(Number(e.target.value))}
            className="rounded-sm border border-stone-300 bg-[#f7f7f4] px-4 py-3 text-sm text-stone-900 outline-none transition-colors focus:border-[#a32b2b]/70 focus:ring-2 focus:ring-[#a32b2b]/10"
          />
        </label>
      </div>

      <label className="flex flex-col gap-2">
        <span className="text-xs font-semibold uppercase tracking-wide text-stone-600">
          {t("projectFields.busyTraffic")}
        </span>
        <select
          value={busyTrafficLevel}
          onChange={(e) => onBusyTrafficLevelChange(e.target.value)}
          className="rounded-sm border border-stone-300 bg-[#f7f7f4] px-4 py-3 text-sm text-stone-900 outline-none transition-colors focus:border-[#a32b2b]/70 focus:ring-2 focus:ring-[#a32b2b]/10"
        >
          <option value="similar">{t("projectFields.trafficSimilar")}</option>
          <option value="two_to_three_times">{t("projectFields.trafficTwoToThree")}</option>
          <option value="five_plus_times">{t("projectFields.trafficFivePlus")}</option>
          <option value="unknown">{t("projectFields.trafficUnknown")}</option>
        </select>
      </label>

      <label className="flex flex-col gap-2">
        <span className="text-xs font-semibold uppercase tracking-wide text-stone-600">
          {t("projectFields.serviceStage")}
        </span>
        <select
          value={serviceStage}
          onChange={(e) => onServiceStageChange(e.target.value)}
          className="rounded-sm border border-stone-300 bg-[#f7f7f4] px-4 py-3 text-sm text-stone-900 outline-none transition-colors focus:border-[#a32b2b]/70 focus:ring-2 focus:ring-[#a32b2b]/10"
        >
          <option value="toy">{t("projectFields.stageToy")}</option>
          <option value="mvp">{t("projectFields.stageMvp")}</option>
          <option value="production">{t("projectFields.stageProduction")}</option>
          <option value="critical">{t("projectFields.stageCritical")}</option>
        </select>
      </label>

      <label className="flex flex-col gap-2 lg:col-span-2">
        <span className="text-xs font-semibold uppercase tracking-wide text-stone-600">
          {t("projectFields.region")}
        </span>
        <select
          value={region}
          onChange={(e) => onRegionChange(e.target.value)}
          className="rounded-sm border border-stone-300 bg-[#f7f7f4] px-4 py-3 text-sm text-stone-900 outline-none transition-colors focus:border-[#a32b2b]/70 focus:ring-2 focus:ring-[#a32b2b]/10"
        >
          {REGIONS.map((r) => (
            <option key={r.code} value={r.code}>
              {regionLabel(r.code, locale)}
            </option>
          ))}
        </select>
      </label>
        </div>
        <div className="grid gap-3 border-t border-stone-300 bg-[#f1f2ee] px-5 py-4 sm:grid-cols-3 sm:px-6">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-wide text-stone-9000">{t("projectFields.region")}</p>
            <p className="mt-1 truncate text-sm font-medium text-stone-800">{regionLabel(region, locale)}</p>
          </div>
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-wide text-stone-9000">{t("projectFields.monthlyUsers")}</p>
            <p className="mt-1 text-sm font-medium text-stone-800">{monthlyUsers.toLocaleString(locale)}</p>
          </div>
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-wide text-stone-9000">{t("projectFields.serviceStage")}</p>
            <p className="mt-1 text-sm font-medium capitalize text-stone-800">{serviceStage}</p>
          </div>
        </div>
      </div>

      <div className="flex flex-col-reverse gap-3 sm:flex-row sm:items-center sm:justify-between">
        <button
          onClick={onSwitchToRecommend}
          className="text-sm font-medium text-stone-600 underline-offset-2 hover:text-stone-800 hover:underline"
        >
          {t("stepProject.switchToRecommend")}
        </button>
        <button
          onClick={onNext}
          disabled={!canProceed}
          className="rounded-sm bg-[#a32b2b] px-6 py-3 font-semibold text-[#ffffff] transition-colors hover:bg-[#842020] disabled:cursor-not-allowed disabled:opacity-40"
        >
          {t("stepProject.next")}
        </button>
      </div>
    </div>
  );
}
