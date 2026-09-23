import { useEffect, useRef, useState } from 'react';
import { CartContent } from './CartContent';
import { Detail, Icon, Modal, ProductImage, Stock } from './components';
import type { IconName } from './components';
import { number, valueText } from './api';
import { useAssistant } from './useAssistant';
import type { Action } from './useAssistant';
import './assistant.css';
export function AssistantPage({ cartPage = false }: { cartPage?: boolean }) {
  const a = useAssistant(cartPage); const [mobile, setMobile] = useState(window.matchMedia('(max-width: 850px)').matches);
  const chatEnd = useRef<HTMLDivElement>(null);
  const resultsHeading = useRef<HTMLDivElement>(null);
  useEffect(() => { document.title = cartPage ? 'Корзина — EKT.KZ' : 'Ассистент EKT.KZ'; }, [cartPage]);
  useEffect(() => { const m = window.matchMedia('(max-width: 850px)'); const update = () => setMobile(m.matches); m.addEventListener('change', update); return () => m.removeEventListener('change', update); }, []);
  useEffect(() => { chatEnd.current?.scrollIntoView({ block: 'nearest' }); }, [a.messages]);
  useEffect(() => { if (a.searched && a.results) resultsHeading.current?.scrollIntoView({ block: 'start' }); }, [a.results, a.searched]);
  const actions: { action: Action; icon: IconName; title: string }[] = [
    { action: 'search', icon: 'search', title: 'Найти товар' }, { action: 'stock', icon: 'box', title: 'Проверить наличие' },
    { action: 'analogs', icon: 'swap', title: 'Подобрать аналог' }, { action: 'certificates', icon: 'file', title: 'Сертификаты' },
    { action: 'terms', icon: 'truck', title: 'Доставка и оплата' },
  ];
  const detail = a.detailLoading ? <div className="ekt-empty" role="status">Загружаем актуальную карточку…</div> : a.selected ? <Detail key={a.selected.id} product={a.selected} busy={a.busy} add={a.propose} panel={a.panel} /> : a.error ? <div className="ekt-empty" role="alert"><h2>Не удалось загрузить карточку</h2><p>{a.error}</p><button className="ekt-primary" onClick={a.retryDetail}>Повторить загрузку</button><button className="ekt-secondary" onClick={() => a.setMobileDetail(false)}>Вернуться к результатам</button></div> : <div className="ekt-detail-empty"><Icon name="box" /><h2>Товар в деталях</h2><p>Выберите карточку слева — здесь появятся характеристики, документы и остатки по складам.</p></div>;
  return <div className="ekt-app">
    <header className="ekt-header"><a href="/" className="ekt-back" aria-label="На главную EKT"><Icon name="arrow" /></a><div className="ekt-avatar"><Icon name="bot" /></div><div className="ekt-heading"><h1>{cartPage ? 'Корзина EKT' : 'Ассистент EKT.KZ'}</h1><p>Товары, характеристики и помощь с выбором</p></div><span className={`ekt-server ${a.online ? 'available' : ''}`}>{a.online == null ? 'Проверяем сервер…' : a.online ? 'Сервер доступен' : 'Нет связи с сервером'}</span><a className="ekt-header-cart" href={cartPage ? '/assistant' : '/cart'}><Icon name={cartPage ? 'search' : 'cart'} /><span>{cartPage ? 'К поиску' : `Корзина${a.cart ? ` (${a.cart.items.length})` : ''}`}</span></a></header>
    <div className="ekt-mode"><span className="ekt-mode-dot" />Поиск по каталогу — ИИ будет подключён позже</div>
    {a.error && <div className="ekt-error" role="alert">{a.error} <button className="ekt-text-button" onClick={a.checkConnection}>Проверить соединение</button></div>}
    {a.needSession && <div className="ekt-session"><span>Для продолжения нужна новая сессия. Прежние операции не повторятся.</span><button className="ekt-primary" disabled={a.busy} onClick={a.newSession}>Создать новую сессию</button></div>}
    {cartPage ? <CartContent cart={a.cart} loadError={a.error} refresh={a.refreshCart} accept={a.acceptCart} fail={a.fail} /> : <main className="ekt-workspace">
      <section className="ekt-conversation" aria-label="Поиск и результаты">
        <div className="ekt-chat-scroll">
          <div className="ekt-messages" role="log" aria-label="История действий">{a.messages.map(m => <div key={m.id} className={`ekt-message ${m.user ? 'user' : ''}`}>{!m.user && <span className="ekt-avatar small"><Icon name="bot" /></span>}<p>{m.text}</p></div>)}<div ref={chatEnd} /></div>
          <nav className="ekt-actions" aria-label="Действия с каталогом">{actions.map(x => <button key={x.action} className="ekt-action" onClick={() => a.act(x.action)}><Icon name={x.icon} />{x.title}</button>)}<a className="ekt-action" href="/cart"><Icon name="cart" />Корзина</a></nav>
          <div className="ekt-section-heading" ref={resultsHeading}><div><h2>{a.searched === 'Подбор аналогов' ? 'Кандидаты на замену' : a.searched ? 'Результаты поиска' : 'Товары из каталога'}</h2><p className="ekt-caption">{a.results ? `${a.results.totalElements} товаров · выберите карточку` : 'Данные из каталога EKT'}</p></div>{a.searched && <button className="ekt-text-button" onClick={() => { a.setQuery(''); void a.load(''); }}>Весь каталог</button>}</div>
          {a.loading && <div className="ekt-loading" role="status">Ищем товары…</div>}
          {!a.loading && a.results && !a.results.items.length && <div className="ekt-empty"><Icon name="search" /><h3>Подходящих товаров не найдено</h3><p>Попробуйте другой артикул или уточните подбор у менеджера.</p></div>}
          <div className="ekt-products" aria-busy={a.loading}>{a.results?.items.map(p => <article key={p.id} className={`ekt-product ${a.selected?.id === p.id ? 'selected' : ''}`}>
            <button className="ekt-product-open" onClick={() => a.choose(p)} aria-label={`Открыть ${p.name}`}><div className="ekt-product-image"><ProductImage key={p.image} product={p} /></div><h3>{p.name}</h3><p className="ekt-caption">Арт. {p.article || 'не указан'}</p></button>
            <div className="ekt-card-price">{number(p.price)}</div><p className="ekt-caption">Валюта не подтверждена</p><Stock quantity={p.quantity} />
            {p.properties ? <div className="ekt-card-specs"><span>Ток: {p.characteristicConflicts?.some(c => c.parameter === 'NOMINALNYY_TOK') ? 'конфликт источников' : valueText(p.properties.NOMINALNYY_TOK)}</span><span>Полюса: {valueText(p.properties.KOLICHESTVO_POLYUSOV)}</span></div> : <p className="ekt-caption">Характеристики — в карточке</p>}
            {!!p.warnings?.length && <p className="ekt-card-warning">Есть предупреждения — проверьте карточку</p>}
            {a.analogNotes[p.id] && <p className="ekt-warning">Предварительный кандидат. {a.analogNotes[p.id]}</p>}
            <button className="ekt-primary" disabled={a.busy || p.price == null || p.quantity == null || p.quantity <= 0} onClick={() => a.propose(p, Number(String(p.properties?.KRATNOST_MIN ?? 1).replace(',', '.')))}><Icon name="cart" />В корзину</button>
          </article>)}</div>
          {!!a.results && a.results.totalPages > 1 && <nav className="ekt-pagination" aria-label="Страницы результатов"><button className="ekt-secondary" disabled={a.results.page === 0 || a.loading} onClick={() => a.load(a.searched, a.results!.page - 1)}>Назад</button><span>{a.results.page + 1} / {a.results.totalPages}</span><button className="ekt-secondary" disabled={a.results.page + 1 >= a.results.totalPages || a.loading} onClick={() => a.load(a.searched, a.results!.page + 1)}>Далее</button></nav>}
        </div>
        <form className="ekt-composer" onSubmit={e => { e.preventDefault(); void a.act('search'); }}><label className="visually-hidden" htmlFor="catalog-query">Название или артикул товара</label><Icon name="search" /><input id="catalog-query" type="search" value={a.query} onChange={e => a.setQuery(e.target.value)} placeholder="Введите название или артикул товара" /><button type="submit" className="ekt-send" aria-label="Отправить поиск"><Icon name="send" /></button><p>Поиск по тексту. Произвольные вопросы пока не обрабатываются.</p></form>
      </section>
      {!mobile && <aside className="ekt-detail" aria-label="Подробная карточка">{detail}</aside>}
    </main>}
    {mobile && a.mobileDetail && !cartPage && <Modal title="Карточка товара" close={() => a.setMobileDetail(false)}>{detail}</Modal>}
    {a.proposal && <Modal title={a.changed ? 'Условия изменились' : 'Подтвердите добавление'} busy={a.busy} close={() => a.setProposal(null)}>{a.changed && <div className="ekt-warning">Цена или остаток изменились. Проверьте новые данные и подтвердите ещё раз. Корзина пока не изменена.</div>}<h3>{a.proposal.name}</h3><dl className="ekt-specs"><div><dt>Количество</dt><dd>{number(a.proposal.quantity)}</dd></div><div><dt>Цена</dt><dd>{number(a.proposal.price)}</dd></div><div><dt>Сумма</dt><dd>{number(a.proposal.quantity * a.proposal.price)}</dd></div><div><dt>Остаток EKT</dt><dd>{number(a.proposal.stock)}</dd></div></dl><p className="ekt-caption">Валюта и единицы не подтверждены. Предложение до {new Date(a.proposal.expiresAt).toLocaleTimeString('ru-RU')}. Это локальная корзина без заказа и резерва EKT.</p>{a.error && <div role="alert" className="ekt-error">{a.error}</div>}<button className="ekt-primary ekt-wide" onClick={a.confirm} disabled={a.busy}>{a.busy ? 'Подождите…' : a.changed ? 'Подтвердить новые условия' : 'Подтвердить добавление'}</button></Modal>}
    {a.terms && <Modal title="Доставка и оплата" close={() => a.setTerms(null)}><div className={!a.terms.verified ? 'ekt-warning' : 'ekt-notice'}>{a.terms.verified ? 'Условия из настроенного источника' : 'Условия покупки не подтверждены'}</div><dl className="ekt-specs"><div><dt>Оплата</dt><dd>{a.terms.payment}</dd></div><div><dt>Доставка</dt><dd>{a.terms.delivery}</dd></div><div><dt>Минимальный заказ</dt><dd>{a.terms.minimumOrder}</dd></div></dl><p>{a.terms.message || a.terms.source}</p></Modal>}
  </div>;
}
