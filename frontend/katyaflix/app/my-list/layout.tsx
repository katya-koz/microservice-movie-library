import { ReactNode } from "react";

export default function ShowsLayout({
  children,
  modal,
}: {
  children: ReactNode;
  modal: ReactNode;
}) {
  return (
    <div className="box w-7/10 h-screen">
      {children}
      {modal}
    </div>
  );
}
