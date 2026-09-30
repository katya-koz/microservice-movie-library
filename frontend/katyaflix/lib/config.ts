export const MEDIA_URL_ROOT = "/media";
export const API_URL_ROOT = "/api";
export const WS_URL_ROOT =
  typeof window === "undefined"
    ? "" // server-side render: don't open sockets here
    : `${window.location.protocol === "https:" ? "wss" : "ws"}://${window.location.host}/api/ws`;
