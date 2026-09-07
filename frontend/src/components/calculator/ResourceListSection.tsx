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
    <div className="rounded-xl border p-5 border-zinc-800">
      <div className="flex items-center justify-between">
        <h3 className="font-medium text-zinc-50">{title}</h3>
        <button
          type="button"
          onClick={addItem}
          className="text-sm font-medium text-zinc-400 hover:text-zinc-50"
        >
          {resolvedAddLabel}
        </button>
      </div>

      {items.length === 0 ? (
        <p className="mt-3 text-sm text-zinc-500">{resolvedEmptyHint}</p>
      ) : (
        <div className="mt-4 flex flex-col gap-4">
          {items.map((item, i) => (
            <div
              key={item._key}
              className="rounded-lg border p-3 border-zinc-900"
            >
              <div className="mb-2 flex items-center justify-between">
                <span className="text-xs font-medium text-zinc-400">
                  {title} #{i + 1}
                </span>
                <button
                  type="button"
                  onClick={() => removeItem(item._key)}
                  className="text-xs text-zinc-400 hover:text-red-400"
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
