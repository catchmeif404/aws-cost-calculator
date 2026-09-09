import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import LanguageSwitcher from "@/components/landing/LanguageSwitcher";

export default function LandingPage() {
  const t = useTranslations("caseLanding");
  const common = useTranslations("common");
  return (
    <div className="case-landing">
      <nav className="archive-nav">
        <a href="https://catchmeif404.com">catchmeif404 / {t("archive")}</a>
        <LanguageSwitcher ariaLabel={common("language")} />
      </nav>
      <main className="case-document">
        <div className="case-reference"><span>REF. 0000-404 / A</span><span>{t("classification")}</span></div>
        <section className="case-cover">
          <div className="case-kicker">EXHIBIT A / {t("category")}</div>
          <h1>AWS CostPilot<span className="case-period">.</span></h1>
          <p className="case-lead">{t("lead")}</p>
          <p className="case-description">{t("description")}</p>
          <div className="case-metadata"><div><span>{t("subject")}</span><strong>AWS / {t("monthlyCost")}</strong></div><div><span>{t("author")}</span><strong className="redacted-name">{t("redacted")}</strong></div><span className="case-stamp">{t("open")}</span></div>
          <div className="case-actions"><Link className="case-primary" href="/calculator">{t("start")} <span aria-hidden="true">↗</span></Link><Link className="case-secondary" href="/login">{t("login")}</Link></div>
          <p className="case-note">{t("note")}</p>
        </section>
        <section className="case-methods">
          <div className="case-section-heading"><h2>{t("methods")}</h2><span>01 / 03</span></div>
          {(["wizard", "visual", "recommend"] as const).map((mode, i) => <Link className="case-method" key={mode} href={`/calculator?mode=${mode}`}><span className="case-method-number">0{i + 1}</span><div><h3>{t(`${mode}Title`)}</h3><p>{t(`${mode}Body`)}</p></div><span aria-hidden="true">↗</span></Link>)}
        </section>
        <footer className="case-footer"><p>{t("footer")}</p><span>catchmeif404 / EXHIBIT A</span></footer>
      </main>
    </div>
  );
}
