"use client";

import { ReactNode } from "react";
import { useTranslations } from "next-intl";

interface Props<T extends { _key: string }> {
  title: string;
  items: T[];
  onChange: (items: T[]) => void;
  createDefault: () => T;
  renderFields: (item: T, onItemChange: (item: T) => void) => ReactNode;
  addLabel?: string;
  emptyHint?: string;
}

export default function ResourceListSection<T extends { _key: string }>({
  title,
  items,
  onChange,
  createDefault,
  renderFields,
  addLabel,
  emptyHint,
}: Props<T>) {
  const t = useTranslations("calculator.resourceListSection");
  const resolvedAddLabel = addLabel ?? t("add");
  const resolvedEmptyHint = emptyHint ?? t("notUsed");
  function addItem() {
    onChange([...items, createDefault()]);
  }

  function updateItem(key: string, updated: T) {
    onChange(items.map((item) => (item._key === key ? updated : item)));
  }

  function removeItem(key: string) {
    onChange(items.filter((item) => item._key !== key));
  }

  return (
    <div className="rounded-sm border border-stone-300 bg-[#ffffff] p-5">
      <div className="flex items-center justify-between">
        <h3 className="font-semibold text-stone-900">{title}</h3>
        <button
          type="button"
          onClick={addItem}
          className="rounded-sm border border-[#a32b2b]/30 px-3 py-1.5 text-xs font-semibold text-[#842020] transition-colors hover:bg-[#a32b2b]/10"
        >
          {resolvedAddLabel}
        </button>
      </div>

      {items.length === 0 ? (
        <p className="mt-3 border border-dashed border-stone-300 px-3 py-4 text-sm text-stone-9000">{resolvedEmptyHint}</p>
      ) : (
        <div className="mt-4 flex flex-col gap-4">
          {items.map((item, i) => (
            <div
              key={item._key}
              className="rounded-sm border border-stone-300 bg-[#f1f2ee] p-3"
            >
              <div className="mb-2 flex items-center justify-between">
                <span className="text-xs font-medium uppercase tracking-wide text-stone-9000">
                  {title} #{i + 1}
                </span>
                <button
                  type="button"
                  onClick={() => removeItem(item._key)}
                  className="text-xs text-stone-9000 transition-colors hover:text-red-300"
                >
                  {t("delete")}
                </button>
              </div>
              <div className="flex flex-col gap-3">
                {renderFields(item, (updated) => updateItem(item._key, updated))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
