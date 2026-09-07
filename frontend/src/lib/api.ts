import type { DiagramSnapshot } from "@/components/calculator/diagramSnapshot";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

const TOKEN_KEY = "acc_token";
export const CREDITS_CHANGED_EVENT = "aws-cost-credits-changed";

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string) {
  if (typeof window === "undefined") return;
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken() {
  if (typeof window === "undefined") return;
  localStorage.removeItem(TOKEN_KEY);
}

export const GOOGLE_LOGIN_URL = `${API_BASE_URL}/oauth2/authorization/google`;
export const KAKAO_LOGIN_URL = `${API_BASE_URL}/oauth2/authorization/kakao`;

// Backend ResourceType enum value (e.g. "ECS", "AURORA"). Kept as a plain string, not a closed
// union, so the frontend never needs a code change when the backend adds a new resource type —
// the full set is discovered at runtime via getResourceCatalog().
export type ResourceType = string;

export interface ProjectResponse {
  id: number;
  name: string;
  region: string;
  monthlyUsers: number;
  requestsPerUser: number;
  busyTrafficLevel: string;
  serviceStage: string;
  createdAt: string;
}

export interface ResourceResponse {
  id: number;
  type: ResourceType;
  configuration: Record<string, unknown>;
}

export interface ResourceCostItem {
  resourceId: number;
  type: ResourceType;
  monthlyCost: number;
}

export interface CalculationResponse {
  totalMonthlyCost: number;
  totalMonthlyCostKrw: number;
  currency: string;
  resources: ResourceCostItem[];
  diagramSnapshot: DiagramSnapshot | null;
  recommendationMetadata: RecommendationMetadata | null;
}

export interface Suggestion {
  message: string;
  estimatedMonthlySavingsUsd: number | null;
}

export interface OptimizationResponse {
  currentMonthlyCost: number;
  optimizedMonthlyCost: number;
  estimatedSavings: number;
  suggestions: Suggestion[];
  provider: string;
  aiGenerated: boolean;
}

class ApiError extends Error {
  status: number | null;

  constructor(message: string, status: number | null = null) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

function getLocale(): string {
  if (typeof document === "undefined") return "ko";
  return document.documentElement.lang || "ko";
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const token = getToken();
  let res: Response;
  try {
    res = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers: {
        "Content-Type": "application/json",
        "X-Locale": getLocale(),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options?.headers,
      },
    });
  } catch {
    throw new ApiError(
      `백엔드 서버(${API_BASE_URL})에 연결할 수 없어요. 서버가 실행 중인지 확인해주세요.`
    );
  }

  if (!res.ok) {
    const body = await res.json().catch(() => null);
    throw new ApiError(body?.message ?? `요청이 실패했어요 (HTTP ${res.status})`, res.status);
  }

  return res.json() as Promise<T>;
}

export interface CreateProjectPayload {
  name: string;
  region: string;
  monthlyUsers: number;
  requestsPerUser: number;
  busyTrafficLevel: string;
  serviceStage: string;
}

