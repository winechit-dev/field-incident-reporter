import type { Metadata } from "next";
import { Suspense } from "react";
import { Geist, Geist_Mono } from "next/font/google";
import { SessionBar } from "@/components/session-bar";
import "./globals.css";

const geistSans = Geist({ variable: "--font-geist-sans", subsets: ["latin"] });
const geistMono = Geist_Mono({ variable: "--font-geist-mono", subsets: ["latin"] });

export const metadata: Metadata = {
  title: "Field Incident Reporter · Operations",
  description: "Triage, filter and resolve incidents reported from the field.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en" className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}>
      <body className="flex min-h-full flex-col">
        <header className="flex items-center justify-between border-b px-6 py-3">
          <span className="font-semibold">Field Incident Reporter</span>
          {/* Reads the session cookie, so it streams in behind a boundary. */}
          <Suspense fallback={null}>
            <SessionBar />
          </Suspense>
        </header>
        <div className="mx-auto w-full max-w-7xl flex-1 px-6 py-6">{children}</div>
      </body>
    </html>
  );
}
