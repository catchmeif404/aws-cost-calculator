"use client";

import { useEffect } from "react";
import { pingSiteVisit } from "@/lib/api";

// Mounted once in the root layout so every page load (any route) counts as a site visit.
export default function SiteVisitTracker() {
  useEffect(() => {
    pingSiteVisit();
  }, []);

  return null;
}
