import { useEffect, useRef, useState } from 'react';
import { api, ApiError, session } from './api';
import type { Cart, Product, Proposal, Results, Terms } from './api';
export type Action = 'search' | 'stock' | 'analogs' | 'certificates' | 'terms';
export interface Message { id: string; text: string; user?: boolean }
export function useAssistant(cartPage: boolean) {
  const [messages, setMessages] = useState<Message[]>([{ id: 'welcome', text: 'Здравствуйте! Здесь можно найти товар по названию или артикулу, посмотреть характеристики и остатки. Для начала введите запрос или выберите товар из каталога.' }]);
  const [results, setResults] = useState<Results | null>(null);
  const initialQuery = new URLSearchParams(window.location.search).get('q')?.slice(0, 200) || '';
  const [query, setQuery] = useState(initialQuery); const [searched, setSearched] = useState('');
  const [loading, setLoading] = useState(false); const [detailLoading, setDetailLoading] = useState(false);
  const [selected, setSelected] = useState<Product | null>(null); const [mobileDetail, setMobileDetail] = useState(false);
  const [cart, setCart] = useState<Cart | null>(null); const [proposal, setProposal] = useState<Proposal | null>(null);
  const [changed, setChanged] = useState(false); const [busy, setBusy] = useState(false);
  const [error, setError] = useState(''); const [needSession, setNeedSession] = useState(false);
  const [online, setOnline] = useState<boolean | null>(null); const [terms, setTerms] = useState<Terms | null>(null);
  const [panel, setPanel] = useState<'stock' | 'certificates' | null>(null);
  const [analogNotes, setAnalogNotes] = useState<Record<number, string>>({});
  const searchAbort = useRef<AbortController | null>(null); const detailAbort = useRef<AbortController | null>(null);
  const lastChosen = useRef<Product | null>(null);
  const mutation = useRef(false); const actionSeq = useRef(0);
  const pending = useRef<{ requestId: string; id: number; quantity: number } | null>(null);
  function say(text: string, user = false) { setMessages(m => [...m, { id: crypto.randomUUID(), text, user }]); }
  function fail(e: unknown) {
    setError(e instanceof Error ? e.message : 'Не удалось выполнить действие.');
    if (e instanceof ApiError && (e.status === 0 || e.status >= 500)) setOnline(false);
    if (e instanceof ApiError && (e.status === 401 || e.code === 'SESSION_REQUIRED')) { setNeedSession(true); setProposal(null); pending.current = null; }
  }
  async function load(q = query, page = 0) {
    searchAbort.current?.abort(); const controller = new AbortController(); searchAbort.current = controller;
    actionSeq.current++; setLoading(true); setError(''); setAnalogNotes({});
    try {
      const result = q.trim() ? await api.search(q.trim(), page, controller.signal) : await api.list(page, controller.signal);
      if (controller.signal.aborted) return;
      setResults(result); setSearched(q.trim());
      const enriched = await Promise.all(result.items.map(async p => {
        try { return { ...p, ...(await api.stored(p.id, controller.signal)).product }; } catch { return p; }
      }));
      if (controller.signal.aborted) return;
      setResults({ ...result, items: enriched });
      if (q.trim()) say(`По запросу «${q.trim()}» найдено товаров: ${result.totalElements}. Выберите карточку, чтобы посмотреть актуальные данные.`);
      else if (!result.items.length) say('Каталог пока пуст. Дождитесь импорта товаров.');
    } catch (e) { if (!controller.signal.aborted) fail(e); } finally { if (!controller.signal.aborted) setLoading(false); }
  }
  async function choose(product: Product) {
    lastChosen.current = product;
    detailAbort.current?.abort(); const controller = new AbortController(); detailAbort.current = controller;
    setSelected(null); setDetailLoading(true); setMobileDetail(true); setPanel(null); setError('');
    try { const p = await api.product(product.id, controller.signal); if (!controller.signal.aborted) setSelected(p); }
    catch (e) { if (!controller.signal.aborted) fail(e); }
    finally { if (!controller.signal.aborted) setDetailLoading(false); }
  }
  async function checkConnection() { try { const h = await api.health(); setOnline(h.status === 'ok'); setError(''); } catch (e) { fail(e); } }
  function acceptCart(next: Cart) { setCart(previous => !previous || next.version >= previous.version ? next : previous); }
  async function refreshCart() { try { acceptCart(await api.cart()); } catch (e) { fail(e); } }
  async function newSession() {
    if (mutation.current) return; mutation.current = true; setBusy(true); setError('');
    try { await api.createSession(); setNeedSession(false); setProposal(null); pending.current = null; setCart(await api.cart()); say('Новая сессия создана. Корзина новой сессии пуста. Повторите нужное действие самостоятельно.'); }
    catch (e) { fail(e); } finally { mutation.current = false; setBusy(false); }
  }
  useEffect(() => {
    let active = true;
    api.health().then(r => { if (active) setOnline(r.status === 'ok'); }).catch(() => { if (active) setOnline(false); });
    (async () => { try { if (!session.get()) await api.createSession(); const c = await api.cart(); if (active) setCart(c); } catch (e) { if (active) fail(e); } })();
    if (!cartPage) void load(initialQuery);
    return () => { active = false; searchAbort.current?.abort(); detailAbort.current?.abort(); };
    // One initialization per mounted page; actions deliberately do not replay after expired sessions.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cartPage]);
  async function act(action: Action) {
    setError('');
    if (action === 'search') { if (query.trim()) { say(query.trim(), true); await load(); } else document.getElementById('catalog-query')?.focus(); return; }
    if (action !== 'terms' && !selected) { say('Сначала выберите товар в результатах поиска.'); return; }
    if (action === 'stock' || action === 'certificates') { setPanel(action); setMobileDetail(true); return; }
    const seq = ++actionSeq.current;
    try {
      if (action === 'terms') { const t = await api.terms(); if (seq === actionSeq.current) { setTerms(t); say(t.verified ? 'Условия покупки получены из настроенного источника.' : 'Условия оплаты, доставки и минимального заказа пока не подтверждены. Уточните их у менеджера EKT.'); } }
      if (action === 'analogs' && selected) {
        searchAbort.current?.abort(); setLoading(true);
        const r = await api.analogs(selected.id);
        if (seq !== actionSeq.current) return;
        setResults({ items: r.items.map(x => x.product.product), totalElements: r.items.length, totalPages: 1, page: 0 });
        setAnalogNotes(Object.fromEntries(r.items.map(x => [x.product.product.id, x.explanation]))); setSearched('Подбор аналогов'); say(r.message); setMobileDetail(false);
      }
    } catch (e) { if (seq === actionSeq.current) fail(e); } finally { if (seq === actionSeq.current) setLoading(false); }
  }
  async function propose(product: Product, quantity: number) {
    if (mutation.current) return;
    if (!Number.isFinite(quantity) || quantity <= 0) { setError('Введите положительное количество.'); return; }
    mutation.current = true; setBusy(true); setError('');
    if (!pending.current || pending.current.id !== product.id || pending.current.quantity !== quantity)
      pending.current = { requestId: crypto.randomUUID(), id: product.id, quantity };
    try { setProposal(await api.propose(pending.current.requestId, product.id, quantity)); pending.current = null; setChanged(false); }
    catch (e) { fail(e); } finally { mutation.current = false; setBusy(false); }
  }
  async function confirm() {
    if (mutation.current || !proposal) return; mutation.current = true; setBusy(true); setError('');
    try {
      const r = await api.confirm(proposal); acceptCart(r.cart);
      if (r.requiresConfirmation) { setProposal(r.proposal); setChanged(true); }
      else if (r.added) { say(`В демонстрационную корзину добавлен товар «${r.proposal.name}». Заказ и резервирование в EKT не выполнялись.`); setProposal(null); }
    } catch (e) { fail(e); } finally { mutation.current = false; setBusy(false); }
  }
  return { messages, results, query, setQuery, searched, loading, detailLoading, selected, mobileDetail, setMobileDetail, cart, acceptCart, fail, proposal, setProposal, changed, busy, error, needSession, online, checkConnection, terms, setTerms, panel, setPanel, analogNotes, load, choose, retryDetail: () => lastChosen.current && choose(lastChosen.current), refreshCart, newSession, act, propose, confirm };
}
