export default function CatalogSkeleton() {
  return (
    <div className="grid grid-cols-2 gap-5 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6">
      {Array.from({ length: 12 }).map((_, i) => (
        <div key={i} className="aspect-[2/3] animate-pulse rounded-lg bg-ink-raised" />
      ))}
    </div>
  );
}
