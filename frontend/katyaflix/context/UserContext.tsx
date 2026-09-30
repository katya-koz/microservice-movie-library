"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";

export type CurrentUser = {
  id: string;
  displayName: string;
  profilePictureUrl: string | null;
};

type UserContextValue = {
  user: CurrentUser | null;
  // True until we've checked localStorage on mount — use this to avoid a
  // flash-redirect to /profiles before we know whether a profile was
  // already selected.
  isLoading: boolean;
  setUser: (user: CurrentUser) => void;
  clearUser: () => void;
};

const STORAGE_KEY = "katyaflix:activeProfile";

const UserContext = createContext<UserContextValue | undefined>(undefined);

export function UserProvider({ children }: { children: React.ReactNode }) {
  const [user, setUserState] = useState<CurrentUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    try {
      const raw = window.localStorage.getItem(STORAGE_KEY);
      if (raw) {
        setUserState(JSON.parse(raw));
      }
    } catch {
      // Corrupt/blocked storage — just fall through to "no profile selected".
    } finally {
      setIsLoading(false);
    }
  }, []);

  const setUser = useCallback((next: CurrentUser) => {
    setUserState(next);
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
  }, []);

  const clearUser = useCallback(() => {
    setUserState(null);
    window.localStorage.removeItem(STORAGE_KEY);
  }, []);

  const value = useMemo(
    () => ({ user, isLoading, setUser, clearUser }),
    [user, isLoading, setUser, clearUser],
  );

  return <UserContext.Provider value={value}>{children}</UserContext.Provider>;
}

export function useUser() {
  const ctx = useContext(UserContext);
  if (!ctx) {
    throw new Error("useUser must be used within a <UserProvider>");
  }
  return ctx;
}
