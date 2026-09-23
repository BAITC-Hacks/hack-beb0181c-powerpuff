import { useEffect, useRef, useState } from 'react';
import { api, ApiError, number } from './api';
import type { Cart, CartItem, Product, Proposal } from './api';
import { Detail, Icon, Modal, ProductImage } from './components';

function CartRow({ item, busy, error, open, apply, remove }: {
  item: CartItem; busy: boolean; error?: string; open: () => void;
  apply: (quantity: number) => void; remove: () => void;
}) {
  const [draft, setDraft] = useState(String(item.quantity));
  const step = item.orderMultiple;
  const value = Number(draft.replace(',', '.'));
  function shift(direction: number) {
    if (step && Number.isFinite(value)) setDraft(String(Number((value + direction * step).toFixed(10))));
  }
  return <article className="ekt-cart-line" aria-busy={busy}>
    <button className="ekt-cart-photo" onClick={open} aria-label={`Открыть изображение товара ${item.name}`}>
      <ProductImage key={item.image} product={{ ...item, id: item.productId }} />
    </button>
    <div className="ekt-cart-info"><button className="ekt-cart-name" onClick={open}><h3>{item.name}</h3></button>
      <p className="ekt-caption">Арт. {item.article || 'не указан'}</p>
      <dl><div><dt>Цена</dt><dd>{number(item.price)}</dd></div><div><dt>В корзине</dt><dd>{number(item.quantity)}</dd></div><div><dt>Сумма</dt><dd>{number(item.subtotal)}</dd></div></dl>
      <form className="ekt-cart-edit" onSubmit={e => { e.preventDefault(); apply(value); }}>
        <label htmlFor={`quantity-${item.productId}`}>Количество</label>
        <div className="ekt-cart-controls"><div className="ekt-stepper">
          <button type="button" disabled={busy || !step || !Number.isFinite(value) || value - step <= 0} aria-label={`Уменьшить количество: ${item.name}`} onClick={() => shift(-1)}>−</button>
          <input id={`quantity-${item.productId}`} aria-describedby={`quantity-hint-${item.productId}`} type="text" inputMode="decimal" required value={draft} onChange={e => setDraft(e.target.value)} disabled={busy} />
          <button type="button" disabled={busy || !step || !Number.isFinite(value)} aria-label={`Увеличить количество: ${item.name}`} onClick={() => shift(1)}>+</button>
        </div><button className="ekt-primary" disabled={busy} type="submit">{busy ? 'Сохраняем…' : 'Применить'}</button>
        <button type="button" className="ekt-remove" disabled={busy} onClick={remove} aria-label={`Удалить: ${item.name}`}>Удалить</button></div>
        <p className="ekt-caption" id={`quantity-hint-${item.productId}`}>{step ? `Кратность по данным EKT: ${number(step)}.` : 'Кратность неизвестна — введите количество вручную.'} Поле не меняет корзину до нажатия «Применить».</p>
      </form>
      {error && <p className="ekt-row-error" role="alert">{error}</p>}
    </div>
  </article>;
}

