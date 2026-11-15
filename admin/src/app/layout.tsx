import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { Geist, Geist_Mono } from "next/font/google";
import { ClipboardList } from "lucide-react";
import { SessionBar } from "@/components/session-bar";
import "./globals.css";

const geistSans = Geist({ variable: "--font-geist-sans", subsets: ["latin"] });
const geistMono = Geist_Mono({ variable: "--font-geist-mono", subsets: ["latin"] });

export const metadata: Metadata = {
  title: { default: "Field Incident Reporter · Operations", template: "%s · Field Incident Reporter" },
  description: "Triage, filter and resolve incidents reported from the field.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en" className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}>
      <body className="flex min-h-full flex-col">
        <a
          href="#main"
          className="sr-only focus:not-sr-only focus:absolute focus:top-2 focus:left-2 focus:z-20 focus:rounded-lg focus:bg-primary focus:px-3 focus:py-2 focus:text-sm focus:text-primary-foreground"
        >
          Skip to content
        </a>
        <header className="sticky top-0 z-10 border-b bg-card/85 backdrop-blur-md">
          <div className="mx-auto flex w-full max-w-7xl items-center justify-between gap-4 px-4 py-3 sm:px-6">
            {/* The dashboard has one section, so the product name is the way home; no sidebar. */}
            <Link href="/incidents" className="flex items-center gap-2 rounded-md font-semibold tracking-tight outline-none focus-visible:ring-3 focus-visible:ring-ring/50">
              <span className="flex size-7 items-center justify-center rounded-lg bg-primary text-primary-foreground">
                <ClipboardList aria-hidden="true" className="size-4" />
              </span>
              Field Incident Reporter
            </Link>
            {/* Reads the session cookie, so it streams in behind a boundary. */}
            <Suspense fallback={null}>
              <SessionBar />
            </Suspense>
          </div>
        </header>
        <div className="mx-auto w-full max-w-7xl flex-1 px-4 py-6 sm:px-6">{children}</div>
      </body>
    </html>
  );
}
