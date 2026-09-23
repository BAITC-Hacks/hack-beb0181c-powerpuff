import { useRef, useState } from "react";
import { NEW_PRODUCTS, SPECIAL_OFFERS } from "../../data/home";
import type { Promo } from "../../types/home";
import "./PromoTabs.css";

type Tab = "new" | "special";

const TABS: { id: Tab; label: string; items: Promo[] }[] = [
  { id: "new", label: "Новинки", items: NEW_PRODUCTS },
  { id: "special", label: "Спец предложения", items: SPECIAL_OFFERS },
];

export function PromoTabs() {
  const [active, setActive] = useState<Tab>("new");
  const trackRef = useRef<HTMLDivElement>(null);
  const current = TABS.find((tab) => tab.id === active) ?? TABS[0];

  const scroll = (direction: 1 | -1) => {
    const track = trackRef.current;
    if (!track) return;
    track.scrollBy({ left: direction * track.clientWidth * 0.8, behavior: "smooth" });
  };

  return (
    <section className="container promo">
      <div className="promo__tabs" role="tablist">
        {TABS.map((tab) => (
          <button
            key={tab.id}
            type="button"
            role="tab"
            aria-selected={tab.id === active}
            className={tab.id === active ? "is-active" : ""}
            onClick={() => {
              setActive(tab.id);
              trackRef.current?.scrollTo({ left: 0 });
            }}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <div className="promo__body">
        <button type="button" className="promo__arrow promo__arrow--prev" onClick={() => scroll(-1)} aria-label="Назад">
          ‹
        </button>
        <div className="promo__track" ref={trackRef} role="tabpanel">
          {current.items.map((item) => (
            <a key={`${active}-${item.id}`} href={item.href} className="promo__card">
              {active === "new" && <span className="promo__label">New</span>}
              <img src={item.image} alt={item.title} loading="lazy" />
            </a>
          ))}
        </div>
        <button type="button" className="promo__arrow promo__arrow--next" onClick={() => scroll(1)} aria-label="Вперёд">
          ›
        </button>
      </div>
    </section>
  );
}
