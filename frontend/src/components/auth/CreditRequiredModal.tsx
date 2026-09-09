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
      <div className="w-full max-w-md overflow-hidden rounded-sm border border-slate-200 bg-white shadow-2xl dark:border-slate-800 dark:bg-white">
        <div className="bg-stone-100 px-6 py-5 text-stone-900">
          <div className="text-xs font-semibold uppercase text-[#a32b2b]">Credits</div>
          <h2 className="mt-1 text-lg font-semibold">{t("title")}</h2>
          <p className="mt-1 text-sm text-stone-700">
            {t("subtitle")}
          </p>
        </div>

        <div className="grid gap-4 p-6 text-sm text-slate-600 dark:text-stone-700">
          <p className="leading-6">
            {t("body")}
          </p>
          <div className="grid grid-cols-2 gap-2">
            <button
              type="button"
              onClick={onClose}
              className="rounded border border-slate-300 px-4 py-2.5 font-semibold text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:text-stone-800 dark:hover:bg-stone-100"
            >
              {t("close")}
            </button>
            <Link
              href="/mypage"
              onClick={onClose}
              className="rounded bg-[#a32b2b] px-4 py-2.5 text-center font-semibold text-[#ffffff] hover:bg-[#842020]"
            >
              {t("mypage")}
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
