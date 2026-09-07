import createMiddleware from "next-intl/middleware";
import { routing } from "./i18n/routing";

export default createMiddleware(routing);

export const config = {
  // Match every path except static assets, Next internals, and API-like files (icons, etc.)
  matcher: ["/((?!api|_next|_vercel|.*\\..*).*)"],
};
