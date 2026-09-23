import type { Banner, Category, Feature, FooterColumn, NavLink, Promo } from "../types/home";

const SITE = "https://ekt.kz";

export const CITY = "Астана";

export const PHONES = ["+7(700) 222-05-14", "+7(747) 222-05-21"];

export const TOP_LINKS: NavLink[] = [
  { label: "Личный кабинет", href: `${SITE}/personal/` },
  { label: "B2B - ЕКТ PRO", href: "https://pro.ekt.kz/" },
  { label: "Покупателям", href: `${SITE}/about/`, hasDropdown: true },
  { label: "Оставить заявку", href: "#request" },
  { label: "ҚАЗ", href: `${SITE}/kz/` },
];

export const MAIN_BANNERS: Banner[] = [
  {
    id: 1,
    title: "QUATTRO NEW",
    image: "/images/banners/banner-1.webp",
    href: "https://ekt.kz/catalog/rozetki_vyklyuchateli_korobki/rozetki_vyklyuchateli_unit/quattro/",
  },
  {
    id: 2,
    title: "Кабель - ГОСТ",
    image: "/images/banners/banner-2.webp",
    href: "https://ekt.kz/catalog/kabel_provod/",
  },
  {
    id: 3,
    title: "Новинка: Муфты ЭРГ Оптима",
    image: "/images/banners/banner-3.webp",
    href: "https://ekt.kz/catalog/novinki/sec_a896c6a4d63e/",
  },
  {
    id: 4,
    title: "Легранд - снижение цен",
    image: "/images/banners/banner-4.webp",
    href: "https://ekt.kz/catalog/rozetki_vyklyuchateli_korobki/rozetki_vyklyuchateli_legrand_mosaic/",
  },
  { id: 5, title: "Инструменты КВТ - 15%", image: "/images/banners/banner-5.webp", href: "#" },
  {
    id: 6,
    title: "Лотки металлические скидка",
    image: "/images/banners/banner-6.webp",
    href: "https://ekt.kz/catalog/kabelenesushchie_sistemy/lotok_metallicheskiy/",
  },
  {
    id: 7,
    title: "Новинка: Промрукав",
    image: "/images/banners/banner-7.webp",
    href: "https://ekt.kz/catalog/novinki/sec_46b200dc26f8/",
  },
  {
    id: 8,
    title: "Новинка: Воздушный АВ CHINT",
    image: "/images/banners/banner-8.webp",
    href: "https://ekt.kz/catalog/novinki/sec_7652fb35a561/",
  },
];

export const SIDE_BANNERS: Banner[] = [
  {
    id: 1,
    title: "QUATTRO NEW",
    image: "/images/side/side-1.webp",
    href: "https://ekt.kz/catalog/rozetki_vyklyuchateli_korobki/rozetki_vyklyuchateli_unit/quattro/",
  },
  { id: 2, title: "Кабель - ГОСТ", image: "/images/side/side-2.webp", href: "https://ekt.kz/catalog/kabel_provod/" },
  {
    id: 3,
    title: "Новинка: Муфты ЭРГ Оптима",
    image: "/images/side/side-3.webp",
    href: "https://ekt.kz/catalog/novinki/sec_a896c6a4d63e/",
  },
  {
    id: 4,
    title: "Легранд - снижение цен",
    image: "/images/side/side-4.webp",
    href: "https://ekt.kz/catalog/rozetki_vyklyuchateli_korobki/rozetki_vyklyuchateli_legrand_mosaic/",
  },
  { id: 5, title: "Инструменты КВТ - 15%", image: "/images/side/side-5.webp", href: "#" },
  {
    id: 6,
    title: "Новинка КВТ",
    image: "/images/side/side-6.webp",
    href: "https://ekt.kz/catalog/novinki/novinka_kvt/",
  },
  {
    id: 7,
    title: "Семинар Владислав Волков",
    image: "/images/side/side-7.webp",
    href: "https://ekt.kz/news/seminar-vladislav-volkov/",
  },
  {
    id: 8,
    title: "Лотки металлические скидка",
    image: "/images/side/side-8.webp",
    href: "https://ekt.kz/catalog/kabelenesushchie_sistemy/lotok_metallicheskiy/",
  },
  {
    id: 9,
    title: "Tandem new",
    image: "/images/side/side-9.webp",
    href: "https://ekt.kz/catalog/svetilniki_lampy/prozhektory/led-prozhektor-tandem-30-50-100w-8500lm-260kh190kh49-5-2700-4000-5700k-ip65-megalight-10-c3a424cb/",
  },
  {
    id: 10,
    title: "Signall / Orient / Tandem for MEGALIGHT",
    image: "/images/side/side-10.webp",
    href: "https://ekt.kz/catalog/svetilniki_lampy/prozhektory/?PAGEN_1=2",
  },
  {
    id: 11,
    title: "Новинка: Воздушный АВ CHINT",
    image: "/images/side/side-11.webp",
    href: "https://ekt.kz/catalog/novinki/sec_7652fb35a561/",
  },
];

