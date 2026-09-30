"use client";

import { ReactNode, useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useTvBackHandler } from "@/lib/tv";

/**
 * D-pad spatial navigation. Any element with `data-tv-focusable` (buttons and
 * links) can receive remote focus; arrow keys move to the nearest one in that
 * direction. Enter/OK triggers the native click on the focused button/link.
 *
 * Optional markers:
 *   data-tv-autofocus  preferred element to focus when a page appears
 *   data-tv-content    fallback: first focusable inside gets initial focus
 */

const FOCUSABLE = "[data-tv-focusable]";

type Dir = "left" | "right" | "up" | "down";

const DIRS: Record<string, Dir> = {
  ArrowLeft: "left",
  ArrowRight: "right",
  ArrowUp: "up",
  ArrowDown: "down",
};

function candidates(): HTMLElement[] {
  return Array.from(document.querySelectorAll<HTMLElement>(FOCUSABLE)).filter(
    (el) => {
      if (el.hasAttribute("disabled")) return false;
      const r = el.getBoundingClientRect();
      return r.width > 0 && r.height > 0;
    },
  );
}

function focusEl(el: HTMLElement, smooth: boolean) {
  el.focus({ preventScroll: true });
  el.scrollIntoView({
    block: "center",
    inline: "center",
    behavior: smooth ? "smooth" : "auto",
  });
}

function focusInitial() {
  const all = candidates();
  const el =
    all.find((c) => c.hasAttribute("data-tv-autofocus")) ??
    all.find((c) => c.closest("[data-tv-content]")) ??
    all[0];
  if (el) focusEl(el, false);
}

function move(dir: Dir) {
  const cur = document.activeElement as HTMLElement | null;
  if (!cur || !cur.matches(FOCUSABLE)) {
    focusInitial();
    return;
  }

  const a = cur.getBoundingClientRect();
  const ax = a.left + a.width / 2;
  const ay = a.top + a.height / 2;
  const horizontal = dir === "left" || dir === "right";

  let best: HTMLElement | null = null;
  let bestScore = Infinity;

  for (const el of candidates()) {
    if (el === cur) continue;
    const b = el.getBoundingClientRect();
    const dx = b.left + b.width / 2 - ax;
    const dy = b.top + b.height / 2 - ay;

    // Must lie in the requested direction (by centre).
    const primary =
      dir === "right" ? dx : dir === "left" ? -dx : dir === "down" ? dy : -dy;
    if (primary <= 1) continue;

    // Off-axis penalty: zero when the two rects overlap on the other axis.
    const secondary = horizontal
      ? Math.max(0, Math.max(a.top, b.top) - Math.min(a.bottom, b.bottom))
      : Math.max(0, Math.max(a.left, b.left) - Math.min(a.right, b.right));

    const score = primary + secondary * 3;
    if (score < bestScore) {
      bestScore = score;
      best = el;
    }
  }

  if (best) focusEl(best, true);
}

export default function TvNavProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();

  // Back: go up one screen; on the home/profile screens let the app exit.
  useTvBackHandler(() => {
    if (pathname === "/tv" || pathname === "/tv/profiles") return false;
    router.back();
    return true;
  });

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const dir = DIRS[e.key];
      if (!dir) return;
      e.preventDefault();
      move(dir);
    };
    window.addEventListener("keydown", onKey);

    // Whenever nothing valid is focused (page change, focused item unmounted,
    // skeleton replaced by real content) put focus somewhere sensible.
    const ensureFocus = () => {
      const active = document.activeElement;
      if (
        active &&
        active !== document.body &&
        active.isConnected &&
        active.matches(FOCUSABLE)
      ) {
        return;
      }
      focusInitial();
    };
    let raf = 0;
    const observer = new MutationObserver(() => {
      cancelAnimationFrame(raf);
      raf = requestAnimationFrame(ensureFocus);
    });
    observer.observe(document.body, { childList: true, subtree: true });
    ensureFocus();

    return () => {
      window.removeEventListener("keydown", onKey);
      observer.disconnect();
      cancelAnimationFrame(raf);
    };
  }, []);

  return <>{children}</>;
}
