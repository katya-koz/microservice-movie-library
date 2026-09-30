"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState } from "react";
import { UserProvider } from "./context/UserContext";
import ProfileGate from "./components/profiles/ProfileGate";

export default function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(() => new QueryClient());

  return (
    <QueryClientProvider client={queryClient}>
      <UserProvider>
        <ProfileGate>{children}</ProfileGate>
      </UserProvider>
    </QueryClientProvider>
  );
}
