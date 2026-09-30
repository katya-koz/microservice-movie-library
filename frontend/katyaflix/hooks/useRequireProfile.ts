"use client";

import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useUser } from "@/context/UserContext";

/**
 * Sends the visitor to /profiles if no profile is active yet. Call this once
 * near the root of the app (see components/profiles/ProfileGate.tsx) rather
 * than in every page.
 */
export function useRequireProfile() {
  const { user, isLoading } = useUser();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (!isLoading && !user && !pathname.startsWith("/profiles")) {
      router.replace("/profiles");
    }
  }, [isLoading, user, pathname, router]);
}
