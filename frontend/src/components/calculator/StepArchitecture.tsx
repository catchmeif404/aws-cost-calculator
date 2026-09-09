"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import type { ResourceCatalogItem } from "@/lib/api";
import { CalculatorForm, createResourceInstance, ResourceInstance } from "./formTypes";
import ResourceListSection from "./ResourceListSection";
import CatalogFieldInput from "./CatalogFieldInput";

interface Props {
  form: CalculatorForm;
  resourceCatalog: ResourceCatalogItem[];
  catalogError: string | null;
  onChange: (form: CalculatorForm) => void;
  onBack: () => void;
  onNext: () => void;
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="flex items-center justify-between gap-4 text-sm">
      <span className="text-stone-600">{label}</span>
      {children}
    </label>
  );
}

const selectClass =
  "rounded-sm border border-zinc-700 bg-white px-3 py-1.5 text-sm text-zinc-50 outline-none focus:border-zinc-900";
const numberClass = selectClass + " w-24";

// The primary/advanced split is a UX choice (keep the wizard's first screen short), not a
// per-type hardcode — everything not in this small starter set just falls into "advanced
// options", catalog item and all, with no per-type knowledge needed here.
const PRIMARY_KINDS = ["ecs", "rds", "redis", "s3", "alb"];

export default function StepArchitecture({ form, resourceCatalog, catalogError, onChange, onBack, onNext }: Props) {
  const t = useTranslations("calculator.stepArchitecture");
  const [showAdvanced, setShowAdvanced] = useState(false);
  const hasName = form.projectName.trim().length > 0;
  const hasResources = form.resources.length > 0;
  const canProceed = hasName && hasResources;

  const primaryItems = PRIMARY_KINDS.map((kind) => resourceCatalog.find((c) => c.kind === kind)).filter(
    (item): item is ResourceCatalogItem => item !== undefined
  );
  const advancedItems = resourceCatalog.filter((item) => !PRIMARY_KINDS.includes(item.kind));

  function itemsFor(kind: string): ResourceInstance[] {
    return form.resources.filter((r) => r.kind === kind);
  }

  function setItemsFor(kind: string, items: ResourceInstance[]) {
    onChange({ ...form, resources: [...form.resources.filter((r) => r.kind !== kind), ...items] });
  }

  function renderResourceSection(catalogItem: ResourceCatalogItem) {
    return (
      <ResourceListSection
        key={catalogItem.kind}
        title={catalogItem.title}
        items={itemsFor(catalogItem.kind)}
        onChange={(items) => setItemsFor(catalogItem.kind, items)}
        createDefault={() => createResourceInstance(catalogItem)}
        renderFields={(item, onItemChange) =>
          catalogItem.fields.length === 0 ? (
            <p className="text-sm text-stone-600">{catalogItem.description}</p>
          ) : (
            <>
              {catalogItem.fields.map((field) => (
                <Field key={field.key} label={field.label}>
                  <CatalogFieldInput
                    field={field}
                    config={item.config}
                    onChange={(config) => onItemChange({ ...item, config })}
                    className={field.type === "select" ? selectClass : numberClass}
                  />
                </Field>
              ))}
            </>
          )
        }
      />
    );
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="border-b border-stone-300 pb-5">
        <div className="flex items-center justify-between gap-4">
          <h2 className="text-xl font-semibold tracking-normal text-stone-900">
          {t("title")}
          </h2>
          <span className="text-xs text-stone-9000">02 / 03</span>
        </div>
        <p className="mt-2 text-sm text-stone-600">
          {t("subtitle")}
        </p>
      </div>

      {catalogError && (
        <p className="text-sm text-red-400">{catalogError}</p>
      )}

      {primaryItems.map(renderResourceSection)}

      {advancedItems.length > 0 && (
        <button
          type="button"
          onClick={() => setShowAdvanced(!showAdvanced)}
          className="self-start text-sm font-medium text-stone-600 underline-offset-2 hover:text-stone-800 hover:underline"
        >
          {showAdvanced
            ? t("advancedHide")
            : t("advancedShow", { items: advancedItems.map((item) => item.title).join(" / ") })}
        </button>
      )}

      {showAdvanced && advancedItems.map(renderResourceSection)}

      {!canProceed && (
        <p className="border-l-2 border-amber-400/70 bg-amber-400/5 px-3 py-2 text-sm text-amber-300">
          {!hasName && !hasResources && t("warnBoth")}
          {!hasName && hasResources && t("warnName")}
          {hasName && !hasResources && t("warnResources")}
        </p>
      )}

      <div className="mt-2 flex gap-3">
        <button
          onClick={onBack}
          className="rounded-sm border border-stone-300 px-5 py-3 font-medium text-stone-700 transition-colors hover:bg-stone-100"
        >
          {t("back")}
        </button>
        <button
          onClick={onNext}
          disabled={!canProceed}
          className="flex-1 rounded-sm bg-[#a32b2b] px-5 py-3 font-medium text-[#ffffff] transition-colors hover:bg-[#842020] disabled:cursor-not-allowed disabled:opacity-40"
        >
          {t("calculate")}
        </button>
      </div>
    </div>
  );
}
