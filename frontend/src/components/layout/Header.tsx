import { useEffect, useState } from "react";
import { api, session } from "../../assistant/api";
import { Icon } from "../../assistant/components";
import { CatalogMenu } from "./CatalogMenu";
import "./Header.css";

interface HeaderProps {
  cartCount?: number;
}

export function Header({ cartCount }: HeaderProps) {
  const [count, setCount] = useState<number | null>(cartCount ?? null);
  useEffect(() => { if (session.get()) api.cart().then(c => setCount(c.items.length)).catch(() => setCount(null)); }, []);
  const [query, setQuery] = useState("");
  const [catalogOpen, setCatalogOpen] = useState(false);

  return (
    <header className="header">
      <div className="container header__inner">
        <a href="/" className="header__logo" aria-label="Группа компаний Электрокомплект — на главную">
          <img src="/images/icons/logo.svg" alt="Группа компаний Электрокомплект" width={176} height={37} />
        </a>

        <button type="button" className="header__catalog" onClick={() => setCatalogOpen(true)}>
          <span>Каталог</span>
          <img src="/images/icons/burger.svg" alt="" width={16} height={14} />
        </button>

        <form className="header__search" role="search" onSubmit={(e) => { e.preventDefault(); window.location.assign(`/assistant${query.trim() ? `?q=${encodeURIComponent(query.trim())}` : ""}`); }}>
          <svg
            className="header__search-icon"
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            aria-hidden="true"
          >
            <circle cx="11" cy="11" r="7" stroke="#8c8c8c" strokeWidth="1.8" />
            <path d="m20 20-3.5-3.5" stroke="#8c8c8c" strokeWidth="1.8" strokeLinecap="round" />
          </svg>
          <input
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Название, артикул или бренд"
            aria-label="Поиск по каталогу"
          />
          <button type="submit" className="header__search-submit" aria-label="Найти товар"><Icon name="search" /></button>
        </form>

        <nav className="header__actions" aria-label="Личные разделы">
          <a href="/assistant" className="header__action" aria-label="Ассистент EKT"><Icon name="bot" /><span>Ассистент</span></a>
          <a href="https://ekt.kz/catalog/compare/" className="header__action" aria-label="Сравнить на сайте EKT">
            <img src="/images/icons/compare.svg" alt="" width={12} height={13} />
            <span>Сравнить</span>
          </a>
          <a href="https://ekt.kz/personal/favorites/" className="header__action" aria-label="Избранное на сайте EKT">
            <img src="/images/icons/heart.svg" alt="" width={19} height={17} />
            <span>Избранное</span>
          </a>
          <a href="/cart" className="header__action header__action--cart" aria-label="Корзина">
            <Icon name="cart" />
            <span>Корзина</span>
            {count !== null && count > 0 && <b className="header__badge">{count}</b>}
          </a>
        </nav>
      </div>

      <CatalogMenu open={catalogOpen} onClose={() => setCatalogOpen(false)} />
    </header>
  );
}
