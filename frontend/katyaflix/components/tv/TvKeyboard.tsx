"use client";

import { Backspace } from "react-bootstrap-icons";

const KEYS = "abcdefghijklmnopqrstuvwxyz0123456789".split("");

type Updater = (prev: string) => string;

/** On-screen keyboard driven by the D-pad: move with arrows, press OK to type. */
export default function TvKeyboard({
  onChange,
}: {
  onChange: (update: Updater) => void;
}) {
  const key =
    "tv-focusable tv-flat flex h-16 items-center justify-center rounded-lg bg-slate-800 text-2xl font-semibold";

  return (
    <div className="grid grid-cols-6 gap-2">
      {KEYS.map((k) => (
        <button
          key={k}
          data-tv-focusable
          onClick={() => onChange((prev) => prev + k)}
          className={key}
        >
          {k.toUpperCase()}
        </button>
      ))}

      <button
        data-tv-focusable
        onClick={() =>
          onChange((prev) => (prev === "" || prev.endsWith(" ") ? prev : prev + " "))
        }
        className={`${key} col-span-2 text-xl`}
      >
        Space
      </button>
      <button
        data-tv-focusable
        onClick={() => onChange((prev) => prev.slice(0, -1))}
        className={`${key} col-span-2 gap-2 text-xl`}
        aria-label="Delete"
      >
        <Backspace /> Delete
      </button>
      <button
        data-tv-focusable
        onClick={() => onChange(() => "")}
        className={`${key} col-span-2 text-xl`}
      >
        Clear
      </button>
    </div>
  );
}