export const CATEGORIES: Category[] = [
  { id: 1, title: "Кабель / Провод", image: "/images/categories/category-1.png", href: "https://ekt.kz/catalog/kabel_provod/" },
  { id: 2, title: "Светильники / Лампы", image: "/images/categories/category-2.png", href: "https://ekt.kz/catalog/svetilniki_lampy/" },
  { id: 3, title: "Низковольтная аппаратура", image: "/images/categories/category-3.png", href: "https://ekt.kz/catalog/nizkovoltnaya_apparatura/" },
  { id: 4, title: "Кабеленесущие системы", image: "/images/categories/category-4.png", href: "https://ekt.kz/catalog/kabelenesushchie_sistemy/" },
  { id: 5, title: "Изделия для монтажа и инструмент", image: "/images/categories/category-5.png", href: "https://ekt.kz/catalog/izdeliya_dlya_montazha_i_instrument/" },
  { id: 6, title: "Прочее оборудование", image: "/images/categories/category-6.png", href: "https://ekt.kz/catalog/prochee_oborudovanie/" },
  { id: 7, title: "Шкафы / Щиты", image: "/images/categories/category-7.png", href: "https://ekt.kz/catalog/shkafy_shchity/" },
  { id: 8, title: "Розетки/Выключатели/Коробки", image: "/images/categories/category-8.png", href: "https://ekt.kz/catalog/rozetki_vyklyuchateli_korobki/" },
  { id: 9, title: "Автоматизация", image: "/images/categories/category-9.png", href: "https://ekt.kz/catalog/avtomatizatsiya/" },
  { id: 10, title: "Видеонаблюдение / СКУД / Сигнализация", image: "/images/categories/category-10.png", href: "https://ekt.kz/catalog/videonablyudenie_skud_signalizatsiya/" },
  { id: 11, title: "Инструмент / КИП", image: "/images/categories/category-11.png", href: "https://ekt.kz/catalog/instrument_kip/" },
  { id: 12, title: "Корзина Электрика", image: "/images/categories/category-12.jpg", href: "https://ekt.kz/catalog/korzina_elektrika/" },
];

export const NEW_PRODUCTS: Promo[] = [
  { id: 1, title: "Выключатели концевые IEK", image: "/images/promo/promo-1.webp", href: "https://ekt.kz/catalog/novinki/novinka_vyklyuchateli_kontsevye_iek/" },
  { id: 2, title: "Гвозди TOUA 3,0×25 мм", image: "/images/promo/promo-2.webp", href: "https://ekt.kz/catalog/novinki/novinka_toua_gvozdi_3_0kh25mm/" },
  { id: 3, title: "Светильники TRIO", image: "/images/promo/promo-3.webp", href: "https://ekt.kz/catalog/novinki/novinka_svetilniki_trio/" },
  { id: 4, title: "Щиты пластиковые FORT IP65", image: "/images/promo/promo-4.webp", href: "https://ekt.kz/catalog/novinki/novinka_rasshirenie_assortimenta_shchity_plastikovye_fort_ip65/" },
  { id: 5, title: "Новинки CHINT", image: "/images/promo/promo-5.webp", href: "https://ekt.kz/catalog/novinki/novinka_chint/" },
  { id: 6, title: "Воздушный автоматический выключатель CHINT", image: "/images/promo/promo-6.webp", href: "https://ekt.kz/catalog/novinki/sec_7652fb35a561/" },
  { id: 7, title: "Муфты ЭРГ Оптима", image: "/images/promo/promo-7.webp", href: "https://ekt.kz/catalog/novinki/sec_a896c6a4d63e/" },
  { id: 8, title: "Новинки ТОРС IEK", image: "/images/promo/promo-8.webp", href: "https://ekt.kz/catalog/novinki/novinka_tors_iek/" },
  { id: 9, title: "Новинки КВТ", image: "/images/promo/promo-9.webp", href: "https://ekt.kz/catalog/novinki/novinka_kvt/" },
  { id: 10, title: "Выключатели-разъединители ВРТ IEK", image: "/images/promo/promo-10.webp", href: "https://ekt.kz/catalog/novinki/novinka_vyklyuchateli_razediniteli_trekhpozitsionnye_vrt_iek/" },
];

