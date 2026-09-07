"use client";

import type { ResourceCatalogField } from "@/lib/api";

interface Props {
  field: ResourceCatalogField;
  config: Record<string, unknown>;
  onChange: (config: Record<string, unknown>) => void;
  className?: string;
}

// ECS's "computePreset" field is a synthetic UI-only control (see ResourceCatalogService on the
// backend) — its value ("0.5:1") isn't itself a config key. Picking an option sets `cpu` and
// `memory` directly instead of writing back a literal `computePreset` key, which CostEngine
// wouldn't understand. Every other field is fully generic: it reads/writes `config[field.key]`.
const COMPUTE_PRESET_KEY = "computePreset";

// Exported so other views (e.g. the visual builder's canvas node summary line) can display a
// field's current value with the same computePreset-aware resolution, without duplicating it.
export function catalogFieldValue(field: ResourceCatalogField, config: Record<string, unknown>): string | number {
  if (field.key === COMPUTE_PRESET_KEY) {
    return `${config.cpu ?? ""}:${config.memory ?? ""}`;
  }
  const value = config[field.key];
  return (value as string | number | undefined) ?? (field.defaultValue as string | number);
}

export default function CatalogFieldInput({ field, config, onChange, className }: Props) {
  const value = catalogFieldValue(field, config);

  if (field.type === "select") {
    return (
      <select
        className={className}
        value={String(value)}
        onChange={(event) => {
          const raw = event.target.value;
          if (field.key === COMPUTE_PRESET_KEY) {
            const [cpu, memory] = raw.split(":").map(Number);
            onChange({ ...config, cpu, memory });
            return;
          }
          const option = field.options.find((o) => String(o.value) === raw);
          onChange({ ...config, [field.key]: option ? option.value : raw });
        }}
      >
        {field.options.map((option) => (
          <option key={String(option.value)} value={String(option.value)}>
            {option.label}
          </option>
        ))}
      </select>
    );
  }

  return (
    <input
      type="number"
      className={className}
      value={Number(value)}
      min={field.min ?? undefined}
      max={field.max ?? undefined}
      step={field.step ?? undefined}
      onChange={(event) => onChange({ ...config, [field.key]: Number(event.target.value) })}
    />
  );
}
