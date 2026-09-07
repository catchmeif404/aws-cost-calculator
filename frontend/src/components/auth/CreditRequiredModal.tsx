"use client";

import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";

interface Props {
  open: boolean;
  onClose: () => void;
}

export default function CreditRequiredModal({ open, onClose }: Props) {
  const t = useTranslations("auth.creditModal");
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
      <div className="w-full max-w-md overflow-hidden rounded-lg border border-slate-200 bg-white shadow-2xl dark:border-slate-800 dark:bg-slate-950">
        <div className="bg-[#232f3e] px-6 py-5 text-white">
          <div className="text-xs font-semibold uppercase text-[#ff9900]">Credits</div>
          <h2 className="mt-1 text-lg font-semibold">{t("title")}</h2>
          <p className="mt-1 text-sm text-slate-300">
            {t("subtitle")}
          </p>
        </div>

        <div className="grid gap-4 p-6 text-sm text-slate-600 dark:text-slate-300">
          <p className="leading-6">
            {t("body")}
          </p>
          <div className="grid grid-cols-2 gap-2">
            <button
              type="button"
              onClick={onClose}
              className="rounded border border-slate-300 px-4 py-2.5 font-semibold text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:text-slate-200 dark:hover:bg-slate-900"
            >
              {t("close")}
            </button>
            <Link
              href="/mypage"
              onClick={onClose}
              className="rounded bg-[#ff9900] px-4 py-2.5 text-center font-semibold text-[#161e2d] hover:bg-[#f2a100]"
            >
              {t("mypage")}
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
