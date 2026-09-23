export type Json = null | boolean | number | string | Json[] | { [key: string]: Json };
export interface Product {
  id: number; name: string; article: string | null; price: number | null; quantity: number | null;
  image?: string | null; url?: string | null; brand?: string | null; description?: string | null;
  properties?: Record<string, Json> | null; warnings?: string[];
  characteristicConflicts?: { code: string; parameter: string; message: string; evidence: { source: string; rawValue: string; normalizedValue: number; unit: string }[] }[];
  stores?: { id: number; name: string; quantity: number | null }[] | null;
  certificateSources?: Record<string, Json>;
}
export interface Results { items: Product[]; totalElements: number; totalPages: number; page: number }
export interface Analogs { message: string; confirmed: boolean; items: { product: { product: Product }; explanation: string; compatibilityStatus?: string }[] }
export interface Terms { verified: boolean; payment: string; delivery: string; minimumOrder: string; source?: string; message?: string }
export interface CartItem { productId: number; name: string; article: string | null; image: string | null; quantity: number; price: number; subtotal: number; orderMultiple: number | null }
export interface Cart { version: number; total: number; items: CartItem[]; cartUrl: string; mode: string; stockReserved: boolean; availabilityConfirmed?: boolean }
export interface Proposal { id: string; productId: number; name: string; quantity: number; price: number; stock: number; revision: number; status: string; expiresAt: string; operation: 'ADD' | 'SET'; cartVersion: number }
export interface CartUpdate { updated: boolean; requiresConfirmation: boolean; proposal: Proposal | null; cart: Cart }
export interface Confirmation { added: boolean; requiresConfirmation: boolean; proposal: Proposal; cart: Cart }
export class ApiError extends Error {
  code: string; status: number;
  constructor(message: string, code = 'NETWORK_ERROR', status = 0) { super(message); this.code = code; this.status = status; }
}
const base = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');
const key = 'ekt.session';
export const session = {
  get: () => sessionStorage.getItem(key),
  clear: () => sessionStorage.removeItem(key),
};
let creating: Promise<string> | null = null;
async function request<T>(path: string, options: RequestInit = {}, auth = false): Promise<T> {
  const headers = new Headers(options.headers);
  if (options.body) headers.set('Content-Type', 'application/json');
  if (auth) {
    const token = session.get();
    if (!token) throw new ApiError('Создайте новую сессию для работы с корзиной.', 'SESSION_REQUIRED', 401);
    headers.set('Authorization', `Bearer ${token}`);
  }
  const timeout = new AbortController();
  const timer = window.setTimeout(() => timeout.abort(), 30000);
  const signal = options.signal ? AbortSignal.any([options.signal, timeout.signal]) : timeout.signal;
  try {
    const response = await fetch(`${base}${path}`, { ...options, headers, signal });
    const body = await response.json().catch(() => null);
    if (!response.ok) {
      const code = body?.code || (response.status === 401 ? 'SESSION_REQUIRED' : 'SERVER_ERROR');
      if (response.status === 401 || code === 'SESSION_REQUIRED') session.clear();
      const messages: Record<string, string> = {
        SESSION_REQUIRED: 'Сессия истекла. Создайте новую сессию. Операция не будет повторена автоматически.',
        INSUFFICIENT_STOCK: 'Количество превышает остаток. Корзина не изменена.',
        CART_CHANGED: 'Корзина изменилась. Обновите её, проверьте количество и примените изменение снова.',
        PROPOSAL_STALE: 'Предложение устарело после изменения корзины. Обновите корзину и создайте новое предложение.',
        CART_ITEM_NOT_FOUND: 'Позиции больше нет в этой корзине. Обновите список.',
        REQUEST_ID_REUSED: 'Этот запрос уже использован. Обновите корзину и повторите действие.',
        INVALID_QUANTITY: 'Введите количество больше нуля. Для удаления используйте кнопку «Удалить».',
        UNKNOWN_MULTIPLE: 'Кратность поставщика не удалось проверить. Уточните её у менеджера.',
        INVALID_MULTIPLE: 'Проверьте кратность заказа в карточке товара.',
        PURCHASE_DATA_UNKNOWN: 'Цена или остаток неизвестны. Добавление недоступно.',
        PROPOSAL_EXPIRED: 'Предложение истекло. Создайте новое предложение.',
        PROPOSAL_CHANGED: 'Условия предложения изменились. Создайте новое предложение.',
        EKT_AUTH_ERROR: 'Backend не смог получить доступ к EKT. Обратитесь к администратору.',
        EKT_UNAVAILABLE: 'Каталог EKT временно недоступен. Попробуйте позже.',
      };
      throw new ApiError(messages[code] || (response.status === 401 ? messages.SESSION_REQUIRED : 'Не удалось выполнить запрос. Попробуйте ещё раз.'), code, response.status);
    }
    if (body === null) throw new ApiError('Сервер вернул некорректный ответ.', 'INVALID_RESPONSE');
    return body as T;
  } catch (error) {
    if (error instanceof ApiError || options.signal?.aborted) throw error;
    throw new ApiError('Сервер недоступен или не успел ответить. Проверьте соединение и повторите запрос.');
  } finally { clearTimeout(timer); }
}
export const api = {
  health: (signal?: AbortSignal) => request<{ status: string }>('/api/health', { signal }),
  async createSession() {
    if (!creating) creating = request<{ token: string }>('/api/session', { method: 'POST' })
      .then(r => { sessionStorage.setItem(key, r.token); return r.token; }).finally(() => { creating = null; });
    return creating;
  },
  list: (page = 0, signal?: AbortSignal) => request<Results>(`/api/catalog/products?page=${page}&size=6`, { signal }),
  search: (q: string, page = 0, signal?: AbortSignal) => request<Results>(`/api/products/search?q=${encodeURIComponent(q)}&page=${page}&size=6`, { signal }),
  product: (id: number, signal?: AbortSignal) => request<Product>(`/api/products/${id}`, { signal }),
  stored: (id: number, signal?: AbortSignal) => request<{ product: Product }>(`/api/catalog/products/${id}`, { signal }),
  analogs: (id: number) => request<Analogs>(`/api/products/${id}/analogs`),
  terms: () => request<Terms>('/api/purchase-terms'),
  cart: () => request<Cart>('/api/cart', {}, true),
  setQuantity: (productId: number, quantity: number, cartVersion: number, requestId: string) => request<CartUpdate>(`/api/cart/items/${productId}`, { method: 'PUT', body: JSON.stringify({ requestId, quantity, cartVersion }) }, true),
  remove: (productId: number, cartVersion: number, requestId: string) => request<Cart>(`/api/cart/items/${productId}?requestId=${encodeURIComponent(requestId)}&cartVersion=${cartVersion}`, { method: 'DELETE' }, true),
  propose: (requestId: string, productId: number, quantity: number) => request<Proposal>('/api/cart/proposals', { method: 'POST', body: JSON.stringify({ requestId, productId, quantity }) }, true),
  confirm: (p: Proposal) => request<Confirmation>(`/api/cart/proposals/${p.id}/confirm`, { method: 'POST', body: JSON.stringify({ revision: p.revision }) }, true),
};
export function safeUrl(value?: string | null) {
  if (!value) return undefined;
  try { const u = new URL(value); return ['https:', 'http:'].includes(u.protocol) ? u.href : undefined; } catch { return undefined; }
}
export const number = (v: number | null | undefined) => v == null ? 'Нет данных' : new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 10 }).format(v);
export const valueText = (v: Json | undefined) => v == null ? 'Нет данных' : typeof v === 'object' ? JSON.stringify(v) : String(v);
