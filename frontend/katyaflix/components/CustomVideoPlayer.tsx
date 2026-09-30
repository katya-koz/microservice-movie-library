"use client";

import {
  forwardRef,
  useEffect,
  useImperativeHandle,
  useRef,
  useState,
} from "react";
import {
  ArrowBarRight,
  ArrowReturnLeft,
  BadgeCc,
  Fullscreen,
  FullscreenExit,
  Pause,
  Play,
  VolumeMute,
  VolumeUp,
} from "react-bootstrap-icons";

export interface SubtitleTrack {
  id: string;
  filePath: string;
  languageCode: string;
  label: string;
  isDefault?: boolean;
}

export interface NextEpisodeInfo {
  episodeId: string;
  title: string;
  episodeNumber: number;
  seasonNumber?: number;
  stillPath?: string | null;
}

export interface CustomVideoPlayerProps {
  src: string;
  mediaType: "MOVIE" | "EPISODE";
  /** Movie title, or show name when mediaType === "EPISODE". */
  title: string;
  /** Episode title, only used when mediaType === "EPISODE". */
  episodeTitle?: string;
  episodeNumber?: number;
  seasonNumber?: number;
  overview?: string;
  subtitles?: SubtitleTrack[];
  nextEpisode?: NextEpisodeInfo | null;

  resumeSeconds?: number;
  resumeMinSeconds?: number;
  autoPlay?: boolean;
  onClose: () => void;
  onPlayNext?: () => void;
  onEnded?: () => void;
  onPlayingChange?: (isPlaying: boolean) => void;
}

const CONTROLS_HIDE_DELAY_MS = 3000;
const IDLE_INFO_DELAY_MS = 10000;

// ---- persisted, global (not per-media) playback preferences ----
// Deliberately NOT keyed by media id: these are "how I like to watch
// anything", so they should carry over the first time a new title loads.
const VOLUME_STORAGE_KEY = "player:volume";
const MUTED_STORAGE_KEY = "player:muted";
const SUBTITLES_STORAGE_KEY = "player:subtitlesOn";

function loadStoredVolume(): number {
  if (typeof window === "undefined") return 1;
  try {
    const raw = window.localStorage.getItem(VOLUME_STORAGE_KEY);
    const parsed = raw !== null ? Number(raw) : NaN;
    return Number.isFinite(parsed) ? Math.min(1, Math.max(0, parsed)) : 1;
  } catch {
    return 1;
  }
}

function loadStoredMuted(): boolean {
  if (typeof window === "undefined") return false;
  try {
    return window.localStorage.getItem(MUTED_STORAGE_KEY) === "true";
  } catch {
    return false;
  }
}

// Returns null when the user has never set a subtitle preference before,
// so callers can fall back to the per-track `isDefault` flag.
function loadStoredSubtitlesOn(): boolean | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = window.localStorage.getItem(SUBTITLES_STORAGE_KEY);
    return raw === null ? null : raw === "true";
  } catch {
    return null;
  }
}

function persist(key: string, value: string) {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.setItem(key, value);
  } catch {
    // Storage can be unavailable (private mode, quota, etc.) — playback
    // still works, the preference just won't be remembered.
  }
}

function formatTime(totalSeconds: number) {
  if (!Number.isFinite(totalSeconds) || totalSeconds < 0) return "0:00";
  const h = Math.floor(totalSeconds / 3600);
  const m = Math.floor((totalSeconds % 3600) / 60);
  const s = Math.floor(totalSeconds % 60);
  const mm = h > 0 ? String(m).padStart(2, "0") : String(m);
  const ss = String(s).padStart(2, "0");
  return h > 0 ? `${h}:${mm}:${ss}` : `${mm}:${ss}`;
}

export const CustomVideoPlayer = forwardRef<
  HTMLVideoElement,
  CustomVideoPlayerProps
