import { WhatsAppButton } from "./WhatsAppButton";
import { useState, type FormEvent } from "react";
import { CATEGORIES, FOOTER_COLUMNS, SUBSCRIBER_TYPES } from "../../data/home";
import "./Footer.css";

export function Footer() {
  const [subscribed, setSubscribed] = useState(false);

  const onSubscribe = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setSubscribed(true);
  };

  return (
    <footer className="footer">
      <div className="container">
        <div className="footer__top">
          <div className="footer__col">
            <h4>Подписаться на рассылку</h4>
            {subscribed ? (
              <p className="footer__done">Спасибо! Вы подписаны на рассылку.</p>
            ) : (
              <form className="footer__subscribe" onSubmit={onSubscribe}>
                <input type="email" required placeholder="Введите ваш e-mail" aria-label="E-mail" />
                <select defaultValue="" required aria-label="Кто вы">
                  <option value="" disabled>
                    Укажите, кто Вы?
                  </option>
                  {SUBSCRIBER_TYPES.map((type) => (
                    <option key={type}>{type}</option>
                  ))}
                </select>
                <button type="submit">Подписаться</button>
              </form>
            )}
          </div>

          {FOOTER_COLUMNS.map((column) => (
            <div className="footer__col" key={column.title}>
              <h4>{column.title}</h4>
              <ul>
                {column.links.map((link) => (
                  <li key={link.label}>
                    <a href={link.href}>{link.label}</a>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <hr />

        <div className="footer__middle">
          <div className="footer__payments">
            <WhatsAppButton />
            <img src="/images/payments/cloudpayments.jpg" alt="Cloudpayments" />
            <img src="/images/payments/payment-2.jpg" alt="Способы оплаты" />
          </div>
          <ul className="footer__catalog">
            <li>
              <a href="https://ekt.kz/catalog/spets_predlozhenie/">Спец Предложение</a>
            </li>
            <li>
              <a href="https://ekt.kz/catalog/novinki/">Новинки</a>
            </li>
            {CATEGORIES.map((category) => (
              <li key={category.id}>
                <a href={category.href}>{category.title}</a>
              </li>
            ))}
          </ul>
        </div>

        <div className="footer__bottom">
          <span>© 2020 Группа компаний Электрокомплект</span>
          <a href="https://ekt.kz/politika-konfidentsialnosti/">Политика конфиденциальности</a>
        </div>
      </div>
    </footer>
  );
}
