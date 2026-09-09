"use client";

import { useTransition } from "react";
import { useLocale, useTranslations } from "next-intl";
import { usePathname, useRouter } from "@/i18n/navigation";
import { routing, type Locale } from "@/i18n/routing";

const LABELS: Record<Locale, string> = {
  ko: "한국어",
  en: "English",
};

export default function LanguageSwitcher() {
  const locale = useLocale() as Locale;
  const t = useTranslations("common");
  const pathname = usePathname();
  const router = useRouter();
  const [pending, startTransition] = useTransition();

  function switchTo(nextLocale: Locale) {
    if (nextLocale === locale) return;
    startTransition(() => {
      router.replace(`${pathname}${window.location.search}${window.location.hash}`, {
        locale: nextLocale,
        scroll: false,
      });
    });
  }

  return (
    <div
      className="inline-flex items-center rounded-full border border-white/15 bg-white/5 p-1"
      role="tablist"
      aria-label={t("language")}
    >
      {routing.locales.map((loc) => (
        <button
          key={loc}
          type="button"
          role="tab"
          aria-selected={loc === locale}
          disabled={pending}
          onClick={() => switchTo(loc)}
          className={`rounded-full px-2.5 py-1 text-xs transition-colors sm:px-3 ${
            loc === locale
              ? "bg-[#ff9900] font-semibold text-[#161e2d]"
              : "text-slate-300 hover:bg-white/10 hover:text-white"
          }`}
        >
          {LABELS[loc]}
        </button>
      ))}
    </div>
  );
}
