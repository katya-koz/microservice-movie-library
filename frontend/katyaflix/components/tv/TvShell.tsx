"use client";

import { ReactNode } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { Search } from "react-bootstrap-icons";
import { useUser } from "@/context/UserContext";
import { useTvRootScale } from "@/lib/tv";
import TvNavProvider from "./TvNavProvider";

// No Upload here on purpose: that page is web-only.
const navItems = [
  { name: "Home", href: "/tv" },
  { name: "My List", href: "/tv/my-list" },
  { name: "Movies", href: "/tv/movies" },
  { name: "Shows", href: "/tv/shows" },
];

export default function TvShell({ children }: { children: ReactNode }) {
  useTvRootScale();
  const pathname = usePathname();
  const router = useRouter();
  const { user, clearUser } = useUser();

  const showNav = !pathname.startsWith("/tv/profiles");

  const isActive = (href: string) =>
    href === "/tv" ? pathname === "/tv" : pathname.startsWith(href);

  return (
    <TvNavProvider>
      <div className="fixed inset-0 z-[300] overflow-hidden bg-slate-950 text-white">
        {showNav && (
          <header className="absolute inset-x-0 top-0 z-20 flex h-24 items-center gap-6 bg-gradient-to-b from-slate-950 via-slate-950/85 to-transparent px-[4vw]">
            <span className="mr-8 text-4xl font-bold tracking-tight">
              KatyaFlix
            </span>

            {navItems.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                data-tv-focusable
                className={`tv-focusable tv-flat rounded-full px-7 py-3 text-2xl font-medium ${
                  isActive(item.href)
                    ? "bg-white/15 text-white"
                    : "text-slate-400"
                }`}
              >
                {item.name}
              </Link>
            ))}

            <div className="ml-auto flex items-center gap-4">
              <Link
                href="/tv/search"
                data-tv-focusable
                aria-label="Search"
                className={`tv-focusable tv-flat flex items-center gap-3 rounded-full px-6 py-3 text-2xl ${
                  isActive("/tv/search")
                    ? "bg-white/15 text-white"
                    : "text-slate-400"
                }`}
              >
                <Search /> Search
              </Link>

              {user && (
                <button
                  data-tv-focusable
                  onClick={() => {
                    clearUser();
                    router.push("/tv/profiles");
                  }}
                  aria-label="Switch profile"
                  className="tv-focusable tv-flat flex items-center gap-3 rounded-full py-1.5 pl-1.5 pr-5 text-2xl text-slate-300"
                >
                  <span className="flex h-12 w-12 items-center justify-center overflow-hidden rounded-full bg-slate-800">
                    {user.profilePictureUrl && (
                      <img
                        src={user.profilePictureUrl}
                        alt=""
                        className="h-full w-full object-cover"
                      />
                    )}
                  </span>
                  <span className="max-w-[10rem] truncate">
                    {user.displayName}
                  </span>
                </button>
              )}
            </div>
          </header>
        )}

        <div className="tv-scroll absolute inset-0 overflow-y-auto">
          {children}
        </div>
      </div>
    </TvNavProvider>
  );
}
