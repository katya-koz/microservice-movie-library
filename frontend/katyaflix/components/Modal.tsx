"use client";

import { useRouter } from "next/navigation";

export default function Modal({ children }: { children: React.ReactNode }) {
  const router = useRouter();

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center overflow-hidden bg-black/20 backdrop-blur-md"
      onClick={() => router.back()}
    >
      <div
        className="h-8/10 w-6/10 overflow-auto rounded-2xl shadow-2xl "
        onClick={(e) => e.stopPropagation()}
      >
        {/* <button className="bg-red-100 z-100 fixed">x</button> */}
        {children}
      </div>
    </div>
  );
}
