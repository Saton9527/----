export function resolveProblemUrl(problemCode: string): string | null {
  const normalized = problemCode.trim().toUpperCase();
  const cfMatch = normalized.match(/^CF\s+(\d+)([A-Z]\d*)$/);
  if (cfMatch) {
    return `https://codeforces.com/problemset/problem/${cfMatch[1]}/${cfMatch[2]}`;
  }
  return null;
}
