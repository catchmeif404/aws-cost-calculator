"use client";

import { useTranslations } from "next-intl";
import { Link, useRouter } from "@/i18n/navigation";
import LoginModal from "@/components/auth/LoginModal";

export default function LoginPage() {
  const t = useTranslations("auth.loginPage");
  const router = useRouter();

  return (
    <div className="flex min-h-screen items-center justify-center px-4 bg-[#0b1220]">
      <LoginModal open onClose={() => router.push("/calculator")} onSuccess={() => router.push("/calculator")} />
      <Link
        href="/calculator"
        className="fixed bottom-8 text-xs font-medium underline-offset-2 hover:underline text-slate-400"
      >
        {t("browseWithoutLogin")}
      </Link>
    </div>
  );
}
