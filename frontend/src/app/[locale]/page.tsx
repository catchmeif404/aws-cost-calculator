import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import ModeShowcase from "@/components/landing/ModeShowcase";
import LanguageSwitcher from "@/components/landing/LanguageSwitcher";

export default function LandingPage() {
  const t = useTranslations("landing");
  const personas = useTranslations("landing.personas").raw("items") as {
    title: string;
    body: string;
    points: string[];
  }[];

  return (
    <div className="min-h-screen bg-[#0b1220] text-slate-100">
      <header className="sticky top-0 z-10 border-b border-white/10 bg-[#0b1220]/90 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-4 sm:px-6 lg:px-8">
          <span className="text-sm font-bold tracking-tight text-white">AWS CostPilot</span>
          <nav className="flex items-center gap-2 sm:gap-3">
            <a href="#features" className="hidden px-3 py-2 text-sm text-slate-300 hover:text-white sm:inline-block">
              {t("nav.features")}
            </a>
            <a href="#personas" className="hidden px-3 py-2 text-sm text-slate-300 hover:text-white sm:inline-block">
              {t("nav.personas")}
            </a>
            <Link
              href="/login"
              className="hidden rounded border border-white/15 bg-white/5 px-4 py-2 text-sm font-semibold text-slate-100 hover:bg-white/10 sm:inline-block"
            >
              {t("nav.login")}
            </Link>
            <Link
              href="/calculator"
              className="rounded bg-[#ff9900] px-4 py-2 text-sm font-semibold text-[#161e2d] hover:bg-[#f2a100]"
            >
              {t("nav.cta")}
            </Link>
            <LanguageSwitcher />
          </nav>
        </div>
      </header>

      <main>
        {/* Hero */}
        <section className="mx-auto max-w-6xl px-4 pt-20 pb-16 text-center sm:px-6 lg:px-8">
          <span className="text-xs font-semibold uppercase tracking-[0.2em] text-[#ff9900]">
            {t("hero.eyebrow")}
          </span>
          <h1 className="mx-auto mt-5 max-w-3xl text-4xl font-black leading-tight tracking-tight text-white sm:text-5xl">
            {t("hero.titleLine1")}
            <br />
            {t("hero.titleLine2")}
          </h1>
          <p className="mx-auto mt-6 max-w-2xl text-base leading-relaxed text-slate-400 sm:text-lg">
            {t("hero.body")}
          </p>
          <div className="mt-9 flex flex-wrap items-center justify-center gap-3">
            <Link
              href="/calculator"
              className="rounded bg-[#ff9900] px-6 py-3 text-sm font-semibold text-[#161e2d] hover:bg-[#f2a100]"
            >
              {t("hero.ctaPrimary")}
            </Link>
            <a
              href="#features"
              className="rounded border border-white/15 px-6 py-3 text-sm font-semibold text-slate-200 hover:bg-white/5"
            >
              {t("hero.ctaSecondary")}
            </a>
          </div>

          <dl className="mx-auto mt-16 grid max-w-2xl grid-cols-3 gap-6 border-t border-white/10 pt-10">
            <div>
              <dt className="sr-only">{t("hero.statResources")}</dt>
              <dd className="text-3xl font-bold text-white">45+</dd>
              <p className="mt-1 text-xs uppercase tracking-wide text-slate-500">{t("hero.statResources")}</p>
            </div>
            <div>
              <dt className="sr-only">{t("hero.statRegions")}</dt>
              <dd className="text-3xl font-bold text-white">5</dd>
              <p className="mt-1 text-xs uppercase tracking-wide text-slate-500">{t("hero.statRegions")}</p>
            </div>
            <div>
              <dt className="sr-only">{t("hero.statCurrency")}</dt>
              <dd className="text-3xl font-bold text-white">USD·KRW</dd>
              <p className="mt-1 text-xs uppercase tracking-wide text-slate-500">{t("hero.statCurrency")}</p>
            </div>
          </dl>
        </section>

        {/* Features — the 3 calculator modes, each landing on the same exported report */}
        <ModeShowcase />

        {/* Feature 3 — optimization & explanation */}
        <section className="border-t border-white/10 py-20">
          <div className="mx-auto max-w-6xl px-4 text-center sm:px-6 lg:px-8">
            <span className="text-xs font-semibold uppercase tracking-[0.2em] text-[#ff9900]">
              {t("optimization.eyebrow")}
            </span>
            <h2 className="mx-auto mt-4 max-w-2xl text-3xl font-bold text-white">
              {t("optimization.title")}
            </h2>
            <p className="mx-auto mt-4 max-w-2xl text-slate-400">
              {t("optimization.body")}
            </p>
            <div className="mx-auto mt-10 grid max-w-3xl gap-4 sm:grid-cols-2">
              <div className="rounded-lg border border-white/10 bg-[#0b1220] p-5 text-left">
                <p className="text-sm font-semibold text-emerald-400">{t("optimization.optimizationLabel")}</p>
                <p className="mt-2 text-sm text-slate-400">
                  {t("optimization.optimizationExample")}
                </p>
              </div>
              <div className="rounded-lg border border-white/10 bg-[#0b1220] p-5 text-left">
                <p className="text-sm font-semibold text-sky-400">{t("optimization.explanationLabel")}</p>
                <p className="mt-2 text-sm text-slate-400">
                  {t("optimization.explanationExample")}
                </p>
              </div>
            </div>
          </div>
        </section>

        {/* Personas */}
        <section id="personas" className="border-t border-white/10 py-20">
          <div className="mx-auto max-w-6xl px-4 sm:px-6 lg:px-8">
            <div className="text-center">
              <span className="text-xs font-semibold uppercase tracking-[0.2em] text-[#ff9900]">{t("personas.eyebrow")}</span>
              <h2 className="mx-auto mt-4 max-w-2xl text-3xl font-bold text-white">
                {t("personas.title")}
              </h2>
            </div>
            <div className="mt-12 grid gap-6 lg:grid-cols-3">
              {personas.map((persona) => (
                <div key={persona.title} className="rounded-xl border border-white/10 bg-[#0e1626] p-6">
                  <h3 className="text-lg font-bold text-white">{persona.title}</h3>
                  <p className="mt-2 text-sm text-slate-400">{persona.body}</p>
                  <ul className="mt-5 flex flex-col gap-2.5 border-t border-white/10 pt-5">
                    {persona.points.map((point) => (
                      <li key={point} className="flex gap-2 text-sm text-slate-300">
                        <span className="text-[#ff9900]">✓</span>
                        <span>{point}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* Final CTA */}
        <section className="border-t border-white/10 bg-[#0e1626] py-20">
          <div className="mx-auto max-w-2xl px-4 text-center sm:px-6 lg:px-8">
            <h2 className="text-3xl font-bold text-white">{t("finalCta.title")}</h2>
            <p className="mt-3 text-slate-400">{t("finalCta.body")}</p>
            <Link
              href="/calculator"
              className="mt-8 inline-flex rounded bg-[#ff9900] px-8 py-3.5 text-sm font-semibold text-[#161e2d] hover:bg-[#f2a100]"
            >
              {t("finalCta.cta")}
            </Link>
          </div>
        </section>
      </main>

      <footer className="border-t border-white/10 py-10">
        <div className="mx-auto flex max-w-6xl flex-col items-center gap-3 px-4 text-center sm:px-6 lg:px-8">
          <span className="text-sm font-bold text-white">AWS CostPilot</span>
          <p className="text-xs text-slate-500">{t("footer.tagline")}</p>
          <div className="mt-2 flex gap-4 text-xs text-slate-500">
            <Link href="/calculator" className="hover:text-slate-300">{t("footer.calculator")}</Link>
            <Link href="/login" className="hover:text-slate-300">{t("footer.login")}</Link>
          </div>
          <p className="mt-4 text-xs text-slate-600">{t("footer.copyright")}</p>
        </div>
      </footer>
    </div>
  );
}
