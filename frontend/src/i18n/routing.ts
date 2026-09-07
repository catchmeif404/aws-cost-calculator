import { defineRouting } from "next-intl/routing";

// Korean is the default and stays at clean URLs (/, /calculator, ...); English lives under
// /en (/en, /en/calculator, ...). This keeps every already-shared/indexed Korean URL unchanged.
export const routing = defineRouting({
  locales: ["ko", "en"],
  defaultLocale: "ko",
  localePrefix: "as-needed",
});

export type Locale = (typeof routing.locales)[number];