export const SPECIAL_OFFERS: Promo[] = SIDE_BANNERS.filter((b) =>
  ["Легранд - снижение цен", "Инструменты КВТ - 15%", "Лотки металлические скидка"].includes(b.title),
);

export const ABOUT_TEXT = [
  "Группа Компаний Электрокомплект – крупнейший производитель и поставщик электротехнической продукции напряжением до 1кВ на рынке Республики Казахстан. На базе нашей компании эффективно реализуется производство: кабельно-проводниковой продукции, труб ПНД и ПВХ, кабельного канала, производство и сборка щитового оборудования.",
  "ГК ЭлектроКомплект – это не только точки продаж по всему Казахстану, производство и собственные торговые марки, это также долгосрочное сотрудничество с крупнейшими производителями электротехники.",
  "Мы несем свет и энергию нашим клиентам, гордимся и ценим то, что делаем для своих клиентов! Работать с нами удобно, надежно и выгодно!",
];

export const ABOUT_IMAGE = "/images/about/about.jpg";

export const FEATURES: Feature[] = [
  { title: "Гарантия Качества", icon: "/images/about/feature-1.png" },
  { title: "Оптимальная Цена", icon: "/images/about/feature-2.png" },
  { title: "Всегда в наличии", icon: "/images/about/feature-3.png" },
  { title: "Индивидуальный подход", icon: "/images/about/feature-4.png" },
];

export const SUBSCRIBER_TYPES = [
  "Потребитель Физ.лицо",
  "Электрик",
  "Торгующая Организация",
  "Монтажная Организация",
  "Строительная Организация",
  "Производственная Организация",
  "Сборщик Щитового оборудования Организация",
  "Прочее Юр.лицо",
];

export const FOOTER_COLUMNS: FooterColumn[] = [
  {
    title: "Группа компаний Электрокомплект",
    links: [
      { label: "О компании", href: `${SITE}/about/` },
      { label: "Вакансии", href: `${SITE}/about/our-team/vacancy/` },
      { label: "Новости", href: `${SITE}/news/` },
      { label: "Контакты", href: `${SITE}/about/contacts/` },
      { label: "Платформа для бизнеса B2B", href: "https://vde4kag10.ukit.me/" },
    ],
  },
  {
    title: "Будь с нами",
    links: [
      { label: "Вход/Регистрация", href: `${SITE}/personal/` },
      { label: "Полезная информация", href: `${SITE}/about/information/` },
      { label: "Часто задаваемые вопросы", href: `${SITE}/about/faq/` },
      { label: "Карьера", href: `${SITE}/about/our-team/` },
      { label: "Сотрудничество", href: `${SITE}/cooperation/` },
    ],
  },
  {
    title: "Интернет магазин",
    links: [
      { label: "Как сделать заказ?", href: `${SITE}/about/howto/` },
      { label: "Доставка и оплата", href: `${SITE}/checkout-delivery/` },
      { label: "Условия возврата и обмена", href: `${SITE}/return/` },
      { label: "AirbaPay", href: `${SITE}/payments/` },
      { label: "Условия по рассрочке", href: `${SITE}/usloviya-po-rassrochke/` },
    ],
  },
];

export const WHATSAPP_URL = "https://api.whatsapp.com/send?phone=77475110521&text=Добрый%20день!";