export function resolveProblemUrl(problemCode: string): string | null {
  const trimmed = problemCode.trim();
  const normalized = trimmed.toUpperCase();
  const cfMatch = normalized.match(/^CF\s+(\d+)([A-Z]\d*)$/);
  if (cfMatch) {
    return `https://codeforces.com/problemset/problem/${cfMatch[1]}/${cfMatch[2]}`;
  }
  const atcMatch = trimmed.match(/^ATC\s+([A-Za-z0-9_-]+)$/);
  if (atcMatch) {
    const taskId = atcMatch[1].toLowerCase();
    const splitIndex = taskId.lastIndexOf('_');
    if (splitIndex > 0) {
      const contestId = taskId.slice(0, splitIndex);
      return `https://atcoder.jp/contests/${contestId}/tasks/${taskId}`;
    }
  }
  return null;
}
