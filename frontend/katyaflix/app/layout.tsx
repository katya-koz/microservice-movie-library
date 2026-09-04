import type { Metadata } from "next";
import Navbar from "@/components/Navbar";
import Providers from "@/providers";
import "./globals.css";

export const metadata: Metadata = {
  title: "Katyaflix",
  description: "Movie and TV Library",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className="flex h-screen flex-col bg-slate-950 text-white">
        <Providers>
          <Navbar />

          <main className="flex min-h-0 flex-1 flex-col items-center justify-center bg-slate-950">
            {children}
          </main>
        </Providers>
      </body>
    </html>
  );
}
