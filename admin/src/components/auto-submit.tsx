"use client";

/**
 * Submits the surrounding form as soon as a dropdown changes, so choosing a filter takes one
 * step instead of two. Text and dates are typed in parts, so they wait for Enter or Apply.
 * Without JavaScript nothing happens here and Apply does the work.
 */
export function AutoSubmit({ children }: { children: React.ReactNode }) {
  return (
    <div
      className="contents"
      onChange={(event) => {
        if (event.target instanceof HTMLSelectElement) event.target.form?.requestSubmit();
      }}
    >
      {children}
    </div>
  );
}
