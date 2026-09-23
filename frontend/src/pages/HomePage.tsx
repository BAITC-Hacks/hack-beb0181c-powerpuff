import { Footer } from "../components/layout/Footer";
import { Header } from "../components/layout/Header";
import { TopBar } from "../components/layout/TopBar";
import { WhatsAppButton } from "../components/layout/WhatsAppButton";
import { AboutSection } from "../components/home/AboutSection";
import { CategoryGrid } from "../components/home/CategoryGrid";
import { HeroBanners } from "../components/home/HeroBanners";
import { PromoTabs } from "../components/home/PromoTabs";

export function HomePage() {
  return (
    <>
      <TopBar />
      <Header />
      <main>
        <HeroBanners />
        <CategoryGrid />
        <PromoTabs />
        <AboutSection />
      </main>
      <Footer />
      <WhatsAppButton />
    </>
  );
}
