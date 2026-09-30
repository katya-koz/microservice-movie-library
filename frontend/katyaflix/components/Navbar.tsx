"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useUser } from "@/context/UserContext";
import { BoxArrowLeft } from "react-bootstrap-icons";

const navItems = [
  { name: "Home", href: "/" },
  { name: "My List", href: "/my-list" },
  { name: "Movies", href: "/movies" },
  { name: "Shows", href: "/shows" },
  { name: "Upload", href: "/upload" },
];

export default function Navbar() {
  const pathname = usePathname();
  const router = useRouter();
  const { user, clearUser } = useUser();

  const handleSwitchProfile = () => {
    clearUser();
    router.push("/profiles");
  };

  return (
    <header className="border-b border-slate-800 bg-slate-950 sticky top-0 z-100">
      <nav className="mx-auto flex h-16 max-w-7xl items-center px-6">
        {/* Logo */}
        <Link
          href="/"
          className="mr-12 text-2xl font-bold tracking-tight text-white"
        >
          KatyaFlix
        </Link>

        {/* Navigation */}
        <div className="flex h-full flex-1 items-center gap-8">
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

        {/* Active profile — click to switch */}
        {user && (
          <button
            onClick={handleSwitchProfile}
            title="Switch profile"
            className="flex items-center gap-4 rounded-full py-1 pl-1 pr-3 text-sm text-slate-300 transition "
          >
            <span className="flex h-[2.5rem] w-[2.5rem] items-center justify-center overflow-hidden rounded-full bg-slate-800">
              {user.profilePictureUrl ? (
                <img
                  src={user.profilePictureUrl}
                  alt={user.displayName}
                  className="h-full w-full object-cover"
                />
              ) : (
                <span className="text-[10px] text-slate-500">pfp</span>
              )}
            </span>
            <span className="max-w-[8rem] text-lg font-bold truncate">
              {user.displayName}
            </span>
            <BoxArrowLeft size={25} className="hover:text-white" />
          </button>
        )}
      </nav>
    </header>
  );
}
