import { Footer } from "../components/layout/Footer";
import { Header } from "../components/layout/Header";
import { TopBar } from "../components/layout/TopBar";
import { AssistantLauncher } from "../components/layout/AssistantLauncher";
import "./HomePage.css";
import { AboutSection } from "../components/home/AboutSection";
import { CategoryGrid } from "../components/home/CategoryGrid";
import { HeroBanners } from "../components/home/HeroBanners";
import { PromoTabs } from "../components/home/PromoTabs";

export function HomePage() {
  return (
    <div className="ekt-home">
      <TopBar />
      <Header />
      <main>
        <HeroBanners />
        <CategoryGrid />
        <PromoTabs />
        <AboutSection />
      </main>
      <Footer />
      <AssistantLauncher />
    </div>
  );
}
