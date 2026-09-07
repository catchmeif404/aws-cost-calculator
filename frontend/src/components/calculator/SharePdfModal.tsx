"use client";

import { useTranslations } from "next-intl";

interface Props {
  imageUrl: string;
  fileName: string;
  shareSupported: boolean;
  sharing: boolean;
  shareError: string | null;
  onShare: () => void;
  onDownload: () => void;
  onClose: () => void;
}

export default function SharePdfModal({
  imageUrl,
  fileName,
  shareSupported,
  sharing,
  shareError,
  onShare,
  onDownload,
  onClose,
}: Props) {
  const t = useTranslations("calculator.sharePdfModal");
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4"
      onClick={onClose}
    >
      {/* Camera-flash + photo-pop entrance, scoped to this modal only. */}
      <style>{`
        @keyframes share-modal-flash {
          0% { opacity: 0.9; }
          100% { opacity: 0; }
        }
        @keyframes share-modal-pop {
          0% { transform: scale(0.82) translateY(10px); opacity: 0; }
          60% { transform: scale(1.03) translateY(0); opacity: 1; }
          100% { transform: scale(1) translateY(0); opacity: 1; }
        }
      `}</style>

      <div
        className="flex max-h-[90vh] w-full max-w-md flex-col overflow-hidden rounded-2xl shadow-xl bg-zinc-900"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between border-b px-5 py-3 border-zinc-800">
          <h3 className="font-medium text-zinc-50">{t("title")}</h3>
          <button
            onClick={onClose}
            aria-label={t("close")}
            className="rounded-full p-1 text-zinc-400 hover:bg-zinc-800 hover:text-zinc-200"
          >
            ✕
          </button>
        </div>

        <div className="relative min-h-0 flex-1 overflow-auto p-4 bg-zinc-950">
          <img
            src={imageUrl}
            alt={fileName}
            className="mx-auto w-full max-w-sm rounded-lg shadow-lg"
            style={{ animation: "share-modal-pop 420ms cubic-bezier(0.22,1,0.36,1)" }}
          />
          <div
            className="pointer-events-none absolute inset-0 bg-white"
            style={{ animation: "share-modal-flash 350ms ease-out forwards" }}
          />
        </div>

        <div className="flex flex-col gap-2 border-t p-4 border-zinc-800">
          {shareSupported ? (
            <button
              onClick={onShare}
              disabled={sharing}
              className="w-full rounded-full px-4 py-3 text-sm font-semibold disabled:opacity-50 bg-[#ff9900] text-[#161e2d] hover:bg-[#f2a100]"
            >
              {sharing ? t("sharing") : t("share")}
            </button>
          ) : (
            <p className="text-center text-xs text-zinc-400">
              {t("shareUnsupported")}
            </p>
          )}
          <button
            onClick={onDownload}
            className="w-full rounded-full border px-4 py-3 text-sm font-medium border-zinc-700 text-zinc-300 hover:bg-zinc-800"
          >
            {t("download")}
          </button>
          {shareError && <p className="text-center text-xs text-red-500">{shareError}</p>}
        </div>
      </div>
    </div>
  );
}
