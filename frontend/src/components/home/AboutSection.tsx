import { ABOUT_IMAGE, ABOUT_TEXT, FEATURES } from "../../data/home";
import { SectionTitle } from "./SectionTitle";
import "./AboutSection.css";

export function AboutSection() {
  return (
    <section className="about">
      <div className="container">
        <SectionTitle>О компании</SectionTitle>

        <div className="about__content">
          <img src={ABOUT_IMAGE} alt="Группа компаний Электрокомплект" className="about__photo" loading="lazy" />
          <div className="about__text">
            {ABOUT_TEXT.map((paragraph) => (
              <p key={paragraph.slice(0, 20)}>{paragraph}</p>
            ))}
          </div>
        </div>

        <ul className="about__features">
          {FEATURES.map((feature) => (
            <li key={feature.title}>
              <img src={feature.icon} alt="" width={80} height={80} loading="lazy" />
              <span>{feature.title}</span>
            </li>
          ))}
        </ul>

        <div className="about__more">
          <a href="https://ekt.kz/about/">Узнать больше</a>
        </div>
      </div>
    </section>
  );
}
