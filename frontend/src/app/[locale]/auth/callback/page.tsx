"use client";

import { useEffect } from "react";
import { useTranslations } from "next-intl";
import { useRouter } from "@/i18n/navigation";
import { setToken } from "@/lib/api";

export default function AuthCallbackPage() {
  const t = useTranslations("auth.authCallback");
  const router = useRouter();

  useEffect(() => {
    const hash = window.location.hash;
    const match = hash.match(/token=([^&]+)/);
    if (match) {
      setToken(decodeURIComponent(match[1]));
    }
    router.replace("/calculator");
  }, [router]);

  return (
    <div className="flex min-h-screen items-center justify-center text-sm text-zinc-500 dark:text-zinc-400">
      {t("processing")}
    </div>
  );
}
