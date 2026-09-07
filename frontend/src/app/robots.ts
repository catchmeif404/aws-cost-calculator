import type { MetadataRoute } from "next";

const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "https://aws-costpilot.trade";

export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: "*",
      allow: "/",
      // Signed-in-only areas: the calculation history page and the user-management admin panel.
      disallow: ["/mypage", "/admin", "/en/mypage", "/en/admin"],
    },
    sitemap: `${SITE_URL}/sitemap.xml`,
  };
}