>(function CustomVideoPlayer(
  {
    src,
    // mediaUrlRoot,
    mediaType,
    title,
    episodeTitle,
    episodeNumber,
    seasonNumber,
    overview,
    subtitles,
    resumeSeconds,
    resumeMinSeconds = 10,
    autoPlay = true,
    nextEpisode,
    onClose,
    onPlayNext,
    onEnded,
    onPlayingChange,
  },
  forwardedRef,
) {
  const internalVideoRef = useRef<HTMLVideoElement | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);
  useImperativeHandle(
    forwardedRef,
    () => internalVideoRef.current as HTMLVideoElement,
  );

  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  // Initialized from localStorage so a brand-new title still opens at the
  // volume/mute level the user last set, instead of defaulting to 100%.
  const [volume, setVolume] = useState(() => loadStoredVolume());
  const [isMuted, setIsMuted] = useState(() => loadStoredMuted());
  const [subtitlesOn, setSubtitlesOn] = useState(() => {
    const stored = loadStoredSubtitlesOn();
    if (stored !== null) return stored;
    return subtitles?.some((t) => t.isDefault) ?? false;
  });
  const [showControls, setShowControls] = useState(true);
  const [showIdleInfo, setShowIdleInfo] = useState(false);
  const [showNextPreview, setShowNextPreview] = useState(false);
  const [showVolumeSlider, setShowVolumeSlider] = useState(false);

  const isPlayingRef = useRef(false);
  // Guards the resume seek so it only ever fires once per `src`. Reset
  // whenever `src` changes (see the "reset per-title state" effect below).
  const hasSeekedRef = useRef(false);
  const hideControlsTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const idleInfoTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const hasSubtitles = !!subtitles && subtitles.length > 0;
  const hasNextEpisode = mediaType === "EPISODE" && !!nextEpisode;

  const [isFullscreen, setIsFullscreen] = useState(false);

  // ---- auto-open fullscreen, exit on unmount ----
  useEffect(() => {
    const el = containerRef.current as
      | (HTMLDivElement & {
          webkitRequestFullscreen?: () => Promise<void>;
        })
      | null;
    const request =
      el?.requestFullscreen?.bind(el) ?? el?.webkitRequestFullscreen?.bind(el);
    request?.().catch(() => {
      // Fullscreen can be blocked (e.g. not triggered by a direct user
      // gesture on some browsers) — playback still works, just windowed.
    });
    return () => {
      if (document.fullscreenElement) {
        document.exitFullscreen?.().catch(() => {});
      }
    };
  }, []);

  const toggleFullscreen = async () => {
    const container = containerRef.current;
    if (!container) return;

    try {
      if (document.fullscreenElement) {
        await document.exitFullscreen();
      } else {
        await container.requestFullscreen();
      }
    } catch {
      // Fullscreen may be unavailable or blocked by the browser.
    }
  };
  useEffect(() => {
    const handleFullscreenChange = () => {
      setIsFullscreen(document.fullscreenElement === containerRef.current);
    };

    document.addEventListener("fullscreenchange", handleFullscreenChange);

    return () => {
      document.removeEventListener("fullscreenchange", handleFullscreenChange);
    };
  }, []);

  // ---- reset per-title state whenever the source changes (e.g. after
  // navigating to the next episode, since this component instance is reused).
  // Subtitles fall back to the stored global preference first, and only
  // use the per-track `isDefault` flag if the user has never set one. ----
  useEffect(() => {
    hasSeekedRef.current = false;
    setCurrentTime(0);
    setDuration(0);
    setSubtitlesOn(() => {
      const stored = loadStoredSubtitlesOn();
      if (stored !== null) return stored;
      return subtitles?.some((t) => t.isDefault) ?? false;
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [src]);

  // ---- apply the stored volume/mute preference to the actual media
  // element. Needed because <video> has no volume attribute — the
  // element itself always boots at volume 1 / unmuted regardless of
  // React state, so this has to be set imperatively once the element
  // (re)acquires a source. ----
  useEffect(() => {
    const video = internalVideoRef.current;
    if (!video) return;
    video.volume = volume;
    video.muted = isMuted;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [src]);

  // ---- wire up native video events ----
  useEffect(() => {
    const video = internalVideoRef.current;
    if (!video) return;

    const handlePlay = () => {
      setIsPlaying(true);
      onPlayingChange?.(true);
    };
    const handlePause = () => {
      setIsPlaying(false);
      onPlayingChange?.(false);
    };
    const handleTimeUpdate = () => setCurrentTime(video.currentTime);
    // NOTE: this only records that metadata (duration) is now known. It
    // deliberately does NOT perform the resume seek — see the dedicated
    // effect below for why: resumeSeconds can still be `undefined` at this
    // point (watchtime fetch not resolved yet), and this handler's closure
    // would otherwise freeze whatever resumeSeconds was at effect-bind
    // time, since this effect only depends on `[src]`.
    const handleLoadedMetadata = () => {
      setDuration(video.duration || 0);
    };
    const handleEnded = () => {
      setIsPlaying(false);
      onEnded?.();
    };
    // Fires on any volume/mute change, whether from our own controls or
    // the browser's native UI — so this is also the single place that
    // persists the user's global volume/mute preference.
    const handleVolumeChange = () => {
      setVolume(video.volume);
      setIsMuted(video.muted);
      persist(VOLUME_STORAGE_KEY, String(video.volume));
      persist(MUTED_STORAGE_KEY, String(video.muted));
    };

    video.addEventListener("play", handlePlay);
    video.addEventListener("pause", handlePause);
    video.addEventListener("timeupdate", handleTimeUpdate);
    video.addEventListener("loadedmetadata", handleLoadedMetadata);
    video.addEventListener("ended", handleEnded);
    video.addEventListener("volumechange", handleVolumeChange);

    return () => {
      video.removeEventListener("play", handlePlay);
      video.removeEventListener("pause", handlePause);
      video.removeEventListener("timeupdate", handleTimeUpdate);
      video.removeEventListener("loadedmetadata", handleLoadedMetadata);
      video.removeEventListener("ended", handleEnded);
      video.removeEventListener("volumechange", handleVolumeChange);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [src]);

  // ---- perform the resume seek exactly once, whenever BOTH pieces of
  // information we need are actually ready: the video's duration (from
  // loadedmetadata) and a real resumeSeconds value (not the "haven't
  // fetched it yet" placeholder). Whichever arrives second is what
  // triggers the seek — so this is correct regardless of whether the
  // watchtime fetch or the video's metadata load wins the race. ----
  useEffect(() => {
    if (hasSeekedRef.current) return;
    if (resumeSeconds === undefined) return; // watchtime not resolved yet
    if (!duration) return; // metadata not loaded yet
    const video = internalVideoRef.current;
    if (!video) return;

    hasSeekedRef.current = true;
    const nearEnd = resumeSeconds >= duration - resumeMinSeconds;
    if (resumeSeconds > resumeMinSeconds && !nearEnd) {
      video.currentTime = resumeSeconds;
    }
  }, [resumeSeconds, duration, resumeMinSeconds]);

  // ---- keep isPlayingRef current for use inside timeout closures ----
  useEffect(() => {
    isPlayingRef.current = isPlaying;
  }, [isPlaying]);

  // ---- toggle subtitle tracks on/off, and persist the preference
  // globally so it applies to the next title too ----
  useEffect(() => {
    const video = internalVideoRef.current;
    if (!video) return;
    const tracks = video.textTracks;
    for (let i = 0; i < tracks.length; i++) {
      if (!subtitlesOn) {
        tracks[i].mode = "hidden";
        continue;
      }
      const meta = subtitles?.[i];
      const anyDefault = subtitles?.some((t) => t.isDefault) ?? false;
      tracks[i].mode =
        meta?.isDefault || (!anyDefault && i === 0) ? "showing" : "hidden";
    }
  }, [subtitlesOn, subtitles]);

  useEffect(() => {
    persist(SUBTITLES_STORAGE_KEY, String(subtitlesOn));
  }, [subtitlesOn]);

  // ---- controls visibility / idle-info timers ----
  const clearHideTimer = () => {
    if (hideControlsTimer.current) clearTimeout(hideControlsTimer.current);
  };
  const clearIdleInfoTimer = () => {
    if (idleInfoTimer.current) clearTimeout(idleInfoTimer.current);
  };
  const scheduleHideControls = () => {
    clearHideTimer();
    hideControlsTimer.current = setTimeout(() => {
      if (isPlayingRef.current) setShowControls(false);
    }, CONTROLS_HIDE_DELAY_MS);
  };
  const scheduleIdleInfo = () => {
    clearIdleInfoTimer();
    idleInfoTimer.current = setTimeout(() => {
      if (!isPlayingRef.current) setShowIdleInfo(true);
    }, IDLE_INFO_DELAY_MS);
  };

  // Whenever play state flips: reset to "controls visible, no idle info",
  // then arm whichever timer is relevant.
  useEffect(() => {
    setShowIdleInfo(false);
    clearIdleInfoTimer();
    clearHideTimer();
    setShowControls(true);
    if (isPlaying) {
      scheduleHideControls();
    } else {
      scheduleIdleInfo();
    }
    return () => {
      clearHideTimer();
      clearIdleInfoTimer();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isPlaying]);

  const registerActivity = () => {
    setShowControls(true);
    setShowIdleInfo(false);
    clearIdleInfoTimer();
    if (isPlayingRef.current) {
      scheduleHideControls();
    } else {
      scheduleIdleInfo();
    }
  };

  // ---- actions ----
  const togglePlay = () => {
    const video = internalVideoRef.current;
    if (!video) return;
    if (video.paused) video.play().catch(() => {});
    else video.pause();
  };

  const toggleMute = () => {
    const video = internalVideoRef.current;
    if (!video) return;
    video.muted = !video.muted;
  };

  const handleVolumeScrub = (fraction: number) => {
    const video = internalVideoRef.current;
    if (!video) return;
    video.volume = fraction;
    video.muted = fraction === 0;
  };

  const handleSeekScrub = (fraction: number) => {
    const video = internalVideoRef.current;
    if (!video || !duration) return;
    const time = fraction * duration;
    video.currentTime = time;
    setCurrentTime(time);
  };

  const exitFullscreenIfActive = () => {
    if (document.fullscreenElement) document.exitFullscreen?.().catch(() => {});
  };

  const handleClose = () => {
    exitFullscreenIfActive();
    onClose();
  };

  const handleNextClick = () => {
    exitFullscreenIfActive();
    onPlayNext?.();
  };

  return (
    <div
      ref={containerRef}
      tabIndex={-1}
      onMouseMove={registerActivity}
      onTouchStart={registerActivity}
      onKeyDown={(e) => {
        registerActivity();
        if (e.code === "Space") {
          e.preventDefault();
          togglePlay();
        } else if (e.key === "m") {
          toggleMute();
        }
      }}
      className="relative h-full w-full overflow-hidden bg-black outline-none"
    >
      <video
        ref={internalVideoRef}
        src={src}
        autoPlay={autoPlay}
        playsInline
        onClick={togglePlay}
        crossOrigin="anonymous"
        className="h-full w-full cursor-pointer"
      >
        {subtitles?.map((track) => (
          <track
            key={track.id}
            kind="subtitles"
            src={`${track.filePath}`}
            srcLang={track.languageCode}
            label={track.label}
            default={track.isDefault}
          />
        ))}
      </video>

      {/* Top gradient + close button */}
      <div
        className={`pointer-events-none absolute inset-x-0 top-0 h-32 bg-gradient-to-b from-black/70 to-transparent transition-opacity duration-300 ${
          showControls ? "opacity-100" : "opacity-0"
        }`}
      />
      <button
        onClick={handleClose}
        aria-label="Close player"
        className={`text-4xl absolute left-4 top-4 z-20 flex h-10 w-10 items-center justify-center rounded-full text-white transition-opacity duration-300 hover:bg-white/10 md:left-6 md:top-6 ${
          showControls ? "opacity-100" : "pointer-events-none opacity-0"
        }`}
      >
        <ArrowReturnLeft />
      </button>

      {/* Idle metadata overlay — paused + 10s of no activity */}
      <div
        className={`pointer-events-none absolute inset-x-0 bottom-28 z-10 px-6 transition-all duration-500 md:bottom-32 md:px-16 ${
          showIdleInfo ? "translate-y-0 opacity-100" : "translate-y-2 opacity-0"
        }`}
      >
        <div className="max-w-xl">
          {mediaType === "EPISODE" ? (
            <>
              <p className="text-sm font-medium text-slate-300">{title}</p>
              <h2 className="mt-1 text-2xl font-semibold text-white">
                {seasonNumber != null && episodeNumber != null
                  ? `S${seasonNumber} E${episodeNumber} — `
                  : ""}
                {episodeTitle}
              </h2>
            </>
          ) : (
            <h2 className="text-2xl font-semibold text-white">{title}</h2>
          )}
          {overview && (
            <p className="mt-2 line-clamp-3 text-sm leading-relaxed text-slate-300">
              {overview}
            </p>
          )}
        </div>
      </div>

      {/* Bottom gradient + controls */}
      <div
        className={`absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/90 via-black/50 to-transparent px-6 pb-5 pt-20 transition-opacity duration-300 md:px-10 ${
          showControls ? "opacity-100" : "pointer-events-none opacity-0"
        }`}
      >
        {/* Seek bar */}
        <div className="flex items-center gap-3">
          <span className="w-12 shrink-0 text-right text-xs tabular-nums text-slate-300">
            {formatTime(currentTime)}
          </span>
          <Scrubber
            value={duration ? currentTime / duration : 0}
            onScrub={handleSeekScrub}
            ariaLabel="Seek"
          />
          <span className="w-12 shrink-0 text-xs tabular-nums text-slate-300">
            {formatTime(duration)}
          </span>
        </div>

        {/* Buttons row */}
        <div className="mt-3 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <button
              onClick={togglePlay}
              aria-label={isPlaying ? "Pause" : "Play"}
              className="text-white hover:text-slate-300 text-4xl"
            >
              {isPlaying ? <Pause /> : <Play />}
            </button>

            <div
              className="flex items-center gap-2"
              onMouseEnter={() => setShowVolumeSlider(true)}
              onMouseLeave={() => setShowVolumeSlider(false)}
            >
              <button
                onClick={toggleMute}
                aria-label={isMuted || volume === 0 ? "Unmute" : "Mute"}
                className="text-white hover:text-slate-300 text-4xl"
              >
                {isMuted || volume === 0 ? <VolumeMute /> : <VolumeUp />}
              </button>
              <div
                className={`overflow-hidden transition-all duration-200 ${
                  showVolumeSlider ? "w-20 opacity-100" : "w-0 opacity-0"
                }`}
              >
                <Scrubber
                  value={isMuted ? 0 : volume}
                  onScrub={handleVolumeScrub}
                  ariaLabel="Volume"
                />
              </div>
            </div>
          </div>

          <div className="flex items-center gap-4">
            {hasSubtitles && (
              <button
                onClick={() => setSubtitlesOn((v) => !v)}
                aria-label={
                  subtitlesOn ? "Turn off subtitles" : "Turn on subtitles"
                }
                aria-pressed={subtitlesOn}
                className={
                  subtitlesOn
                    ? "text-white text-4xl"
                    : "text-slate-400 hover:text-slate-200 text-4xl"
                }
              >
                <BadgeCc
                  className={`${subtitlesOn ? "font-white" : "font-slate-300"} text-4xl`}
                />
              </button>
            )}

            {hasNextEpisode && (
              <div
                className="relative"
                onMouseEnter={() => setShowNextPreview(true)}
                onMouseLeave={() => setShowNextPreview(false)}
              >
                <div
                  className={`absolute bottom-full right-0 mb-3 w-64 origin-bottom-right overflow-hidden rounded-md bg-slate-900 shadow-xl ring-1 ring-white/10 transition-all duration-150 ${
                    showNextPreview
                      ? "scale-100 opacity-100"
                      : "pointer-events-none scale-95 opacity-0"
                  }`}
                >
                  <div className="aspect-video w-full bg-slate-800">
                    {nextEpisode?.stillPath && (
                      <img
                        src={`${nextEpisode.stillPath}`}
                        alt={nextEpisode.title}
                        className="h-full w-full object-cover"
                      />
                    )}
                  </div>
                  <div className="p-3">
                    <p className="text-xs text-slate-400">
                      {nextEpisode?.seasonNumber != null
                        ? `S${nextEpisode.seasonNumber} E${nextEpisode.episodeNumber}`
                        : `Episode ${nextEpisode?.episodeNumber}`}
                    </p>
                    <p className="truncate text-sm font-medium text-white">
                      {nextEpisode?.title}
                    </p>
                  </div>
                </div>

                <button
                  onClick={handleNextClick}
                  aria-label="Play next episode"
                  className="text-white hover:text-slate-300 text-4xl"
                >
                  <ArrowBarRight />
                </button>
              </div>
            )}

            <button
              onClick={toggleFullscreen}
              aria-label={isFullscreen ? "Exit fullscreen" : "Enter fullscreen"}
              className="text-white hover:text-slate-300 text-4xl"
            >
              {isFullscreen ? <FullscreenExit /> : <Fullscreen />}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
});

/* ------------------------------------------------------------------ */
/* Custom scrubber — shared by the seek bar and volume slider. Plain   */
/* divs + pointer events instead of <input type="range"> so it can be  */
/* styled precisely instead of fighting browser default thumb/track    */
/* styles.                                                              */
/* ------------------------------------------------------------------ */

function Scrubber({
  value,
  onScrub,
  ariaLabel,
}: {
  value: number;
  onScrub: (fraction: number) => void;
  ariaLabel: string;
}) {
  const trackRef = useRef<HTMLDivElement | null>(null);
  const [dragging, setDragging] = useState(false);

  const clamp = (n: number) => Math.min(1, Math.max(0, n));

  const fractionFromClientX = (clientX: number) => {
    const el = trackRef.current;
    if (!el) return value;
    const rect = el.getBoundingClientRect();
    if (rect.width === 0) return value;
    return clamp((clientX - rect.left) / rect.width);
  };

  useEffect(() => {
    if (!dragging) return;
    const handleMove = (e: PointerEvent) =>
      onScrub(fractionFromClientX(e.clientX));
    const handleUp = () => setDragging(false);
    window.addEventListener("pointermove", handleMove);
    window.addEventListener("pointerup", handleUp);
    return () => {
      window.removeEventListener("pointermove", handleMove);
      window.removeEventListener("pointerup", handleUp);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dragging]);

  return (
    <div
      ref={trackRef}
      role="slider"
      aria-label={ariaLabel}
      aria-valuemin={0}
      aria-valuemax={1}
      aria-valuenow={Number(value.toFixed(2))}
      tabIndex={0}
      onPointerDown={(e) => {
        setDragging(true);
        onScrub(fractionFromClientX(e.clientX));
      }}
      onKeyDown={(e) => {
        if (e.key === "ArrowLeft") onScrub(clamp(value - 0.05));
        if (e.key === "ArrowRight") onScrub(clamp(value + 0.05));
      }}
      className="group/scrubber relative flex h-4 flex-1 cursor-pointer items-center"
    >
      <div className="h-1 w-full overflow-hidden rounded-full bg-white/25">
        <div
          className="h-full rounded-full bg-white"
          style={{ width: `${value * 100}%` }}
        />
      </div>
      <div
        className={`absolute top-1/2 h-3 w-3 -translate-x-1/2 -translate-y-1/2 rounded-full bg-white shadow transition-opacity ${
          dragging
            ? "opacity-100"
            : "opacity-0 group-hover/scrubber:opacity-100"
        }`}
        style={{ left: `${value * 100}%` }}
      />
    </div>
  );
}
