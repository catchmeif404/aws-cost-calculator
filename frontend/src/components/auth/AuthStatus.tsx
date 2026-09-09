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
      <div className="flex flex-wrap items-center justify-end gap-2 text-sm font-semibold text-stone-900">
        <span className="rounded border border-stone-300 bg-stone-100 px-3 py-1.5">
          {t("credits", { name: user.nickname, balance: balance ?? 0 })}
        </span>
        {user.role === "ADMIN" && (
          <Link
            href="/admin"
            className="rounded border border-stone-300 px-3 py-1.5 hover:bg-stone-200"
          >
            {t("users")}
          </Link>
        )}
        <Link
          href="/mypage"
          className="rounded bg-[#a32b2b] px-3 py-1.5 font-semibold text-[#ffffff] hover:bg-[#842020]"
        >
          {t("mypage")}
        </Link>
        <button
          onClick={handleLogout}
          className="rounded border border-stone-300 px-3 py-1.5 hover:bg-stone-200"
        >
          {t("logout")}
        </button>
      </div>
    );
  }

  return (
    <Link
      href="/login"
      className="rounded border border-stone-300 bg-stone-100 px-4 py-2 text-sm font-semibold text-stone-900 hover:bg-stone-200"
    >
      {t("login")}
    </Link>
  );
}
