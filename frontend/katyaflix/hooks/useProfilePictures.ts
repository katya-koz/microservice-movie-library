"use client";

import { fetchProfilePictures } from "@/api/profilePictures";
import { useQuery } from "@tanstack/react-query";

// All selectable avatar images, for the profile picture editor carousel.
export function useProfilePictures() {
  return useQuery({
    queryKey: ["profile-pictures"],
    queryFn: fetchProfilePictures,
  });
}
