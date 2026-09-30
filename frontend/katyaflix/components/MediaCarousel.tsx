"use client";

import { useEffect, useRef, useState } from "react";
import type { MediaSummary } from "@/types/media";
import Poster from "./Poster";
import { ChevronLeft, ChevronRight } from "react-bootstrap-icons";

type MediaCarouselProps = {
  title: string;
  items: MediaSummary[];
  isLoading?: boolean;
};

// How far one click of an arrow scrolls, as a fraction of the row's
// visible width — 0.9 rather than 1.0 so the last-visible card before the
// click stays partially visible as a continuity cue.
const SCROLL_STEP_FRACTION = 0.9;

export default function MediaCarousel({
  title,
  items,
  isLoading,
}: MediaCarouselProps) {
  const scrollRef = useRef<HTMLDivElement | null>(null);
  const [canScrollLeft, setCanScrollLeft] = useState(false);
  const [canScrollRight, setCanScrollRight] = useState(false);

  const updateScrollState = () => {
    const el = scrollRef.current;
    if (!el) return;
    setCanScrollLeft(el.scrollLeft > 0);
    // Ceil to absorb sub-pixel rounding so the button doesn't stay
    // "active" when the row is actually fully scrolled.
    setCanScrollRight(
      Math.ceil(el.scrollLeft + el.clientWidth) < el.scrollWidth,
    );
  };

  useEffect(() => {
    const el = scrollRef.current;
    if (!el) return;

    updateScrollState();

    el.addEventListener("scroll", updateScrollState, { passive: true });
    // Re-check when the row's own size changes (e.g. loading -> loaded
    // swaps skeletons for real posters, which can change scrollWidth).
    const resizeObserver = new ResizeObserver(updateScrollState);
    resizeObserver.observe(el);

    return () => {
      el.removeEventListener("scroll", updateScrollState);
      resizeObserver.disconnect();
    };
  }, [items, isLoading]);

  const scrollByPage = (direction: 1 | -1) => {
    const el = scrollRef.current;
    if (!el) return;
    el.scrollBy({
      left: direction * el.clientWidth * SCROLL_STEP_FRACTION,
      behavior: "smooth",
    });
  };

  // Hide the whole row rather than showing an empty shelf.
  if (!isLoading && items.length === 0) return null;

  return (
    <section>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-4xl font-bold">{title}</h1>
        {!isLoading && (
          <span className="text-sm text-slate-500">
            {items.length} {items.length === 1 ? "item" : "items"}
          </span>
        )}
      </div>

      <div className="group relative">
        <div
          ref={scrollRef}
          className="grid grid-flow-col auto-cols-[15em] gap-4 overflow-x-auto px-8 pb-4 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
        >
          {isLoading &&
            Array.from({ length: 6 }).map((_, i) => (
              <div
                key={i}
                className="aspect-[2/3] w-[160px] animate-pulse rounded-md bg-slate-800"
              />
            ))}

          {!isLoading &&
            items.map((item) => (
              <div key={item.id} className="w-[15em] py-4">
                <Poster
                  key={item.id}
                  summary={{
                    id: item.id,
                    mediaType: item.mediaType,
                    title: item.title,
                    posterPath: item.posterPath,
                  }}
                ></Poster>
              </div>
            ))}
        </div>

        {/* Scroll buttons: only shown once there's somewhere to scroll to,
            and only visible on hover so they don't clutter a row that
            already fits on screen. */}
        {!isLoading && canScrollLeft && (
          <button
            onClick={() => scrollByPage(-1)}
            aria-label={`Scroll ${title} left`}
            className="text-4xl absolute inset-y-0 left-0 z-10 flex w-12 items-center justify-center bg-gradient-to-r from-black/80 to-transparent text-white opacity-0 transition-opacity duration-200 group-hover:opacity-100"
          >
            <ChevronLeft />
          </button>
        )}

        {!isLoading && canScrollRight && (
          <button
            onClick={() => scrollByPage(1)}
            aria-label={`Scroll ${title} right`}
            className="text-4xl absolute inset-y-0 right-0 z-10 flex w-12 items-center justify-center bg-gradient-to-l from-black/80 to-transparent text-white opacity-0 transition-opacity duration-200 group-hover:opacity-100"
          >
            <ChevronRight />
          </button>
        )}
      </div>
    </section>
  );
}
