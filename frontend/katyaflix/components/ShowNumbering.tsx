import { NumberSource, ShowStructure } from "@/types/uploadTree";

function NumberInput({
  value,
  min,
  source,
  conflict,
  onCommit,
  onReset,
}: {
  value: number;
  min: number;
  source: NumberSource;
  conflict: boolean;
  onCommit: (n: number) => void;
  onReset: () => void;
}) {
  return (
    <div className="flex items-center gap-2">
      <input
        key={value /* re-sync after commit */}
        type="number"
        min={min}
        defaultValue={value}
        onBlur={(e) => {
          const n = parseInt(e.target.value, 10);
          if (Number.isNaN(n) || n < min) {
            e.target.value = String(value);
            return;
          }
          if (n !== value) onCommit(n);
        }}
        onKeyDown={(e) => e.key === "Enter" && e.currentTarget.blur()}
        className={`w-16 rounded border bg-ink px-2 py-1 font-mono text-xs text-paper ${
          conflict ? "border-signal-error" : "border-ink-line"
        }`}
      />
      <span className="font-mono text-[10px] uppercase tracking-widest text-paper-faint">
        {source === "sequential"
          ? "guessed"
          : source === "override"
            ? "edited"
            : "from name"}
      </span>
      {source === "override" && (
        <button
          type="button"
          onClick={onReset}
          className="font-mono text-[10px] uppercase tracking-widest text-paper-muted hover:text-paper"
        >
          reset
        </button>
      )}
    </div>
  );
}

export default function ShowNumbering({
  structure,
  onSeason,
  onEpisode,
}: {
  structure: ShowStructure;
  onSeason: (folderId: string, n: number | null) => void;
  onEpisode: (videoId: string, n: number | null) => void;
}) {
  return (
    <div className="mt-4 rounded-lg border border-ink-line bg-ink-raised">
      <p className="border-b border-ink-line px-4 py-2 font-mono text-xs uppercase tracking-widest text-paper-muted">
        Season &amp; episode numbers
      </p>
      <div className="max-h-96 space-y-4 overflow-y-auto p-4">
        {structure.seasons.map((season) => (
          <div key={season.folderId}>
            <div className="flex items-center justify-between gap-4">
              <p
                className="truncate font-mono text-xs text-paper"
                title={season.folderName}
              >
                {season.folderName}
              </p>
              <NumberInput
                value={season.seasonNumber}
                min={0}
                source={season.source}
                conflict={season.conflict}
                onCommit={(n) => onSeason(season.folderId, n)}
                onReset={() => onSeason(season.folderId, null)}
              />
            </div>
            <ul className="mt-2 space-y-1 border-l border-ink-line pl-3">
              {season.episodes.map((ep) => (
                <li
                  key={ep.video.id}
                  className="flex items-center justify-between gap-4"
                >
                  <span
                    className="truncate font-mono text-[11px] text-paper-muted"
                    title={ep.video.name}
                  >
                    {ep.video.name}
                  </span>
                  <NumberInput
                    value={ep.episodeNumber}
                    min={1}
                    source={ep.source}
                    conflict={ep.conflict}
                    onCommit={(n) => onEpisode(ep.video.id, n)}
                    onReset={() => onEpisode(ep.video.id, null)}
                  />
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>
    </div>
  );
}
