import { API_URL_ROOT } from "@/lib/config";
import { Profile } from "@/types/profiles";

export async function fetchProfiles(): Promise<Profile[]> {
  const res = await fetch(`${API_URL_ROOT}/profiles`);
  if (!res.ok) throw new Error("Failed to load profiles");
  return res.json();
}

export async function fetchProfile(id: string): Promise<Profile> {
  const res = await fetch(`${API_URL_ROOT}/profiles/${id}`);
  if (!res.ok) throw new Error("Failed to load profile");
  return res.json();
}
