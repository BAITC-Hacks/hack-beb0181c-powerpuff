export interface NavLink {
  label: string;
  href: string;
  hasDropdown?: boolean;
}

export interface Banner {
  id: number;
  title: string;
  image: string;
  href: string;
}

export interface Category {
  id: number;
  title: string;
  image: string;
  href: string;
}

export interface Promo {
  id: number;
  title: string;
  image: string;
  href: string;
}

export interface Feature {
  title: string;
  icon: string;
}

export interface FooterColumn {
  title: string;
  links: NavLink[];
}