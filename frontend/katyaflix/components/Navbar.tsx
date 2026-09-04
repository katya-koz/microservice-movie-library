"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const navItems = [
  { name: "Home", href: "/" },
  { name: "My List", href: "/my-list" },
  { name: "Movies", href: "/movies" },
  { name: "Shows", href: "/shows" },
  { name: "Upload", href: "/upload" },
];

export default function Navbar() {
  const pathname = usePathname();

  return (
    <header className="border-b border-slate-800 bg-slate-950">
      <nav className="mx-auto flex h-16 max-w-7xl items-center px-6">
        {/* Logo */}
        <Link
          href="/"
          className="mr-12 text-2xl font-bold tracking-tight text-white"
        >
          KatyaFlix
        </Link>

        {/* Navigation */}
        <div className="flex h-full items-center gap-8">
          {navItems.map((item) => {
            const isActive =
              item.href === "/"
                ? pathname === "/"
                : pathname.startsWith(item.href);

            return (
              <Link
                key={item.href}
                href={item.href}
                className={`relative flex h-full items-center text-sm font-medium transition-colors ${
                  isActive ? "text-white" : "text-slate-400 hover:text-white"
                }`}
              >
                {item.name}

                {/* Active indicator */}
                {isActive && (
                  <span className="absolute bottom-0 left-0 right-0 h-0.5 rounded-full bg-white" />
                )}
              </Link>
            );
          })}
        </div>
      </nav>
    </header>
  );
}
