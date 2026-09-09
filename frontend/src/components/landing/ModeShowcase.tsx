import { useTranslations } from "next-intl";

const RESOURCE_EXAMPLES = ["ECS", "RDS", "Redis", "ALB", "S3", "Lambda", "DynamoDB", "CloudFront"];

function ResultStrip({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between rounded-sm bg-white px-4 py-3 text-sm">
      <span className="text-stone-600">{label}</span>
      <span className="font-bold text-stone-900">{value}</span>
    </div>
  );
}

function ResourceTags({ more }: { more: string }) {
  return (
    <div className="mt-6 flex flex-wrap gap-2">
      {RESOURCE_EXAMPLES.map((r) => (
        <span key={r} className="rounded-full border border-stone-300 px-3 py-1 text-xs text-stone-700">
          {r}
        </span>
      ))}
      <span className="rounded-full border border-stone-300 px-3 py-1 text-xs text-stone-9000">{more}</span>
    </div>
  );
}

// A form mockup with the wizard's actual field labels/placeholders (StepProject.tsx) plus its
// resource picker (StepArchitecture.tsx) — the 정보 입력 flow is really two steps, info then
// resources, so showing only the info form undersold what actually happens before you get a price.
function WizardMock({ t, resultLabel }: { t: ReturnType<typeof useTranslations<"modeShowcase.wizard">>; resultLabel: string }) {
  const resources = [
    { label: "ECS", name: "ECS Fargate", detail: "0.5 vCPU · 1GB · Task ×2", price: "$66.33", bg: "bg-orange-100", text: "text-orange-800", border: "border-orange-200" },
    { label: "RDS", name: "RDS", detail: "db.t4g.micro · 20GB", price: "$41.16", bg: "bg-blue-100", text: "text-blue-800", border: "border-blue-200" },
    { label: "ALB", name: "ALB", detail: "Internet-facing · LCU 1", price: "$22.27", bg: "bg-violet-100", text: "text-violet-800", border: "border-violet-200" },
  ];
  return (
    <div className="rounded-xl border border-stone-300 bg-white p-5 text-zinc-900 shadow-2xl">
      <p className="text-xs font-semibold uppercase tracking-wide text-[#a32b2b]">{t("step1")}</p>
      <div className="mt-3 flex items-center gap-3 text-xs">
        <span className="rounded-sm border border-zinc-200 px-2.5 py-1.5 text-stone-600">{t("namePlaceholder")}</span>
        <span className="rounded-sm border border-zinc-200 px-2.5 py-1.5 font-medium text-zinc-900">{t("usersValue")}</span>
        <span className="rounded-sm border border-zinc-200 px-2.5 py-1.5 font-medium text-zinc-900">{t("requestsValue")}</span>
      </div>

      <p className="mt-4 text-xs font-semibold uppercase tracking-wide text-[#a32b2b]">{t("step2")}</p>
      <div className="mt-3 flex flex-col gap-2">
        {resources.map((r) => (
          <div key={r.name} className="flex items-center justify-between rounded-sm border border-zinc-200 p-2">
            <div className="flex items-center gap-2">
              <span className={`grid h-8 w-10 shrink-0 place-items-center rounded-sm border text-[10px] font-bold ${r.bg} ${r.text} ${r.border}`}>
                {r.label}
              </span>
              <div>
                <div className="text-xs font-semibold text-zinc-900">{r.name}</div>
                <div className="text-[11px] text-zinc-500">{r.detail}</div>
              </div>
            </div>
            <span className="text-xs font-medium text-zinc-500">{r.price}</span>
          </div>
        ))}
        <div className="rounded-sm border border-dashed border-zinc-300 px-3 py-2 text-center text-[11px] text-stone-600">
          {t("addResource")}
        </div>
      </div>

      <div className="mt-4 flex justify-center text-stone-700">↓</div>
      <ResultStrip label={resultLabel} value="$147.28 · ₩203,246" />
    </div>
  );
}

// A canvas mockup with the same resource-node coloring as VisualArchitectureBuilder's real
// drag-and-drop canvas (diagramSnapshot.ts's category tones).
function DragBuilderMock({ t, resultLabel }: { t: ReturnType<typeof useTranslations<"modeShowcase.dragBuilder">>; resultLabel: string }) {
  const nodes = [
    { label: "ECS", sub: t("ecs"), bg: "bg-orange-100", text: "text-orange-800", border: "border-orange-200" },
    { label: "RDS", sub: t("rds"), bg: "bg-blue-100", text: "text-blue-800", border: "border-blue-200" },
    { label: "Redis", sub: t("redis"), bg: "bg-blue-100", text: "text-blue-800", border: "border-blue-200" },
    { label: "ALB", sub: t("alb"), bg: "bg-violet-100", text: "text-violet-800", border: "border-violet-200" },
  ];
  return (
    <div className="rounded-xl border border-stone-300 bg-white p-5 text-zinc-900 shadow-2xl">
      <p className="text-xs font-semibold uppercase tracking-wide text-[#a32b2b]">{t("diagramTitle")}</p>
      <div className="relative mt-4 rounded-sm border border-dashed border-slate-300 bg-[linear-gradient(#e5e7eb_1px,transparent_1px),linear-gradient(90deg,#e5e7eb_1px,transparent_1px)] bg-[size:20px_20px] p-4">
        <div className="grid grid-cols-2 gap-3">
          {nodes.map((n) => (
            <div key={n.label} className={`flex items-center gap-2 rounded-sm border p-2 shadow-sm ${n.bg} ${n.border}`}>
              <span className={`grid h-8 w-10 shrink-0 place-items-center rounded-sm border bg-white text-[10px] font-bold ${n.border} ${n.text}`}>
                {n.label}
              </span>
              <span className="min-w-0">
                <span className="block truncate text-xs font-semibold text-zinc-900">{n.label}</span>
                <span className="block truncate text-[10px] text-zinc-500">{n.sub}</span>
              </span>
            </div>
          ))}
        </div>
      </div>
      <div className="mt-4 flex justify-center text-stone-700">↓</div>
      <ResultStrip label={resultLabel} value="$147.28 · ₩203,246" />
    </div>
  );
}

