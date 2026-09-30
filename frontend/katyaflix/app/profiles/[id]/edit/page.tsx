"use client";

import { useParams } from "next/navigation";
import { useProfile } from "@/hooks/useProfiles";
import ProfileEditor from "@/components/profiles/ProfileEditor";

export default function EditProfilePage() {
  const { id } = useParams<{ id: string }>();
  const { data: profile, isLoading, error } = useProfile(id);

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-950">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-slate-700 border-t-white" />
      </div>
    );
  }

  if (error || !profile) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-950">
        <p className="text-red-400">Failed to load profile.</p>
      </div>
    );
  }

  return <ProfileEditor mode="edit" profile={profile} />;
}
