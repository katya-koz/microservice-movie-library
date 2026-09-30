export type Profile = {
  id: string;
  displayName: string;
  profilePictureId: string | null;
  profilePictureUrl: string | null;
};

export type CreateProfileInput = {
  displayName: string;
  profilePictureId?: string | null;
};

export type UpdateProfileInput = {
  id: string;
  displayName?: string;
  profilePictureId?: string | null;
};
