import { useEffect } from "react";
import { CATEGORIES } from "../../data/home";
import "./CatalogMenu.css";

interface CatalogMenuProps {
  open: boolean;
  onClose: () => void;
}

export function CatalogMenu({ open, onClose }: CatalogMenuProps) {
  useEffect(() => {
    if (!open) return undefined;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, onClose]);

  return (
    <div className={`catalog-menu${open ? " is-open" : ""}`} aria-hidden={!open}>
      <div className="catalog-menu__backdrop" onClick={onClose} />
      <aside className="catalog-menu__panel" aria-label="Каталог">
        <div className="catalog-menu__head">
          <b>Каталог</b>
          <button type="button" onClick={onClose} aria-label="Закрыть каталог">
            ×
          </button>
        </div>
        <ul>
          {CATEGORIES.map((category) => (
            <li key={category.id}>
              <a href={category.href}>
                <img src={category.image} alt="" loading="lazy" />
                {category.title}
              </a>
            </li>
          ))}
        </ul>
      </aside>
    </div>
  );
}
