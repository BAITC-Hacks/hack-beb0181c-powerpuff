import { CATEGORIES } from "../../data/home";
import { SectionTitle } from "./SectionTitle";
import "./CategoryGrid.css";

export function CategoryGrid() {
  return (
    <section className="container categories">
      <SectionTitle>Каталог продукции</SectionTitle>
      <div className="categories__grid">
        {CATEGORIES.map((category) => (
          <a key={category.id} href={category.href} className="categories__item">
            <span className="categories__image">
              <img src={category.image} alt="" loading="lazy" />
            </span>
            <span className="categories__title">{category.title}</span>
          </a>
        ))}
      </div>
    </section>
  );
}
