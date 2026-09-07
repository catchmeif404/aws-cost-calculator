"use client";

import Link from "next/link";
import { FormEvent, useCallback, useEffect, useState } from "react";
import {
  AdminUserResponse,
  adjustAdminUserCredits,
  ApiError,
  getAdminUsers,
  getMe,
  getToken,
  updateAdminUserRole,
} from "@/lib/api";

function formatDate(value: string) {
  return new Date(value).toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });
}

export default function AdminUsersPage() {
  const [users, setUsers] = useState<AdminUserResponse[]>([]);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(() => Boolean(getToken()));
  const [error, setError] = useState<string | null>(null);
  const [initialToken] = useState(() => getToken());
  const [isAdmin, setIsAdmin] = useState<boolean | null>(() => (initialToken ? null : false));

  const [creditAmount, setCreditAmount] = useState<Record<number, string>>({});
  const [creditReason, setCreditReason] = useState<Record<number, string>>({});
  const [busyUserId, setBusyUserId] = useState<number | null>(null);

  const refresh = useCallback(async (q?: string) => {
    if (!getToken()) {
      setLoading(false);
      return;
    }
    try {
      const items = await getAdminUsers(q);
      setUsers(items);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "회원 목록을 불러오지 못했어요.");
    } finally {
      setLoading(false);
    }
  }, []);

  const initialize = useCallback(async () => {
    if (!initialToken) {
      return;
    }
    try {
      const me = await getMe();
      setIsAdmin(me.role === "ADMIN");
      if (me.role === "ADMIN") {
        await refresh();
        return;
      }
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "권한 확인에 실패했어요.");
    } finally {
      setLoading(false);
    }
  }, [initialToken, refresh]);

  useEffect(() => {
    void Promise.resolve().then(initialize);
  }, [initialize]);

  function handleSearch(event: FormEvent) {
    event.preventDefault();
    setLoading(true);
    void refresh(query);
  }

  async function handleAdjustCredits(userId: number) {
    const rawAmount = creditAmount[userId]?.trim();
    const reason = creditReason[userId]?.trim();
    const amount = Number(rawAmount);
    if (!rawAmount || Number.isNaN(amount) || amount === 0) {
      setError("지급/차감할 크레딧 수량을 입력해주세요 (0이 아닌 숫자, 차감은 음수).");
      return;
    }
    if (!reason) {
      setError("크레딧 조정 사유를 입력해주세요.");
      return;
    }
    setBusyUserId(userId);
    setError(null);
    try {
      const updated = await adjustAdminUserCredits(userId, amount, reason);
      setUsers((current) => current.map((u) => (u.id === userId ? updated : u)));
      setCreditAmount((current) => ({ ...current, [userId]: "" }));
      setCreditReason((current) => ({ ...current, [userId]: "" }));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "크레딧 조정에 실패했어요.");
    } finally {
      setBusyUserId(null);
    }
  }

  async function handleToggleRole(user: AdminUserResponse) {
    const nextRole = user.role === "ADMIN" ? "USER" : "ADMIN";
    if (!window.confirm(`${user.nickname}님의 역할을 ${nextRole}로 변경할까요?`)) {
      return;
    }
    setBusyUserId(user.id);
    setError(null);
    try {
      const updated = await updateAdminUserRole(user.id, nextRole);
      setUsers((current) => current.map((u) => (u.id === user.id ? updated : u)));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "역할 변경에 실패했어요.");
    } finally {
      setBusyUserId(null);
    }
  }

  if (isAdmin === null || loading) {
    return (
      <main className="min-h-screen px-4 py-8 bg-[#0b1220]">
        <div className="mx-auto max-w-3xl rounded-lg border p-6 shadow-sm border-slate-800 bg-slate-950">
          <h1 className="text-2xl font-bold text-slate-50">회원 관리</h1>
          <p className="mt-3 text-sm text-slate-300">불러오는 중...</p>
        </div>
      </main>
    );
  }

  if (!initialToken) {
    return (
      <main className="min-h-screen px-4 py-8 bg-[#0b1220]">
        <div className="mx-auto max-w-3xl rounded-lg border p-6 shadow-sm border-slate-800 bg-slate-950">
          <h1 className="text-2xl font-bold text-slate-50">회원 관리</h1>
          <p className="mt-3 text-sm text-slate-300">로그인 후 사용할 수 있습니다.</p>
          <Link className="mt-5 inline-flex rounded bg-[#ff9900] px-5 py-2.5 text-sm font-semibold text-[#161e2d]" href="/login">
            로그인하기
          </Link>
        </div>
      </main>
    );
  }

  if (!isAdmin) {
    return (
      <main className="min-h-screen px-4 py-8 bg-[#0b1220]">
        <div className="mx-auto max-w-3xl rounded-lg border p-6 shadow-sm border-slate-800 bg-slate-950">
          <h1 className="text-2xl font-bold text-slate-50">관리자 전용</h1>
          <p className="mt-3 text-sm text-slate-300">회원 관리는 관리자만 사용할 수 있습니다.</p>
          <Link className="mt-5 inline-flex rounded border px-5 py-2.5 text-sm font-semibold border-slate-700 text-slate-100" href="/calculator">
            계산기로 돌아가기
          </Link>
        </div>
      </main>
    );
  }

  return (
    <main className="min-h-screen px-4 py-8 bg-[#0b1220] text-slate-50 sm:px-6 lg:px-8">
      <div className="mx-auto flex w-full max-w-5xl flex-col gap-6">
        <div className="flex items-center justify-between gap-4">
          <div>
            <div className="text-xs font-semibold uppercase text-[#ff9900]">Admin</div>
            <h1 className="mt-1 text-2xl font-bold">회원 관리</h1>
            <p className="mt-1 text-sm text-slate-400">전체 회원 {users.length}명 (최대 200명 표시)</p>
          </div>
          <Link className="rounded border px-4 py-2 text-sm font-semibold border-slate-700" href="/calculator">
            계산기로 돌아가기
          </Link>
        </div>

        {error && (
          <div className="rounded border px-4 py-3 text-sm border-red-900 bg-red-950 text-red-300">
            {error}
          </div>
        )}

        <form onSubmit={handleSearch} className="flex gap-2">
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="이메일 또는 닉네임 검색"
            className="flex-1 rounded border px-3 py-2 text-sm border-slate-700 bg-slate-900"
          />
          <button className="rounded border px-4 py-2 text-sm font-semibold border-slate-700 hover:bg-slate-900">
            검색
          </button>
        </form>

        <div className="flex flex-col gap-3">
          {users.length === 0 ? (
            <p className="rounded-lg border p-6 text-sm text-slate-400 border-slate-800 bg-slate-950">
              조건에 맞는 회원이 없습니다.
            </p>
          ) : (
            users.map((user) => (
              <div key={user.id} className="rounded-lg border p-4 border-slate-800 bg-slate-950">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <div className="flex items-center gap-2 font-semibold">
                      {user.nickname}
                      <span
                        className={`rounded-full px-2 py-0.5 text-xs font-semibold ${
                          user.role === "ADMIN" ? "bg-indigo-950 text-indigo-300" : "bg-slate-800 text-slate-400"
                        }`}
                      >
                        {user.role}
                      </span>
                      {!user.emailVerified && (
                        <span className="rounded-full bg-amber-950 px-2 py-0.5 text-xs font-semibold text-amber-300">
                          이메일 미인증
                        </span>
                      )}
                    </div>
                    <p className="mt-1 text-xs text-slate-400">
                      {user.email} · 가입일 {formatDate(user.createdAt)}
                    </p>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="text-sm font-semibold text-[#ff9900]">크레딧 {user.creditBalance}</span>
                    <button
                      type="button"
                      onClick={() => handleToggleRole(user)}
                      disabled={busyUserId === user.id}
                      className="rounded border px-3 py-1.5 text-xs font-semibold border-slate-700 hover:bg-slate-900 disabled:opacity-50"
                    >
                      {user.role === "ADMIN" ? "일반으로 변경" : "관리자로 변경"}
                    </button>
                  </div>
                </div>

                <div className="mt-3 flex flex-wrap items-center gap-2 border-t pt-3 border-slate-900">
                  <input
                    type="number"
                    value={creditAmount[user.id] ?? ""}
                    onChange={(e) => setCreditAmount((current) => ({ ...current, [user.id]: e.target.value }))}
                    placeholder="±크레딧"
                    className="w-28 rounded border px-2 py-1.5 text-sm border-slate-700 bg-slate-900"
                  />
                  <input
                    value={creditReason[user.id] ?? ""}
                    onChange={(e) => setCreditReason((current) => ({ ...current, [user.id]: e.target.value }))}
                    placeholder="조정 사유 (예: 이벤트 지급)"
                    className="flex-1 min-w-[160px] rounded border px-2 py-1.5 text-sm border-slate-700 bg-slate-900"
                  />
                  <button
                    type="button"
                    onClick={() => handleAdjustCredits(user.id)}
                    disabled={busyUserId === user.id}
                    className="rounded bg-[#ff9900] px-3 py-1.5 text-xs font-semibold text-[#161e2d] hover:bg-[#f2a100] disabled:opacity-50"
                  >
                    크레딧 조정
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </main>
  );
}
