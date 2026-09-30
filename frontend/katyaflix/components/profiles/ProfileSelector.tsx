"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
// import { Check, Pencil, Plus } from "lucide-react";
import { useProfiles } from "@/hooks/useProfiles";
import { type Profile } from "@/types/profiles";
import { useUser } from "@/context/UserContext";
import { Check, PencilFill, Plus } from "react-bootstrap-icons";

export default function ProfileSelector() {
  const router = useRouter();
  const { setUser } = useUser();
  const { data: profiles, isLoading, error } = useProfiles();
  const [manageMode, setManageMode] = useState(false);

  const handleSelect = (profile: Profile) => {
    if (manageMode) {
      router.push(`/profiles/${profile.id}/edit`);
      return;
    }
    setUser({
      id: profile.id,
      displayName: profile.displayName,
      profilePictureUrl: profile.profilePictureUrl,
    });
    router.push("/");
  };

  if (isLoading) {
    return (
      <div className="flex h-full items-center justify-center bg-slate-950">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-slate-700 border-t-white" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex h-full items-center justify-center bg-slate-950">
        <p className="text-red-400">Failed to load profiles.</p>
      </div>
    );
  }

  return (
    <div className="flex h-full flex-col items-center justify-center gap-10 bg-slate-950 px-6">
      <h1 className="text-3xl font-semibold text-white">
        {manageMode ? "Manage profiles" : "Select a profile"}
      </h1>

      <div className="flex flex-wrap items-start justify-center gap-8">
        {profiles?.map((profile) => (
          <button
            key={profile.id}
            onClick={() => handleSelect(profile)}
            className="group flex flex-col items-center gap-2"
          >
            <div
              className={`relative flex  h-[10vw] w-[10vw] items-center justify-center overflow-hidden rounded-full border-4 bg-slate-800 text-slate-500 transition ${
                manageMode
                  ? "border-slate-500 group-hover:border-white"
                  : "border-transparent group-hover:border-white"
              }`}
            >
              {profile.profilePictureUrl ? (
                <img
                  src={profile.profilePictureUrl}
                  alt={profile.displayName}
                  className="h-full w-full object-cover"
                />
              ) : (
                <span className="text-xs">pfp</span>
              )}

              {manageMode && (
                <div className="absolute inset-0 flex items-center justify-center bg-black/50 opacity-0 transition group-hover:opacity-100"></div>
              )}
            </div>
            <span className="truncate text-sm text-slate-300 group-hover:text-white">
              {profile.displayName}
            </span>
          </button>
        ))}
      </div>

      <div className="flex items-center gap-4">
        <button
          onClick={() => router.push("/profiles/new")}
          title="Add profile"
          className="flex h-[3vw] w-[3vw] items-center justify-center rounded-full border-4 border-slate-600 text-slate-300 transition hover:border-white hover:text-white"
        >
          <Plus size={50} />
        </button>
        <button
          onClick={() => setManageMode((prev) => !prev)}
          title="Manage profiles"
          className={`flex h-[3vw] w-[3vw] items-center justify-center rounded-full border-4 transition ${
            manageMode
              ? "border-white text-white"
              : "border-slate-600 text-slate-300 hover:border-white hover:text-white"
          }`}
        >
          {manageMode ? (
            <Check size={40} />
          ) : (
            // <Check className="h-5 w-5" />
            <PencilFill size={30} />
            // <Pencil className="h-5 w-5" />
          )}
        </button>
      </div>
    </div>
  );
}
