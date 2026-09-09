"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { ApiError, GOOGLE_LOGIN_URL, login, setToken, signup } from "@/lib/api";

interface Props {
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export default function LoginModal({ open, onClose, onSuccess }: Props) {
  const t = useTranslations("auth.loginModal");
  const [mode, setMode] = useState<"login" | "signup">("login");
  const [email, setEmail] = useState("");
  const [nickname, setNickname] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!open) return null;

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const response =
        mode === "login"
          ? await login({ email, password })
          : await signup({ email, nickname, password });
      setToken(response.token);
      onSuccess();
      onClose();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : t("failed"));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
      <div className="w-full max-w-md overflow-hidden rounded-sm border shadow-2xl border-slate-800 bg-white">
        <div className="flex items-start justify-between gap-4 bg-stone-100 px-6 py-5 text-stone-900">
          <div>
            <div className="text-xs font-semibold uppercase text-[#a32b2b]">{t("brand")}</div>
            <h2 className="mt-1 text-lg font-semibold">
              {mode === "login" ? t("loginTitle") : t("signupTitle")}
            </h2>
            <p className="mt-1 text-sm text-stone-700">
              {t("subtitle")}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="grid h-8 w-8 place-items-center rounded border border-stone-300 text-sm text-stone-800 hover:bg-stone-200"
          >
            x
          </button>
        </div>

        <form onSubmit={handleSubmit} className="grid gap-4 p-6">
          <label className="grid gap-2">
            <span className="text-sm font-medium text-stone-700">{t("email")}</span>
            <input
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
              className="rounded border px-3 py-2 text-sm outline-none focus:border-[#a32b2b] border-slate-700 bg-stone-100 text-stone-900"
              placeholder="you@example.com"
            />
          </label>

          {mode === "signup" && (
            <label className="grid gap-2">
              <span className="text-sm font-medium text-stone-700">{t("nickname")}</span>
              <input
                value={nickname}
                onChange={(event) => setNickname(event.target.value)}
                className="rounded border px-3 py-2 text-sm outline-none focus:border-[#a32b2b] border-slate-700 bg-stone-100 text-stone-900"
                placeholder={t("nicknamePlaceholder")}
              />
            </label>
          )}

          <label className="grid gap-2">
            <span className="text-sm font-medium text-stone-700">{t("password")}</span>
            <input
              type="password"
              minLength={8}
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
              className="rounded border px-3 py-2 text-sm outline-none focus:border-[#a32b2b] border-slate-700 bg-stone-100 text-stone-900"
              placeholder={t("passwordPlaceholder")}
            />
          </label>

          {error && (
            <div className="rounded border px-3 py-2 text-sm border-red-900 bg-red-950 text-red-300">
              {error}
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="rounded bg-[#a32b2b] px-4 py-2.5 text-sm font-semibold text-[#ffffff] hover:bg-[#842020] disabled:opacity-50"
          >
            {loading ? t("processing") : mode === "login" ? t("login") : t("signup")}
          </button>

          <button
            type="button"
            onClick={() => {
              setMode(mode === "login" ? "signup" : "login");
              setError(null);
            }}
            className="text-sm font-medium underline-offset-2 hover:underline text-stone-600"
          >
            {mode === "login" ? t("noAccount") : t("haveAccount")}
          </button>
        </form>

        <div className="border-t p-6 border-slate-800">
          <a
            href={GOOGLE_LOGIN_URL}
            className="flex items-center justify-center gap-3 rounded border px-4 py-2.5 text-sm font-medium transition border-slate-600 bg-stone-100 text-stone-900 hover:bg-stone-200"
          >
            <svg width="18" height="18" viewBox="0 0 18 18" aria-hidden="true">
              <path fill="#4285F4" d="M17.64 9.2045c0-.6381-.0573-1.2518-.1636-1.8409H9v3.4814h4.8436c-.2086 1.125-.8427 2.0782-1.7959 2.7164v2.2581h2.9087c1.7018-1.5668 2.6836-3.874 2.6836-6.615z" />
              <path fill="#34A853" d="M9 18c2.43 0 4.4673-.8059 5.9564-2.1805l-2.9087-2.2581c-.8059.54-1.8368.8591-3.0477.8591-2.3441 0-4.3282-1.5831-5.036-3.7104H.9573v2.3318C2.4382 15.9832 5.4818 18 9 18z" />
              <path fill="#FBBC05" d="M3.964 10.71c-.18-.54-.2822-1.1168-.2822-1.71s.1023-1.17.2823-1.71V4.9582H.9573A8.9965 8.9965 0 000 9c0 1.4523.3477 2.8268.9573 4.0418L3.964 10.71z" />
              <path fill="#EA4335" d="M9 3.5795c1.3214 0 2.5077.4541 3.4405 1.346l2.5813-2.5814C13.4632.8918 11.426 0 9 0 5.4818 0 2.4382 2.0168.9573 4.9582L3.964 7.29C4.6718 5.1627 6.6559 3.5795 9 3.5795z" />
            </svg>
            {t("continueWithGoogle")}
          </a>
        </div>
      </div>
    </div>
  );
}
