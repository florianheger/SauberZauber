import { relativeTime } from './relative-time';

describe('relativeTime', () => {
  const now = new Date('2026-10-10T12:00:00Z');
  const ago = (ms: number) => new Date(now.getTime() - ms);
  const minute = 60_000;

  it('formats the design boundaries', () => {
    expect(relativeTime(ago(20_000), now)).toBe('gerade eben');
    expect(relativeTime(ago(5 * minute), now)).toBe('vor 5 Min.');
    expect(relativeTime(ago(59 * minute), now)).toBe('vor 59 Min.');
    expect(relativeTime(ago(3 * 60 * minute), now)).toBe('vor 3 Std.');
    expect(relativeTime(ago(26 * 60 * minute), now)).toBe('gestern');
    expect(relativeTime(ago(5 * 24 * 60 * minute), now)).toBe('vor 5 Tagen');
  });
});
