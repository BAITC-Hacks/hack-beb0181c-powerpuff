import "./SectionTitle.css";

export function SectionTitle({ children }: { children: string }) {
  return <h2 className="section-title">{children}</h2>;
}
