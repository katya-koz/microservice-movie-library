import { ReactNode } from "react";

export default function MoviesLayout({
  children,
  modal,
}: {
  children: ReactNode;
  modal: ReactNode;
}) {
  return (
    <div className="box w-8/10 h-screen">
      {children}
      {modal}
    </div>
  );
}
