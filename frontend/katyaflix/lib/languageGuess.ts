const LANGUAGE_CODES = new Set([
  "en",
  "es",
  "fr",
  "de",
  "it",
  "pt",
  "nl",
  "ru",
  "ja",
  "zh",
  "ko",
  "ar",
  "hi",
  "tr",
  "pl",
  "sv",
  "no",
  "da",
  "fi",
  "cs",
  "el",
  "he",
]);

const LANGUAGE_NAMES: Record<string, string> = {
  en: "English",
  es: "Spanish",
  fr: "French",
  de: "German",
  it: "Italian",
  pt: "Portuguese",
  nl: "Dutch",
  ru: "Russian",
  ja: "Japanese",
  zh: "Chinese",
  ko: "Korean",
  ar: "Arabic",
  hi: "Hindi",
  tr: "Turkish",
  pl: "Polish",
  sv: "Swedish",
  no: "Norwegian",
  da: "Danish",
  fi: "Finnish",
  cs: "Czech",
  el: "Greek",
  he: "Hebrew",
};

function stem(fileName: string): string {
  return fileName.replace(/\.[^./]+$/, "");
}

/** Looks for a known language token in the filename (e.g. "ep1.en.srt" -> "en").
 * Falls back to "en" (guessed=true) when nothing recognizable is found. */
export function guessLanguageCode(fileName: string): {
  code: string;
  guessed: boolean;
} {
  const tokens = stem(fileName)
    .toLowerCase()
    .split(/[.\-_ ]+/);
  for (const tok of tokens) {
    if (LANGUAGE_CODES.has(tok)) return { code: tok, guessed: false };
  }
  return { code: "en", guessed: true };
}

export function languageLabel(code: string, guessed: boolean): string {
  const name = LANGUAGE_NAMES[code] ?? code.toUpperCase();
  return guessed ? `${name} (guessed)` : name;
}
