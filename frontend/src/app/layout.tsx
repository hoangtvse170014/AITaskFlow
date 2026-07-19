import type { Metadata } from "next";
import { Providers } from "@/components/providers/Providers";
import "./globals.css";

export const metadata: Metadata = {
  title: "TaskFlow - Modern Task Management",
  description: "A modern, lightweight task management application inspired by Jira, Linear, and Notion",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className="antialiased">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
