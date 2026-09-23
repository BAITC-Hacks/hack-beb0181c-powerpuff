import { useState } from "react";
import { CatalogMenu } from "./CatalogMenu";
import "./Header.css";

interface HeaderProps {
  cartCount?: number;
}

export function Header({ cartCount = 0 }: HeaderProps) {
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

        <form className="header__search" role="search" onSubmit={(e) => e.preventDefault()}>
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
            placeholder="Поиск"
            aria-label="Поиск по каталогу"
          />
          <button type="button" className="header__search-action" aria-label="Голосовой поиск">
            <img src="/images/icons/mic.svg" alt="" width={24} height={24} />
          </button>
          <button type="button" className="header__search-action" aria-label="Поиск по фото">
            <img src="/images/icons/camera.svg" alt="" width={24} height={24} />
          </button>
        </form>

        <nav className="header__actions" aria-label="Личные разделы">
          <a href="https://ekt.kz/catalog/compare/" className="header__action">
            <img src="/images/icons/compare.svg" alt="" width={12} height={13} />
            <span>Сравнить</span>
          </a>
          <a href="https://ekt.kz/personal/favorites/" className="header__action">
            <img src="/images/icons/heart.svg" alt="" width={19} height={17} />
            <span>Избранное</span>
          </a>
          <a href="https://ekt.kz/personal/cart/" className="header__action header__action--cart">
            <img src="/images/icons/cart.svg" alt="" width={18} height={17} />
            <span>Корзина</span>
            <b className="header__badge">{cartCount}</b>
          </a>
        </nav>
      </div>

      <CatalogMenu open={catalogOpen} onClose={() => setCatalogOpen(false)} />
    </header>
  );
}
