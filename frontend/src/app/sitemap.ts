import type { MetadataRoute } from "next";
import { routing } from "@/i18n/routing";

const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "https://aws-costpilot.trade";

// Public, unauthenticated pages only — /mypage and /admin require login and shouldn't be
// indexed, so they're deliberately left out.
const PATHS = ["", "/calculator", "/login"];

function localizedPath(path: string, locale: string) {
  if (locale === routing.defaultLocale) {
    return `${SITE_URL}${path}` || SITE_URL;
  }
  return `${SITE_URL}/${locale}${path}`;
}

export default function sitemap(): MetadataRoute.Sitemap {
  return PATHS.map((path) => ({
    url: localizedPath(path, routing.defaultLocale),
    lastModified: new Date(),
    alternates: {
      languages: Object.fromEntries(routing.locales.map((locale) => [locale, localizedPath(path, locale)])),
    },
  }));
}
