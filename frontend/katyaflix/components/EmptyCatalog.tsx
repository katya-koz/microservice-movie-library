import Link from 'next/link';
import { UploadIcon } from './icons';

export default function EmptyCatalog({ label, href, cta }: { label: string; href: string; cta: string }) {
  return (
    <div className="rounded-lg border border-dashed border-ink-line bg-ink-raised/40 py-20 text-center">
      <p className="font-display text-2xl font-bold text-paper">{label}</p>
      <Link
        href={href}
        className="mt-5 inline-flex items-center gap-2 rounded bg-marquee px-5 py-2.5 font-mono text-xs font-semibold uppercase tracking-widest text-ink hover:bg-marquee-bright"
      >
        <UploadIcon className="h-4 w-4" />
        {cta}
      </Link>
    </div>
  );
}