export function CartContent({ cart, loadError, refresh, accept, fail }: {
  cart: Cart | null; loadError: string; refresh: () => Promise<void>; accept: (cart: Cart) => void; fail: (e: unknown) => void;
}) {
  const [busy, setBusy] = useState<Set<number>>(new Set());
  const active = useRef(new Set<number>());
  const [errors, setErrors] = useState<Record<number, string>>({});
  const [notice, setNotice] = useState('');
  const [removing, setRemoving] = useState<CartItem | null>(null);
  const [proposal, setProposal] = useState<Proposal | null>(null);
  const [detailId, setDetailId] = useState<number | null>(null);
  const [product, setProduct] = useState<Product | null>(null);
  const [detailError, setDetailError] = useState('');
  const detailAbort = useRef<AbortController | null>(null);
  // Keep the same mutation key and base version after an uncertain network failure.
  const pending = useRef(new Map<number, { id: string; kind: string; quantity?: number; version: number }>());
  const scroll = useRef<HTMLElement>(null);
  const savedScroll = useRef(0);
  useEffect(() => () => detailAbort.current?.abort(), []);
  function closeDetail() {
    detailAbort.current?.abort(); setDetailId(null); setProduct(null);
    requestAnimationFrame(() => { if (scroll.current) scroll.current.scrollTop = savedScroll.current; });
  }
  async function openDetail(id: number, retry = false) {
    if (!retry) savedScroll.current = scroll.current?.scrollTop || 0;
    detailAbort.current?.abort(); const abort = new AbortController(); detailAbort.current = abort;
    setDetailId(id); setProduct(null); setDetailError('');
    try { const p = await api.product(id, abort.signal); if (!abort.signal.aborted) setProduct(p); }
    catch (e) { if (!abort.signal.aborted) setDetailError(e instanceof Error ? e.message : 'Не удалось загрузить карточку.'); }
  }
  function key(item: CartItem, kind: string, quantity?: number) {
    let operation = pending.current.get(item.productId);
    if (!operation || operation.kind !== kind || operation.quantity !== quantity) {
      operation = { id: crypto.randomUUID(), kind, quantity, version: cart!.version };
      pending.current.set(item.productId, operation);
    }
    return operation;
  }
  async function run(id: number, task: () => Promise<void>) {
    if (active.current.has(id)) return;
    active.current.add(id); setBusy(new Set(active.current)); setNotice('');
    setErrors(e => ({ ...e, [id]: '' }));
    try { await task(); }
    catch (e) {
      setErrors(errors => ({ ...errors, [id]: e instanceof Error ? e.message : 'Не удалось сохранить изменение.' }));
      if (e instanceof ApiError && e.status > 0 && e.status < 500) pending.current.delete(id);
      if (e instanceof ApiError && e.status === 401) { setProposal(null); setRemoving(null); pending.current.clear(); fail(e); }
    } finally { active.current.delete(id); setBusy(new Set(active.current)); }
  }
  function apply(item: CartItem, quantity: number) {
    if (!Number.isFinite(quantity) || quantity <= 0) { setErrors(e => ({ ...e, [item.productId]: 'Введите количество больше нуля. Для удаления используйте «Удалить».' })); return; }
    void run(item.productId, async () => {
      const op = key(item, 'SET', quantity);
      const result = await api.setQuantity(item.productId, quantity, op.version, op.id);
      accept(result.cart); pending.current.delete(item.productId);
      if (result.requiresConfirmation && result.proposal) setProposal(result.proposal);
      else { setProposal(null); setNotice('Количество и итог обновлены.'); }
    });
  }
  function confirmRemove() {
    if (!removing) return; const item = removing;
    void run(item.productId, async () => {
      const op = key(item, 'DELETE');
      accept(await api.remove(item.productId, op.version, op.id)); pending.current.delete(item.productId);
      setRemoving(null); setProposal(null); setNotice(`Товар «${item.name}» удалён.`);
    });
  }
  function confirmPrice() {
    if (!proposal) return; const current = proposal;
    void run(current.productId, async () => {
      const result = await api.confirm(current); accept(result.cart);
      if (result.requiresConfirmation) setProposal(result.proposal);
      else { setProposal(null); setNotice('Количество и цена обновлены.'); }
    });
  }
  return <>
    <main className="ekt-cart-page" ref={scroll}>
      <div className="ekt-section-heading"><h2>Ваша корзина</h2><button className="ekt-secondary" onClick={refresh}>Обновить</button></div>
      <div className="ekt-warning">Демонстрационная корзина. Заказ и резервирование в EKT не выполняются. Доступность отгрузки, валюта и единицы не подтверждены.</div>
      <p className="ekt-cart-notice" role="status">{notice}</p>
      {!cart ? <p role="status">{loadError ? 'Не удалось загрузить корзину.' : 'Загружаем корзину…'}</p> : !cart.items.length ? <div className="ekt-empty"><Icon name="cart" /><h2>Корзина пока пуста</h2><p>Найдите товар и подтвердите его добавление.</p><a className="ekt-primary" href="/assistant">К поиску</a></div> : <>
        <div className="ekt-cart-items">{cart.items.map(item => <CartRow key={`${item.productId}:${item.quantity}:${item.price}`} item={item} busy={busy.has(item.productId)} error={errors[item.productId]} open={() => void openDetail(item.productId)} apply={q => apply(item, q)} remove={() => setRemoving(item)} />)}</div>
        <p className="ekt-cart-total">Итого по данным EKT <strong>{number(cart.total)}</strong></p>
        <p className="ekt-caption">Валюта не подтверждена. Суммы рассчитаны backend по сохранённым ценам.</p>
      </>}
    </main>
    {detailId !== null && <Modal title="Карточка товара" close={closeDetail}>
      {product ? <Detail key={product.id} product={product} panel={null} busy={false} showAdd={false} add={() => {}} /> : detailError ? <div className="ekt-empty" role="alert"><h3>Не удалось загрузить карточку</h3><p>{detailError}</p><button className="ekt-secondary" onClick={() => void openDetail(detailId, true)}>Повторить загрузку</button></div> : <p className="ekt-loading" role="status">Загружаем карточку…</p>}
    </Modal>}
    {removing && <Modal title="Удалить товар?" close={() => setRemoving(null)} busy={busy.has(removing.productId)}>
      <p className="ekt-confirm-name">{removing.name}</p><p>Позиция будет удалена из вашей демонстрационной корзины.</p>
      {errors[removing.productId] && <p className="ekt-row-error" role="alert">{errors[removing.productId]}</p>}
      <div className="ekt-confirm-actions"><button className="ekt-secondary" disabled={busy.has(removing.productId)} onClick={() => setRemoving(null)}>Оставить</button><button className="ekt-primary" disabled={busy.has(removing.productId)} onClick={confirmRemove}>{busy.has(removing.productId) ? 'Удаляем…' : 'Удалить товар'}</button></div>
    </Modal>}
    {proposal && <Modal title="Подтвердите новую цену" close={() => setProposal(null)} busy={busy.has(proposal.productId)}>
      <p className="ekt-confirm-name">{proposal.name}</p><div className="ekt-warning">Цена или остаток изменились. Корзина пока не изменена. Проверьте условия перед повторным подтверждением.</div>
      <dl className="ekt-specs"><div><dt>Новое количество</dt><dd>{number(proposal.quantity)}</dd></div><div><dt>Новая цена</dt><dd>{number(proposal.price)}</dd></div><div><dt>Новая сумма</dt><dd>{number(proposal.quantity * proposal.price)}</dd></div><div><dt>Остаток EKT</dt><dd>{number(proposal.stock)}</dd></div></dl>
      <p className="ekt-caption">Валюта и единицы не подтверждены.</p>{errors[proposal.productId] && <p className="ekt-row-error" role="alert">{errors[proposal.productId]}</p>}
      <button className="ekt-primary ekt-wide" disabled={busy.has(proposal.productId)} onClick={confirmPrice}>{busy.has(proposal.productId) ? 'Сохраняем…' : 'Подтвердить новые условия'}</button>
    </Modal>}
  </>;
}
