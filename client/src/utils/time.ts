/**
 * Parsing for the timestamps the API returns.
 *
 * The wire format is a Java LocalDateTime with no zone: "2026-08-16T14:03:11.482". The server pins
 * its JVM to UTC (see Application.useUtc) so that string is always UTC -- but `new Date(...)` reads
 * an offset-less string as *local* time, which quietly shifts every timestamp by the viewer's
 * offset. In IST that made a post created seconds ago look 5.5 hours old, so anything comparing a
 * timestamp against "now" was wrong before it started.
 *
 * Everything that renders or compares a server timestamp goes through here, so that assumption
 * lives in one place and moves in one edit if the API ever sends offsets itself.
 */

/** Null rather than an Invalid Date, so callers have to deal with the missing case. */
export function parseServerTime(value: string | null | undefined): Date | null {
  if (!value) return null;

  // Tolerate a zone if the server ever starts sending one; only assume UTC when none is present.
  const hasZone = /(?:Z|[+-]\d{2}:?\d{2})$/.test(value);
  const date = new Date(hasZone ? value : `${value}Z`);
  return Number.isNaN(date.getTime()) ? null : date;
}

/** Absolute date and time in the viewer's own zone. */
export function formatDateTime(value: string | null | undefined): string {
  return parseServerTime(value)?.toLocaleString() ?? '';
}

/** Clock time only -- for chat bubbles, where the date is implied by position in the thread. */
export function formatTimeOfDay(value: string | null | undefined): string {
  return (
    parseServerTime(value)?.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) ?? ''
  );
}

/**
 * Whether a timestamp falls inside the last `hours`. A missing timestamp is not recent: accounts
 * created before the column existed send null, and guessing would mark all of them new at once.
 *
 * Clock skew is handled by treating the future as recent -- a timestamp a few seconds ahead of the
 * viewer's clock is a freshly created thing, not an old one.
 */
export function isWithinHours(value: string | null | undefined, hours: number): boolean {
  const date = parseServerTime(value);
  if (!date) return false;
  return Date.now() - date.getTime() < hours * 60 * 60 * 1000;
}
