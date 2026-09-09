import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import Calculator from "@/components/calculator/Calculator";
import AuthStatus from "@/components/auth/AuthStatus";
import LanguageSwitcher from "@/components/landing/LanguageSwitcher";

export default function CalculatorPage() {
  const t = useTranslations("calculatorPage");
  const common = useTranslations("common");
  return (
    <div className="flex min-h-screen flex-1 flex-col bg-[#f7f7f4] text-stone-900">
      <header className="border-b border-stone-300 bg-[#f1f2ee] px-4 py-4 text-stone-900 sm:px-6 lg:px-10">
        <div className="mx-auto flex w-full max-w-7xl flex-wrap items-center justify-between gap-4">
          <div className="min-w-0">
            <Link href="/" className="inline-flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.16em] text-[#a32b2b]">
              <span className="grid h-6 w-6 place-items-center border border-[#a32b2b] text-[10px] text-[#a32b2b]">A</span>
              {t("brand")}
            </Link>
            <h1 className="mt-3 text-xl font-bold tracking-normal sm:text-2xl">{t("title")}</h1>
            <p className="mt-1 max-w-2xl text-sm text-stone-600">
              {t("subtitle")}
            </p>
          </div>
          <div className="flex shrink-0 flex-wrap items-center gap-3">
            <LanguageSwitcher ariaLabel={common("language")} />
            <AuthStatus />
          </div>
        </div>
      </header>
      <main className="w-full flex-1 px-4 py-6 sm:px-6 lg:px-10 lg:py-8">
        <div className="mx-auto max-w-7xl">
          <div className="mb-6 flex items-center justify-between gap-4 border-b border-stone-300 pb-4">
            <div>
              <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-stone-9000">{t("workEyebrow")}</p>
              <p className="mt-1 text-sm text-stone-600">{t("workBody")}</p>
            </div>
            <div className="hidden items-center gap-2 text-xs text-stone-9000 sm:flex">
              {t("catalogStatus")}
            </div>
          </div>
          <Calculator />
        </div>
      </main>
    </div>
  );
}
