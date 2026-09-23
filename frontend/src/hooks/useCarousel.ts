import { useCallback, useEffect, useState } from "react";

export function useCarousel(length: number, intervalMs = 5000) {
  const [index, setIndex] = useState(0);
  const [paused, setPaused] = useState(false);

  const goTo = useCallback((i: number) => setIndex(((i % length) + length) % length), [length]);
  const next = useCallback(() => setIndex((i) => (i + 1) % length), [length]);
  const prev = useCallback(() => setIndex((i) => (i - 1 + length) % length), [length]);

  useEffect(() => {
    if (paused || length < 2) return undefined;
    const timer = setInterval(next, intervalMs);
    return () => clearInterval(timer);
  }, [paused, length, intervalMs, next]);

  return { index, goTo, next, prev, pause: () => setPaused(true), resume: () => setPaused(false) };
}