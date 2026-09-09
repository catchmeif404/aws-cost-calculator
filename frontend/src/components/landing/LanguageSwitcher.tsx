"use client";

import { useTransition } from "react";
import { useLocale } from "next-intl";
import { usePathname } from "@/i18n/navigation";
import { routing, type Locale } from "@/i18n/routing";

const LABELS: Record<Locale, string> = {
  ko: "한국어",
  en: "English",
};

export default function LanguageSwitcher({ ariaLabel }: { ariaLabel: string }) {
  const pathname = usePathname();
  const [pending, startTransition] = useTransition();
  const locale = useLocale() as Locale;

  function switchTo(nextLocale: Locale) {
    if (nextLocale === locale) return;
    startTransition(() => {
      document.cookie = `NEXT_LOCALE=${nextLocale}; Path=/; Max-Age=31536000; SameSite=Lax`;
      // An explicit locale URL also updates the locale cookie through middleware.
      window.location.assign(`/${nextLocale}${pathname === "/" ? "" : pathname}${window.location.search}${window.location.hash}`);
    });
  }

  return (
    <div
      className="inline-flex items-center rounded-full border border-stone-300 bg-stone-100 p-1"
      role="tablist"
      aria-label={ariaLabel}
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
              ? "bg-[#a32b2b] font-semibold text-[#ffffff]"
              : "text-stone-700 hover:bg-stone-200 hover:text-stone-900"
          }`}
        >
          {LABELS[loc]}
        </button>
      ))}
    </div>
  );
}
