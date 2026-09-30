import type { Metadata } from "next";
import localFont from "next/font/local";
import Navbar from "@/components/Navbar";
import Providers from "@/providers";
import "./globals.css";

export const metadata: Metadata = {
  title: "Katyaflix",
  description: "Movie and TV Library",
};

const notoSans = localFont({
  src: "../fonts/NotoSans-VariableFont_wdth,wght.ttf",
  variable: "--font-noto-sans",
  weight: "100 900",
  style: "normal",
  display: "swap",
});

export default function RootLayout({
  children,
  modal,
}: Readonly<{
  children: React.ReactNode;
  modal: React.ReactNode;
}>) {
  return (
    <html lang="en" className="bg-slate-950">
      <body
        className={`${notoSans.variable} flex h-screen flex-col bg-slate-950 text-white`}
      >
        <Providers>
          <Navbar />

          <main className="relative flex flex-1 flex-col h-full items-center justify-start bg-slate-950 pt-6 align-top">
            {children}
            {modal}
          </main>
        </Providers>
      </body>
    </html>
  );
}
