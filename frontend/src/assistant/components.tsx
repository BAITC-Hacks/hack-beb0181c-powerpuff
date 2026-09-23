import { useEffect, useId, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { number, safeUrl, valueText } from './api';
import type { Json, Product } from './api';
export type IconName = 'search' | 'box' | 'swap' | 'file' | 'truck' | 'cart' | 'send' | 'arrow' | 'close' | 'bot' | 'check';
const paths: Record<IconName, string> = {
 search:'M21 21l-5-5M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0',
 box:'M3 6l9-4 9 4v12l-9 4-9-4V6zm0 0l9 5 9-5M12 11v11M7 4l9 5',
 swap:'M4 7h16l-4-4M20 17H4l4 4M20 7l-4 4M4 17l4-4',
 file:'M14 2H5v20h14V7l-5-5zm0 0v6h5M8 12h8M8 16h6',
 truck:'M2 5h12v12H2V5zm12 5h4l4 4v3h-8M8 18a2 2 0 1 1-4 0 2 2 0 0 1 4 0m12 0a2 2 0 1 1-4 0 2 2 0 0 1 4 0',
 cart:'M2 3h3l3 13h12l2-9H6M10 21h.01M18 21h.01',
 send:'M3 3l18 9-18 9 4-9-4-9zm4 9h14',
 arrow:'M20 12H4m6-6-6 6 6 6', close:'M5 5l14 14M5 19L19 5',
 bot:'M12 3v3M8 14h.01M16 14h.01M5 8h14v12H5V8zm-3 4v4m20-4v4M9 17h6',
 check:'M4 12l5 5L20 6',
};
export function Icon({ name }: { name: IconName }) { return <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d={paths[name]} /></svg>; }
export function ProductImage({ product }: { product: Product }) {
  const [failed, setFailed] = useState(false); const url = safeUrl(product.image);
  return url && !failed ? <img src={url} alt={product.name} loading="lazy" onError={() => setFailed(true)} /> : <div className="ekt-image-placeholder"><Icon name="box" /><span>Нет изображения</span></div>;
}
const labels: Record<string, string> = { OBYEM: 'Тип', TORGOVAYA_MARKA: 'Производитель', KOLICHESTVO_POLYUSOV: 'Количество полюсов', NOMINALNOE_NAPRYAZHENIE: 'Номинальное напряжение', NOMINALNYY_TOK: 'Номинальный ток', NOMINALNAYA_OTKLYUCHAYUSHCHAYA_SPOSOBNOST: 'Отключающая способность', TIP_USTANOVKI: 'Тип установки', KHARAKTERISTIKA_SRABATYVANIYA: 'Характеристика срабатывания', KRATNOST_MIN: 'Кратность по данным EKT' };
export function Stock({ quantity }: { quantity: number | null | undefined }) { return <span className={`ekt-stock ${quantity === 0 ? 'empty' : ''}`}>{quantity == null ? 'Остаток неизвестен' : quantity === 0 ? 'Нет в наличии по данным EKT' : `Остаток EKT: ${number(quantity)}`}</span>; }
export function Modal({ title, children, close, busy = false }: { title: string; children: ReactNode; close: () => void; busy?: boolean }) {
  const ref = useRef<HTMLDialogElement>(null); const id = useId();
  useEffect(() => { const d = ref.current; d?.showModal(); return () => d?.close(); }, []);
  return <dialog ref={ref} className="ekt-modal" aria-labelledby={id} onCancel={e => { e.preventDefault(); if (!busy) close(); }}><div className="ekt-modal-heading"><h2 id={id}>{title}</h2><button type="button" aria-label="Закрыть окно" onClick={close} disabled={busy} className="ekt-icon-button"><Icon name="close" /></button></div>{children}</dialog>;
}
function DocumentValue({ value }: { value: Json }) {
  if (typeof value === 'string' && safeUrl(value)) return <a className="ekt-link" href={safeUrl(value)} target="_blank" rel="noreferrer">Открыть документ EKT ↗</a>;
  if (Array.isArray(value)) return <ul>{value.map((v, i) => <li key={i}><DocumentValue value={v} /></li>)}</ul>;
  if (value && typeof value === 'object') return <dl>{Object.entries(value).map(([k, v]) => <div key={k}><dt>{k}</dt><dd><DocumentValue value={v} /></dd></div>)}</dl>;
  return <span>{valueText(value)}</span>;
}
export function Detail({ product, busy, add, panel, showAdd = true }: { showAdd?: boolean; product: Product; busy: boolean; add: (p: Product, q: number) => void; panel: 'stock' | 'certificates' | null }) {
  const multiple = Number(String(product.properties?.KRATNOST_MIN ?? 1).replace(',', '.'));
  const [qty, setQty] = useState(String(Number.isFinite(multiple) && multiple > 0 ? multiple : 1)); const stockRef = useRef<HTMLElement>(null); const docsRef = useRef<HTMLElement>(null);
  useEffect(() => { if (panel === 'stock') stockRef.current?.scrollIntoView({ block: 'start' }); if (panel === 'certificates') docsRef.current?.scrollIntoView({ block: 'start' }); }, [panel]);
  const certs = Object.entries(product.certificateSources || {});
  function property(k: string, v: Json | undefined) {
    const conflict = product.characteristicConflicts?.find(c => c.parameter === k);
    return conflict ? <span className="ekt-conflict-field">{[...new Set(conflict.evidence.map(e => e.rawValue))].join(' / ')} — конфликт источников</span> : valueText(v);
  }
  const unavailable = product.quantity == null || product.quantity <= 0 || product.price == null;
  return <>
    <div className="ekt-detail-image"><ProductImage key={product.image} product={product} /></div>
    <Stock quantity={product.quantity} /><h2>{product.name}</h2><p className="ekt-muted">Арт. {product.article || 'не указан'}</p>
    <div className="ekt-price">{number(product.price)}</div><p className="ekt-caption">Цена по данным EKT · валюта не подтверждена</p>
    {!!product.warnings?.length && <div className="ekt-warning" role="note"><strong>Проверьте характеристики</strong>{product.warnings.map((w, i) => <p key={i}>{w}</p>)}</div>}
    {showAdd && <><form className="ekt-add-row" onSubmit={e => { e.preventDefault(); add(product, Number(qty)); }}>
      <label>Количество<input aria-label="Количество товара" type="number" min="0.0000000001" step="any" required value={qty} onChange={e => setQty(e.target.value)} /></label>
      <button className="ekt-primary" disabled={busy || unavailable} type="submit"><Icon name="cart" />В корзину</button>
    </form><p className="ekt-caption">Сначала покажем предложение. Добавление — после вашего подтверждения.</p></>}
    {safeUrl(product.url) && <a className="ekt-external" href={safeUrl(product.url)} target="_blank" rel="noreferrer">Перейти на страницу товара EKT ↗</a>}
    <section><h3>Основные характеристики</h3><dl className="ekt-specs">{Object.entries(labels).map(([k, label]) => <div key={k}><dt>{label}</dt><dd>{property(k, product.properties?.[k])}</dd></div>)}</dl>
      <details><summary>Все исходные свойства</summary><dl className="ekt-specs">{Object.entries(product.properties || {}).map(([k, v]) => <div key={k}><dt>{k}</dt><dd>{property(k, v)}</dd></div>)}</dl></details>
    </section>
    <section ref={stockRef}><h3>Остатки по складам</h3><p className="ekt-caption">Данные поставщика. Доступность для продажи, резервы и единицы количества требуют уточнения.</p>{product.stores?.length ? <dl className="ekt-specs">{product.stores.map(s => <div key={s.id}><dt>{s.name}</dt><dd>{number(s.quantity)}</dd></div>)}</dl> : <p>Нет данных по складам.</p>}</section>
    <section ref={docsRef}><h3>Сертификаты</h3>{certs.length ? certs.map(([k, v]) => <div key={k}><p className="ekt-caption">Источник: {k}</p><DocumentValue value={v} /></div>) : <p>В полученной карточке сертификаты не найдены. Уточните документы у менеджера EKT.</p>}</section>
    <section><h3>Описание</h3><p className="ekt-description">{product.description || 'Описание отсутствует.'}</p></section>
  </>;
}