// A description-in / architecture-out mockup matching RecommendPanel's actual flow: a free-text
// service description goes in, an AI-picked architecture comes out.
function AiRecommendMock({ t, resultLabel }: { t: ReturnType<typeof useTranslations<"modeShowcase.aiRecommend">>; resultLabel: string }) {
  return (
    <div className="rounded-xl border border-stone-300 bg-white p-5 text-zinc-900 shadow-2xl">
      <p className="text-xs font-semibold uppercase tracking-wide text-[#a32b2b]">{t("descriptionLabel")}</p>
      <p className="mt-2 rounded-sm border border-zinc-200 bg-zinc-50 px-3 py-2.5 text-sm text-zinc-700">
        {t("descriptionExample")}
      </p>
      <div className="mt-3 flex justify-center text-stone-700">↓ 🤖</div>
      <div className="rounded-sm border border-indigo-200 bg-indigo-50 p-3 text-sm">
        <p className="font-semibold text-indigo-900">{t("recommendationTitle")}</p>
        <p className="mt-1 text-xs text-indigo-800">{t("recommendationDetail")}</p>
      </div>
      <div className="mt-4 flex justify-center text-stone-700">↓</div>
      <ResultStrip label={resultLabel} value="$147.28 · ₩203,246" />
    </div>
  );
}

export default function ModeShowcase() {
  const t = useTranslations("modeShowcase");
  const tWizard = useTranslations("modeShowcase.wizard");
  const tDrag = useTranslations("modeShowcase.dragBuilder");
  const tAi = useTranslations("modeShowcase.aiRecommend");
  const resultLabel = t("resultLabel");

  const modes = [
    {
      key: "wizard",
      eyebrow: tWizard("eyebrow"),
      title: [tWizard("titleLine1"), tWizard("titleLine2")],
      body: tWizard("body"),
      node: <WizardMock t={tWizard} resultLabel={resultLabel} />,
      mockOrder: "lg:order-2",
      textOrder: "lg:order-1",
      showTags: true,
    },
    {
      key: "drag",
      eyebrow: tDrag("eyebrow"),
      title: [tDrag("titleLine1"), tDrag("titleLine2")],
      body: tDrag("body"),
      node: <DragBuilderMock t={tDrag} resultLabel={resultLabel} />,
      mockOrder: "lg:order-1",
      textOrder: "lg:order-2",
      showTags: true,
    },
    {
      key: "ai",
      eyebrow: tAi("eyebrow"),
      title: [tAi("titleLine1"), tAi("titleLine2")],
      body: tAi("body"),
      node: <AiRecommendMock t={tAi} resultLabel={resultLabel} />,
      mockOrder: "lg:order-2",
      textOrder: "lg:order-1",
      showTags: false,
    },
  ];

  return (
    <section id="features" className="border-t border-stone-300 bg-[#0e1626] py-20">
      <div className="mx-auto max-w-6xl px-4 sm:px-6 lg:px-8">
        <div className="text-center">
          <span className="text-xs font-semibold uppercase tracking-[0.2em] text-[#a32b2b]">
            {t("eyebrow")}
          </span>
          <h2 className="mx-auto mt-4 max-w-2xl text-3xl font-bold text-stone-900">
            {t("title")}
          </h2>
        </div>

        <div className="mt-16 flex flex-col gap-16">
          {modes.map((mode) => (
            <div key={mode.key} className="grid gap-10 lg:grid-cols-2 lg:items-center">
              <div className={mode.textOrder}>
                <span className="text-xs font-semibold uppercase tracking-[0.2em] text-[#a32b2b]">
                  {mode.eyebrow}
                </span>
                <h3 className="mt-4 text-3xl font-bold text-balance text-stone-900">
                  {mode.title[0]}
                  <br />
                  {mode.title[1]}
                </h3>
                <p className="mt-4 text-stone-600">{mode.body}</p>
                {mode.showTags && <ResourceTags more={t("moreResources")} />}
              </div>
              <div className={mode.mockOrder}>{mode.node}</div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
