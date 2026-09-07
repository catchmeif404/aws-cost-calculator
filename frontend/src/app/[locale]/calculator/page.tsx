import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import Calculator from "@/components/calculator/Calculator";
import AuthStatus from "@/components/auth/AuthStatus";

export default function CalculatorPage() {
  const t = useTranslations("calculatorPage");
  return (
    <div className="flex min-h-screen flex-1 flex-col bg-[#0b1220] text-slate-50">
      <header className="border-b border-white/10 bg-[#161e2d] px-4 py-5 text-white sm:px-6 lg:px-8">
        <div className="flex w-full flex-wrap items-center justify-between gap-4">
          <div className="min-w-0">
            <Link href="/" className="text-xs font-semibold uppercase text-[#ff9900]">
              {t("brand")}
            </Link>
            <h1 className="mt-2 text-2xl font-bold">{t("title")}</h1>
            <p className="mt-1 text-sm text-slate-300">
              {t("subtitle")}
            </p>
          </div>
          <div className="shrink-0">
            <AuthStatus />
          </div>
        </div>
      </header>
      <main className="px-4 py-6 sm:px-6 lg:px-8">
        <Calculator />
      </main>
    </div>
  );
}
