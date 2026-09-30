import { API_URL_ROOT } from "@/lib/config";
import { ProfilePicture } from "@/types/profilePicture";

export async function fetchProfilePictures(): Promise<ProfilePicture[]> {
  const res = await fetch(`${API_URL_ROOT}/profile-pictures`);
  if (!res.ok) throw new Error("Failed to load profile pictures");
  return res.json();
}
