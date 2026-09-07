import { createNavigation } from "next-intl/navigation";
import { routing } from "./routing";

// Locale-aware replacements for next/link and next/navigation — Link automatically prefixes
// hrefs with /en when the current locale is English, so every internal link keeps working
// without hardcoding the locale in each href.
export const { Link, redirect, usePathname, useRouter, getPathname } = createNavigation(routing);
