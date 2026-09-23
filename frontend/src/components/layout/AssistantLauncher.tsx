import { useState } from 'react';
import { Icon } from '../../assistant/components';
import './AssistantLauncher.css';

export function AssistantLauncher() {
  const [compact, setCompact] = useState(false);
  return <aside className={`assistant-launcher ${compact ? 'is-compact' : ''}`} aria-label="Помощник по каталогу">
    {!compact && <div className="assistant-launcher__hint"><strong>Поможем найти нужное</strong><span>Товар, характеристики и наличие — в одном месте.</span></div>}
    <div className="assistant-launcher__row">
      <button type="button" className="assistant-launcher__toggle" aria-label={compact ? 'Развернуть помощника' : 'Свернуть помощника'} aria-expanded={!compact} onClick={() => setCompact(!compact)}><Icon name={compact ? 'arrow' : 'close'} /></button>
      <a href="/assistant" className="assistant-launcher__link" aria-label="Открыть ассистента EKT — поиск по каталогу">
        <span className="assistant-launcher__avatar"><Icon name="bot" /></span>
        {!compact && <><span className="assistant-launcher__copy"><strong>Ассистент EKT.KZ</strong><span>Поиск по каталогу · без ИИ</span></span><svg className="assistant-launcher__arrow" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true"><path d="m9 5 7 7-7 7" /></svg></>}
      </a>
    </div>
  </aside>;
}
