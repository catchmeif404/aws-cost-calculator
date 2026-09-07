"use client";

import { useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import {
  clearToken,
  CREDITS_CHANGED_EVENT,
  getMe,
  getMyCredits,
  getToken,
  UserResponse,
} from "@/lib/api";

export default function AuthStatus() {
  const t = useTranslations("authStatus");
  const [user, setUser] = useState<UserResponse | null>(null);
  const [balance, setBalance] = useState<number | null>(null);

  useEffect(() => {
    if (!getToken()) return;

    function refreshCredits() {
      getMyCredits()
        .then((credits) => setBalance(credits.balance))
        .catch(() => undefined);
    }

    Promise.all([getMe(), getMyCredits()])
      .then(([me, credits]) => {
        setUser(me);
        setBalance(credits.balance);
      })
      .catch(() => {
        // Stale/invalid token — fall back to the logged-out state.
        clearToken();
        setUser(null);
      });

    window.addEventListener(CREDITS_CHANGED_EVENT, refreshCredits);
    return () => window.removeEventListener(CREDITS_CHANGED_EVENT, refreshCredits);
  }, []);

  function handleLogout() {
    clearToken();
    window.location.reload();
  }

  if (user) {
    return (
      <div className="flex flex-wrap items-center justify-end gap-2 text-sm font-semibold text-slate-100">
        <span className="rounded border border-white/15 bg-white/5 px-3 py-1.5">
          {t("credits", { name: user.nickname, balance: balance ?? 0 })}
        </span>
        {user.role === "ADMIN" && (
          <Link
            href="/admin"
            className="rounded border border-white/20 px-3 py-1.5 hover:bg-white/10"
          >
            {t("users")}
          </Link>
        )}
        <Link
          href="/mypage"
          className="rounded bg-[#ff9900] px-3 py-1.5 font-semibold text-[#161e2d] hover:bg-[#f2a100]"
        >
          {t("mypage")}
        </Link>
        <button
          onClick={handleLogout}
          className="rounded border border-white/20 px-3 py-1.5 hover:bg-white/10"
        >
          {t("logout")}
        </button>
      </div>
    );
  }

  return (
    <Link
      href="/login"
      className="rounded border border-white/15 bg-white/5 px-4 py-2 text-sm font-semibold text-slate-100 hover:bg-white/10"
    >
      {t("login")}
    </Link>
  );
}
