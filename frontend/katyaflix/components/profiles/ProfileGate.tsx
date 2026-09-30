"use client";

import { useRequireProfile } from "@/hooks/useRequireProfile";
// enforce user must be selected before showing the rest of th eapp
export default function ProfileGate({
  children,
}: {
  children: React.ReactNode;
}) {
  useRequireProfile();
  return <>{children}</>;
}
