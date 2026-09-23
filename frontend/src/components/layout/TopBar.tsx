import { CITY, PHONES, TOP_LINKS } from "../../data/home";
import "./TopBar.css";

export function TopBar() {
  return (
    <div className="top-bar">
      <div className="container top-bar__inner">
        <button type="button" className="top-bar__city">
          <img src="/images/icons/pin.svg" alt="" width={15} height={17} />
          {CITY}
        </button>

        <nav className="top-bar__nav" aria-label="Информация для покупателей">
          {TOP_LINKS.map((link) => (
            <a key={link.label} href={link.href} className="top-bar__link">
              {link.label}
              {link.hasDropdown && <img src="/images/icons/arrow-down.svg" alt="" width={12} height={7} />}
            </a>
          ))}
        </nav>

        <div className="top-bar__phones">
          <img src="/images/icons/phone.svg" alt="" width={17} height={17} />
          <div>
            {PHONES.map((phone) => (
              <a key={phone} href={`tel:${phone.replace(/[^\d+]/g, "")}`}>
                {phone}
              </a>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
