"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useProfilePictures } from "@/hooks/useProfilePictures";
import {
  useCreateProfile,
  useDeleteProfile,
  useUpdateProfile,
} from "@/hooks/useProfiles";

import { type Profile } from "@/types/profiles";
import { useUser } from "@/context/UserContext";
import { Check, Trash, X } from "react-bootstrap-icons";

type ProfileEditorProps =
  | { mode: "create"; profile?: undefined }
  | { mode: "edit"; profile: Profile };

export default function ProfileEditor({ mode, profile }: ProfileEditorProps) {
  const router = useRouter();
  const { user, setUser, clearUser } = useUser();
  const { data: pictures, isLoading: picturesLoading } = useProfilePictures();

  const [displayName, setDisplayName] = useState(profile?.displayName ?? "");
  const [selectedPictureId, setSelectedPictureId] = useState<string | null>(
    profile?.profilePictureId ?? null,
  );

  const createProfile = useCreateProfile();
  const updateProfile = useUpdateProfile();
  const deleteProfile = useDeleteProfile();

  useEffect(() => {
    // Default new profiles to the first available picture once options load.
    if (mode === "create" && !selectedPictureId && pictures?.length) {
      setSelectedPictureId(pictures[0].id);
    }
  }, [mode, pictures, selectedPictureId]);

  const selectedPicture = pictures?.find((p) => p.id === selectedPictureId);
  const isSaving = createProfile.isPending || updateProfile.isPending;
  const isDeleting = deleteProfile.isPending;

  const handleSave = async () => {
    const trimmedName = displayName.trim();
    if (!trimmedName) return;

    if (mode === "create") {
      await createProfile.mutateAsync({
        displayName: trimmedName,
        profilePictureId: selectedPictureId,
      });
    } else {
      const updated = await updateProfile.mutateAsync({
        id: profile.id,
        displayName: trimmedName,
        profilePictureId: selectedPictureId,
      });
      // Keep the active session in sync if this is the currently-active profile.
      if (user?.id === profile.id) {
        setUser({
          id: updated.id,
          displayName: updated.displayName,
          profilePictureUrl: updated.profilePictureUrl,
        });
      }
    }
    router.push("/profiles");
  };

  const handleDelete = async () => {
    if (mode !== "edit") return;
    await deleteProfile.mutateAsync(profile.id);
    if (user?.id === profile.id) {
      clearUser();
    }
    router.push("/profiles");
  };

  return (
    // <div className="flex min-h-screen items-center justify-center bg-slate-950 px-6">
    <div className="w-full max-w-2xl rounded-2xl bg-slate-900 p-8">
      <div className="flex items-center gap-6">
        <div className="flex h-[8vw] w-[8vw] shrink-0 items-center justify-center overflow-hidden rounded-full bg-slate-800 text-slate-500">
          {selectedPicture ? (
            <img
              src={selectedPicture.url}
              alt=""
              className="h-full w-full object-cover"
            />
          ) : (
            <span className="text-xs">pfp</span>
          )}
        </div>

        <input
          value={displayName}
          onChange={(e) => setDisplayName(e.target.value)}
          placeholder="Profile name"
          maxLength={40}
          className="flex-1 rounded-md border-4 border-slate-700 bg-slate-950 px-4 py-2.5 text-white placeholder:text-slate-500 focus:border-white focus:outline-none"
        />
      </div>

      <div className="mt-8">
        <p className="mb-3 text-lg text-slate-400">Choose a picture</p>

        {picturesLoading ? (
          <div className="h-24 animate-pulse rounded-lg bg-slate-800" />
        ) : (
          <div className="flex gap-3 overflow-x-auto rounded-lg border-4 border-slate-800 bg-slate-950 p-3">
            {pictures?.map((picture) => {
              const isSelected = picture.id === selectedPictureId;
              return (
                <button
                  key={picture.id}
                  onClick={() => setSelectedPictureId(picture.id)}
                  title={picture.name}
                  className={`h-[6vw] w-[6vw] shrink-0 overflow-hidden rounded-full transition ${
                    isSelected
                      ? "border-white"
                      : "border-transparent opacity-70 hover:opacity-100"
                  }`}
                >
                  <img
                    src={picture.url}
                    alt={picture.name}
                    className="h-full w-full object-cover"
                  />
                </button>
              );
            })}
          </div>
        )}
      </div>

      <div className="mt-8 flex items-center justify-between">
        {mode === "edit" ? (
          <button
            onClick={handleDelete}
            disabled={isDeleting}
            className="flex items-center gap-2 text-sm font-medium text-red-400 transition hover:text-red-300 disabled:opacity-50"
          >
            <Trash />
            Delete profile
          </button>
        ) : (
          <span />
        )}

        <div className="flex items-center gap-3">
          <button
            onClick={() => router.push("/profiles")}
            title="Cancel"
            className="flex h-12 w-12 items-center justify-center rounded-full border-4 border-slate-600 text-slate-300 transition hover:border-white hover:text-white"
          >
            <X size={30} />
          </button>
          <button
            onClick={handleSave}
            disabled={isSaving || !displayName.trim()}
            title="Save"
            className="flex h-12 w-12 items-center justify-center rounded-full border-4 border-green-400 text-green-400 transition hover:bg-green-400 hover:text-black disabled:opacity-40"
          >
            <Check size={30} />
          </button>
        </div>
      </div>
    </div>
    // </div>
  );
}
