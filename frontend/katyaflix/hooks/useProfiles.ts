"use client";

import { fetchProfiles, fetchProfile } from "@/api/profiles";
import { API_URL_ROOT } from "@/lib/config";
import {
  CreateProfileInput,
  Profile,
  UpdateProfileInput,
} from "@/types/profiles";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

// See all profiles, for the profile selector.
export function useProfiles() {
  return useQuery({
    queryKey: ["profiles"],
    queryFn: fetchProfiles,
  });
}

// A single profile, for the editor screen.
export function useProfile(id?: string) {
  return useQuery({
    queryKey: ["profile", id],
    queryFn: () => fetchProfile(id as string),
    enabled: !!id,
  });
}

export function useCreateProfile() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: CreateProfileInput): Promise<Profile> => {
      const res = await fetch(`${API_URL_ROOT}/profiles`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(input),
      });
      if (!res.ok) throw new Error("Failed to create profile");
      return res.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["profiles"] });
    },
  });
}

export function useUpdateProfile() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      id,
      ...input
    }: UpdateProfileInput): Promise<Profile> => {
      const res = await fetch(`${API_URL_ROOT}/profiles/${id}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(input),
      });
      if (!res.ok) throw new Error("Failed to update profile");
      return res.json();
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ["profiles"] });
      queryClient.invalidateQueries({ queryKey: ["profile", variables.id] });
    },
  });
}

export function useDeleteProfile() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: string): Promise<void> => {
      const res = await fetch(`${API_URL_ROOT}/profiles/${id}`, {
        method: "DELETE",
      });
      if (!res.ok) throw new Error("Failed to delete profile");
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["profiles"] });
    },
  });
}
