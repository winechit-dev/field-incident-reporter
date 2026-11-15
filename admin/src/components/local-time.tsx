"use client";

import { useSyncExternalStore } from "react";

const utc = new Intl.DateTimeFormat("en-GB", { dateStyle: "medium", timeStyle: "short", timeZone: "UTC" });
const local = new Intl.DateTimeFormat("en-GB", { dateStyle: "medium", timeStyle: "short" });
const subscribe = () => () => {}; // the time zone never changes while the page is open

/**
 * The API sends UTC. The server cannot know the viewer's zone, so it renders UTC and the
 * browser switches to local time right after hydrating, without a hydration mismatch.
 */
export function LocalTime({ iso }: { iso: string }) {
  const text = useSyncExternalStore(
    subscribe,
    () => local.format(new Date(iso)),
    () => `${utc.format(new Date(iso))} UTC`,
  );
  return (
    <time dateTime={iso} title={`${iso} (UTC)`}>
      {text}
    </time>
  );
}
