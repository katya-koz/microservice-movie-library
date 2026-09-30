export function TvSpinner() {
  return (
    <div className="flex h-screen items-center justify-center">
      <div className="h-16 w-16 animate-spin rounded-full border-4 border-slate-700 border-t-white" />
    </div>
  );
}

export function TvMessage({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex h-screen items-center justify-center px-[4vw]">
      <p className="text-3xl text-slate-300">{children}</p>
    </div>
  );
}
