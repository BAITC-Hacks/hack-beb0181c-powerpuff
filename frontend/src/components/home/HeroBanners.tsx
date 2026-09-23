import { MAIN_BANNERS, SIDE_BANNERS } from "../../data/home";
import { useCarousel } from "../../hooks/useCarousel";
import "./HeroBanners.css";

export function HeroBanners() {
  const main = useCarousel(MAIN_BANNERS.length, 5000);
  const side = useCarousel(SIDE_BANNERS.length, 4000);

  return (
    <section className="container hero" aria-label="Акции и новинки">
      <div className="hero__main" onMouseEnter={main.pause} onMouseLeave={main.resume}>
        <div className="hero__track" style={{ transform: `translateX(-${main.index * 100}%)` }}>
          {MAIN_BANNERS.map((banner, i) => (
            <a
              key={banner.id}
              href={banner.href}
              className="hero__slide"
              aria-hidden={i !== main.index}
              tabIndex={i === main.index ? 0 : -1}
            >
              <img src={banner.image} alt={banner.title} loading={i === 0 ? "eager" : "lazy"} />
            </a>
          ))}
        </div>

        <button
          type="button"
          className="hero__arrow hero__arrow--prev"
          onClick={main.prev}
          aria-label="Предыдущий баннер"
        >
          ‹
        </button>
        <button
          type="button"
          className="hero__arrow hero__arrow--next"
          onClick={main.next}
          aria-label="Следующий баннер"
        >
          ›
        </button>

        <div className="hero__dots">
          {MAIN_BANNERS.map((banner, i) => (
            <button
              key={banner.id}
              type="button"
              className={i === main.index ? "is-active" : ""}
              onClick={() => main.goTo(i)}
              aria-label={`Баннер ${i + 1}: ${banner.title}`}
            />
          ))}
        </div>
      </div>

      <div className="hero__side" onMouseEnter={side.pause} onMouseLeave={side.resume}>
        <div className="hero__track" style={{ transform: `translateX(-${side.index * 100}%)` }}>
          {SIDE_BANNERS.map((banner, i) => (
            <a
              key={banner.id}
              href={banner.href}
              className="hero__slide"
              aria-hidden={i !== side.index}
              tabIndex={i === side.index ? 0 : -1}
            >
              <img src={banner.image} alt={banner.title} loading="lazy" />
            </a>
          ))}
        </div>

        <div className="hero__counter">
          <button type="button" onClick={side.prev} aria-label="Предыдущий">
            ‹
          </button>
          <span>
            {side.index + 1} / {SIDE_BANNERS.length}
          </span>
          <button type="button" onClick={side.next} aria-label="Следующий">
            ›
          </button>
          <i style={{ width: `${((side.index + 1) / SIDE_BANNERS.length) * 100}%` }} />
        </div>
      </div>
    </section>
  );
}