export function createProject(payload: CreateProjectPayload) {
  return request<ProjectResponse>("/api/projects", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export interface ProjectHistoryItem {
  calculationId: number;
  projectId: number;
  projectName: string;
  region: string;
  totalMonthlyCostUsd: number;
  totalMonthlyCostKrw: number;
  calculatedAt: string;
}

// Priced as of calculatedAt — not recomputed against current pricing.
export function getMyProjects() {
  return request<ProjectHistoryItem[]>("/api/projects");
}

export function addResource(
  projectId: number,
  type: ResourceType,
  configuration: Record<string, unknown>
) {
  return request<ResourceResponse>(`/api/projects/${projectId}/resources`, {
    method: "POST",
    body: JSON.stringify({ type, configuration }),
  });
}

// Doesn't spend credits (the backend no longer charges here) — only recommendArchitecture does,
// so this doesn't need to notify credit-balance listeners. diagramSnapshot/recommendationMetadata
// are only ever non-null when calculating from the drag builder / an applied AI recommendation —
// passed through so the saved calculation's history report can show the same diagram and
// recommendation context later (see mypage).
export function calculate(
  projectId: number,
  diagramSnapshot?: DiagramSnapshot | null,
  recommendationMetadata?: RecommendationMetadata | null
) {
  return request<CalculationResponse>(`/api/projects/${projectId}/calculate`, {
    method: "POST",
    body: JSON.stringify({
      diagramSnapshot: diagramSnapshot ?? null,
      recommendationMetadata: recommendationMetadata ?? null,
    }),
  });
}

// Re-fetches the last saved calculation (for 마이페이지's 계산 기록) — priced as of when it was
// saved, not recomputed against current pricing.
export function getSavedCalculation(projectId: number) {
  return request<CalculationResponse>(`/api/projects/${projectId}/calculate`);
}

// AI-driven, credit-gated — spends 1 credit each call, unlike calculate().
export function optimize(projectId: number) {
  return request<OptimizationResponse>(`/api/projects/${projectId}/optimize`, {
    method: "POST",
  }).then((response) => {
    notifyCreditsChanged();
    return response;
  });
}

export interface ExplanationResponse {
  explanation: string;
  provider: string;
  aiGenerated: boolean;
}

export function explain(projectId: number) {
  return request<ExplanationResponse>(`/api/projects/${projectId}/explain`, {
    method: "POST",
  });
}

export interface ResourceCost {
  type: ResourceType;
  configuration?: Record<string, unknown>;
  monthlyCost: number;
}

export interface RecommendationResponse {
  tierName: string;
  description: string;
  estimatedMonthlyRequests: number;
  analysisMode: string;
  recommendationReason: string;
  provider: string;
  aiGenerated: boolean;
  totalMonthlyCostUsd: number;
  totalMonthlyCostKrw: number;
  resources: ResourceCost[];
  additionalRecommendations: string[];
  note: string | null;
}

// Carried from an applied RecommendationResponse through to a saved Calculation (opaque JSON on
// the backend — see CalculationResponse.diagramSnapshot for the same pass-through treatment) so a
// calculation history report can still show what the AI recommended, not just the final resource
// costs.
export interface RecommendationMetadata {
  tierName: string;
  description: string;
  recommendationReason: string;
  additionalRecommendations: string[];
  aiGenerated: boolean;
}

export interface ServiceRecommendationPayload {
  serviceName: string;
  monthlyUsers: number;
  requestsPerUser: number;
  busyTrafficLevel: string;
  serviceStage: string;
  serviceType: string;
  serviceDescription: string;
  region: string;
}

export function recommendArchitecture(payload: ServiceRecommendationPayload) {
  return request<RecommendationResponse>("/api/architecture-recommendations", {
    method: "POST",
    body: JSON.stringify(payload),
  }).then((response) => {
    notifyCreditsChanged();
    return response;
  });
}

function notifyCreditsChanged() {
  if (typeof window === "undefined") return;
  window.dispatchEvent(new Event(CREDITS_CHANGED_EVENT));
}

export interface RegionResponse {
  code: string;
  label: string;
}

export function getRegions() {
  return request<RegionResponse[]>("/api/regions");
}

export interface ResourceCatalogOption {
  label: string;
  value: string | number | boolean;
}

export interface ResourceCatalogField {
  key: string;
  label: string;
  type: "select" | "number";
  defaultValue: string | number | boolean;
  options: ResourceCatalogOption[];
  min: number | null;
  max: number | null;
  step: number | null;
  costRelevant: boolean;
}

export interface ResourceCatalogItem {
  type: ResourceType;
  kind: string;
  title: string;
  shortName: string;
  category: string;
  description: string;
  tone: string;
  defaults: Record<string, string | number | boolean>;
  fields: ResourceCatalogField[];
}

export function getResourceCatalog() {
  return request<ResourceCatalogItem[]>("/api/resource-catalog");
}

export interface UserResponse {
  id: number;
  email: string;
  nickname: string;
  profileImageUrl: string | null;
  emailVerified: boolean;
  role: "USER" | "ADMIN";
  createdAt: string;
}

export function getMe() {
  return request<UserResponse>("/api/users/me");
}

export interface CreditTransactionResponse {
  id: number;
  amount: number;
  type: "SIGNUP_BONUS" | "USAGE" | "PURCHASE" | "REFUND" | "ADMIN_ADJUST";
  description: string;
  createdAt: string;
}

export interface CreditBalanceResponse {
  balance: number;
  recentTransactions: CreditTransactionResponse[];
}

export function getMyCredits() {
  return request<CreditBalanceResponse>("/api/credits/me");
}

export interface AdminUserResponse {
  id: number;
  email: string;
  nickname: string;
  role: "USER" | "ADMIN";
  emailVerified: boolean;
  createdAt: string;
  creditBalance: number;
}

export function getAdminUsers(query?: string) {
  const q = query && query.trim() ? `?query=${encodeURIComponent(query.trim())}` : "";
  return request<AdminUserResponse[]>(`/api/admin/users${q}`);
}

export function adjustAdminUserCredits(userId: number, amount: number, description: string) {
  return request<AdminUserResponse>(`/api/admin/users/${userId}/credits`, {
    method: "POST",
    body: JSON.stringify({ amount, description }),
  });
}

export function updateAdminUserRole(userId: number, role: "USER" | "ADMIN") {
  return request<AdminUserResponse>(`/api/admin/users/${userId}/role`, {
    method: "PUT",
    body: JSON.stringify({ role }),
  });
}

// Fire-and-forget beacon called once per page load — never throws, and doesn't parse a response
// body (the endpoint returns none), unlike the typed request() helper used everywhere else.
const VISITOR_ID_STORAGE_KEY = "acc_visitor_id";

// Anonymous, persisted per-browser (not per-login) - the ping fires on every page load
// regardless of auth state, so this is what distinct-visitor DAU/MAU is computed from
// server-side. Not a user identity: never sent anywhere except this ping.
function getOrCreateVisitorId(): string | null {
  try {
    const existing = window.localStorage.getItem(VISITOR_ID_STORAGE_KEY);
    if (existing) return existing;
    const generated = crypto.randomUUID();
    window.localStorage.setItem(VISITOR_ID_STORAGE_KEY, generated);
    return generated;
  } catch {
    // Storage blocked (private mode, disabled localStorage, etc.) - the ping still counts
    // toward raw view totals, just not distinct-visitor counts.
    return null;
  }
}

export function pingSiteVisit() {
  if (typeof window === "undefined") return;
  fetch(`${API_BASE_URL}/api/site-visits/ping`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      hostname: window.location.hostname,
      visitorId: getOrCreateVisitorId(),
    }),
    keepalive: true,
  }).catch(() => undefined);
}

export interface AuthResponse {
  token: string;
  user: UserResponse;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface SignupPayload extends LoginPayload {
  nickname?: string;
}

export function login(payload: LoginPayload) {
  return request<AuthResponse>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function signup(payload: SignupPayload) {
  return request<AuthResponse>("/api/auth/signup", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export { ApiError };
