export async function captureCardCanvas(element: HTMLElement): Promise<HTMLCanvasElement> {
  // html2canvas-pro (not the classic html2canvas) — Tailwind v4's default palette uses
  // oklch() colors, which the original html2canvas 1.x can't parse and fails on.
  const { default: html2canvas } = await import("html2canvas-pro");
  return html2canvas(element, {
    scale: 2,
    backgroundColor: "#ffffff",
    useCORS: true,
  });
}

export async function canvasToPdfBlob(canvas: HTMLCanvasElement): Promise<Blob> {
  const { jsPDF } = await import("jspdf");
  const imageData = canvas.toDataURL("image/png");

  // Convert the captured pixel size to PDF points (72pt = 1in, canvas is rendered at 96dpi * scale).
  const pxToPt = 72 / 96;
  const widthPt = (canvas.width / 2) * pxToPt;
  const heightPt = (canvas.height / 2) * pxToPt;

  const pdf = new jsPDF({
    orientation: heightPt > widthPt ? "portrait" : "landscape",
    unit: "pt",
    format: [widthPt, heightPt],
  });

  pdf.addImage(imageData, "PNG", 0, 0, widthPt, heightPt);
  return pdf.output("blob");
}

export function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

export function canShareFiles(): boolean {
  if (typeof navigator === "undefined" || !navigator.share) return false;
  if (!navigator.canShare) return true; // assume best-effort support
  try {
    const probe = new File([""], "probe.pdf", { type: "application/pdf" });
    return navigator.canShare({ files: [probe] });
  } catch {
    return false;
  }
}

/** Returns true if the OS share sheet was opened (including if the user cancelled it). */
export async function sharePdfBlob(
  blob: Blob,
  filename: string,
  title: string
): Promise<boolean> {
  if (!navigator.share) return false;
  const file = new File([blob], filename, { type: "application/pdf" });
  try {
    await navigator.share({ files: [file], title });
    return true;
  } catch (e) {
    if (e instanceof Error && e.name === "AbortError") return true; // user cancelled, not an error
    throw e;
  }
}
