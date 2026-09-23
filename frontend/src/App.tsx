import { HomePage } from "./pages/HomePage";
import { AssistantPage } from "./assistant/AssistantPage";

export default function App() {
  const path = window.location.pathname.replace(/\/$/, '');
  if (path === '/assistant' || path === '/cart') return <AssistantPage cartPage={path === '/cart'} />;
  return <HomePage />;
}
